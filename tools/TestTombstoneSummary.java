package com.magusview.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class TestTombstoneSummary {
    private static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((value & ~127L) != 0) { out.write((int) (value & 127) | 128); value >>>= 7; }
        out.write((int) value);
        return out.toByteArray();
    }
    private static byte[] join(byte[]... items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] item : items) out.writeBytes(item);
        return out.toByteArray();
    }
    private static byte[] number(int id, long value) { return join(varint((long) id << 3), varint(value)); }
    private static byte[] message(int id, byte[] value) { return join(varint(((long) id << 3) | 2), varint(value.length), value); }
    private static byte[] text(int id, String value) { return message(id, value.getBytes(StandardCharsets.UTF_8)); }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }

    public static void main(String[] args) throws Exception {
        byte[] frame = join(number(1, 0x1234), text(4, "memcpy"), number(5, 12), text(6, "libmagusview.so"), text(8, "abc123"));
        byte[] thread = join(number(1, 42), text(2, "camera-worker"), message(4, frame), text(5, "PRIVATE MEMORY OMIT"));
        byte[] otherThread = join(number(1, 43), text(2, "OTHER THREAD OMIT"));
        byte[] signal = join(text(2, "SIGSEGV"), text(4, "SEGV_MAPERR"), number(9, 0xfeed));
        byte[] proto = join(number(6, 42), message(10, signal), text(14, "sample abort"),
                message(15, text(1, "sample cause")), message(16, join(number(1, 43), message(2, otherThread))),
                message(16, join(number(1, 42), message(2, thread))), text(500, "UNKNOWN FIELD OMIT"));
        String report = TombstoneSummary.summarize(proto);
        check(report.contains("SIGSEGV / SEGV_MAPERR"));
        check(report.contains("feed") && report.contains("camera-worker"));
        check(report.contains("pc 0x1234 libmagusview.so (memcpy+12) BuildId=abc123"));
        check(report.contains("sample cause") && report.contains("sample abort"));
        check(!report.contains("OMIT"));
        check(TombstoneSummary.summarize(new byte[0]).contains("thread: 0"));
        for (byte[] invalid : new byte[][] {{(byte) 0x80}, {18, 127}, {0},
                {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                 (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 127}}) {
            boolean rejected = false;
            try { TombstoneSummary.summarize(invalid); } catch (IOException e) { rejected = true; }
            check(rejected);
        }
        System.out.println("PASS: native signal/backtrace extraction, fault-thread selection, memory omission, unknown fields, malformed inputs");
    }
}
