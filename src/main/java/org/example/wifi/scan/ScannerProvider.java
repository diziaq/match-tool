package org.example.wifi.scan;

import org.example.Platform;
import org.example.PlatformDependent;
import org.example.shell.ShellRunner;

/**
 * Platform-dependent factory that creates the appropriate {@link NetworkScanner}.
 *
 * <p>Obtained via {@link org.example.Platform#newScanner}; callers should not reference the
 * concrete implementations ({@code MacOsScannerProvider}, {@code LinuxScannerProvider}) directly.
 */
@FunctionalInterface
public interface ScannerProvider extends PlatformDependent {
    /** Creates a {@link NetworkScanner} that uses {@code shell} to run the OS scan command. */
    NetworkScanner create(ShellRunner shell);

    static ScannerProvider of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsScannerProvider();
            case LINUX -> new LinuxScannerProvider();
        };
    }
}
