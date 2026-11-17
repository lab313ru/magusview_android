package com.magusview.android;

/** Reserve room for joining segments and publishing a copy without deleting sources. */
final class RecordingStorage {
    static final long RESERVE = 512L * 1024 * 1024;
    static final long WARNING = 1024L * 1024 * 1024;
    static long required(long bytes, int parts) {
        long copies = parts > 1 ? 2 : 1;
        bytes = Math.max(0, bytes);
        return bytes > (Long.MAX_VALUE - RESERVE) / copies
            ? Long.MAX_VALUE : RESERVE + bytes * copies;
    }
    static boolean mustStop(long free, long bytes, int parts) {
        return free >= 0 && free <= required(bytes, parts);
    }
    static boolean low(long free, long bytes, int parts) {
        return free >= 0 && free - required(bytes, parts) <= WARNING;
    }
}
