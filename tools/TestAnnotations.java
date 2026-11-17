package com.magusview.android;

public class TestAnnotations {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    static void near(float a, float b) { check(Math.abs(a-b) < .0001f); }
    public static void main(String[] args) {
        Annotations scene = new Annotations();
        Annotations.Shape a = new Annotations.Shape("Line", 0xffff0000, .012f, .8f, .7f, .2f, .1f, "test");
        scene.add(a);
        var snapshot = scene.snapshot();
        var moved = a.moved(1, -1);
        near(moved.x1(), 1); near(moved.x2(), .4f);
        near(moved.y1(), .6f); near(moved.y2(), 0);
        check(moved.label().equals(a.label())); near(moved.strokeWidth(), .012f);
        scene.replace(0, a, moved);
        check(snapshot.get(0).equals(a)); check(scene.snapshot().get(0).equals(moved));
        // A stale gesture must not overwrite a subsequent edit.
        scene.replace(0, a, a.moved(.1f, .1f)); check(scene.snapshot().get(0).equals(moved));
        scene.undo(); check(scene.snapshot().get(0).equals(a));
        scene.clear(); check(scene.snapshot().isEmpty());
        scene.undo(); check(scene.snapshot().get(0).equals(a));
        scene.undo(); check(scene.snapshot().isEmpty());
        scene.undo(); check(scene.snapshot().isEmpty());
        for (int i=0;i<60;i++) scene.add(a);
        for (int i=0;i<60;i++) scene.undo();
        check(scene.snapshot().size()==10);
        scene.add(a); scene.reset(); scene.undo(); check(scene.snapshot().isEmpty());
        check(snapshot.get(0).equals(a)); // A captured photo's snapshot survives reset.
        scene.add(a); scene.undo(); check(scene.snapshot().isEmpty());
        scene.add(a); scene.add(moved);
        scene.remove(0, moved); check(scene.snapshot().size()==2);
        scene.remove(0, a); check(scene.snapshot().size()==1 && scene.snapshot().get(0).equals(moved));
        scene.undo(); check(scene.snapshot().size()==2 && scene.snapshot().get(0).equals(a));
        System.out.println("PASS: movement bounds, label/width preservation, immutable capture snapshot, stale move, undo move/clear, bounded history");
    }
}
