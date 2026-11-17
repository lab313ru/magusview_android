package com.magusview.android;

public class TestPreviewZoom {
    static void near(float a, float b) { if (Math.abs(a-b) > .001f) throw new AssertionError(a + " != " + b); }
    public static void main(String[] args) {
        PreviewZoom z = new PreviewZoom();
        // The image point under the pinch stays under the fingers as they move.
        z.pinch(2, 100, 50, 120, 70);
        near(z.scale, 2); near(z.x, -80); near(z.y, -30);
        near((120-z.x)/z.scale, 100); near((70-z.y)/z.scale, 50);
        // Wide image in a tall view: keep vertical letterboxing centered.
        z.constrain(1000, 1000, 1600, 900);
        near(z.x, -80); near(z.y, -30);
        z.x=9999; z.y=-9999;
        z.constrain(1000, 1000, 1600, 900);
        near(z.x, 500); near(z.y, -62.5f);
        z.pinch(100,0,0,0,0);near(z.scale,8);
        z.pinch(.001f,0,0,0,0);near(z.scale,1);
        z.constrain(1000,1000,1600,900);near(z.x,0);near(z.y,0);
        z.pinch(Float.NaN,0,0,0,0);near(z.scale,1);
        z.pinch(3,0,0,50,80);z.reset();near(z.scale,1);near(z.x,0);near(z.y,0);
        System.out.println("PASS: pinch anchor, inverse coordinates, pan bounds, zoom limits, reset");
    }
}
