package org.example;

import org.example.io.Logger;
import org.example.shell.ShellRunner;
import org.example.wifi.connect.NetworkConnector;
import org.example.wifi.connect.ShellNetworkConnector;
import org.example.wifi.scan.NetworkScanner;
import org.example.wifi.scan.ScannerProvider;

/**
 * Detects the current operating system and provides factories for all OS-dependent components.
 * Only macOS and Linux are supported; any other OS causes an immediate {@link UnsupportedOperationException}.
 */
public enum Platform {
    MACOS, LINUX;

    /**
     * Detects the OS from the {@code os.name} system property.
     *
     * @throws UnsupportedOperationException if the OS is neither macOS nor Linux
     */
    public static Platform detect() {
        String name = System.getProperty("os.name", "");
        String lower = name.toLowerCase();
        if (lower.contains("mac"))   return MACOS;
        if (lower.contains("linux")) return LINUX;
        throw new UnsupportedOperationException(
            "Unsupported OS: \"" + name + "\" — only macOS and Linux are supported");
    }

    /**
     * Resolves a {@link PlatformDependent} implementation for this platform by invoking
     * the {@code static T of(Platform)} method on the given interface reflectively.
     */
    public <T extends PlatformDependent> T resolve(Class<T> type) {
        try {
            return type.cast(type.getMethod("of", Platform.class).invoke(null, this));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                type.getSimpleName() + " must declare a static of(Platform) method", e);
        }
    }

    /** Creates a platform-appropriate {@link NetworkScanner} backed by the given shell runner. */
    public NetworkScanner newScanner(ShellRunner shell) {
        return resolve(ScannerProvider.class).create(shell);
    }

    /** Creates a platform-appropriate {@link NetworkConnector} backed by the given shell runner and logger. */
    public NetworkConnector newConnector(ShellRunner shell, Logger logger) {
        return new ShellNetworkConnector(shell, this, logger);
    }
}
