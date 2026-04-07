package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

@FunctionalInterface
public interface ConnectCommand extends PlatformDependent {
    String build(String ssid, String password);

    static ConnectCommand of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsConnectCommand();
            case LINUX -> new LinuxConnectCommand();
        };
    }
}
