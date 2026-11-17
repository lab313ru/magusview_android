package com.magusview.android;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/** Joins the camera's video-only MP4 segments without decoding or re-encoding. */
final class VideoSegments {
    static File join(List<File> files) throws IOException {
        if (files.isEmpty()) throw new IOException("No recorded segments");
        if (files.size() == 1) return files.get(0);
        File output = File.createTempFile("MagusView_joined_", ".mp4", files.get(0).getParentFile());
        boolean complete = false;
        MediaMuxer muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        try {
            MediaFormat first = null;
            int target = -1;
            long offset = 0;
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            for (File file : files) {
                MediaExtractor source = new MediaExtractor();
                try {
                    source.setDataSource(file.getAbsolutePath());
                    int track = -1;
                    for (int i = 0; i < source.getTrackCount(); i++) {
                        String mime = source.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
                        if (mime != null && mime.startsWith("video/")) track = i;
                        else if (mime != null && mime.startsWith("audio/")) throw new IOException("Unexpected audio track; segments retained");
                    }
                    if (track < 0) throw new IOException("Segment has no video track");
                    MediaFormat format = source.getTrackFormat(track);
                    source.selectTrack(track);
                    // A resumed stream can begin before the next keyframe. Discard only
                    // those undecodable leading samples, never insert the paused time.
                    while (source.getSampleTime() >= 0 && (source.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) == 0)
                        source.advance();
                    if (source.getSampleTime() < 0) continue; // Very short empty segment.
                    if (first == null) {
                        first = format;
                        target = muxer.addTrack(format);
                        muxer.start();
                    } else requireCompatible(first, format);
                    long base = source.getSampleTime(), last = -1, step = 33333;
                    while (source.getSampleTime() >= 0) {
                        long size = source.getSampleSize();
                        if (size < 0 || size > 64 * 1024 * 1024) throw new IOException("Invalid encoded frame size");
                        if (buffer.capacity() < size) buffer = ByteBuffer.allocateDirect((int) size);
                        buffer.clear();
                        int count = source.readSampleData(buffer, 0);
                        if (count < 0) break;
                        long time = source.getSampleTime() - base;
                        if (time < 0 || (last >= 0 && time <= last)) throw new IOException("Unsupported reordered video timestamps; segments retained");
                        if (last >= 0) step = time - last;
                        info.set(0, count, offset + time,
                            (source.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0 ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
                        muxer.writeSampleData(target, buffer, info);
                        last = time;
                        source.advance();
                    }
                    if (last >= 0) offset += last + step;
                } finally { source.release(); }
            }
            if (target < 0) throw new IOException("No playable recorded frames");
            info.set(0, 0, offset, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            muxer.writeSampleData(target, ByteBuffer.allocateDirect(0), info);
            muxer.stop();
            complete = true;
            return output;
        } catch (RuntimeException e) { throw new IOException("Cannot join recording segments", e); }
        finally {
            muxer.release();
            if (!complete && !output.delete()) CrashDiagnostics.record("Incomplete joined file retained: " + output.getName());
        }
    }

    private static void requireCompatible(MediaFormat a, MediaFormat b) throws IOException {
        if (!Objects.equals(a.getString(MediaFormat.KEY_MIME), b.getString(MediaFormat.KEY_MIME))
                || a.getInteger(MediaFormat.KEY_WIDTH) != b.getInteger(MediaFormat.KEY_WIDTH)
                || a.getInteger(MediaFormat.KEY_HEIGHT) != b.getInteger(MediaFormat.KEY_HEIGHT))
            throw new IOException("Camera video format changed during recording");
        for (String key : new String[] { "csd-0", "csd-1", "csd-2" }) {
            if (!Objects.equals(a.getByteBuffer(key), b.getByteBuffer(key)))
                throw new IOException("Camera codec changed during recording");
        }
    }
}
