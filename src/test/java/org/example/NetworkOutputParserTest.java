package org.example;

import org.example.wifi.Network;
import org.example.wifi.scan.NetworkOutputParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkOutputParserTest {

    @Nested
    class MacOsParsingTests {

        @Test
        void parsesStandardOutput() {
            String output = """
                    HomeWiFi|-45
                    CoffeeShop|-67
                    Office5G|-32
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertEquals(3, result.size());
            assertEquals("HomeWiFi", result.get(0).ssid());
            assertEquals("-45 dBm", result.get(0).signal());
        }

        @Test
        void skipsBlankSSIDs() {
            String output = """
                    RealNetwork|-50
                    |-80
                    AnotherOne|-60
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertEquals(2, result.size());
            assertEquals("RealNetwork", result.get(0).ssid());
            assertEquals("AnotherOne", result.get(1).ssid());
        }

        @Test
        void handlesEmptyOutput() {
            assertTrue(NetworkOutputParser.parseMacOs("").isEmpty());
        }

        @Test
        void handlesGarbageLines() {
            String output = """
                    some random log line
                    WARNING: something
                    GoodNetwork|-55
                    another bad line
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertEquals(1, result.size());
            assertEquals("GoodNetwork", result.get(0).ssid());
        }
    }

    @Nested
    class LinuxParsingTests {

        @Test
        void parsesNmcliOutput() {
            String output = """
                    MyRouter:85
                    Neighbor:42
                    Office:91
                    """;
            List<Network> result = NetworkOutputParser.parseLinux(output);

            assertEquals(3, result.size());
            assertEquals("MyRouter", result.get(0).ssid());
            assertEquals("85%", result.get(0).signal());
        }

        @Test
        void skipsBlankSSIDs() {
            String output = """
                    Good:80
                    :50
                    AlsoGood:70
                    """;
            List<Network> result = NetworkOutputParser.parseLinux(output);

            assertEquals(2, result.size());
        }

        @Test
        void handlesEmptyOutput() {
            assertTrue(NetworkOutputParser.parseLinux("").isEmpty());
        }
    }

    @Nested
    class DeduplicationTests {

        @Test
        void sortsByNameCaseInsensitive() {
            var nets = List.of(
                new Network("Zebra", "-30 dBm"),
                new Network("alpha", "-50 dBm"),
                new Network("Beta", "-40 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertEquals("alpha", result.get(0).ssid());
            assertEquals("Beta", result.get(1).ssid());
            assertEquals("Zebra", result.get(2).ssid());
        }

        @Test
        void deduplicatesKeepingStrongestSignal() {
            var nets = List.of(
                new Network("Office", "-67 dBm"),
                new Network("Office", "-34 dBm"),
                new Network("Office", "-50 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertEquals(1, result.size());
            assertEquals("-34 dBm", result.get(0).signal());
        }

        @Test
        void deduplicatesCaseInsensitive() {
            var nets = List.of(
                new Network("office", "-60 dBm"),
                new Network("Office", "-40 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertEquals(1, result.size());
        }

        @Test
        void handlesEmptyList() {
            assertTrue(NetworkOutputParser.deduplicated(List.of()).isEmpty());
        }
    }
}
