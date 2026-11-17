package com.magusview.android;

import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CameraService extends android.app.Service {
    private final Handler main = new Handler(Looper.getMainLooper());
    // Process-wide because the native SDK handle survives Activity recreation.
    private static final ScheduledThreadPoolExecutor camera = new ScheduledThreadPoolExecutor(1);
    private static final ExecutorService media = Executors.newSingleThreadExecutor();
    private final AtomicBoolean displayPending = new AtomicBoolean();
    private final StringBuilder diagnostics = new StringBuilder();
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private WifiManager.MulticastLock multicast;
    private Bitmap latestBitmap;
    final Annotations annotations = new Annotations();
    private volatile java.util.List<Annotations.Shape> photoAnnotations = java.util.List.of();
    private boolean photoSaving, recordCommandPending;
    private volatile boolean photoRequested;
    private volatile boolean recordingActive, recordingPaused, videoSaving;
    private volatile long recordedMillis;
    private final java.util.ArrayList<File> recordingSegments = new java.util.ArrayList<>();
    private volatile long recordingStarted;
    private File recordingFile; // Camera executor only.
    private volatile boolean reconnecting;
    private volatile long freeStorage = -1;
    private volatile boolean storageLow;
    private volatile String recordingNotice = "";
    private int interruptions;
    private File watchedRecording;
    private long watchedBytes, lastRecordingGrowth;
    private final Runnable storageMonitor = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            try { checkRecordingHealth(); }
            catch (RuntimeException e) { CrashDiagnostics.record("Storage check failed: " + e); }
            if (!destroyed) camera.schedule(this, 5, TimeUnit.SECONDS);
        }
    };

    String storageStatus() {
        String free = freeStorage < 0 ? "Checking storage…" : android.text.format.Formatter.formatFileSize(this, freeStorage) + " free";
        return free + (storageLow ? " · Low storage" : "")
            + (recordingNotice.isEmpty() ? "" : "\n" + recordingNotice);
    }
    boolean storageWarning() { return storageLow || !recordingNotice.isEmpty(); }
    private long availableStorage() {
        // Private MP4s and primary shared Movies normally share a volume. Check both.
        long privateFree = new android.os.StatFs(getFilesDir().getAbsolutePath()).getAvailableBytes();
        long sharedFree = new android.os.StatFs(android.os.Environment.getExternalStorageDirectory().getAbsolutePath()).getAvailableBytes();
        return Math.min(privateFree, sharedFree);
    }
    private void recordingNotice(String message, boolean notifyUser) {
        recordingNotice = message;
        CrashDiagnostics.record(message);
        main.post(() -> {
            updateCaptureControls();
            if (notifyUser && !destroyed) {
                android.app.PendingIntent open = android.app.PendingIntent.getActivity(this, 0,
                    new android.content.Intent(this, MainActivity.class), android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                getSystemService(android.app.NotificationManager.class).notify(2,
                    new android.app.Notification.Builder(this, "recording")
                        .setSmallIcon(R.drawable.pip_record).setContentTitle("MagusView recording notice")
                        .setContentText(message).setStyle(new android.app.Notification.BigTextStyle().bigText(message))
                        .setContentIntent(open).setAutoCancel(true).build());
            }
        });
    }
    private void checkRecordingHealth() {
        freeStorage = availableStorage();
        long bytes = 0;
        for (File part : recordingSegments) bytes += part.length();
        if (recordingFile != null) bytes += recordingFile.length();
        int parts = recordingSegments.size() + (recordingFile == null ? 0 : 1);
        storageLow = RecordingStorage.low(freeStorage, bytes, parts);
        if (recordingActive && RecordingStorage.mustStop(freeStorage, bytes, parts)) {
            recordingNotice("Recording stopped: low storage. Saving captured footage; free space before recording again.", true);
            finishRecording();
        } else if (recordingActive && recordingFile != null) {
            long now = SystemClock.elapsedRealtime(), size = recordingFile.length();
            if (watchedRecording != recordingFile || size != watchedBytes) {
                watchedRecording = recordingFile; watchedBytes = size; lastRecordingGrowth = now;
            } else if (now - lastRecordingGrowth > 30000) {
                recordingNotice("Recording stopped: video file stopped growing for 30 seconds. Saving available footage.", true);
                finishRecording();
            }
        } else watchedRecording = null;
        main.post(this::updateCaptureControls);
    }

    private volatile String cameraId; // Camera executor only.
    private boolean recorderNeedsReset;
    private boolean recordingDamaged; // Camera executor only; never publish a partial session as complete.
    private volatile int generation;
    private volatile boolean visible;
    private volatile Network selectedNetwork;
    // These fields belong exclusively to the camera executor.
    private ByteBuffer pixels;
    private int width, height;
    private long lastFrame, lastAcquireLog, frames;
    private boolean streamEndPending;
    private long lastEofLog;
    private final android.content.BroadcastReceiver powerReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            logPower(intent.getAction());
        }
    };
    private void logPower(String reason) {
        android.os.PowerManager power = getSystemService(android.os.PowerManager.class);
        String state = reason + "; screenOn=" + power.isInteractive() + "; idle=" + power.isDeviceIdleMode()
            + "; batterySaver=" + power.isPowerSaveMode() + "; batteryExempt=" + power.isIgnoringBatteryOptimizations(getPackageName())
            + "; wakeHeld=" + (wakeLock != null && wakeLock.isHeld()) + "; recording=" + recordingActive;
        if (android.os.Build.VERSION.SDK_INT >= 33) state += "; lowPowerStandby=" + power.isLowPowerStandbyEnabled();
        CrashDiagnostics.record(state);
    }

    interface Listener {
        void changed();
        void select(String[] devices, int token);
        void message(String message);
    }
    static final String RECORD = "com.magusview.android.RECORD";
    static final String PHOTO = "com.magusview.android.PHOTO";
    static final String PAUSE = "com.magusview.android.PAUSE";
    static final String STOP = "com.magusview.android.STOP";
    private Listener listener;
    private String statusText = "Ready to use your current Wi-Fi.";
    private boolean connecting, foreground, startRequested, discoveryAttempted;
    private volatile boolean destroyed;
    private android.os.PowerManager.WakeLock wakeLock;
    private final android.os.Binder binder = new LocalBinder();
    final class LocalBinder extends android.os.Binder { CameraService service() { return CameraService.this; } }
    @Override public android.os.IBinder onBind(android.content.Intent intent) { return binder; }
    @Override public void onCreate() {
        super.onCreate();
        connectivity = getSystemService(ConnectivityManager.class);
        getSystemService(android.app.NotificationManager.class).createNotificationChannel(
            new android.app.NotificationChannel("recording", "Camera recording", android.app.NotificationManager.IMPORTANCE_LOW));
        wakeLock = getSystemService(android.os.PowerManager.class).newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK, "MagusView:recording");
        wakeLock.setReferenceCounted(false);
        android.content.IntentFilter powerEvents = new android.content.IntentFilter();
        powerEvents.addAction(android.content.Intent.ACTION_SCREEN_OFF);
        powerEvents.addAction(android.content.Intent.ACTION_SCREEN_ON);
        powerEvents.addAction(android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        powerEvents.addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(powerReceiver, powerEvents, RECEIVER_NOT_EXPORTED);
        else registerReceiver(powerReceiver, powerEvents);
        logPower("Camera service created");
        camera.execute(storageMonitor);
        log("MagusView 0.44 camera interface; Android " + android.os.Build.VERSION.RELEASE);
    }
    @Override public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        if (intent != null && RECORD.equals(intent.getAction())) {
            startRequested = false;
            try {
                showRecordingNotification();
                if (!wakeLock.isHeld()) wakeLock.acquire();
                logPower("Recording requested");
                if (!recordingActive && !recordCommandPending && !videoSaving && latestBitmap != null) toggleRecording();
                else settleForeground();
            } catch (RuntimeException e) {
                mediaMessage("Cannot start background recording: " + e.getMessage());
                settleForeground();
            }
        } else if (intent != null && STOP.equals(intent.getAction())) stopRecording();
        else if (intent != null && PHOTO.equals(intent.getAction())) { savePhoto(); settleForeground(); }
        else if (intent != null && PAUSE.equals(intent.getAction())) { pauseOrContinue(); settleForeground(); }
        else settleForeground();
        return START_NOT_STICKY;
    }
    private void showRecordingNotification() {
        android.app.PendingIntent open = android.app.PendingIntent.getActivity(this, 0,
            new android.content.Intent(this, MainActivity.class), android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        String title = videoSaving ? "MagusView is saving video" : recordingActive
            ? (reconnecting ? "MagusView reconnecting" : recordingPaused ? "MagusView recording paused" : "MagusView is recording") : "MagusView is preparing recording";
        long elapsed = recordedMillis + (recordingActive && !recordingPaused && recordingStarted > 0
            ? SystemClock.elapsedRealtime() - recordingStarted : 0);
        if (recordingPaused || reconnecting || videoSaving) {
            long seconds = Math.max(0, elapsed / 1000);
            title += String.format(Locale.ROOT, " · %02d:%02d", seconds / 60, seconds % 60);
        }
        android.app.Notification.Builder builder = new android.app.Notification.Builder(this, "recording")
            .setSmallIcon(R.drawable.pip_photo).setContentTitle(title)
            .setContentText(reconnecting ? "Camera offline; recording will resume automatically." : videoSaving ? "Finishing your video…" : photoSaving ? "Saving photo…"
                : recordCommandPending ? "Applying recording command…" : "Expand for photo and recording controls.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true);
        // Android advances the chronometer itself, even with the screen off. Use
        // accumulated active time so pauses are excluded without per-second updates.
        if (recordingActive && !recordingPaused && recordingStarted > 0) {
            builder.setWhen(System.currentTimeMillis() - Math.max(0, elapsed))
                .setShowWhen(true).setUsesChronometer(true);
        } else builder.setShowWhen(false).setUsesChronometer(false);
        if (canPhoto()) builder.addAction(notificationAction(PHOTO, R.drawable.pip_photo, "Take photo"));
        if (canPause()) builder.addAction(notificationAction(PAUSE,
            recordingPaused ? R.drawable.pip_play : R.drawable.pip_pause, recordingPaused ? "Continue" : "Pause"));
        if (recordingActive && canRecord()) builder.addAction(notificationAction(STOP, R.drawable.pip_stop, "Stop and save"));
        startForeground(1, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        foreground = true;
        notificationState = notificationState();
    }
    private String notificationState = "";
    private String notificationState() {
        return recordingActive + ":" + recordingPaused + ":" + videoSaving + ":" + photoSaving
            + ":" + reconnecting + ":" + recordCommandPending + ":" + startRequested + ":" + (latestBitmap != null);
    }
    private android.app.Notification.Action notificationAction(String action, int icon, String label) {
        android.app.PendingIntent intent = android.app.PendingIntent.getService(this, action.hashCode(),
            new android.content.Intent(this, CameraService.class).setAction(action),
            android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        return new android.app.Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, icon), label, intent).build();
    }
    // Main thread: keep process protection until the MP4 has been published to MediaStore.
    private void settleForeground() {
        if (destroyed) return;
        if (startRequested || recordingActive || recordCommandPending || videoSaving) return;
        if (wakeLock.isHeld()) wakeLock.release();
        if (foreground) { stopForeground(STOP_FOREGROUND_REMOVE); foreground = false; }
        if (!visible && connecting) stopSession("Recording finished. Tap Find camera to reconnect.");
        stopSelf();
    }
    void attach(Listener observer) {
        listener = observer;
        visible = true;
        updateCaptureControls();
        // Search once for a fresh service. Reattaching to an existing recording or
        // returning after an explicit disconnect must not start another search.
        if (!discoveryAttempted) begin();
    }
    void detach(Listener observer) {
        if (listener != observer) return;
        listener = null; visible = false;
        if (!startRequested && !recordingActive && !recordCommandPending && !videoSaving) {
            stopSession("Preview stopped. Tap Find camera to reconnect.");
            stopSelf();
        }
    }
    Bitmap bitmap() { return latestBitmap; }
    String status() { return statusText; }
    boolean canConnect() { return !connecting && !startRequested && !recordCommandPending && !videoSaving; }
    boolean canPhoto() { return latestBitmap != null && !photoSaving; }
    boolean canRecord() { return (latestBitmap != null || recordingActive) && !startRequested && !recordCommandPending && !videoSaving; }
    boolean paused() { return recordingPaused; }
    String recordingTimeLabel() {
        long seconds = Math.max(0, (recordedMillis + (recordingActive && !recordingPaused && recordingStarted > 0
            ? SystemClock.elapsedRealtime() - recordingStarted : 0)) / 1000);
        return (reconnecting ? "RECONNECTING  " : recordingPaused ? "PAUSED  " : "● REC  ")
            + String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
    private boolean focusCommandPending;
    void focusAt(float x, float y) {
        if (focusCommandPending || latestBitmap == null) return;
        final int token = generation;
        focusCommandPending = true;
        camera.execute(() -> {
            try {
                if (generation != token) return;
                int[] area = CameraSdk.control(27);
                int zone = FocusTarget.zone(x, y, area[2]);
                if (area[0] != 0 || area[5] < 0 || zone < 0) {
                    mediaMessage("This camera does not support focus-area selection. Hold the preview for manual focus.");
                    return;
                }
                int result = CameraSdk.setControl(26, 0);
                if (result >= 0) result = CameraSdk.setControl(27, zone);
                if (result >= 0) result = CameraSdk.setControl(26, 2);
                mediaMessage(result < 0 ? "Focus request failed: " + hex(result) : "Focusing on the selected area");
            } finally { main.post(() -> focusCommandPending = false); }
        });
    }
    void readControls(java.util.function.Consumer<java.util.Map<Integer, int[]>> callback) {
        final int token = generation;
        camera.execute(() -> {
            if (generation != token) return;
            java.util.Map<Integer, int[]> values = new java.util.LinkedHashMap<>();
            for (int id : CameraControls.PARAMETERS) values.put(id, CameraSdk.control(id));
            post(token, () -> callback.accept(values));
        });
    }
    void changeControl(int id, int value, Runnable done) {
        final int token = generation;
        camera.execute(() -> {
            if (generation != token) return;
            int result = 0;
            // Manual values must not be immediately overwritten by automatic modes.
            if (id == 1 || id == 2) result = CameraSdk.setControl(11, 0);
            else if (id == 4 || id == 5 || (id >= 19 && id <= 21)) result = CameraSdk.setControl(12, 0);
            else if (id == 25) result = CameraSdk.setControl(26, 0);
            if (result >= 0) result = CameraSdk.setControl(id, value);
            mediaMessage(result < 0 ? "Camera setting failed: " + hex(result)
                : id == 26 && value == 2 ? "Autofocus requested" : "Camera setting applied");
            post(token, done);
        });
    }
    void restoreImageDefaults(Runnable done) {
        final int token = generation;
        camera.execute(() -> {
            if (generation != token) return;
            int failures = 0;
            // Restore manual image values first, then automatic modes. Never reset
            // network, encoder, focus position, or focus calibration.
            for (int id : new int[] {11, 12}) {
                int[] c = CameraSdk.control(id);
                if (c[0] == 0 && c[5] >= 0 && CameraSdk.setControl(id, 0) < 0) ++failures;
            }
            for (int id : new int[] {1,2,3,4,5,6,7,8,9,10,18,19,20,21,11,12}) {
                int[] c = CameraSdk.control(id);
                if (c[0] == 0 && c[5] >= 0 && CameraSdk.setControl(id, c[3]) < 0) ++failures;
            }
            mediaMessage(failures == 0 ? "Image defaults restored" : "Some image settings could not be restored: " + failures);
            post(token, done);
        });
    }
    String captureStatus() {
        if (videoSaving) return "Saving video…";
        if (reconnecting) return "Reconnecting to camera…";
        if (startRequested || recordCommandPending) return "Preparing…";
        if (recordingActive) {
            long seconds = (recordedMillis + (recordingPaused || recordingStarted == 0 ? 0 : SystemClock.elapsedRealtime() - recordingStarted)) / 1000;
            return (reconnecting ? "RECONNECTING  " : recordingPaused ? "PAUSED  " : "● REC  ") + String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        }
        return latestBitmap != null ? (photoSaving ? "Saving photo…" : "Live preview")
            : connecting || !discoveryAttempted ? "Discovering cameras…" : "Camera offline";
    }
    boolean canPause() { return recordingActive && !reconnecting && !recordCommandPending && !videoSaving; }
    boolean recording() { return recordingActive; }
    String recordLabel() { return videoSaving ? "Saving…" : recordingActive ? "Stop" : "Record"; }
    void requestRecording() {
        if (!canRecord() || recordingActive) return;
        // Reserve the session before Android delivers onStartCommand: pressing Home
        // immediately after Record must not let Activity.onStop close the camera.
        startRequested = true;
        updateCaptureControls();
        try {
            startForegroundService(new android.content.Intent(this, CameraService.class).setAction(RECORD));
        } catch (RuntimeException e) {
            startRequested = false;
            mediaMessage("Cannot start recording: " + e.getMessage());
            settleForeground();
        }
    }
    void stopRecording() {
        if (videoSaving) return;
        recordCommandPending = true;
        updateCaptureControls();
        camera.execute(() -> {
            try { finishRecording(); }
            finally { main.post(() -> { recordCommandPending = false; updateCaptureControls(); settleForeground(); }); }
        });
    }
    @Override public void onDestroy() {
        logPower("Camera service destroyed");
        unregisterReceiver(powerReceiver);
        listener = null; visible = false;
        destroyed = true;
        stopSession(null);
        releaseNetwork();
        if (wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    private void log(String message) {
        // Only called on the main thread.
        if (diagnostics.length() > 16000) diagnostics.delete(0, 8000);
        diagnostics.append(message).append('\n');
        CrashDiagnostics.record(message);
        statusText = message; updateCaptureControls();
    }

    private void updateCaptureControls() {
        if (listener != null) listener.changed();
        if (foreground && !destroyed && !notificationState.equals(notificationState())) showRecordingNotification();
    }
    private void mediaMessage(String message) {
        CrashDiagnostics.record(message);
        main.post(() -> {
            updateCaptureControls();
            if (listener != null) listener.message(message);
        });
    }

    void savePhoto() {
        if (latestBitmap == null || photoSaving) return;
        photoAnnotations = annotations.snapshot();
        photoSaving = true;
        photoRequested = true;
        updateCaptureControls();
    }

    // Camera executor, while the acquired SDK frame is still held.
    private void capturePhoto() {
        photoRequested = false;
        final Bitmap snapshot;
        try {
            int[] size = CameraSdk.size();
            ByteBuffer full = ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(size[0], size[1]), 4));
            int result = CameraSdk.pull(full);
            if (result < 0) throw new IOException("Photo frame copy: " + hex(result));
            snapshot = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
            full.rewind();
            snapshot.copyPixelsFromBuffer(full);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            main.post(() -> { photoSaving = false; mediaMessage("Photo capture failed: " + e.getMessage()); });
            return;
        }
        media.execute(() -> {
            String message;
            try {
                AnnotationView.drawShapes(new android.graphics.Canvas(snapshot),
                    new android.graphics.RectF(0, 0, snapshot.getWidth(), snapshot.getHeight()),
                    photoAnnotations, new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG));
                MediaFiles.photo(getApplicationContext(), snapshot); message = "Photo saved to Pictures/MagusView";
            }
            catch (IOException | RuntimeException e) { message = "Photo save failed: " + e.getMessage(); }
            snapshot.recycle();
            final String result = message;
            main.post(() -> { photoSaving = false; mediaMessage(result); });
        });
    }

    private void toggleRecording() {
        if (latestBitmap == null || recordCommandPending || videoSaving) return;
        final int token = generation;
        recordCommandPending = true;
        updateCaptureControls();
        camera.execute(() -> {
            try {
                if (generation != token) return;
                if (recordingActive) { finishRecording(); return; }
                recordingSegments.clear();
                freeStorage = availableStorage();
                if (RecordingStorage.mustStop(freeStorage, 0, 1)) {
                    recordingNotice("Not enough free storage to start recording. Free at least 512 MiB and try again.", true);
                    return;
                }
                recordingNotice = "";
                interruptions = 0;
                main.post(() -> getSystemService(android.app.NotificationManager.class).cancel(2));
                recordingDamaged = false;
                recordedMillis = 0;
                recordingPaused = false;
                startSegment();
                recordingStarted = SystemClock.elapsedRealtime();
                recordingActive = true;
                mediaMessage("Video recording started");
            } catch (IOException | RuntimeException e) { mediaMessage("Recording failed: " + e.getMessage()); }
            finally {
                main.post(() -> { recordCommandPending = false; updateCaptureControls(); settleForeground(); });
            }
        });
    }

    private void startSegment() throws IOException {
        if (recorderNeedsReset) resetRecorderSession();
        CrashDiagnostics.record("Starting recording segment; last decoded frame "
            + (SystemClock.elapsedRealtime() - lastFrame) + " ms ago; EOF pending=" + streamEndPending);
        File directory = new File(getFilesDir(), "recordings");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create recording folder");
        File file = File.createTempFile("MagusView_", ".mp4", directory);
        int result = CameraSdk.record(file.getAbsolutePath());
        if (result < 0) { file.delete(); throw new IOException("SDK recording start: " + hex(result)); }
        recordingFile = file;
    }

    private void endSegment() throws IOException {
        if (recordingFile == null) return;
        int result = CameraSdk.record(null);
        recorderNeedsReset = true;
        File file = recordingFile;
        recordingFile = null;
        recordingSegments.add(file);
        if (result < 0) {
            recordingDamaged = true;
            throw new IOException("Video finalization failed " + hex(result) + "; temporary file retained");
        }
        CrashDiagnostics.record("Recording segment finalized: " + file.length() + " bytes");
    }

    // NAM can fail to record again on a handle after Record(null). A fresh handle
    // produced three playable consecutive clips in the Windows SDK regression probe.
    // This method and poll() share one executor, so no frame can be held concurrently.
    private void resetRecorderSession() throws IOException {
        final int token = generation;
        CrashDiagnostics.record("Refreshing SDK session before next recording segment");
        CameraSdk.close();
        int result = cameraId == null ? -1 : CameraSdk.open(cameraId);
        if (result >= 0) result = CameraSdk.start();
        if (result < 0) {
            fail(token, "Camera reconnect failed: " + hex(result));
            throw new IOException("Cannot refresh camera recording session");
        }
        long deadline = SystemClock.elapsedRealtime() + 20000;
        while (generation == token && SystemClock.elapsedRealtime() < deadline) {
            int acquired = CameraSdk.acquireFrame();
            if (acquired >= 0) {
                CameraSdk.releaseFrame();
                lastFrame = SystemClock.elapsedRealtime();
                streamEndPending = false;
                recorderNeedsReset = false;
                CrashDiagnostics.record("Fresh camera session received a decoded frame; recorder ready");
                return;
            }
            try { Thread.sleep(5); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        if (generation == token) fail(token, "No frames after refreshing the camera recording session");
        throw new IOException("Camera recording session refresh cancelled or timed out");
    }

    void pauseOrContinue() {
        if (!canPause()) return;
        recordCommandPending = true;
        updateCaptureControls();
        camera.execute(() -> {
            try {
                if (!recordingActive) return;
                if (recordingPaused) {
                    startSegment();
                    recordingStarted = SystemClock.elapsedRealtime();
                    recordingPaused = false;
                    mediaMessage("Recording continued");
                } else {
                    recordedMillis += SystemClock.elapsedRealtime() - recordingStarted;
                    recordingPaused = true;
                    endSegment();
                    mediaMessage("Recording paused; live preview continues");
                }
            } catch (IOException | RuntimeException e) { mediaMessage("Recording command failed: " + e.getMessage()); }
            finally { main.post(() -> {
                recordCommandPending = false;
                if (foreground) showRecordingNotification();
                updateCaptureControls();
            }); }
        });
    }

    // Always called on the camera executor, before closing the camera on every exit path.
    private void finishRecording() {
        if (!recordingActive) return;
        if (!recordingPaused && recordingStarted > 0) recordedMillis += SystemClock.elapsedRealtime() - recordingStarted;
        try { endSegment(); }
        catch (IOException e) { mediaMessage(e.getMessage()); }
        recordingActive = false;
        recordingPaused = false;
        java.util.ArrayList<File> segments = new java.util.ArrayList<>(recordingSegments);
        final boolean damaged = recordingDamaged;
        final int gaps = interruptions;
        recordingSegments.clear();
        videoSaving = true;
        mediaMessage("Saving video…");
        media.execute(() -> {
            File joined = null;
            try {
                if (damaged) throw new IOException("A recording segment could not be finalized");
                if (segments.size() > 1) for (File segment : segments) MediaFiles.awaitVideoReady(segment);
                joined = VideoSegments.join(segments);
                MediaFiles.video(getApplicationContext(), joined);
                for (File file : segments) if (!file.delete()) CrashDiagnostics.record("Published segment retained: " + file.getName());
                if (!segments.contains(joined)) joined.delete();
                String savedMessage = "Video saved to Movies/MagusView" + (gaps > 0
                    ? "; " + gaps + " connection interruption(s), offline time omitted" : "");
                if (gaps > 0 && (recordingNotice.isEmpty() || recordingNotice.startsWith("Connection restored")
                    || recordingNotice.startsWith("Recording interrupted"))) recordingNotice(savedMessage, true);
                mediaMessage(savedMessage);
            } catch (IOException | RuntimeException e) {
                // Derived join output is expendable; keep original segments for fallback.
                if (joined != null && !segments.contains(joined)) joined.delete();
                CrashDiagnostics.record("Combined video save failed: " + e.getMessage());
                int saved = 0, failed = 0;
                for (File part : segments) {
                    try {
                        MediaFiles.video(getApplicationContext(), part);
                        ++saved;
                        part.delete();
                    } catch (IOException | RuntimeException partError) {
                        ++failed;
                        CrashDiagnostics.record("Retained segment " + part.getName() + ": " + partError.getMessage());
                    }
                }
                String result = "Saved " + saved + " video part(s) to Movies/MagusView; " + failed
                    + " part(s) could not be saved and were retained privately. See diagnostics.";
                recordingNotice(result, true);
                mediaMessage(result);
            } finally {
                videoSaving = false;
                main.post(() -> { updateCaptureControls(); settleForeground(); });
            }
        });
    }

    private void post(int token, Runnable action) {
        main.post(() -> { if (generation == token) action.run(); });
    }

    void begin() {
        if (!canConnect()) return;
        discoveryAttempted = true;
        stopSession(null);
        releaseNetwork();
        final int token = ++generation;
        connecting = true; updateCaptureControls();
        log("Waiting for a Wi-Fi connection…");
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                main.post(() -> {
                    if (networkCallback != this) return;
                    if (selectedNetwork != null) return;
                    selectedNetwork = network;
                    if (!connectivity.bindProcessToNetwork(network)) {
                        stopSession("Could not route camera traffic over Wi-Fi.");
                        return;
                    }
                    if (multicast != null && multicast.isHeld()) multicast.release();
                    multicast = getSystemService(WifiManager.class).createMulticastLock("MagusView discovery");
                    multicast.setReferenceCounted(false);
                    multicast.acquire();
                    logNetwork(network);
                    log("Searching current Wi-Fi for cameras…");
                    if (!reconnecting) {
                        final int currentToken = generation;
                        camera.execute(() -> initialize(currentToken));
                    }
                });
            }
            @Override public void onLost(Network network) {
                main.post(() -> {
                    if (networkCallback != this) return;
                    if (network.equals(selectedNetwork)) {
                        selectedNetwork = null;
                        connectivity.bindProcessToNetwork(null);
                        if (!reconnecting) fail(generation, "Wi-Fi connection lost");
                    }
                });
            }
            @Override public void onBlockedStatusChanged(Network network, boolean blocked) {
                main.post(() -> {
                    if (networkCallback != this) return;
                    if (network.equals(selectedNetwork)) logPower("Camera Wi-Fi blocked=" + blocked);
                });
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        try {
            connectivity.registerNetworkCallback(request, networkCallback);
        } catch (RuntimeException e) {
            networkCallback = null;
            stopSession("Network registration failed: " + e.getMessage());
            return;
        }
        main.postDelayed(() -> {
            if (generation == token && selectedNetwork == null) stopSession("No connected Wi-Fi network is available.");
        }, 15000);
    }

    private void logNetwork(Network network) {
        LinkProperties links = connectivity.getLinkProperties(network);
        if (links != null) {
            log("Wi-Fi interface: " + links.getInterfaceName() + "; phone addresses: " + links.getLinkAddresses());
            log("Wi-Fi routes: " + links.getRoutes());
        }
        Network active = connectivity.getActiveNetwork();
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(active);
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            log("A VPN is active; it may restrict local camera traffic.");
        }
    }

    private void initialize(int token) {
        if (generation != token) return;
        try {
            String version = CameraSdk.version();
            CameraSdk.enableWifi();
            post(token, () -> log("Wi-Fi SDK " + version + "; broadcast discovery started; default transport; BGR24 decode"));
            discover(token, SystemClock.elapsedRealtime() + 20000);
        } catch (LinkageError | RuntimeException e) {
            fail(token, "Cannot initialize camera SDK: " + e);
        }
    }

    private void discover(int token, long deadline) {
        if (generation != token) return;
        String[] devices = CameraSdk.discover();
        if (devices.length > 0) {
            post(token, () -> {
                String[] names = new String[devices.length / 2];
                for (int i = 0; i < names.length; i++) names[i] = devices[i * 2];
                log("Discovered " + names.length + " camera(s).");
                if (listener != null) listener.select(devices, token);
            });
        } else if (SystemClock.elapsedRealtime() >= deadline) {
            fail(token, "No Wi-Fi camera found after 20 seconds. Confirm the camera is online on this LAN, then retry. Save the diagnostics file if desktop MAGUSView can still find it.");
        } else {
            camera.schedule(() -> discover(token, deadline), 1, TimeUnit.SECONDS);
        }
    }

    private volatile int previewRequest;
    void cancelCameraPreviews() { ++previewRequest; }
    void previewCameras(String[] devices, int token, java.util.function.BiConsumer<Integer, Bitmap> callback) {
        final int request = ++previewRequest;
        // The SDK exposes one process-wide handle. Probe and close each camera on
        // its existing executor before the selected camera can be opened.
        for (int i = 0; i + 1 < devices.length; i += 2) {
            final int row = i / 2;
            final String id = devices[i + 1];
            camera.execute(() -> {
                if (generation != token || previewRequest != request) return;
                Bitmap thumbnail = null;
                try {
                    int result = CameraSdk.open(id);
                    if (result >= 0) result = CameraSdk.start();
                    long deadline = SystemClock.elapsedRealtime() + 4000;
                    while (result >= 0 && generation == token && previewRequest == request
                            && SystemClock.elapsedRealtime() < deadline) {
                        if (CameraSdk.acquireFrame() >= 0) {
                            try {
                                int[] size = CameraSdk.size();
                                double scale = Math.min(1, Math.min(320.0 / size[0], 180.0 / size[1]));
                                int w = Math.max(1, (int) Math.round(size[0] * scale));
                                int h = Math.max(1, (int) Math.round(size[1] * scale));
                                ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
                                if (CameraSdk.pullPreview(buffer, w, h) >= 0) {
                                    thumbnail = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                    buffer.rewind(); thumbnail.copyPixelsFromBuffer(buffer);
                                }
                            } finally { CameraSdk.releaseFrame(); }
                            break;
                        }
                        try { Thread.sleep(10); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    }
                    CrashDiagnostics.record("Discovery preview " + row + ": " + (thumbnail == null ? "unavailable" : "ready"));
                } catch (RuntimeException e) {
                    CrashDiagnostics.record("Discovery preview failed: " + e);
                } finally { CameraSdk.close(); }
                final Bitmap ready = thumbnail;
                main.post(() -> {
                    if (generation == token && previewRequest == request) callback.accept(row, ready);
                    else if (ready != null) ready.recycle();
                });
            });
        }
    }
    void selectCamera(int token, String id, String name) {
        cancelCameraPreviews();
        if (generation == token) camera.execute(() -> open(token, id, name));
    }
    void cancelSelection(int token) { if (generation == token) stopSession("Camera selection cancelled."); }

    private void open(int token, String id, String name) {
        if (generation != token) return;
        post(token, () -> log("Opening camera " + name + "…"));
        CrashDiagnostics.record("Calling native open");
        int result = CameraSdk.open(id);
        CrashDiagnostics.record("Native open returned " + hex(result));
        if (result < 0) { fail(token, "Open failed: " + hex(result)); return; }
        cameraId = id;
        recorderNeedsReset = false;
        pixels = null;
        width = height = 0;
        CrashDiagnostics.record("Calling native stream start");
        result = CameraSdk.start();
        CrashDiagnostics.record("Native stream start returned " + hex(result));
        if (result < 0) { fail(token, "Stream start failed: " + hex(result)); return; }
        frames = 0;
        lastAcquireLog = 0;
        streamEndPending = false;
        lastFrame = SystemClock.elapsedRealtime();
        post(token, () -> log(name + " • stream started; waiting for first decoded frame…"));
        poll(token, name);
    }

    private void poll(int token, String description) {
        if (generation != token) return;
        int event = CameraSdk.event();
        if (event == 0x80) {
            fail(token, "Camera stopped; SDK event " + hex(event)); return;
        }
        if (event == 0x81) {
            streamEndPending = true;
            if (SystemClock.elapsedRealtime() - lastEofLog >= 1000) {
                lastEofLog = SystemClock.elapsedRealtime();
                CrashDiagnostics.record("SDK EOF; last decoded frame " + (lastEofLog - lastFrame) + " ms ago");
                logPower("Stream interrupted");
            }
            // EOF alone does not establish that this live preview has stopped. Keep
            // draining and use the existing frame-loss timeout to confirm failure.
        }
        {
            boolean traceAcquire = frames < 3 && SystemClock.elapsedRealtime() - lastAcquireLog >= 1000;
            if (traceAcquire) {
                lastAcquireLog = SystemClock.elapsedRealtime();
                CrashDiagnostics.record("Before acquireFrame; displayed=" + frames);
            }
            int result = CameraSdk.acquireFrame();
            if (traceAcquire) CrashDiagnostics.record("After acquireFrame: " + hex(result));
            if (result >= 0) {
                if (reconnecting) {
                    CameraSdk.releaseFrame();
                    lastFrame = SystemClock.elapsedRealtime();
                    try {
                        if (recordingActive && !recordingPaused) {
                            startSegment();
                            recordingStarted = SystemClock.elapsedRealtime();
                        }
                        reconnecting = false;
                        if (recordingActive) recordingNotice("Connection restored; recording "
                            + (recordingPaused ? "remains paused" : "resumed")
                            + ". " + interruptions + " interruption(s); offline time omitted.", false);
                        post(token, () -> log("Camera reconnected" + (recordingActive
                            ? "; recording " + (recordingPaused ? "remains paused" : "resumed after interruption") : "")));
                    } catch (IOException | RuntimeException e) {
                        fail(token, "Could not resume recording: " + e.getMessage());
                        return;
                    }
                    camera.schedule(() -> poll(token, description), 2, TimeUnit.MILLISECONDS);
                    return;
                }
                if (streamEndPending) {
                    streamEndPending = false;
                    CrashDiagnostics.record("Decoded frame received after SDK EOF; stream remains active");
                }
                // Drain decoded frames at stream rate even when UI display is busy. The
                // SDK recording pipeline otherwise stalls behind its decoded-frame queue.
                lastFrame = SystemClock.elapsedRealtime();
                if (photoRequested) capturePhoto();
                if (!visible || !displayPending.compareAndSet(false, true)) {
                    CameraSdk.releaseFrame();
                    camera.schedule(() -> poll(token, description), 2, TimeUnit.MILLISECONDS);
                    return;
                }
                Bitmap bitmap;
                boolean sizeChanged;
                int previewWidth, previewHeight;
                try {
                    int[] size = CameraSdk.size();
                    if (frames < 3) CrashDiagnostics.record("Decoded size " + size[0] + "x" + size[1]);
                    sizeChanged = size[0] != width || size[1] != height;
                    double scale = Math.min(1.0, Math.min(1280.0 / size[0], 720.0 / size[1]));
                    previewWidth = Math.max(1, (int) Math.round(size[0] * scale));
                    previewHeight = Math.max(1, (int) Math.round(size[1] * scale));
                    if (pixels == null || sizeChanged) {
                        width = size[0]; height = size[1];
                        pixels = ByteBuffer.allocateDirect(previewWidth * previewHeight * 4);
                    }
                    if (frames < 3) CrashDiagnostics.record("Before native RGBA copy; buffer=" + pixels.capacity());
                    int copyResult = CameraSdk.pullPreview(pixels, previewWidth, previewHeight);
                    if (frames < 3) CrashDiagnostics.record("After native RGBA copy: " + hex(copyResult));
                    if (copyResult < 0) {
                        displayPending.set(false);
                        fail(token, "Frame copy failed: " + hex(copyResult)); return;
                    }
                    pixels.rewind();
                    bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(pixels);
                    if (frames < 3) CrashDiagnostics.record("Bitmap created and filled");
                } catch (RuntimeException | OutOfMemoryError e) {
                    displayPending.set(false);
                    fail(token, "Cannot prepare preview frame: " + e); return;
                } finally {
                    if (frames < 3) CrashDiagnostics.record("Before SDK frame return");
                    CameraSdk.releaseFrame();
                    if (frames < 3) CrashDiagnostics.record("After SDK frame return");
                }
                lastFrame = SystemClock.elapsedRealtime();
                long count = ++frames;
                String frameDescription = description + " • " + width + " × " + height;
                main.post(() -> {
                    try {
                        if (visible && generation == token) {
                            if (count <= 3) CrashDiagnostics.record("Before ImageView presentation");
                            latestBitmap = bitmap;
                            if (count <= 3) CrashDiagnostics.record("After ImageView presentation");
                            statusText = frameDescription;
                            updateCaptureControls();
                            if (count == 1) log(frameDescription + " • first frame received");
                            else if (sizeChanged) log(frameDescription + " • preview size changed");
                        } else bitmap.recycle();
                    } finally { displayPending.set(false); }
                });
            } else {
                long timeout = frames == 0 ? 20000 : 10000;
                if (SystemClock.elapsedRealtime() - lastFrame > timeout) {
                    fail(token, (frames == 0 ? "No first decoded frame after 20 seconds" : "No frames for 10 seconds")
                            + "; last acquire result " + hex(result)
                            + (streamEndPending ? "; SDK EOF confirmed by frame loss" : "")); return;
                }
            }
        }
        camera.schedule(() -> poll(token, description), 2, TimeUnit.MILLISECONDS);
    }

    private void fail(int token, String message) {
        post(token, () -> {
            if (cameraId == null || (!visible && !recordingActive)) { stopSession(message); return; }
            final int retryToken = ++generation;
            if (!reconnecting && recordingActive) {
                ++interruptions;
                recordingNotice("Recording interrupted; reconnecting. Missing footage cannot be recovered.", false);
            }
            reconnecting = true;
            latestBitmap = null;
            if (photoRequested) {
                photoRequested = false;
                photoSaving = false;
                mediaMessage("Photo cancelled: camera connection interrupted");
            }
            log("Reconnecting to camera… " + message);
            camera.execute(() -> {
                if (generation != retryToken) return;
                if (recordingActive && !recordingPaused && recordingStarted > 0) {
                    recordedMillis += Math.max(0, lastFrame - recordingStarted);
                    recordingStarted = 0;
                }
                try { endSegment(); }
                catch (IOException e) { mediaMessage(e.getMessage()); }
                post(retryToken, () -> { if (foreground) showRecordingNotification(); updateCaptureControls(); });
                CameraSdk.close();
                pixels = null;
                camera.schedule(() -> retryConnection(retryToken), 3, TimeUnit.SECONDS);
            });
        });
    }

    private void retryConnection(int token) {
        if (generation != token) return;
        try {
            // Match the opaque SDK identity, never silently select another camera.
            String[] devices = selectedNetwork == null ? new String[0] : CameraSdk.discover();
            for (int i = 0; i + 1 < devices.length; i += 2) {
                if (devices[i + 1].equals(cameraId)) {
                    open(token, devices[i + 1], devices[i]);
                    return;
                }
            }
            CrashDiagnostics.record("Reconnect: selected camera not discovered yet");
        } catch (RuntimeException e) { CrashDiagnostics.record("Reconnect discovery: " + e); }
        camera.schedule(() -> retryConnection(token), 3, TimeUnit.SECONDS);
    }
    private static String hex(int code) { return String.format(Locale.ROOT, "0x%08X", code); }

    void stopSession(String message) {
        reconnecting = false;
        logPower("Stopping camera session: " + message);
        final int stoppedToken = ++generation;
        camera.execute(() -> {
            try {
                finishRecording();
                CameraSdk.close();
                cameraId = null;
            } catch (LinkageError ignored) { /* Library load failure is reported on connect. */ }
            pixels = null;
            main.post(() -> {
                if (!destroyed && generation == stoppedToken) releaseNetwork();
                settleForeground();
            });
        });
        if (photoRequested) {
            photoRequested = false;
            photoSaving = false;
            mediaMessage("Photo capture cancelled: camera disconnected");
        }
        latestBitmap = null;
        connecting = false;
        if (message != null) log(message);
        updateCaptureControls();
    }

    private void releaseNetwork() {
        if (networkCallback != null) {
            connectivity.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        connectivity.bindProcessToNetwork(null);
        selectedNetwork = null;
        if (multicast != null) { if (multicast.isHeld()) multicast.release(); multicast = null; }

    }

}
