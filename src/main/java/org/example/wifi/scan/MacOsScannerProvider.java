package org.example.wifi.scan;

import org.example.shell.ShellRunner;

/** Creates {@link MacOsNetworkScanner} instances. Resolved via {@link org.example.Platform#MACOS}. */
class MacOsScannerProvider implements ScannerProvider {

    @Override
    public NetworkScanner create(ShellRunner shell) {
        return new MacOsNetworkScanner(shell);
    }
}
