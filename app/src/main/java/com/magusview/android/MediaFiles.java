package com.magusview.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Publishes completed files atomically through MediaStore; requires no broad storage access. */
final class MediaFiles {
    private interface Writer { void write(OutputStream output) throws IOException; }

    static Uri photo(Context context, Bitmap bitmap) throws IOException {
        return publish(context, false, output -> {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) throw new IOException("JPEG encoding failed");
        });
    }

    static Uri video(Context context, File file) throws IOException {
        awaitVideoReady(file);
        return publish(context, true, output -> {
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
        });
    }

    // SDK Record(null) may return before a finalized MP4 is readable. Keep the
    // foreground service alive while checking, and never publish failed validation.
    static void awaitVideoReady(File file) throws IOException {
        long deadline = android.os.SystemClock.elapsedRealtime() + 10000;
        IOException last = new IOException("Recording contains no data");
        int attempt = 0;
        while (true) {
            long bytes = file.length();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            String duration = null, width = null;
            try {
                if (bytes == 0) throw new IOException("Recording contains no data");
                retriever.setDataSource(file.getAbsolutePath());
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                if (duration == null || Long.parseLong(duration) <= 0 || width == null || Integer.parseInt(width) <= 0)
                    throw new IOException("Recording has no playable video frames");
                if (file.length() != bytes) throw new IOException("Recording is still being written");
                CrashDiagnostics.record("Validated video " + file.getName() + "; bytes=" + bytes
                    + "; durationMs=" + duration + "; width=" + width + "; retries=" + attempt);
                return;
            } catch (IOException | RuntimeException e) {
                last = new IOException("Recording not ready: " + e.getMessage(), e);
                if (attempt % 4 == 0) CrashDiagnostics.record("Waiting for MP4 finalization: " + file.getName()
                    + "; bytes=" + bytes + "; durationMs=" + duration + "; width=" + width);
            } finally { retriever.release(); }
            if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                CrashDiagnostics.record("MP4 validation timed out: " + file.getName() + "; bytes=" + file.length());
                throw last;
            }
            ++attempt;
            try { Thread.sleep(250); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Video validation interrupted", e); }
        }
    }

    private static Uri publish(Context context, boolean video, Writer writer) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "MagusView_" + stamp + (video ? ".mp4" : ".jpg"));
        values.put(MediaStore.MediaColumns.MIME_TYPE, video ? "video/mp4" : "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, video ? "Movies/MagusView" : "Pictures/MagusView");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = resolver.insert(collection, values);
        if (uri == null) throw new IOException("Gallery could not create a media item");
        boolean published = false;
        try {
            try (OutputStream output = resolver.openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Gallery output is unavailable");
                writer.write(output);
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(uri, ready, null, null) != 1) throw new IOException("Gallery could not publish the media item");
            published = true;
            return uri;
        } finally {
            if (!published) {
                try { resolver.delete(uri, null, null); } catch (RuntimeException ignored) { /* Preserve the original failure. */ }
            }
        }
    }
}
