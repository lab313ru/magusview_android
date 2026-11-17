package com.magusview.android;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Cancel drawing/focus when a second finger enters; suppress taps until all lift. */
final class ZoomPreview extends FrameLayout {
    private final PreviewZoom zoom = new PreviewZoom();
    private boolean zoomEnabled;
    private ImageView image;
    private float focusX, focusY;
    private final ScaleGestureDetector detector;
    private AnnotationView annotations;
    private boolean multiTouch;
    ZoomPreview(Context context) {
        super(context);
        setClipChildren(true);
        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector d) {
                focusX = d.getFocusX() - getWidth() / 2f;
                focusY = d.getFocusY() - getHeight() / 2f;
                return zoomEnabled;
            }
            @Override public boolean onScale(ScaleGestureDetector d) {
                if (!zoomEnabled) return false;
                float nextX = d.getFocusX() - getWidth() / 2f;
                float nextY = d.getFocusY() - getHeight() / 2f;
                zoom.pinch(d.getScaleFactor(), focusX, focusY, nextX, nextY);
                focusX = nextX; focusY = nextY;
                applyZoom();
                return true;
            }
        });
        detector.setQuickScaleEnabled(false);
    }
    void layers(ImageView image, AnnotationView annotations) { this.image = image; this.annotations = annotations; }
    void setZoomEnabled(boolean enabled) {
        if (zoomEnabled == enabled) return;
        zoomEnabled = enabled;
        if (!enabled) resetZoom();
    }
    void resetZoom() { zoom.reset(); applyZoom(); }
    private void applyZoom() {
        if (image == null || annotations == null) return;
        zoom.constrain(getWidth(), getHeight(), annotations.imageWidth, annotations.imageHeight);
        for (android.view.View layer : new android.view.View[] {image, annotations}) {
            layer.setPivotX(getWidth() / 2f); layer.setPivotY(getHeight() / 2f);
            layer.setScaleX(zoom.scale); layer.setScaleY(zoom.scale);
            layer.setTranslationX(zoom.x); layer.setTranslationY(zoom.y);
        }
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        applyZoom();
    }
    private final android.graphics.Paint lensPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    @Override protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!zoomEnabled || annotations == null || !annotations.editing || !annotations.movingObject()
                || image == null || image.getDrawable() == null) return;
        float density = getResources().getDisplayMetrics().density;
        float margin = 6 * density;
        float w = Math.min(160 * density, getWidth() - 2 * margin);
        float h = Math.min(112 * density, getHeight() - 2 * margin);
        if (w <= 0 || h <= 0) return;
        // Touch coordinates are local to the scaled annotation layer. Map back to
        // the unscaled container so the inset stays the same size at any drawing zoom.
        float[] finger = {annotations.dragX, annotations.dragY};
        annotations.getMatrix().mapPoints(finger);
        finger[0] += annotations.getLeft(); finger[1] += annotations.getTop();
        float left = Math.max(margin, Math.min(getWidth() - margin - w, finger[0] - w / 2));
        float top = finger[1] - 36 * density - h;
        top = Math.max(margin, Math.min(getHeight() - margin - h, top));
        android.graphics.RectF lens = new android.graphics.RectF(left, top, left + w, top + h);
        android.graphics.Path outline = new android.graphics.Path();
        outline.addRoundRect(lens, 10 * density, 10 * density, android.graphics.Path.Direction.CW);
        int saved = canvas.save();
        canvas.clipPath(outline);
        canvas.drawColor(0xff171b20);
        canvas.translate(lens.centerX(), lens.centerY());
        canvas.scale(2.5f, 2.5f);
        canvas.translate(-finger[0], -finger[1]);
        // Redraw just the image and annotations, never this container or the inset.
        // This keeps the loupe live without allocating a screenshot for each frame.
        for (android.view.View layer : new android.view.View[] {image, annotations}) {
            int layerSave = canvas.save();
            canvas.translate(layer.getLeft(), layer.getTop());
            canvas.concat(layer.getMatrix());
            layer.draw(canvas);
            canvas.restoreToCount(layerSave);
        }
        canvas.restoreToCount(saved);
        lensPaint.setStyle(android.graphics.Paint.Style.STROKE);
        lensPaint.setStrokeWidth(2 * density); lensPaint.setColor(0xffe4edf7);
        canvas.drawRoundRect(lens, 10 * density, 10 * density, lensPaint);
        // A small crosshair marks the exact point hidden under the finger.
        for (int pass = 0; pass < 2; ++pass) {
            lensPaint.setColor(pass == 0 ? 0xff000000 : 0xffffffff);
            lensPaint.setStrokeWidth((pass == 0 ? 3 : 1) * density);
            canvas.drawLine(lens.centerX() - 7*density, lens.centerY(), lens.centerX() + 7*density, lens.centerY(), lensPaint);
            canvas.drawLine(lens.centerX(), lens.centerY() - 7*density, lens.centerX(), lens.centerY() + 7*density, lensPaint);
        }
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (event.getPointerCount() > 1 && !multiTouch) {
            multiTouch = true;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
            if (annotations != null) annotations.cancelDraft();
        }
        detector.onTouchEvent(event);
        if (multiTouch) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL)
                multiTouch = false;
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
