package org.example.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pair is a plain carrier; the only behaviour worth pinning is its {@code a <-> b} rendering, which
 * the engine's match logging relies on.
 */
class TestPair {

    @Test
    void rendersAsArrowBetweenComponents() {
        assertThat(new Pair<>("HomeLab", "secret")).hasToString("HomeLab <-> secret");
    }

    @Test
    void exposesBothComponents() {
        var pair = new Pair<>(1, "x");

        assertThat(pair.a()).isEqualTo(1);
        assertThat(pair.b()).isEqualTo("x");
    }
}
