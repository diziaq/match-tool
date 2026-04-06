package org.example.wifi.scan;

import org.example.wifi.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestLinuxNetworkScanner {

    @Test
    void scan_parsesShellOutputIntoNetworks() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "HomeWiFi:75\nOffice:90\n");

        List<Network> result = scanner.scan();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ssid()).isEqualTo("HomeWiFi");
        assertThat(result.get(0).signal()).isEqualTo("75%");
    }

    @Test
    void scan_runsCorrectNmcliCommand() throws Exception {
        String[] capturedCmd = new String[1];
        var scanner = new LinuxNetworkScanner(cmd -> { capturedCmd[0] = cmd; return ""; });

        scanner.scan();

        assertThat(capturedCmd[0]).contains("nmcli").contains("SSID,SIGNAL");
    }

    @Test
    void scan_deduplicatesResults() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "Net:50\nNet:80\nNet:30\n");

        List<Network> result = scanner.scan();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).signal()).isEqualTo("80%");
    }

    @Test
    void scan_returnsEmptyListForBlankOutput() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "");

        assertThat(scanner.scan()).isEmpty();
    }

    @Test
    void scan_sortsByNameCaseInsensitive() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "Zebra:50\nalpha:60\nBeta:70\n");

        List<Network> result = scanner.scan();

        assertThat(result).extracting(Network::ssid)
            .containsExactly("alpha", "Beta", "Zebra");
    }
}
