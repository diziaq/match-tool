package org.example.wifi;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestNetwork {

    @Nested
    class RecordContract {

        @Test
        void getters() {
            var net = new Network("HomeWiFi", "-45 dBm");

            assertThat(net.ssid()).isEqualTo("HomeWiFi");
            assertThat(net.signal()).isEqualTo("-45 dBm");
        }

        @Test
        void equalityByValue() {
            assertThat(new Network("A", "-50 dBm")).isEqualTo(new Network("A", "-50 dBm"));
        }

        @Test
        void inequalityWhenSsidDiffers() {
            assertThat(new Network("A", "-50 dBm")).isNotEqualTo(new Network("B", "-50 dBm"));
        }

        @Test
        void inequalityWhenSignalDiffers() {
            assertThat(new Network("A", "-50 dBm")).isNotEqualTo(new Network("A", "-60 dBm"));
        }
    }

    @Nested
    class Ordering {

        @Test
        void compareToIsCaseInsensitive() {
            var a = new Network("apple", "-50 dBm");
            var b = new Network("Apple", "-40 dBm");

            assertThat(a.compareTo(b)).isZero();
        }

        @Test
        void compareToOrdering() {
            var alpha = new Network("Alpha", "-50 dBm");
            var beta  = new Network("Beta",  "-40 dBm");
            var zebra = new Network("Zebra", "-30 dBm");

            assertThat(alpha.compareTo(beta)).isNegative();
            assertThat(beta.compareTo(zebra)).isNegative();
            assertThat(zebra.compareTo(alpha)).isPositive();
        }

        @Test
        void sortingWithCompareTo() {
            var nets = new ArrayList<>(List.of(
                new Network("Zebra", "-30 dBm"),
                new Network("alpha", "-50 dBm"),
                new Network("Beta",  "-40 dBm")
            ));
            nets.sort(null);

            assertThat(nets).extracting(Network::ssid)
                .containsExactly("alpha", "Beta", "Zebra");
        }

        // Opinion: case-insensitive ordering means "Office" and "office" sort as equal neighbours;
        // the relative order between them is unspecified, but both must appear adjacent in a sorted
        // list — deduplicated() relies on this to collapse them with a TreeMap.
        @Test
        void sameNameDifferentCase_sortsAsEqual() {
            var lower = new Network("office", "-60 dBm");
            var upper = new Network("Office", "-40 dBm");

            assertThat(lower.compareTo(upper)).isZero();
            assertThat(upper.compareTo(lower)).isZero();
        }
    }
}
