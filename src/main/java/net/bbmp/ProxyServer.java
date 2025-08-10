/* Copyright (c) 2025 BlueBerryCodingGroup
 * Licensed under the MIT License
 * https://opensource.org/licenses/MIT
 */
package net.bbmp;

import java.net.*;
import java.util.concurrent.*;

public class ProxyServer {
    private final Config cfg;
    public ProxyServer(Config cfg) { this.cfg = cfg; }
    public void start() throws Exception {
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress("0.0.0.0", cfg.listenPort));
        ExecutorService pool = Executors.newCachedThreadPool();
        while (true) pool.submit(new ConnectionHandler(ss.accept(), cfg));
    }
}