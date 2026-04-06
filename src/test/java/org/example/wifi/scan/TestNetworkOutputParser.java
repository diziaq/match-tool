package org.example.wifi.scan;

import org.example.wifi.Network;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestNetworkOutputParser {

    @Nested
    class MacOsParsing {

        @Test
        void parsesStandardOutput() {
            String output = """
                    HomeWiFi|-45
                    CoffeeShop|-67
                    Office5G|-32
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).ssid()).isEqualTo("HomeWiFi");
            assertThat(result.get(0).signal()).isEqualTo("-45 dBm");
        }

        @Test
        void appendsDbmSuffix() {
            List<Network> result = NetworkOutputParser.parseMacOs("Net|-72\n");

            assertThat(result.get(0).signal()).isEqualTo("-72 dBm");
        }

        @Test
        void skipsBlankSSIDs() {
            String output = """
                    RealNetwork|-50
                    |-80
                    AnotherOne|-60
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertThat(result).extracting(Network::ssid)
                .containsExactly("RealNetwork", "AnotherOne");
        }

        @Test
        void skipsLinesWithoutPipe() {
            String output = """
                    no pipe here
                    GoodNetwork|-55
                    """;
            List<Network> result = NetworkOutputParser.parseMacOs(output);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("GoodNetwork");
        }

        @Test
        void handlesEmptyOutput() {
            assertThat(NetworkOutputParser.parseMacOs("")).isEmpty();
        }

        @Test
        void handlesBlankOnlyOutput() {
            assertThat(NetworkOutputParser.parseMacOs("   \n   \n")).isEmpty();
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

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("GoodNetwork");
        }

        @Test
        void signalPartPreservedAsIs() {
            List<Network> result = NetworkOutputParser.parseMacOs("Net|-45\n");

            assertThat(result.get(0).signal()).isEqualTo("-45 dBm");
        }
    }

    @Nested
    class LinuxParsing {

        @Test
        void parsesNmcliOutput() {
            String output = """
                    MyRouter:85
                    Neighbor:42
                    Office:91
                    """;
            List<Network> result = NetworkOutputParser.parseLinux(output);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).ssid()).isEqualTo("MyRouter");
            assertThat(result.get(0).signal()).isEqualTo("85%");
        }

        @Test
        void appendsPercentSuffix() {
            List<Network> result = NetworkOutputParser.parseLinux("Router:77\n");

            assertThat(result.get(0).signal()).isEqualTo("77%");
        }

        @Test
        void skipsBlankSSIDs() {
            String output = """
                    Good:80
                    :50
                    AlsoGood:70
                    """;
            List<Network> result = NetworkOutputParser.parseLinux(output);

            assertThat(result).hasSize(2);
        }

        @Test
        void skipsLinesWithoutColon() {
            String output = """
                    no colon here
                    Valid:60
                    """;
            List<Network> result = NetworkOutputParser.parseLinux(output);

            assertThat(result).hasSize(1);
        }

        @Test
        void handlesEmptyOutput() {
            assertThat(NetworkOutputParser.parseLinux("")).isEmpty();
        }
    }

    @Nested
    class Deduplication {

        @Test
        void sortsByNameCaseInsensitive() {
            var nets = List.of(
                new Network("Zebra", "-30 dBm"),
                new Network("alpha", "-50 dBm"),
                new Network("Beta",  "-40 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).extracting(Network::ssid)
                .containsExactly("alpha", "Beta", "Zebra");
        }

        @Test
        void deduplicatesKeepingStrongestSignal() {
            var nets = List.of(
                new Network("Office", "-67 dBm"),
                new Network("Office", "-34 dBm"),
                new Network("Office", "-50 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).signal()).isEqualTo("-34 dBm");
        }

        @Test
        void deduplicatesCaseInsensitive() {
            var nets = List.of(
                new Network("office", "-60 dBm"),
                new Network("Office", "-40 dBm")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
        }

        @Test
        void deduplicatesKeepingStrongerPercentSignal() {
            var nets = List.of(
                new Network("Net", "50%"),
                new Network("Net", "80%"),
                new Network("Net", "30%")
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).signal()).isEqualTo("80%");
        }

        @Test
        void handlesEmptyList() {
            assertThat(NetworkOutputParser.deduplicated(List.of())).isEmpty();
        }

        @Test
        void singleNetwork_returnedAsIs() {
            var nets = List.of(new Network("Solo", "-50 dBm"));
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("Solo");
        }
    }
}
