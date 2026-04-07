package org.example.wifi.scan;

import org.example.shell.ShellRunner;

/** Creates {@link LinuxNetworkScanner} instances. Resolved via {@link org.example.Platform#LINUX}. */
class LinuxScannerProvider implements ScannerProvider {

    @Override
    public NetworkScanner create(ShellRunner shell) {
        return new LinuxNetworkScanner(shell);
    }
}
