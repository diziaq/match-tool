package org.example.wifi.scan;

import org.example.Platform;
import org.example.PlatformDependent;
import org.example.shell.ShellRunner;

@FunctionalInterface
public interface ScannerProvider extends PlatformDependent {
    NetworkScanner create(ShellRunner shell);

    static ScannerProvider of(Platform platform) {
        return switch (platform) {
            case MACOS -> MacOsNetworkScanner::new;
            case LINUX -> LinuxNetworkScanner::new;
        };
    }
}
