/* Copyright (c) 2025 BlueBerryCodingGroup
 * Licensed under the MIT License
 * https://opensource.org/licenses/MIT
 */
package net.bbmp;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class App {
    public static void main(String[] args) throws Exception {
        Config cfg = Config.fromArgs(args);
        if (cfg.devMode) SwingUtilities.invokeLater(() -> { FlatDarkLaf.setup(); PacketMonitor.get().showWindow(); });
        new ProxyServer(cfg).start();
    }
}