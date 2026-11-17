package com.magusview.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Map;

final class CameraControls {
    static final int[] PARAMETERS = {26,25,11,1,2,3,12,4,5,19,20,21,6,7,8,9,10,18};
    private static final String[] NAMES = {"Focus mode", "Manual focus position", "Auto exposure", "Exposure time", "Gain",
        "Auto exposure target", "White balance", "Color temperature", "Tint", "White balance red", "White balance green",
        "White balance blue", "Contrast", "Hue", "Saturation", "Brightness", "Gamma", "Sharpness"};
    private final Activity activity;
    private final CameraService service;
    private final LinearLayout content;
    private final AlertDialog dialog;

    static void show(Activity activity, CameraService service) { new CameraControls(activity, service).load(); }
    static void manualFocus(Activity activity, CameraService service) {
        service.readControls(values -> {
            if (activity.isDestroyed() || activity.isFinishing()) return;
            int[] range = values.get(25);
            if (range == null || range[0] != 0 || range[5] < 0 || range[2] <= range[1]) {
                android.widget.Toast.makeText(activity, "Manual focus is not available", android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            LinearLayout panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
            int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
            panel.setPadding(pad, 0, pad, 0);
            TextView position = new TextView(activity);
            android.widget.SeekBar slider = new android.widget.SeekBar(activity);
            slider.setMax(range[2] - range[1]);
            slider.setProgress(Math.max(0, Math.min(slider.getMax(), range[4] - range[1])));
            position.setText("Focus position: " + (slider.getProgress() + range[1]));
            panel.addView(position); panel.addView(slider);
            LinearLayout steps = new LinearLayout(activity);
            Button minus = new Button(activity); minus.setText("−"); minus.setContentDescription("Decrease focus position");
            Button plus = new Button(activity); plus.setText("+"); plus.setContentDescription("Increase focus position");
            steps.addView(minus, new LinearLayout.LayoutParams(0, -2, 1));
            steps.addView(plus, new LinearLayout.LayoutParams(0, -2, 1)); panel.addView(steps);
            Runnable apply = () -> {
                slider.setEnabled(false); minus.setEnabled(false); plus.setEnabled(false);
                service.changeControl(25, slider.getProgress() + range[1], () -> {
                    slider.setEnabled(true); minus.setEnabled(true); plus.setEnabled(true);
                });
            };
            slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(android.widget.SeekBar bar, int value, boolean user) {
                    position.setText("Focus position: " + (value + range[1]));
                }
                public void onStartTrackingTouch(android.widget.SeekBar bar) {}
                public void onStopTrackingTouch(android.widget.SeekBar bar) { apply.run(); }
            });
            minus.setOnClickListener(v -> { if (slider.getProgress() > 0) { slider.setProgress(slider.getProgress() - 1); apply.run(); } });
            plus.setOnClickListener(v -> { if (slider.getProgress() < slider.getMax()) { slider.setProgress(slider.getProgress() + 1); apply.run(); } });
            AlertDialog focus = new AlertDialog.Builder(activity).setTitle("Manual focus").setView(panel).setPositiveButton("Done", null).create();
            focus.show();
            if (focus.getWindow() != null) {
                focus.getWindow().setGravity(android.view.Gravity.BOTTOM);
                focus.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        });
    }
    private CameraControls(Activity activity, CameraService service) {
        this.activity = activity; this.service = service;
        content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding / 2);
        ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        dialog = new AlertDialog.Builder(activity).setTitle("Camera controls").setView(scroll)
            .setNegativeButton("Close", null).create();
        dialog.show();
    }
    private void load() {
        if (!dialog.isShowing() || activity.isDestroyed()) return;
        content.removeAllViews();
        TextView wait = new TextView(activity); wait.setText("Reading camera settings…"); content.addView(wait);
        service.readControls(this::render);
    }
    private boolean supported(int[] c) { return c != null && c[0] == 0 && c[5] >= 0; }
    private void render(Map<Integer, int[]> values) {
        if (!dialog.isShowing() || activity.isDestroyed()) return;
        content.removeAllViews();
        TextView note = new TextView(activity);
        note.setText("Changes apply to the camera and may be remembered. Setting exposure/gain, white balance gains, or focus position switches that control to manual.");
        content.addView(note);
        for (int i = 0; i < PARAMETERS.length; i++) {
            int id = PARAMETERS[i]; int[] c = values.get(id); String name = NAMES[i];
            if (!supported(c)) continue;
            String current = Integer.toString(c[4]);
            if (id == 11) current = c[4] == 0 ? "Manual" : "Auto";
            if (id == 12) current = c[4] == 0 ? "Manual" : c[4] == 1 ? "Auto" : "Region";
            if (id == 26) current = c[4] == 0 ? "Manual" : c[4] == 1 ? "Continuous" : c[4] == 2 ? "One-shot" : "Calibration";
            button(name + ": " + current, () -> {
                if (id == 11 || id == 12 || id == 26) {
                    String[] choices = id == 26 ? new String[] {"Manual", "Continuous autofocus"} : new String[] {"Manual", "Automatic"};
                    new AlertDialog.Builder(activity).setTitle(name).setItems(choices, (d, index) -> {
                        if (index >= c[1] && index <= c[2]) apply(id, index);
                    }).show();
                } else edit(id, name, c);
            });
        }
        button("Restore image defaults…", () -> new AlertDialog.Builder(activity)
            .setTitle("Restore image defaults?")
            .setMessage("Reset exposure, gain, white balance and color adjustments to the camera's defaults? Focus and connection settings will stay unchanged.")
            .setNegativeButton("Cancel", null).setPositiveButton("Restore", (d, w) -> {
                disable(); service.restoreImageDefaults(this::load);
            }).show());
    }
    private void button(String label, Runnable action) {
        Button button = new Button(activity); button.setAllCaps(false); button.setText(label);
        button.setOnClickListener(v -> action.run()); content.addView(button);
    }
    private void disable() { for (int i = 0; i < content.getChildCount(); i++) content.getChildAt(i).setEnabled(false); }
    private void apply(int id, int value) { disable(); service.changeControl(id, value, this::load); }
    private void edit(int id, String name, int[] c) {
        EditText value = new EditText(activity);
        value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        value.setText(Integer.toString(c[4])); value.selectAll();
        AlertDialog editor = new AlertDialog.Builder(activity).setTitle(name)
            .setMessage("Range: " + c[1] + "–" + c[2] + " · Default: " + c[3])
            .setView(value).setNegativeButton("Cancel", null).setPositiveButton("Apply", null).create();
        editor.setOnShowListener(d -> editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int number = Integer.parseInt(value.getText().toString().trim());
                if (number < c[1] || number > c[2]) throw new NumberFormatException();
                editor.dismiss(); apply(id, number);
            } catch (NumberFormatException e) { value.setError("Enter a value from " + c[1] + " to " + c[2]); }
        }));
        editor.show();
    }
}
