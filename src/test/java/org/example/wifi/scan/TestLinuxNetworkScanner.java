package org.example.wifi.scan;

import org.example.wifi.Network;
import org.example.wifi.Strength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestLinuxNetworkScanner {

    @Nested
    class BaseCases {

        @Test
        void scan_parsesShellOutputIntoNetworks() throws Exception {
            var scanner = new LinuxNetworkScanner(cmd -> "HomeWiFi:75\nOffice:90\n");

            List<Network> result = scanner.scan();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).ssid()).isEqualTo("HomeWiFi");
            assertThat(result.get(0).strength().value()).isEqualTo(75);
            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void scan_runsCorrectNmcliCommand() throws Exception {
            String[] capturedCmd = new String[1];
            var scanner = new LinuxNetworkScanner(cmd -> { capturedCmd[0] = cmd; return ""; });

            scanner.scan();

            assertThat(capturedCmd[0]).contains("nmcli").contains("SSID,SIGNAL");
        }

        @Test
        void scan_sortsByNameCaseInsensitive() throws Exception {
            var scanner = new LinuxNetworkScanner(cmd -> "Zebra:50\nalpha:60\nBeta:70\n");

            List<Network> result = scanner.scan();

            assertThat(result).extracting(Network::ssid)
                .containsExactly("alpha", "Beta", "Zebra");
        }
    }

    @Nested
    class Deduplication {

        @Test
        void scan_deduplicatesKeepingStrongestSignal() throws Exception {
            var scanner = new LinuxNetworkScanner(cmd -> "Net:50\nNet:80\nNet:30\n");

            List<Network> result = scanner.scan();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).strength().value()).isEqualTo(80);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void scan_returnsEmptyListForBlankOutput() throws Exception {
            var scanner = new LinuxNetworkScanner(cmd -> "");

            assertThat(scanner.scan()).isEmpty();
        }

        // Opinion: nmcli uses ':' as delimiter so SSIDs containing ':' cause the signal field to
        // be non-numeric ("HQ") which now causes the whole line to be silently skipped rather
        // than producing a truncated entry. Pinning this behaviour documents the limitation.
        @Test
        void scan_ssidWithColonInName_lineIsSkipped() throws Exception {
            var scanner = new LinuxNetworkScanner(cmd -> "Corp:HQ:75\n");

            List<Network> result = scanner.scan();

            // "Corp:HQ:75".split(":") → ["Corp","HQ","75"]; parts[1]="HQ" is not an integer
            assertThat(result).isEmpty();
        }
    }
}
