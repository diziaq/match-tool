package org.example.wifi.scan;

import org.example.wifi.Network;
import org.example.wifi.Strength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Drives the macOS scanner with a fake shell so no real {@code swift} scan runs. The scanner
 * extracts the bundled {@code wifi_scan.swift} resource and runs it; here the shell just returns
 * canned pipe-delimited output. macOS signals are negative dBm, so the strongest signal is the
 * highest value. The sibling {@code TestLinuxNetworkScanner} owns the nmcli (percent) side.
 */
class TestMacOsNetworkScanner {

    @Nested
    class BaseCases {

        @Test
        void parsesPipeDelimitedOutputIntoNetworks() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "HomeWiFi|-45\nOffice|-70\n");

            List<Network> result = scanner.scan();

            assertThat(result).extracting(Network::ssid).containsExactly("HomeWiFi", "Office");
            assertThat(result.get(0).strength().value()).isEqualTo(-45);
            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void runsSwiftScanCommand() throws Exception {
            String[] capturedCmd = new String[1];
            var scanner = new MacOsNetworkScanner(cmd -> { capturedCmd[0] = cmd; return ""; });

            scanner.scan();

            assertThat(capturedCmd[0]).startsWith("swift ").contains("wifi_scan");
        }

        @Test
        void sortsByNameCaseInsensitive() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "Zebra|-50\nalpha|-60\nBeta|-70\n");

            List<Network> result = scanner.scan();

            assertThat(result).extracting(Network::ssid).containsExactly("alpha", "Beta", "Zebra");
        }
    }

    @Nested
    class Deduplication {

        /** Same SSID from several access points collapses to the strongest (highest dBm). */
        @Test
        void deduplicatesKeepingStrongestSignal() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "Net|-80\nNet|-45\nNet|-90\n");

            List<Network> result = scanner.scan();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).strength().value()).isEqualTo(-45);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void returnsEmptyListForBlankOutput() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "");

            assertThat(scanner.scan()).isEmpty();
        }

        @Test
        void skipsLineWithoutPipeDelimiter() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "HomeWiFi -45\nOffice|-70\n");

            List<Network> result = scanner.scan();

            assertThat(result).extracting(Network::ssid).containsExactly("Office");
        }

        @Test
        void skipsLineWithNonNumericSignal() throws Exception {
            var scanner = new MacOsNetworkScanner(cmd -> "HomeWiFi|strong\nOffice|-70\n");

            List<Network> result = scanner.scan();

            assertThat(result).extracting(Network::ssid).containsExactly("Office");
        }
    }
}
