package com.magusview.android;

/** View-space zoom; source image and annotation coordinates remain unchanged. */
final class PreviewZoom {
    float scale = 1, x, y;
    void reset() { scale = 1; x = y = 0; }
    void pinch(float factor, float oldX, float oldY, float newX, float newY) {
        if (!Float.isFinite(factor) || factor <= 0) return;
        float next = Math.max(1, Math.min(8, scale * factor));
        float ratio = next / scale;
        x = newX - (oldX - x) * ratio;
        y = newY - (oldY - y) * ratio;
        scale = next;
    }
    void constrain(float viewWidth, float viewHeight, float imageWidth, float imageHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) return;
        float fit = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        float maxX = Math.max(0, (imageWidth * fit * scale - viewWidth) / 2);
        float maxY = Math.max(0, (imageHeight * fit * scale - viewHeight) / 2);
        x = Math.max(-maxX, Math.min(maxX, x));
        y = Math.max(-maxY, Math.min(maxY, y));
    }
}
