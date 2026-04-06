package org.example.wifi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {

    @Test
    void getters() {
        var net = new Network("HomeWiFi", "-45 dBm");

        assertEquals("HomeWiFi", net.ssid());
        assertEquals("-45 dBm", net.signal());
    }

    @Test
    void equalityByValue() {
        assertEquals(new Network("A", "-50 dBm"), new Network("A", "-50 dBm"));
    }

    @Test
    void inequalityWhenSsidDiffers() {
        assertNotEquals(new Network("A", "-50 dBm"), new Network("B", "-50 dBm"));
    }

    @Test
    void inequalityWhenSignalDiffers() {
        assertNotEquals(new Network("A", "-50 dBm"), new Network("A", "-60 dBm"));
    }

    @Test
    void compareToIsCaseInsensitive() {
        var a = new Network("apple", "-50 dBm");
        var b = new Network("Apple", "-40 dBm");

        assertEquals(0, a.compareTo(b));
    }

    @Test
    void compareToOrdering() {
        var alpha = new Network("Alpha", "-50 dBm");
        var beta  = new Network("Beta",  "-40 dBm");
        var zebra = new Network("Zebra", "-30 dBm");

        assertTrue(alpha.compareTo(beta) < 0);
        assertTrue(beta.compareTo(zebra) < 0);
        assertTrue(zebra.compareTo(alpha) > 0);
    }

    @Test
    void sortingWithCompareTo() {
        var nets = new ArrayList<>(List.of(
            new Network("Zebra", "-30 dBm"),
            new Network("alpha", "-50 dBm"),
            new Network("Beta",  "-40 dBm")
        ));
        nets.sort(null);

        assertEquals("alpha", nets.get(0).ssid());
        assertEquals("Beta",  nets.get(1).ssid());
        assertEquals("Zebra", nets.get(2).ssid());
    }
}
