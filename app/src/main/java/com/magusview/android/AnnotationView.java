package com.magusview.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

final class AnnotationView extends View {
    interface Finished { void drawn(Annotations.Shape shape); }
    Annotations scene;
    String tool = "Line";
    int color = 0xffff4040;
    float strokeWidth = .004f;
    boolean editing;
    private int selectedIndex = -1;
    private Annotations.Shape selection;
    Runnable selectionChanged;
    Annotations.Shape selectedShape() {
        if (!tool.equals("Move") || scene == null || selection == null) return null;
        List<Annotations.Shape> shapes = scene.snapshot();
        return selectedIndex >= 0 && selectedIndex < shapes.size() && shapes.get(selectedIndex) == selection ? selection : null;
    }
    void deleteSelected() {
        Annotations.Shape selected = selectedShape();
        if (selected == null) return;
        scene.remove(selectedIndex, selected);
        selection = null; selectedIndex = -1;
        cancelDraft();
        if (selectionChanged != null) selectionChanged.run();
    }
    void editSelected(Annotations.Shape original, String label, float width, int color) {
        if (original == null || selectedShape() != original) return;
        Annotations.Shape edited = new Annotations.Shape(original.kind(), color, width,
            original.x1(), original.y1(), original.x2(), original.y2(), label);
        scene.replace(selectedIndex, original, edited);
        if (!original.equals(edited)) selection = edited;
        invalidate();
        if (selectionChanged != null) selectionChanged.run();
    }
    private int movingIndex = -1;
    private Annotations.Shape movingOriginal;
    private float downX, downY;
    float dragX, dragY;
    boolean movingObject() { return movingOriginal != null; }
    private void refreshMagnifier() { if (getParent() instanceof View parent) parent.invalidate(); }
    int imageWidth, imageHeight;
    Finished finished;
    private Annotations.Shape draft;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    AnnotationView(Context context) {
        super(context);
        setContentDescription("Drawing layer. Drag across the camera image to draw the selected shape.");
    }
    void cancelDraft() { draft = null; movingIndex = -1; movingOriginal = null; invalidate(); refreshMagnifier(); }
    private RectF imageRect() {
        if (imageWidth <= 0 || imageHeight <= 0) return new RectF();
        float scale = Math.min((float) getWidth() / imageWidth, (float) getHeight() / imageHeight);
        float width = imageWidth * scale, height = imageHeight * scale;
        return new RectF((getWidth() - width) / 2, (getHeight() - height) / 2,
            (getWidth() + width) / 2, (getHeight() + height) / 2);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF rect = imageRect();
        if (rect.isEmpty() || scene == null) return;
        List<Annotations.Shape> shapes = scene.snapshot();
        if (movingIndex >= 0 && movingIndex < shapes.size() && draft != null) shapes.set(movingIndex, draft);
        drawShapes(canvas, rect, shapes, paint);
        if (draft != null && movingIndex < 0) drawShapes(canvas, rect, List.of(draft), paint);
        Annotations.Shape selected = movingIndex >= 0 ? draft : selectedShape();
        if (editing && selected != null) {
            int saved = canvas.save(); canvas.clipRect(rect);
            float unit = getResources().getDisplayMetrics().density / getScaleX();
            paint.setStyle(Paint.Style.STROKE); paint.setColor(0xff40cfff); paint.setStrokeWidth(1.5f * unit);
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[] {5*unit, 4*unit}, 0));
            canvas.drawRect(rect.left + Math.min(selected.x1(), selected.x2()) * rect.width() - 5*unit,
                rect.top + Math.min(selected.y1(), selected.y2()) * rect.height() - 5*unit,
                rect.left + Math.max(selected.x1(), selected.x2()) * rect.width() + 5*unit,
                rect.top + Math.max(selected.y1(), selected.y2()) * rect.height() + 5*unit, paint);
            paint.setPathEffect(null); canvas.restoreToCount(saved);
        }
    }
    static void drawShapes(Canvas canvas, RectF rect, List<Annotations.Shape> shapes, Paint paint) {
        int save = canvas.save(); canvas.clipRect(rect);
        float unit = Math.min(rect.width(), rect.height());
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setTextSize(Math.max(12, unit * .035f));
        for (Annotations.Shape shape : shapes) {
            paint.setStrokeWidth(Math.max(1, unit * shape.strokeWidth()));
            float x1 = rect.left + shape.x1() * rect.width(), y1 = rect.top + shape.y1() * rect.height();
            float x2 = rect.left + shape.x2() * rect.width(), y2 = rect.top + shape.y2() * rect.height();
            paint.setColor(shape.color()); paint.setStyle(Paint.Style.STROKE);
            float left = Math.min(x1, x2), top = Math.min(y1, y2);
            if (shape.kind().equals("Line")) canvas.drawLine(x1, y1, x2, y2, paint);
            else if (shape.kind().equals("Rectangle")) canvas.drawRect(left, top, Math.max(x1, x2), Math.max(y1, y2), paint);
            else {
                float radius = Math.min(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 2;
                canvas.drawCircle((x1 + x2) / 2, (y1 + y2) / 2, radius, paint);
            }
            if (!shape.label().isEmpty()) {
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(unit * .003f, 0, 0, android.graphics.Color.BLACK);
                float labelX = Math.max(rect.left + 2, Math.min(left, rect.right - paint.measureText(shape.label()) - 2));
                float labelY = Math.max(rect.top + paint.getTextSize(), top - unit * .012f);
                canvas.drawText(shape.label(), labelX, labelY, paint);
                paint.clearShadowLayer();
            }
        }
        canvas.restoreToCount(save);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!editing || scene == null) return false;
        RectF rect = imageRect();
        if (rect.isEmpty()) return false;
        float x = Math.max(0, Math.min(1, (event.getX() - rect.left) / rect.width()));
        float y = Math.max(0, Math.min(1, (event.getY() - rect.top) / rect.height()));
        if (tool.equals("Move")) return moveObject(event, rect, x, y);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!rect.contains(event.getX(), event.getY())) return true;
                draft = new Annotations.Shape(tool, color, strokeWidth, x, y, x, y, ""); break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (draft == null) return true;
                draft = new Annotations.Shape(draft.kind(), draft.color(), draft.strokeWidth(), draft.x1(), draft.y1(), x, y, "");
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performClick();
                    if (Math.hypot((x - draft.x1()) * rect.width(), (y - draft.y1()) * rect.height()) > 8 && finished != null)
                        finished.drawn(draft);
                    draft = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL: draft = null; break;
            default: return true;
        }
        invalidate(); return true;
    }
    private boolean moveObject(MotionEvent event, RectF rect, float x, float y) {
        dragX = event.getX(); dragY = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelDraft();
                selection = null; selectedIndex = -1;
                List<Annotations.Shape> shapes = scene.snapshot();
                for (int i = shapes.size() - 1; i >= 0; --i) {
                    if (hit(shapes.get(i), rect, event.getX(), event.getY())) {
                        movingIndex = i; movingOriginal = shapes.get(i); draft = movingOriginal;
                        selectedIndex = i; selection = movingOriginal;
                        downX = x; downY = y; break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (movingOriginal != null) {
                    draft = movingOriginal.moved(x - downX, y - downY);
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        scene.replace(movingIndex, movingOriginal, draft);
                        selection = draft.equals(movingOriginal) ? movingOriginal : draft;
                        performClick(); cancelDraft();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL: cancelDraft(); break;
            default: break;
        }
        invalidate(); refreshMagnifier();
        if (selectionChanged != null) selectionChanged.run();
        return true;
    }
    private boolean hit(Annotations.Shape shape, RectF rect, float x, float y) {
        if (!rect.contains(x, y)) return false;
        float x1 = rect.left + shape.x1() * rect.width(), y1 = rect.top + shape.y1() * rect.height();
        float x2 = rect.left + shape.x2() * rect.width(), y2 = rect.top + shape.y2() * rect.height();
        float unit = Math.min(rect.width(), rect.height());
        float tolerance = Math.max(12 * getResources().getDisplayMetrics().density / getScaleX(), unit * shape.strokeWidth() / 2);
        float left = Math.min(x1, x2), top = Math.min(y1, y2);
        paint.setTextSize(Math.max(12, unit * .035f));
        float labelWidth = paint.measureText(shape.label());
        float labelX = Math.max(rect.left + 2, Math.min(left, rect.right - labelWidth - 2));
        float labelY = Math.max(rect.top + paint.getTextSize(), top - unit * .012f);
        if (!shape.label().isEmpty() && new RectF(labelX - tolerance, labelY - paint.getTextSize() - tolerance,
            labelX + labelWidth + tolerance, labelY + tolerance).contains(x, y)) return true;
        if (shape.kind().equals("Rectangle")) return new RectF(left - tolerance, top - tolerance,
            Math.max(x1, x2) + tolerance, Math.max(y1, y2) + tolerance).contains(x, y);
        if (shape.kind().equals("Circle")) return Math.hypot(x - (x1+x2)/2, y - (y1+y2)/2)
            <= Math.min(Math.abs(x2-x1), Math.abs(y2-y1))/2 + tolerance;
        float dx = x2-x1, dy = y2-y1, length = dx*dx+dy*dy;
        float t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x-x1)*dx+(y-y1)*dy)/length));
        return Math.hypot(x-x1-t*dx, y-y1-t*dy) <= tolerance;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
