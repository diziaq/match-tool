package org.example.wifi.scan;

import org.example.shell.ShellRunner;

public class NetworkScannerFactory {

    public static NetworkScanner forCurrentOs(ShellRunner shell) {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") ? new MacOsNetworkScanner(shell) : new LinuxNetworkScanner(shell);
    }
}
