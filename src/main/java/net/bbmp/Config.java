package net.bbmp;

import java.util.*;

public class Config {
    public int listenPort = 25565;
    public String remoteHost = "play.hypixel.net";
    public int remotePort = 25565;
    public boolean devMode = false;
    static final Set<String> TRUEY = new HashSet<>(Arrays.asList("1","true","yes","on"));
    public static Config fromArgs(String[] args) {
        Config c = new Config();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            try {
                switch (a) {
                    case "-port": c.listenPort = Integer.parseInt(args[++i]); break;
                    case "-ip": c.remoteHost = args[++i]; break;
                    case "-rport": c.remotePort = Integer.parseInt(args[++i]); break;
                    case "-devmode": c.devMode = (i+1 < args.length) && TRUEY.contains(args[++i].toLowerCase()); break;
                    default: break;
                }
            } catch (Exception ignored) {}
        }
        return c;
    }
}