package org.example.matcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    @Test
    void toStringFormat() {
        var match = new Match<>("left", "right");

        assertEquals("left <-> right", match.toString());
    }

    @Test
    void leftAndRightGetters() {
        var match = new Match<>("network", "password");

        assertEquals("network", match.left());
        assertEquals("password", match.right());
    }

    @Test
    void equalityByValue() {
        var a = new Match<>("x", 42);
        var b = new Match<>("x", 42);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityWhenLeftDiffers() {
        assertNotEquals(new Match<>("a", "v"), new Match<>("b", "v"));
    }

    @Test
    void inequalityWhenRightDiffers() {
        assertNotEquals(new Match<>("k", "x"), new Match<>("k", "y"));
    }

    @Test
    void worksWithMixedTypes() {
        var match = new Match<>(1, true);

        assertEquals(1, match.left());
        assertEquals(true, match.right());
        assertEquals("1 <-> true", match.toString());
    }
}
