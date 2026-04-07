package org.example.wifi.scan;

import org.example.shell.ShellRunner;

class LinuxScannerProvider implements ScannerProvider {

    @Override
    public NetworkScanner create(ShellRunner shell) {
        return new LinuxNetworkScanner(shell);
    }
}
