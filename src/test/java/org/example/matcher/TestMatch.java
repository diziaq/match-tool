package org.example.matcher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestMatch {

    @Test
    void toStringFormat() {
        var match = new Match<>("left", "right");

        assertThat(match.toString()).isEqualTo("left <-> right");
    }

    @Test
    void leftAndRightGetters() {
        var match = new Match<>("network", "password");

        assertThat(match.left()).isEqualTo("network");
        assertThat(match.right()).isEqualTo("password");
    }

    @Test
    void equalityByValue() {
        var a = new Match<>("x", 42);
        var b = new Match<>("x", 42);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void inequalityWhenLeftDiffers() {
        assertThat(new Match<>("a", "v")).isNotEqualTo(new Match<>("b", "v"));
    }

    @Test
    void inequalityWhenRightDiffers() {
        assertThat(new Match<>("k", "x")).isNotEqualTo(new Match<>("k", "y"));
    }

    @Test
    void worksWithMixedTypes() {
        var match = new Match<>(1, true);

        assertThat(match.left()).isEqualTo(1);
        assertThat(match.right()).isEqualTo(true);
        assertThat(match.toString()).isEqualTo("1 <-> true");
    }
}
