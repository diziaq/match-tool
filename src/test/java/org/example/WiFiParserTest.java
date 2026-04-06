package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WiFiParserTest {

    @Nested
    class MacParsingTests {

        @Test
        void parsesStandardOutput() {
            String output = """
                    HomeWiFi|-45
                    CoffeeShop|-67
                    Office5G|-32
                    """;
            List<WiFiNetwork> result = WiFiParser.parseMacOutput(output);

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
            List<WiFiNetwork> result = WiFiParser.parseMacOutput(output);

            assertEquals(2, result.size());
            assertEquals("RealNetwork", result.get(0).ssid());
            assertEquals("AnotherOne", result.get(1).ssid());
        }

        @Test
        void handlesEmptyOutput() {
            List<WiFiNetwork> result = WiFiParser.parseMacOutput("");
            assertTrue(result.isEmpty());
        }

        @Test
        void handlesGarbageLines() {
            String output = """
                    some random log line
                    WARNING: something
                    GoodNetwork|-55
                    another bad line
                    """;
            List<WiFiNetwork> result = WiFiParser.parseMacOutput(output);

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
            List<WiFiNetwork> result = WiFiParser.parseLinuxOutput(output);

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
            List<WiFiNetwork> result = WiFiParser.parseLinuxOutput(output);

            assertEquals(2, result.size());
        }

        @Test
        void handlesEmptyOutput() {
            assertTrue(WiFiParser.parseLinuxOutput("").isEmpty());
        }
    }

    @Nested
    class DedupAndSortTests {

        @Test
        void sortsByNameCaseInsensitive() {
            var nets = List.of(
                new WiFiNetwork("Zebra", "-30 dBm"),
                new WiFiNetwork("alpha", "-50 dBm"),
                new WiFiNetwork("Beta", "-40 dBm")
            );
            List<WiFiNetwork> result = WiFiParser.dedupAndSort(nets);

            assertEquals("alpha", result.get(0).ssid());
            assertEquals("Beta", result.get(1).ssid());
            assertEquals("Zebra", result.get(2).ssid());
        }

        @Test
        void deduplicatesKeepingStrongestSignal() {
            var nets = List.of(
                new WiFiNetwork("Office", "-67 dBm"),
                new WiFiNetwork("Office", "-34 dBm"),
                new WiFiNetwork("Office", "-50 dBm")
            );
            List<WiFiNetwork> result = WiFiParser.dedupAndSort(nets);

            assertEquals(1, result.size());
            assertEquals("-34 dBm", result.get(0).signal());
        }

        @Test
        void deduplicatesCaseInsensitive() {
            var nets = List.of(
                new WiFiNetwork("office", "-60 dBm"),
                new WiFiNetwork("Office", "-40 dBm")
            );
            List<WiFiNetwork> result = WiFiParser.dedupAndSort(nets);

            assertEquals(1, result.size());
        }

        @Test
        void handlesEmptyList() {
            assertTrue(WiFiParser.dedupAndSort(List.of()).isEmpty());
        }
    }
}
