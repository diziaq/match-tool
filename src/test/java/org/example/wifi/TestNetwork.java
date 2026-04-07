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
            var net = Network.of("HomeWiFi", -45);

            assertThat(net.ssid()).isEqualTo("HomeWiFi");
            assertThat(net.strength()).isInstanceOf(Strength.Best.class);
            assertThat(net.strength().value()).isEqualTo(-45);
        }

        @Test
        void equalityByValue() {
            assertThat(Network.of("A", -50)).isEqualTo(Network.of("A", -50));
        }

        @Test
        void inequalityWhenSsidDiffers() {
            assertThat(Network.of("A", -50)).isNotEqualTo(Network.of("B", -50));
        }

        @Test
        void inequalityWhenStrengthDiffers() {
            assertThat(Network.of("A", -50)).isNotEqualTo(Network.of("A", -60));
        }
    }

    @Nested
    class StrengthClassification {

        // dBm thresholds
        @Test
        void dbm_bestAtOrAboveMinusSeventySeven() {
            assertThat(Network.of("n", -67).strength()).isInstanceOf(Strength.Best.class);
            assertThat(Network.of("n", -45).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void dbm_normalBetweenMinusEightyAndMinusSixtyEight() {
            assertThat(Network.of("n", -68).strength()).isInstanceOf(Strength.Normal.class);
            assertThat(Network.of("n", -80).strength()).isInstanceOf(Strength.Normal.class);
        }

        @Test
        void dbm_weakBelowMinusEighty() {
            assertThat(Network.of("n", -81).strength()).isInstanceOf(Strength.Weak.class);
            assertThat(Network.of("n", -95).strength()).isInstanceOf(Strength.Weak.class);
        }

        // % thresholds
        @Test
        void percent_bestAtOrAboveSeventy() {
            assertThat(Network.of("n", 70).strength()).isInstanceOf(Strength.Best.class);
            assertThat(Network.of("n", 90).strength()).isInstanceOf(Strength.Best.class);
        }

        @Test
        void percent_normalBetweenFortyAndSixtyNine() {
            assertThat(Network.of("n", 40).strength()).isInstanceOf(Strength.Normal.class);
            assertThat(Network.of("n", 69).strength()).isInstanceOf(Strength.Normal.class);
        }

        @Test
        void percent_weakBelowForty() {
            assertThat(Network.of("n", 39).strength()).isInstanceOf(Strength.Weak.class);
            assertThat(Network.of("n", 10).strength()).isInstanceOf(Strength.Weak.class);
        }

        @Test
        void strengthStoresRawValue() {
            assertThat(Network.of("n", -55).strength().value()).isEqualTo(-55);
            assertThat(Network.of("n", 75).strength().value()).isEqualTo(75);
        }
    }

    @Nested
    class Accessibility {

        @Test
        void bestIsAccessible() {
            assertThat(Network.of("n", -45).isAccessible()).isTrue();
        }

        @Test
        void normalIsAccessible() {
            assertThat(Network.of("n", -75).isAccessible()).isTrue();
        }

        @Test
        void weakIsNotAccessible() {
            assertThat(Network.of("n", -90).isAccessible()).isFalse();
        }

        @Test
        void unknownSignalFactory_treatedAsAccessible() {
            // Networks loaded from a file have no real signal — treated as Normal(0)
            assertThat(Network.of("TargetSSID").isAccessible()).isTrue();
        }
    }

    @Nested
    class Ordering {

        @Test
        void compareToIsCaseInsensitive() {
            assertThat(Network.of("apple", -50).compareTo(Network.of("Apple", -40))).isZero();
        }

        @Test
        void compareToOrdering() {
            var alpha = Network.of("Alpha", -50);
            var beta  = Network.of("Beta",  -40);
            var zebra = Network.of("Zebra", -30);

            assertThat(alpha.compareTo(beta)).isNegative();
            assertThat(beta.compareTo(zebra)).isNegative();
            assertThat(zebra.compareTo(alpha)).isPositive();
        }

        @Test
        void sortingWithCompareTo() {
            var nets = new ArrayList<>(List.of(
                Network.of("Zebra", -30),
                Network.of("alpha", -50),
                Network.of("Beta",  -40)
            ));
            nets.sort(null);

            assertThat(nets).extracting(Network::ssid)
                .containsExactly("alpha", "Beta", "Zebra");
        }

        @Test
        void sameNameDifferentCase_sortsAsEqual() {
            assertThat(Network.of("office", -60).compareTo(Network.of("Office", -40))).isZero();
        }
    }
}
