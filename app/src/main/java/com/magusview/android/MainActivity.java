package com.magusview.android;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity implements CameraService.Listener {
    private final Handler main = new Handler(Looper.getMainLooper());
    private CameraService service;
    private boolean bound;
    private AlertDialog selection;
    private TextView status;
    private ImageView preview;
    private ZoomPreview previewContainer;
    private LinearLayout toolRow;
    private Button widthButton;
    private final java.util.ArrayList<Button> drawingActions = new java.util.ArrayList<>();
    private TextView previewRecording;
    private AnnotationView drawing;
    private Button drawButton;
    private boolean drawingMode;
    private final Runnable previewClock = new Runnable() {
        @Override public void run() {
            updatePreviewRecording();
            main.postDelayed(this, 1000);
        }
    };
    private Button photoButton, recordButton, pauseButton;
    private TextView recordingStatus, storageStatus;
    private final java.util.ArrayList<Button> drawingTools = new java.util.ArrayList<>();
    private String pipControls = "";
    private LinearLayout root;
    private boolean pipAvailable;
    private final ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((CameraService.LocalBinder) binder).service();
            service.attach(MainActivity.this);
        }
        public void onServiceDisconnected(ComponentName name) { service = null; render(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            if (isInPictureInPictureMode()) v.setPadding(0, 0, 0, 0);
            else v.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            return insets;
        });
        root.setBackgroundColor(android.graphics.Color.rgb(18, 20, 24));
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("MagusView");
        title.setTextSize(22);
        title.setTextColor(android.graphics.Color.WHITE);
        root.addView(title);
        Button saved = new Button(this);
        saved.setText("Captures");
        saved.setOnClickListener(v -> showCaptureFolders());
        toolbar.addView(saved, new LinearLayout.LayoutParams(0, -2, 1));
        drawButton = new Button(this);
        drawButton.setText("Draw");
        drawButton.setOnClickListener(v -> {
            drawingMode = !drawingMode;
            drawing.cancelDraft();
            render();
        });
        toolbar.addView(drawButton, new LinearLayout.LayoutParams(0, -2, 1));
        Button menu = new Button(this);
        menu.setText("More");
        menu.setOnClickListener(v -> showMenu(menu));
        toolbar.addView(menu, new LinearLayout.LayoutParams(0, -2, 1));
        status = new TextView(this);
        status.setText("Ready to use your current Wi-Fi.");
        status.setTextSize(12);
        status.setMaxLines(2);
        root.addView(status);
        recordingStatus = new TextView(this);
        recordingStatus.setTextSize(18);
        recordingStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        recordingStatus.setPadding(0, padding / 2, 0, padding / 2);
        root.addView(recordingStatus);
        storageStatus = new TextView(this);
        storageStatus.setTextSize(12);
        storageStatus.setPadding(0, 0, 0, padding / 2);
        root.addView(storageStatus);
        toolRow = new LinearLayout(this);
        toolRow.setPadding(padding / 4, padding / 4, padding / 4, padding / 4);
        toolRow.setBackgroundColor(0xff242a33);
        toolRow.setContentDescription("Drawing toolbar");
        toolRow.setOrientation(LinearLayout.VERTICAL);
        LinearLayout shapesRow = new LinearLayout(this);
        toolRow.addView(shapesRow);
        for (String tool : new String[] {"Move", "Line", "Circle", "Rectangle"}) {
            Button button = new Button(this);
            button.setText(tool);
            button.setTextSize(12);
            button.setMinWidth(0);
            button.setPadding(4, 0, 4, 0);
            button.setOnClickListener(v -> {
                if (tool.equals("Move")) {
                    drawing.cancelDraft(); drawing.tool = tool; render();
                    message("Drag a shape or its label to move it.");
                } else chooseDrawingColor(tool);
            });
            drawingTools.add(button);
            shapesRow.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        }
        LinearLayout actionsRow = new LinearLayout(this);
        toolRow.addView(actionsRow);
        for (String action : new String[] {"Label", "Width", "Color", "Delete", "Undo", "Clear"}) {
            Button button = new Button(this);
            button.setText(action); button.setTextSize(12); button.setMinWidth(0); button.setPadding(4, 0, 4, 0);
            if (action.equals("Width")) widthButton = button;
            drawingActions.add(button);
            actionsRow.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
            button.setOnClickListener(v -> {
                if (service == null) return;
                drawing.cancelDraft();
                switch (action) {
                    case "Delete": drawing.deleteSelected(); break;
                    case "Label": editObjectLabel(); break;
                    case "Width": chooseLineWidth(); break;
                    case "Color": chooseDrawingColor(drawing.tool); break;
                    case "Undo": service.annotations.undo(); render(); break;
                    case "Clear": clearDrawings(); break;
                }
            });
        }
        root.addView(toolRow);
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setContentDescription("Camera preview. Tap to focus on an area. Hold for manual focus. In Draw mode, pinch with two fingers to zoom the preview.");
        android.view.GestureDetector focusGestures = new android.view.GestureDetector(this,
            new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDown(android.view.MotionEvent event) { return true; }
                @Override public boolean onSingleTapUp(android.view.MotionEvent event) {
                    float[] point = focusPoint(event);
                    if (point != null) service.focusAt(point[0], point[1]);
                    return true;
                }
                @Override public void onLongPress(android.view.MotionEvent event) {
                    if (focusPoint(event) != null) CameraControls.manualFocus(MainActivity.this, service);
                }
            });
        preview.setOnTouchListener((v, event) -> {
            if (isInPictureInPictureMode()) return false;
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) v.performClick();
            return focusGestures.onTouchEvent(event);
        });
        previewContainer = new ZoomPreview(this);
        previewContainer.addView(preview, new android.widget.FrameLayout.LayoutParams(-1, -1));
        drawing = new AnnotationView(this);
        drawing.finished = this::labelDrawing;
        drawing.selectionChanged = this::render;
        previewContainer.layers(preview, drawing);
        previewContainer.addView(drawing, new android.widget.FrameLayout.LayoutParams(-1, -1));
        previewRecording = new TextView(this);
        previewRecording.setTextSize(14);
        previewRecording.setTypeface(null, android.graphics.Typeface.BOLD);
        previewRecording.setTextColor(android.graphics.Color.WHITE);
        previewRecording.setShadowLayer(2 * getResources().getDisplayMetrics().density, 0, 0, android.graphics.Color.BLACK);
        previewRecording.setPadding(padding / 2, padding / 4, padding / 2, padding / 4);
        android.widget.FrameLayout.LayoutParams badge = new android.widget.FrameLayout.LayoutParams(-2, -2,
            android.view.Gravity.TOP | android.view.Gravity.START);
        badge.setMargins(padding / 2, padding / 2, padding / 2, padding / 2);
        previewContainer.addView(previewRecording, badge);
        root.addView(previewContainer, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout captureControls = new LinearLayout(this);
        photoButton = new Button(this);
        photoButton.setText("Take photo");
        photoButton.setOnClickListener(v -> { if (service != null) service.savePhoto(); });
        captureControls.addView(photoButton, new LinearLayout.LayoutParams(0, -2, 1));
        recordButton = new Button(this);
        recordButton.setText("Record video");
        recordButton.setOnClickListener(v -> record());
        captureControls.addView(recordButton, new LinearLayout.LayoutParams(0, -2, 1));
        pauseButton = new Button(this);
        pauseButton.setText("Pause recording");
        pauseButton.setOnClickListener(v -> { if (service != null) service.pauseOrContinue(); });
        captureControls.addView(pauseButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(captureControls);
        root.addView(toolbar);
        render();
        setContentView(root);
    }
    private void showMenu(android.view.View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Find camera").setEnabled(service != null && service.canConnect());
        popup.getMenu().add(0, 2, 1, "Disconnect").setEnabled(service != null);
        popup.getMenu().add(0, 7, 2, "Save diagnostics file");
        popup.getMenu().add(0, 6, 3, "Background recording");
        popup.getMenu().add(0, 8, 4, "Reset drawing zoom").setEnabled(drawingMode);
        popup.getMenu().add(0, 4, 0, "Camera controls").setEnabled(service != null && service.bitmap() != null);
        popup.getMenu().add(0, 5, 0, "Manual focus").setEnabled(service != null && service.bitmap() != null);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1 && service != null) service.begin();
            else if (item.getItemId() == 2 && service != null) {
                drawing.cancelDraft();
                service.annotations.reset();
                drawingMode = false;
                previewContainer.resetZoom();
                service.stopSession("Disconnected");
                render();
            }
            else if (item.getItemId() == 4 && service != null) CameraControls.show(this, service);
            else if (item.getItemId() == 5 && service != null) CameraControls.manualFocus(this, service);
            else if (item.getItemId() == 6) showBackgroundSettings();
            else if (item.getItemId() == 8) previewContainer.resetZoom();
            else if (item.getItemId() == 7) {
                message("Saving diagnostics…");
                android.content.Context app = getApplicationContext();
                new Thread(() -> {
                    String result;
                    try { CrashDiagnostics.saveReport(app); result = "Diagnostics saved to Downloads/MagusView"; }
                    catch (java.io.IOException | RuntimeException e) { result = "Diagnostics save failed: " + e.getMessage(); }
                    final String saved = result;
                    main.post(() -> { if (!isDestroyed()) message(saved); });
                }, "save-diagnostics").start();
            }
            return true;
        });
        popup.show();
    }
    private void clearDrawings() {
        if (service == null) return;
        CameraService target = service;
        new AlertDialog.Builder(this).setTitle("Clear all drawings?")
            .setMessage("Previously saved photos stay unchanged. You can undo this clear while this camera session remains open.")
            .setNegativeButton("Cancel", null).setPositiveButton("Clear", (d, w) -> {
                target.annotations.clear(); drawing.cancelDraft(); render();
            }).show();
    }
    private void chooseLineWidth() {
        float[] widths = {.002f, .004f, .008f, .012f, .02f};
        String[] names = {"Extra thin", "Thin", "Medium", "Thick", "Extra thick"};
        Annotations.Shape object = drawing.selectedShape();
        float currentWidth = object == null ? drawing.strokeWidth : object.strokeWidth();
        int selected = 1;
        for (int i = 0; i < widths.length; ++i) if (Math.abs(currentWidth - widths[i]) < .0001f) selected = i;
        new AlertDialog.Builder(this).setTitle(object == null ? "Line width for new shapes" : "Selected object width")
            .setSingleChoiceItems(names, selected, (dialog, index) -> {
                if (object == null) drawing.strokeWidth = widths[index];
                else drawing.editSelected(object, object.label(), widths[index], object.color());
                dialog.dismiss();
            }).setNegativeButton("Cancel", null).show();
    }
    private void chooseDrawingColor(String tool) {
        if (service == null || service.bitmap() == null) return;
        Annotations.Shape object = tool.equals("Move") ? drawing.selectedShape() : null;
        int[] colors = {0xffff4040, 0xffffd740, 0xff40e060, 0xff40cfff, 0xffff60dd, 0xffffffff, 0xff000000};
        new AlertDialog.Builder(this).setTitle(object != null ? "Selected object color" : tool.equals("Move") ? "Color for new shapes" : tool + " color")
            .setItems(new String[] {"Red", "Yellow", "Green", "Blue", "Pink", "White", "Black"},
                (dialog, index) -> {
                    drawing.cancelDraft();
                    drawing.tool = tool;
                    if (object == null) drawing.color = colors[index];
                    else drawing.editSelected(object, object.label(), object.strokeWidth(), colors[index]);
                    drawingMode = true;
                    render();
                    if (!tool.equals("Move")) message("Drag to draw a " + tool.toLowerCase(java.util.Locale.ROOT) + ", then add a label.");
                }).setNegativeButton("Cancel", null).show();
    }
    private void editObjectLabel() {
        Annotations.Shape object = drawing.selectedShape();
        if (object == null) return;
        android.widget.EditText label = new android.widget.EditText(this);
        label.setSingleLine(true);
        label.setText(object.label()); label.setSelection(label.length());
        label.setHint("Leave empty to remove label");
        label.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(80)});
        new AlertDialog.Builder(this).setTitle("Selected object label").setView(label)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", (d, w) ->
                drawing.editSelected(object, label.getText().toString().trim(), object.strokeWidth(), object.color())).show();
    }
    private void labelDrawing(Annotations.Shape shape) {
        if (service == null) return;
        CameraService target = service;
        android.widget.EditText label = new android.widget.EditText(this);
        label.setHint("Optional label");
        label.setSingleLine(true);
        label.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(80)});
        new AlertDialog.Builder(this).setTitle("Drawing label").setView(label)
            .setNegativeButton("Cancel drawing", null).setPositiveButton("Add", (d, w) -> {
                if (service != target) return;
                target.annotations.add(new Annotations.Shape(shape.kind(), shape.color(), shape.strokeWidth(), shape.x1(), shape.y1(),
                    shape.x2(), shape.y2(), label.getText().toString().trim()));
                drawing.invalidate();
            }).show();
    }
    private void showCaptureFolders() {
        new AlertDialog.Builder(this).setTitle("Open capture folder")
            .setItems(new String[] { "Photos — Pictures/MagusView", "Videos — Movies/MagusView" }, (dialog, index) -> {
                String folder = index == 0 ? "Pictures/MagusView" : "Movies/MagusView";
                android.net.Uri uri = android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", "primary:" + folder);
                Intent open = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,
                    android.provider.DocumentsContract.Document.MIME_TYPE_DIR);
                try { startActivity(open); }
                catch (android.content.ActivityNotFoundException | SecurityException e) {
                    new AlertDialog.Builder(this).setTitle("Capture folder")
                        .setMessage("Your file manager cannot open this folder directly. Open Files and go to Internal storage/"
                            + folder + ". The folder is created after your first capture is saved.")
                        .setPositiveButton("OK", null).show();
                }
            }).setNegativeButton("Cancel", null).show();
    }
    private void showBackgroundSettings() {
        boolean exempt = getSystemService(android.os.PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        new AlertDialog.Builder(this).setTitle("Background recording")
            .setMessage(exempt ? "Android battery optimization is already disabled for MagusView. If locked-screen recording still stops, check the phone's app battery settings and save the diagnostics file."
                : "Continuous Wi-Fi recording needs network access while the screen is locked. Allow MagusView to run without Android battery optimization. This can increase battery use during recording.")
            .setPositiveButton(exempt ? "App settings" : "Allow background recording", (dialog, which) -> {
                Intent settings = new Intent(exempt ? android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    : android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:" + getPackageName()));
                try { startActivity(settings); }
                catch (android.content.ActivityNotFoundException e) {
                    message("Open phone Settings → Apps → MagusView → Battery to allow background activity.");
                }
            }).setNegativeButton("Close", null).show();
    }
    private float[] focusPoint(android.view.MotionEvent event) {
        if (service == null || service.bitmap() == null || isInPictureInPictureMode()) return null;
        android.graphics.Bitmap bitmap = service.bitmap();
        return FocusTarget.point(event.getX(), event.getY(), preview.getWidth(), preview.getHeight(), bitmap.getWidth(), bitmap.getHeight());
    }
    private void render() {
        drawButton.setEnabled(service != null && service.bitmap() != null);
        drawButton.setText(drawingMode ? "Focus mode" : "Draw mode");
        drawButton.setContentDescription(drawingMode ? "Switch to focus mode" : "Switch to drawing mode");
        toolRow.setVisibility(drawingMode && !isInPictureInPictureMode() ? android.view.View.VISIBLE : android.view.View.GONE);
        for (Button button : drawingActions) button.setEnabled(service != null && service.bitmap() != null
            && (!(button.getText().toString().equals("Label") || button.getText().toString().equals("Delete")) || drawing.selectedShape() != null));
        for (Button button : drawingTools) {
            button.setEnabled(service != null && service.bitmap() != null);
            boolean selected = drawingMode && button.getText().toString().equals(drawing.tool);
            button.setSelected(selected);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? 0xff385a7c : 0xff343c48));
            button.setTextColor(button.isEnabled() ? android.graphics.Color.WHITE : 0xff9299a3);
            button.setContentDescription(button.getText() + (selected ? ", selected. Choose color" : ". Choose color"));
        }
        storageStatus.setText(service == null ? "Checking storage…" : service.storageStatus());
        storageStatus.setTextColor(service != null && service.storageWarning() ? 0xffffc857 : 0xffb9c5d6);
        drawing.editing = drawingMode && !isInPictureInPictureMode();
        previewContainer.setZoomEnabled(drawing.editing);
        drawing.scene = service == null ? null : service.annotations;
        if (service != null && service.bitmap() != null) {
            drawing.imageWidth = service.bitmap().getWidth(); drawing.imageHeight = service.bitmap().getHeight();
        }
        drawing.invalidate();
        updatePreviewRecording();
        recordingStatus.setText(service == null ? "Discovering cameras…" : service.captureStatus());
        recordingStatus.setTextColor(service != null && service.recording()
            ? (service.paused() ? 0xffffc857 : 0xffff6666) : 0xffb9c5d6);
        photoButton.setEnabled(service != null && service.canPhoto());
        recordButton.setEnabled(service != null && service.canRecord());
        recordButton.setText(service == null ? "Record" : service.recordLabel());
        pauseButton.setEnabled(service != null && service.canPause());
        pauseButton.setText(service != null && service.paused() ? "Continue" : "Pause");
        if (service != null) { status.setText(service.status()); preview.setImageBitmap(service.bitmap()); }
        boolean available = service != null && service.bitmap() != null;
        String controls = service == null ? "" : service.canPhoto() + ":" + service.canPause() + ":" + service.paused() + ":" + service.canRecord() + ":" + service.recording();
        if (available != pipAvailable || !controls.equals(pipControls)) {
            pipControls = controls;
            pipAvailable = available;
            updatePip();
        }
    }
    private void updatePreviewRecording() {
        if (previewRecording == null) return;
        boolean active = isInPictureInPictureMode() && service != null && service.recording();
        previewRecording.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
        if (!active) return;
        String label = service.recordingTimeLabel();
        android.text.SpannableString styled = new android.text.SpannableString(label);
        if (!service.paused()) styled.setSpan(new android.text.style.ForegroundColorSpan(0xffff4040),
            0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        previewRecording.setTextColor(service.paused() ? 0xffffc857 : android.graphics.Color.WHITE);
        previewRecording.setText(styled);
    }
    private android.app.PictureInPictureParams pipParams() {
        android.app.PictureInPictureParams.Builder builder = new android.app.PictureInPictureParams.Builder()
            .setAspectRatio(new android.util.Rational(16, 9));
        android.graphics.Rect rect = new android.graphics.Rect();
        if (preview.getGlobalVisibleRect(rect)) builder.setSourceRectHint(rect);
        if (android.os.Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(pipAvailable);
        java.util.ArrayList<android.app.RemoteAction> actions = new java.util.ArrayList<>();
        actions.add(pipAction(CameraService.PHOTO, R.drawable.pip_photo, "Take photo", service != null && service.canPhoto()));
        if (service != null && service.recording()) {
            boolean paused = service.paused();
            actions.add(pipAction(CameraService.PAUSE, paused ? R.drawable.pip_play : R.drawable.pip_pause,
                paused ? "Continue recording" : "Pause recording", service.canPause()));
            actions.add(pipAction(CameraService.STOP, R.drawable.pip_stop, "Stop recording and save", service.canRecord()));
        } else {
            actions.add(pipAction(CameraService.RECORD, R.drawable.pip_record, "Start recording", service != null && service.canRecord()));
        }
        builder.setActions(actions);
        return builder.build();
    }
    private android.app.RemoteAction pipAction(String action, int icon, String label, boolean enabled) {
        Intent command = new Intent(this, CameraService.class).setAction(action);
        int flags = android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        android.app.PendingIntent intent = CameraService.RECORD.equals(action)
            ? android.app.PendingIntent.getForegroundService(this, action.hashCode(), command, flags)
            : android.app.PendingIntent.getService(this, action.hashCode(), command, flags);
        android.app.RemoteAction result = new android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithResource(this, icon), label, label, intent);
        result.setEnabled(enabled);
        return result;
    }
    private boolean supportsPip() {
        return getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }
    private void updatePip() {
        if (!supportsPip()) return;
        try { setPictureInPictureParams(pipParams()); }
        catch (IllegalStateException e) { CrashDiagnostics.record("Floating preview unavailable: " + e.getMessage()); }
    }
    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (android.os.Build.VERSION.SDK_INT < 31 && pipAvailable && supportsPip()) {
            try { enterPictureInPictureMode(pipParams()); }
            catch (IllegalStateException e) { CrashDiagnostics.record("Floating preview unavailable: " + e.getMessage()); }
        }
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip, android.content.res.Configuration configuration) {
        super.onPictureInPictureModeChanged(inPip, configuration);
        drawing.cancelDraft();
        drawing.editing = drawingMode && !inPip;
        updatePreviewRecording();
        for (int i = 0; i < root.getChildCount(); i++) {
            android.view.View child = root.getChildAt(i);
            if (child != previewContainer) child.setVisibility(inPip ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        render();
        root.requestApplyInsets();
    }
    @Override public void changed() { render(); }
    @Override public void message(String text) { android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show(); }
    @Override public void select(String[] devices, int token) {
        if (service == null) return;
        if (selection != null) selection.dismiss();
        CameraService target = service;
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(12 * getResources().getDisplayMetrics().density);
        java.util.ArrayList<ImageView> images = new java.util.ArrayList<>();
        java.util.ArrayList<TextView> labels = new java.util.ArrayList<>();
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(rows);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Select camera").setView(scroll)
            .setNegativeButton("Cancel", (d, which) -> target.cancelSelection(token))
            .setOnCancelListener(d -> target.cancelSelection(token)).create();
        selection = dialog;
        for (int i = 0; i + 1 < devices.length; i += 2) {
            final String name = devices[i], id = devices[i + 1];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad, pad, pad);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setClickable(true); row.setFocusable(true);
            row.setContentDescription("Select " + name);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackgroundColor(0xff20242a);
            image.setContentDescription("Snapshot from " + name);
            row.addView(image, new LinearLayout.LayoutParams(pad * 10, pad * 6));
            TextView label = new TextView(this);
            label.setText(name + "\nLoading preview…");
            label.setPadding(pad, 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            images.add(image); labels.add(label); rows.addView(row);
            row.setOnClickListener(v -> {
                if (service == target) target.selectCamera(token, id, name);
                dialog.dismiss();
            });
        }
        dialog.setOnDismissListener(d -> {
            target.cancelCameraPreviews();
            if (selection == dialog) selection = null;
        });
        dialog.show();
        target.previewCameras(devices, token, (row, bitmap) -> {
            if (!dialog.isShowing() || service != target) { if (bitmap != null) bitmap.recycle(); return; }
            images.get(row).setImageBitmap(bitmap);
            labels.get(row).setText(devices[row * 2] + (bitmap == null ? "\nPreview unavailable · tap to connect" : "\nTap to connect"));
        });
    }
    private void record() {
        if (service == null || !service.canRecord()) return;
        if (service.recording()) { service.stopRecording(); return; }
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notificationAsked", false)) {
            getPreferences(0).edit().putBoolean("notificationAsked", true).apply();
            requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 1);
            return;
        }
        service.requestRecording();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1) record();
    }
    @Override protected void onStart() {
        super.onStart();
        bound = bindService(new Intent(this, CameraService.class), connection, BIND_AUTO_CREATE);
        main.removeCallbacks(previewClock);
        main.post(previewClock);
    }
    @Override protected void onStop() {
        main.removeCallbacks(previewClock);
        if (selection != null) { selection.dismiss(); selection = null; }
        if (service != null) { service.detach(this); service = null; }
        if (bound) { unbindService(connection); bound = false; }
        preview.setImageDrawable(null);
        pipAvailable = false;
        updatePip();
        super.onStop();
    }
}
