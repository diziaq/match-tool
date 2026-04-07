package org.example.wifi.scan;

import org.example.shell.ShellRunner;

class MacOsScannerProvider implements ScannerProvider {

    @Override
    public NetworkScanner create(ShellRunner shell) {
        return new MacOsNetworkScanner(shell);
    }
}
