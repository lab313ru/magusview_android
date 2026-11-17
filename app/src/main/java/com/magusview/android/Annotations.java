package com.magusview.android;

import java.util.ArrayList;
import java.util.List;

/** Camera-image coordinates, independent of preview size or orientation. */
final class Annotations {
    record Shape(String kind, int color, float strokeWidth, float x1, float y1, float x2, float y2, String label) {
        Shape moved(float dx, float dy) {
            dx = Math.max(-Math.min(x1, x2), Math.min(1 - Math.max(x1, x2), dx));
            dy = Math.max(-Math.min(y1, y2), Math.min(1 - Math.max(y1, y2), dy));
            return new Shape(kind, color, strokeWidth, x1 + dx, y1 + dy, x2 + dx, y2 + dy, label);
        }
    }
    private final ArrayList<Shape> shapes = new ArrayList<>();
    private final java.util.ArrayDeque<List<Shape>> history = new java.util.ArrayDeque<>();
    private void remember() {
        if (history.size() == 50) history.removeFirst();
        history.addLast(new ArrayList<>(shapes));
    }
    synchronized List<Shape> snapshot() { return new ArrayList<>(shapes); }
    synchronized void add(Shape shape) { remember(); shapes.add(shape); }
    synchronized void replace(int index, Shape original, Shape replacement) {
        if (index < 0 || index >= shapes.size() || !shapes.get(index).equals(original) || original.equals(replacement)) return;
        remember(); shapes.set(index, replacement);
    }
    synchronized void remove(int index, Shape expected) {
        if (index < 0 || index >= shapes.size() || shapes.get(index) != expected) return;
        remember(); shapes.remove(index);
    }
    synchronized void undo() {
        if (!history.isEmpty()) { shapes.clear(); shapes.addAll(history.removeLast()); }
    }
    synchronized void reset() { shapes.clear(); history.clear(); }
    synchronized void clear() { if (!shapes.isEmpty()) { remember(); shapes.clear(); } }
}
