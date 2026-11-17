package com.magusview.android;

import java.nio.ByteBuffer;

/** All calls run on the single camera executor. No camera handles cross threads. */
final class CameraSdk {
    static { System.loadLibrary("magusview"); }
    static native String version();
    static native void enableWifi();
    /** Alternating camera display names and opaque SDK IDs. */
    static native String[] discover();
    static native int open(String id);
    static native int[] size();
    static native int start();
    /** Acquires one decoded frame; size() now describes that frame, not initial metadata. */
    static native int acquireFrame();
    /** Must run in a finally block after successful acquisition. */
    static native void releaseFrame();
    /** Copies the acquired frame without fetching another one. */
    static native int pull(ByteBuffer pixels);
    static native int pullPreview(ByteBuffer pixels, int width, int height);
    static native int event();
    /** disabled, min, max, default, current, HRESULT. */
    static native int[] control(int parameter);
    static native int setControl(int parameter, int value);
    /** Records the received camera stream to an app-private MP4; null finalizes recording. */
    static native int record(String path);
    static native void close();
}
