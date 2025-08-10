/* Copyright (c) 2025 BlueBerryCodingGroup
 * Licensed under the MIT License
 * https://opensource.org/licenses/MIT
 */
package net.bbmp;

import java.io.*;
import java.net.*;

public class ConnectionHandler implements Runnable {
    private final Socket client;
    private final Config cfg;
    public ConnectionHandler(Socket client, Config cfg) { this.client = client; this.cfg = cfg; }
    public void run() {
        String clientAddr = client.getRemoteSocketAddress().toString();
        PacketMonitor.log("connect " + clientAddr);
        try (Socket upstream = new Socket()) {
            upstream.connect(new InetSocketAddress(cfg.remoteHost, cfg.remotePort), 10000);
            upstream.setTcpNoDelay(true);
            client.setTcpNoDelay(true);
            InputStream cIn = client.getInputStream();
            OutputStream cOut = client.getOutputStream();
            InputStream sIn = upstream.getInputStream();
            OutputStream sOut = upstream.getOutputStream();
            ConnectionState state = new ConnectionState();
            byte[] first = PacketIO.readFramedPacket(cIn);
            if (first == null) { PacketMonitor.log("disconnect " + clientAddr); PacketIO.closeQuietly(client); return; }
            try { int ns = PacketIO.peekHandshakeNextState(first); if (ns != -1) state.nextState = ns; } catch (Exception ignored) {}
            byte[] rewritten = PacketIO.tryRewriteHandshakeServerAddress(first, cfg.remoteHost);
            if (rewritten != first) PacketMonitor.add("C→S", "Handshake", 0x00, PacketIO.framedLen(first), "server_addr→" + cfg.remoteHost, Tag.HANDSHAKE);
            sOut.write(rewritten); sOut.flush();
            if (state.nextState == 1) {
                byte[] statusReply = PacketIO.readFramedPacket(sIn);
                if (statusReply != null) {
                    int pid = PacketIO.peekPacketId(statusReply);
                    String name = (pid == 0x00) ? "Status Response" : String.format("Unknown(0x%02X)", pid);
                    byte[] scrubbed = (pid == 0x00) ? PacketIO.rewriteStatusJson(statusReply, cfg.remoteHost, "localhost") : null;
                    if (scrubbed != null) { PacketMonitor.add("S→C", name, pid, PacketIO.framedLen(scrubbed), "scrubbed", Tag.HANDSHAKE); cOut.write(scrubbed); }
                    else { PacketMonitor.add("S→C", name, pid, PacketIO.framedLen(statusReply), "pass", Tag.HANDSHAKE); cOut.write(statusReply); }
                    cOut.flush();
                }
                PacketIO.closeQuietly(cOut); PacketIO.closeQuietly(sIn); PacketIO.closeQuietly(sOut); PacketIO.closeQuietly(cIn);
                return;
            }
            Thread t1 = PacketIO.rawPipe(cIn, sOut, "C→S");
            Thread t2 = PacketIO.rawPipe(sIn, cOut, "S→C");
            t1.join(); t2.join();
        } catch (Exception e) {
            PacketMonitor.log("error " + e.getMessage());
        } finally {
            PacketIO.closeQuietly(client);
            PacketMonitor.log("disconnect " + clientAddr);
        }
    }
    static class ConnectionState { volatile int nextState = -1; }
}