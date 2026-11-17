package com.magusview.android;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

final class CrashDiagnostics {
    private static File current, previous;

    static synchronized void initialize(Context context) {
        if (current != null) return;
        current = new File(context.getFilesDir(), "diagnostic-session.txt");
        previous = new File(context.getFilesDir(), "diagnostic-previous.txt");
        try {
            if (current.exists()) Files.copy(current.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try (FileOutputStream ignored = new FileOutputStream(current)) { /* truncate new session */ }
        } catch (IOException e) { Log.w("MagusView", "Diagnostic rotation failed", e); }
        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            record("UNCAUGHT on " + thread.getName() + ": " + Log.getStackTraceString(error));
            if (original != null) original.uncaughtException(thread, error);
            else { Process.killProcess(Process.myPid()); System.exit(10); }
        });
        record("MagusView 0.44 process started " + new Date() + "; pid=" + Process.myPid());
    }

    static synchronized void record(String message) {
        if (current == null) return;
        try {
            if (current.length() >= 60000) {
                byte[] old = Files.readAllBytes(current.toPath());
                int start = Math.max(0, old.length - 32000);
                while (start < old.length && old[start] != '\n') ++start;
                try (FileOutputStream trim = new FileOutputStream(current)) {
                    trim.write("Earlier diagnostic entries trimmed; recent events retained.\n".getBytes(StandardCharsets.UTF_8));
                    if (start < old.length) trim.write(old, start + 1, old.length - start - 1);
                }
            }
            try (FileOutputStream output = new FileOutputStream(current, true)) {
                output.write((System.currentTimeMillis() + " " + message + "\n").getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (IOException ignored) { /* Diagnostics must not break preview. */ }
    }

    private static String read(File file) throws IOException {
        if (file == null || !file.exists()) return "No saved session.\n";
        try (InputStream input = new FileInputStream(file)) {
            return new String(readBounded(input, 65536), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while (output.size() < limit && (count = input.read(buffer, 0, Math.min(buffer.length, limit - output.size()))) != -1) {
            if (count > 0) output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    // Called off the UI thread; Android can take time retrieving a native tombstone.
    static String report(Context context) {
        StringBuilder result = new StringBuilder("MagusView crash diagnostics\n");
        try {
            result.append("CURRENT SESSION\n").append(read(current));
            List<ApplicationExitInfo> exits = context.getSystemService(ActivityManager.class)
                    .getHistoricalProcessExitReasons(context.getPackageName(), 0, 5);
            result.append("ANDROID EXIT HISTORY\n");
            boolean traceRead = false;
            for (ApplicationExitInfo exit : exits) {
                result.append(new Date(exit.getTimestamp())).append(" reason=").append(reason(exit.getReason()))
                        .append(" status=").append(exit.getStatus()).append(" pid=").append(exit.getPid())
                        .append(" pssKB=").append(exit.getPss()).append(" rssKB=").append(exit.getRss())
                        .append(" description=").append(exit.getDescription()).append('\n');
                if (!traceRead && (exit.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE
                        || exit.getReason() == ApplicationExitInfo.REASON_ANR)) {
                    try (InputStream input = exit.getTraceInputStream()) {
                        if (input == null) { result.append("No system trace available for this exit.\n"); continue; }
                        traceRead = true;
                        byte[] data = readBounded(input, 8 * 1024 * 1024 + 1);
                        if (data.length > 8 * 1024 * 1024) result.append("System trace exceeds 8 MiB limit.\n");
                        else if (exit.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE) result.append(TombstoneSummary.summarize(data));
                        else result.append(new String(data, 0, Math.min(data.length, 24000), StandardCharsets.UTF_8));
                    } catch (IOException | RuntimeException e) { result.append("Trace could not be read: ").append(e).append('\n'); }
                }
            }
            if (exits.isEmpty()) result.append("Android has no previous exit record for this app.\n");
            result.append("PREVIOUS SESSION\n").append(read(previous));
        } catch (IOException | RuntimeException e) { result.append("Diagnostic collection error: ").append(e); }
        return result.toString();
    }
    static void saveReport(Context context) throws IOException {
        String report = report(context);
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
            "MagusView_diagnostics_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.ROOT).format(new Date()) + ".txt");
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/MagusView");
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
        android.content.ContentResolver resolver = context.getContentResolver();
        android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Cannot create diagnostics file");
        boolean published = false;
        try {
            try (java.io.OutputStream output = resolver.openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Cannot open diagnostics file");
                output.write(report.getBytes(StandardCharsets.UTF_8));
            }
            android.content.ContentValues ready = new android.content.ContentValues();
            ready.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(uri, ready, null, null) != 1) throw new IOException("Cannot publish diagnostics file");
            published = true;
        } finally {
            if (!published) try { resolver.delete(uri, null, null); } catch (RuntimeException ignored) {}
        }
    }

    private static String reason(int reason) {
        return switch (reason) {
            case ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH";
            case ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH";
            case ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY";
            case ApplicationExitInfo.REASON_ANR -> "ANR";
            case ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED";
            default -> Integer.toString(reason);
        };
    }
}
