package net.bbmp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public class PacketIO {
    static byte[] readFramedPacket(InputStream in) throws IOException {
        int[] lenInfo = readVarInt(in);
        if (lenInfo == null) return null;
        int length = lenInfo[0];
        if (length < 0 || length > 5000000) return null;
        byte[] payload = in.readNBytes(length);
        if (payload.length != length) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, length);
        out.write(payload);
        return out.toByteArray();
    }

    static int framedLen(byte[] framed) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(framed);
            int[] len = readVarInt(bin);
            return (len == null) ? -1 : len[0];
        } catch (Exception e) {
            return -1;
        }
    }

    static int peekPacketId(byte[] framed) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(framed);
            int[] len = readVarInt(bin);
            if (len == null) return -1;
            int L = len[0];
            if (L < 0 || L > 5000000) return -1;
            byte[] payload = bin.readNBytes(L);
            if (payload.length != L) return -1;
            ByteArrayInputStream pin = new ByteArrayInputStream(payload);
            int[] pid = readVarInt(pin);
            return (pid == null) ? -1 : pid[0];
        } catch (Exception e) {
            return -1;
        }
    }

    static int peekHandshakeNextState(byte[] framed) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(framed);
        int[] lenI = readVarInt(bin);
        if (lenI == null) return -1;
        int length = lenI[0];
        if (length < 0 || length > 5000000) return -1;
        byte[] payload = bin.readNBytes(length);
        if (payload.length != length) return -1;

        ByteArrayInputStream pin = new ByteArrayInputStream(payload);
        int[] pidI = readVarInt(pin);
        if (pidI == null || pidI[0] != 0x00) return -1;
        int[] proto = readVarInt(pin);
        if (proto == null) return -1;
        String addr = readMCString(pin);
        if (addr == null) return -1;
        int b1 = pin.read(), b2 = pin.read();
        if (b1 < 0 || b2 < 0) return -1;
        int[] state = readVarInt(pin);
        if (state == null) return -1;
        return state[0];
    }

    static byte[] tryRewriteHandshakeServerAddress(byte[] framed, String newAddress) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(framed);
            int[] lenI = readVarInt(bin);
            if (lenI == null) return framed;
            int length = lenI[0];
            if (length < 0 || length > 5000000) return framed;
            byte[] payload = bin.readNBytes(length);
            if (payload.length != length) return framed;

            ByteArrayInputStream pin = new ByteArrayInputStream(payload);
            int[] pidI = readVarInt(pin);
            if (pidI == null || pidI[0] != 0x00) return framed;

            int[] proto = readVarInt(pin);
            if (proto == null) return framed;
            String addr = readMCString(pin);
            if (addr == null) return framed;
            int b1 = pin.read(), b2 = pin.read();
            if (b1 < 0 || b2 < 0) return framed;
            int port = ((b1 & 0xFF) << 8) | (b2 & 0xFF);
            int[] stateI = readVarInt(pin);
            if (stateI == null) return framed;

            ByteArrayOutputStream pout = new ByteArrayOutputStream();
            writeVarInt(pout, 0x00);
            writeVarInt(pout, proto[0]);
            writeMCString(pout, newAddress);
            pout.write((port >> 8) & 0xFF);
            pout.write(port & 0xFF);
            writeVarInt(pout, stateI[0]);

            byte[] newPayload = pout.toByteArray();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeVarInt(out, newPayload.length);
            out.write(newPayload);
            return out.toByteArray();
        } catch (Exception e) {
            return framed;
        }
    }

    static byte[] rewriteStatusJson(byte[] framed, String upstreamHost, String replacement) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(framed);
            int[] lenI = readVarInt(bin);
            if (lenI == null) return null;
            int length = lenI[0];
            if (length < 0 || length > 5000000) return null;
            byte[] payload = bin.readNBytes(length);
            if (payload.length != length) return null;

            ByteArrayInputStream pin = new ByteArrayInputStream(payload);
            int[] pidI = readVarInt(pin);
            if (pidI == null) return null;
            if (pidI[0] != 0x00) return null;
            String json = readMCString(pin);
            if (json == null) return null;

            String scrubbed = scrubHostInString(json, upstreamHost, replacement);
            scrubbed = scrubIPsToLocalhost(scrubbed);

            if (json.equals(scrubbed)) return framed;

            ByteArrayOutputStream pout = new ByteArrayOutputStream();
            writeVarInt(pout, 0x00);
            writeMCString(pout, scrubbed);
            byte[] newPayload = pout.toByteArray();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeVarInt(out, newPayload.length);
            out.write(newPayload);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    static Thread rawPipe(InputStream in, OutputStream out, String tag) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[8192];
            try {
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                    out.flush();
                    PacketMonitor.add(tag, "RAW", -1, r, "bytes", Tag.RAW);
                }
            } catch (IOException ignored) {
            } finally {
                closeQuietly(out);
                closeQuietly(in);
            }
        }, "bbmp-" + (tag.equals("C→S") ? "c2s" : "s2c"));
        t.setDaemon(true);
        t.start();
        return t;
    }

    static int[] readVarInt(InputStream in) throws IOException {
        int numRead = 0, result = 0, read;
        do {
            read = in.read();
            if (read == -1) return null;
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) return null;
        } while ((read & 0x80) != 0);
        return new int[]{result, numRead};
    }

    static void writeVarInt(OutputStream out, int value) throws IOException {
        do {
            int temp = value & 0x7F;
            value >>>= 7;
            if (value != 0) temp |= 0x80;
            out.write(temp);
        } while (value != 0);
    }

    static String readMCString(InputStream in) throws IOException {
        int[] lenI = readVarInt(in);
        if (lenI == null) return null;
        int len = lenI[0];
        if (len < 0 || len > 5000000) return null;
        byte[] data = in.readNBytes(len);
        if (data.length != len) return null;
        return new String(data, StandardCharsets.UTF_8);
    }

    static void writeMCString(OutputStream out, String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, data.length);
        out.write(data);
    }

    // FIXED: escape backslashes in regex patterns
    static final Pattern IPV4 = Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(?:\\.|$)){4}\\b");
    static final Pattern IPV6 = Pattern.compile("\\b([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}\\b");

    static String scrubHostInString(String s, String upstreamHost, String replacement) {
        String out = s.replace(upstreamHost, replacement);
        out = out.replaceAll("(?i)\\b" + Pattern.quote(upstreamHost) + "\\b", replacement);
        return out;
    }

    static String scrubIPsToLocalhost(String s) {
        s = IPV4.matcher(s).replaceAll("localhost");
        s = IPV6.matcher(s).replaceAll("localhost");
        return s;
    }

    static void closeQuietly(Closeable c) {
        try { if (c != null) c.close(); } catch (IOException ignored) {}
    }
}