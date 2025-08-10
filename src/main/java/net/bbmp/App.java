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