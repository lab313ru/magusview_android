package com.magusview.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Reads only crash metadata/backtraces, never memory dumps, from Android's tombstone proto.
 * Schema: android.googlesource.com/platform/system/core/+/refs/heads/main/debuggerd/proto/tombstone.proto
 */
final class TombstoneSummary {
    private record Field(int number, long value, byte[] data, int start, int end) {
        String text() { return new String(data, start, Math.min(end - start, 4096), StandardCharsets.UTF_8); }
        List<Field> children() throws IOException { return parse(data, start, end); }
    }

    static String summarize(byte[] data) throws IOException {
        List<Field> root = parse(data, 0, data.length);
        StringBuilder out = new StringBuilder();
        long tid = number(root, 6);
        out.append("Native crash thread: ").append(tid).append('\n');
        for (Field field : root) {
            if (field.number == 10) {
                List<Field> signal = field.children();
                out.append("Signal: ").append(text(signal, 2)).append(" / ").append(text(signal, 4));
                out.append("; fault address 0x").append(Long.toHexString(number(signal, 9))).append('\n');
            } else if (field.number == 14) out.append("Abort: ").append(field.text()).append('\n');
            else if (field.number == 15) out.append("Cause: ").append(text(field.children(), 1)).append('\n');
        }
        for (Field field : root) {
            if (field.number != 16) continue;
            List<Field> entry = field.children();
            if (number(entry, 1) != tid) continue;
            for (Field value : entry) {
                if (value.number != 2) continue;
                List<Field> thread = value.children();
                out.append("Thread name: ").append(text(thread, 2)).append('\n');
                int index = 0;
                for (Field frame : thread) {
                    if (frame.number != 4 || index >= 40) continue;
                    List<Field> trace = frame.children();
                    out.append('#').append(index++).append(" pc 0x").append(Long.toHexString(number(trace, 1)))
                            .append(' ').append(text(trace, 6)).append(" (").append(text(trace, 4))
                            .append('+').append(number(trace, 5)).append(") BuildId=").append(text(trace, 8)).append('\n');
                }
            }
        }
        return out.toString();
    }

    private static long number(List<Field> fields, int id) {
        for (Field field : fields) if (field.number == id) return field.value;
        return 0;
    }
    private static String text(List<Field> fields, int id) {
        for (Field field : fields) if (field.number == id) return field.text();
        return "";
    }
    private static long varint(byte[] data, int[] cursor, int end) throws IOException {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (cursor[0] >= end) throw new IOException("Truncated protobuf");
            int value = data[cursor[0]++] & 255;
            if (shift == 63 && value > 1) throw new IOException("Overflowing protobuf integer");
            result |= (long) (value & 127) << shift;
            if ((value & 128) == 0) return result;
        }
        throw new IOException("Invalid protobuf integer");
    }
    private static List<Field> parse(byte[] data, int start, int end) throws IOException {
        List<Field> result = new ArrayList<>();
        int[] cursor = {start};
        while (cursor[0] < end) {
            long key = varint(data, cursor, end);
            int id = (int) (key >>> 3), wire = (int) (key & 7);
            if (id <= 0) throw new IOException("Invalid protobuf field");
            if (wire == 0) {
                result.add(new Field(id, varint(data, cursor, end), data, cursor[0], cursor[0]));
            } else {
                long length;
                if (wire == 1) length = 8;
                else if (wire == 5) length = 4;
                else if (wire == 2) length = varint(data, cursor, end);
                else throw new IOException("Unsupported protobuf wire type " + wire);
                if (length < 0 || length > end - cursor[0]) throw new IOException("Invalid protobuf length");
                int next = cursor[0] + (int) length;
                result.add(new Field(id, 0, data, cursor[0], next));
                cursor[0] = next;
            }
            if (result.size() > 100000) throw new IOException("Too many protobuf fields");
        }
        return result;
    }
}
