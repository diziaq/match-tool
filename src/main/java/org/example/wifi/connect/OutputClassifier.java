package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

@FunctionalInterface
public interface OutputClassifier extends PlatformDependent {
    ConnectOutcome classify(String output);

    static OutputClassifier of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsOutputClassifier();
            case LINUX -> new LinuxOutputClassifier();
        };
    }
}
