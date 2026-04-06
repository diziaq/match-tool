package org.example.wifi.scan;

import org.example.shell.ShellRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkScannerFactoryTest {

    private String originalOsName;
    private final ShellRunner noopShell = cmd -> "";

    @BeforeEach
    void saveOsName() {
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void restoreOsName() {
        System.setProperty("os.name", originalOsName);
    }

    @Test
    void returnsMacOsScanner_forMacOsX() {
        System.setProperty("os.name", "Mac OS X");

        assertInstanceOf(MacOsNetworkScanner.class, NetworkScannerFactory.forCurrentOs(noopShell));
    }

    @Test
    void returnsMacOsScanner_forMacOs() {
        System.setProperty("os.name", "macOS");

        assertInstanceOf(MacOsNetworkScanner.class, NetworkScannerFactory.forCurrentOs(noopShell));
    }

    @Test
    void returnsLinuxScanner_forLinux() {
        System.setProperty("os.name", "Linux");

        assertInstanceOf(LinuxNetworkScanner.class, NetworkScannerFactory.forCurrentOs(noopShell));
    }

    @Test
    void returnsLinuxScanner_forUnknownOs() {
        System.setProperty("os.name", "Windows 11");

        assertInstanceOf(LinuxNetworkScanner.class, NetworkScannerFactory.forCurrentOs(noopShell));
    }

    @Test
    void returnsLinuxScanner_forEmptyOsName() {
        System.setProperty("os.name", "");

        assertInstanceOf(LinuxNetworkScanner.class, NetworkScannerFactory.forCurrentOs(noopShell));
    }
}
