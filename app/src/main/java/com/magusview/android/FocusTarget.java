package com.magusview.android;

final class FocusTarget {
    static float[] point(float x, float y, int viewWidth, int viewHeight, int imageWidth, int imageHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) return null;
        float scale = Math.min((float) viewWidth / imageWidth, (float) viewHeight / imageHeight);
        float width = imageWidth * scale, height = imageHeight * scale;
        float nx = (x - (viewWidth - width) / 2) / width;
        float ny = (y - (viewHeight - height) / 2) / height;
        return nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1 ? new float[] {nx, ny} : null;
    }
    static int zone(float x, float y, int packedGrid) {
        int columns = packedGrid >>> 16, rows = packedGrid & 65535;
        if (!(x >= 0 && x <= 1 && y >= 0 && y <= 1) || columns == 0 || rows == 0
                || (long) columns * rows > Integer.MAX_VALUE) return -1;
        return Math.min(rows - 1, (int) (y * rows)) * columns + Math.min(columns - 1, (int) (x * columns));
    }
}
