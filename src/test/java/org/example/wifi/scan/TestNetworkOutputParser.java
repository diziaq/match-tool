package org.example.wifi.scan;

import org.example.wifi.Network;
import org.example.wifi.Strength;
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
            assertThat(result.get(0).strength().value()).isEqualTo(-45);
            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void classifiesSignalStrength() {
            // -45 → Best, -75 → Normal, -90 → Weak
            List<Network> result = NetworkOutputParser.parseMacOs("A|-45\nB|-75\nC|-90\n");

            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
            assertThat(result.get(1).strength()).isInstanceOf(Strength.Normal.class);
            assertThat(result.get(2).strength()).isInstanceOf(Strength.Weak.class);
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
        void skipsLinesWithNonIntegerSignal() {
            // Lines with pipe but unparseable signal are silently dropped
            List<Network> result = NetworkOutputParser.parseMacOs("Good|-55\nBad|notANumber\n");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("Good");
        }

        @Test
        void handlesEmptyOutput() {
            assertThat(NetworkOutputParser.parseMacOs("")).isEmpty();
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
            assertThat(result.get(0).strength().value()).isEqualTo(85);
            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void classifiesSignalStrength() {
            // 85% → Best, 50% → Normal, 20% → Weak
            List<Network> result = NetworkOutputParser.parseLinux("A:85\nB:50\nC:20\n");

            assertThat(result.get(0).strength()).isInstanceOf(Strength.Best.class);
            assertThat(result.get(1).strength()).isInstanceOf(Strength.Normal.class);
            assertThat(result.get(2).strength()).isInstanceOf(Strength.Weak.class);
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
        void skipsLinesWithNonIntegerSignal() {
            List<Network> result = NetworkOutputParser.parseLinux("Good:75\nBad:notANumber\n");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("Good");
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
                Network.of("Zebra", -30),
                Network.of("alpha", -50),
                Network.of("Beta",  -40)
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).extracting(Network::ssid)
                .containsExactly("alpha", "Beta", "Zebra");
        }

        @Test
        void deduplicatesKeepingStrongestDbmSignal() {
            var nets = List.of(
                Network.of("Office", -67),
                Network.of("Office", -34),
                Network.of("Office", -50)
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).strength().value()).isEqualTo(-34);
        }

        @Test
        void deduplicatesCaseInsensitive() {
            var nets = List.of(
                Network.of("office", -60),
                Network.of("Office", -40)
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
        }

        @Test
        void deduplicatesKeepingStrongestPercentSignal() {
            var nets = List.of(
                Network.of("Net", 50),
                Network.of("Net", 80),
                Network.of("Net", 30)
            );
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).strength().value()).isEqualTo(80);
        }

        @Test
        void handlesEmptyList() {
            assertThat(NetworkOutputParser.deduplicated(List.of())).isEmpty();
        }

        @Test
        void singleNetwork_returnedAsIs() {
            var nets = List.of(Network.of("Solo", -50));
            List<Network> result = NetworkOutputParser.deduplicated(nets);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ssid()).isEqualTo("Solo");
        }
    }
}
