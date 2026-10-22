package org.example.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * CachingSource pre-loads only within an explicit bound. The key guarantees: a small delegate is
 * read once and replayed, while a delegate that exceeds the bound (or is effectively infinite) is
 * never materialised and is re-read lazily instead. The probe reads at most {@code maxCached + 1}
 * items, which is what keeps an infinite delegate safe.
 */
class TestCachingSource {

    @Nested
    class WithinBound {

        @Test
        void readsDelegateOnceThenReplaysFromCache() {
            AtomicInteger opens = new AtomicInteger();
            Source<Integer> delegate = () -> {
                opens.incrementAndGet();
                return Stream.of(1, 2, 3);
            };
            var caching = new CachingSource<>(delegate, 10);

            assertThat(caching.stream().toList()).containsExactly(1, 2, 3);
            assertThat(caching.stream().toList()).containsExactly(1, 2, 3);

            assertThat(opens).hasValue(1);
        }
    }

    @Nested
    class OverBound {

        /** A delegate larger than the bound must not be cached; every use re-reads it. */
        @Test
        void reReadsDelegateWhenItExceedsBound() {
            AtomicInteger opens = new AtomicInteger();
            Source<Integer> delegate = () -> {
                opens.incrementAndGet();
                return Stream.of(1, 2, 3, 4, 5);
            };
            var caching = new CachingSource<>(delegate, 3);

            assertThat(caching.stream().toList()).containsExactly(1, 2, 3, 4, 5);
            assertThat(caching.stream().toList()).containsExactly(1, 2, 3, 4, 5);

            // one probe + two real reads
            assertThat(opens).hasValue(3);
        }

        /** The probe must stop at maxCached+1 items so an infinite delegate does not hang. */
        @Test
        void probesAtMostBoundPlusOneItemsFromInfiniteDelegate() {
            AtomicInteger drawn = new AtomicInteger();
            Source<Integer> infinite = () -> Stream.iterate(1, n -> n + 1).peek(n -> drawn.incrementAndGet());
            var caching = new CachingSource<>(infinite, 4);

            long firstFive = caching.stream().limit(5).count();

            assertThat(firstFive).isEqualTo(5);
            // probe drew 5 (bound+1) to decide; the limit(5) consume drew 5 more
            assertThat(drawn).hasValue(10);
        }
    }

    @Nested
    class Validation {

        @Test
        void rejectsNullDelegate() {
            assertThatThrownBy(() -> new CachingSource<>(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delegate");
        }

        @Test
        void rejectsNonPositiveBound() {
            assertThatThrownBy(() -> new CachingSource<>(() -> Stream.of(1), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxCached");
        }
    }
}
