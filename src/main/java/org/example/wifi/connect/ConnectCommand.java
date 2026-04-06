package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

@FunctionalInterface
public interface ConnectCommand extends PlatformDependent {
    String build(String ssid, String password);

    static ConnectCommand of(Platform platform) {
        return switch (platform) {
            case MACOS -> (ssid, password) ->
                "networksetup -setairportnetwork en0 " + quoted(ssid) + " " + quoted(password);
            case LINUX -> (ssid, password) ->
                "nmcli dev wifi connect " + quoted(ssid) + " password " + quoted(password);
        };
    }

    private static String quoted(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
