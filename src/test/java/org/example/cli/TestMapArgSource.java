package org.example.cli;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TestMapArgSource {

    @Test
    void loadsValuesFromMap() {
        Map<String, String> raw = new MapArgSource(
            Map.of("a", "1", "b", "2")).load();

        assertThat(raw)
            .containsEntry("a", "1")
            .containsEntry("b", "2");
    }

    @Test
    void emptyMap() {
        assertThat(new MapArgSource(Map.of()).load()).isEmpty();
    }

    @Test
    void defensiveCopyAtConstruction() {
        Map<String, String> original = new HashMap<>();
        original.put("a", "1");

        MapArgSource source = new MapArgSource(original);

        original.put("a", "changed");
        original.put("b", "new");

        Map<String, String> loaded = source.load();
        assertThat(loaded).containsEntry("a", "1");
        assertThat(loaded).doesNotContainKey("b");
    }

    @Test
    void defensiveCopyOnLoad() {
        MapArgSource source = new MapArgSource(Map.of("a", "1"));

        Map<String, String> first = source.load();
        first.put("b", "injected");

        Map<String, String> second = source.load();
        assertThat(second).doesNotContainKey("b");
    }
}
