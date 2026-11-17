package com.magusview.android;
public class TestFocusTarget {
    public static void main(String[] args) {
        int grid = (7 << 16) | 7;
        if (FocusTarget.zone(0, 0, grid) != 0 || FocusTarget.zone(1, 1, grid) != 48
                || FocusTarget.zone(.5f, .5f, grid) != 24) throw new AssertionError("Grid mapping");
        if (FocusTarget.zone(Float.NaN, .5f, grid) != -1 || FocusTarget.zone(.5f, .5f, 0) != -1)
            throw new AssertionError("Invalid grid/input");
        if (FocusTarget.point(100, 0, 1080, 1920, 1280, 720) != null) throw new AssertionError("Letterbox accepted");
        float[] center = FocusTarget.point(540, 960, 1080, 1920, 1280, 720);
        if (center == null || Math.abs(center[0] - .5) > .001 || Math.abs(center[1] - .5) > .001)
            throw new AssertionError("Portrait mapping");
        if (FocusTarget.point(0, 400, 1920, 1080, 720, 1280) != null) throw new AssertionError("Pillarbox accepted");
        System.out.println("PASS: focus grid center/edges, invalid coordinates and letterbox mapping");
    }
}
