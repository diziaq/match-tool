package org.example.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Source is a fresh-stream supplier. These pin that {@link Source#stream()} is re-iterable and that
 * {@link Source#map} stays lazy and re-iterable rather than materialising.
 */
class TestSource {

    @Test
    void streamIsFreshOnEachCall() {
        Source<Integer> source = () -> Stream.of(1, 2, 3);

        assertThat(source.stream().toList()).containsExactly(1, 2, 3);
        assertThat(source.stream().toList()).containsExactly(1, 2, 3);
    }

    @Test
    void mapAppliesFunctionToEachItem() {
        Source<Integer> source = () -> Stream.of(1, 2, 3);

        Source<String> mapped = source.map(n -> "n" + n);

        assertThat(mapped.stream().toList()).containsExactly("n1", "n2", "n3");
    }

    @Test
    void mapStaysReiterable() {
        Source<Integer> source = () -> Stream.of(1, 2);
        Source<String> mapped = source.map(String::valueOf);

        List<String> first = mapped.stream().toList();
        List<String> second = mapped.stream().toList();

        assertThat(first).isEqualTo(second).containsExactly("1", "2");
    }
}
