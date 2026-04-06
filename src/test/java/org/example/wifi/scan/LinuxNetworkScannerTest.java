package org.example.wifi.scan;

import org.example.wifi.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinuxNetworkScannerTest {

    @Test
    void scan_parsesShellOutputIntoNetworks() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "HomeWiFi:75\nOffice:90\n");

        List<Network> result = scanner.scan();

        assertEquals(2, result.size());
        assertEquals("HomeWiFi", result.get(0).ssid());
        assertEquals("75%", result.get(0).signal());
    }

    @Test
    void scan_runsCorrectNmcliCommand() throws Exception {
        String[] capturedCmd = new String[1];
        var scanner = new LinuxNetworkScanner(cmd -> { capturedCmd[0] = cmd; return ""; });

        scanner.scan();

        assertTrue(capturedCmd[0].contains("nmcli"));
        assertTrue(capturedCmd[0].contains("SSID,SIGNAL"));
    }

    @Test
    void scan_deduplicatesResults() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "Net:50\nNet:80\nNet:30\n");

        List<Network> result = scanner.scan();

        assertEquals(1, result.size());
        assertEquals("80%", result.get(0).signal());
    }

    @Test
    void scan_returnsEmptyListForBlankOutput() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "");

        assertTrue(scanner.scan().isEmpty());
    }

    @Test
    void scan_sortsByNameCaseInsensitive() throws Exception {
        var scanner = new LinuxNetworkScanner(cmd -> "Zebra:50\nalpha:60\nBeta:70\n");

        List<Network> result = scanner.scan();

        assertEquals("alpha", result.get(0).ssid());
        assertEquals("Beta",  result.get(1).ssid());
        assertEquals("Zebra", result.get(2).ssid());
    }
}
