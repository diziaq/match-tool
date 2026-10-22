package org.example.engine;

import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link Source} decorator that pre-loads its delegate once, but only within an explicit bound.
 *
 * <p>Pre-loading is the exception, not the rule: the engine is built to consume sources lazily so it
 * never depends on holding a whole source in memory. Use this only when the inner source is known to
 * be small and re-reading it per outer item is wasteful.
 *
 * <p>On first use it probes at most {@code maxCached + 1} items:
 * <ul>
 *   <li>if the delegate fits within {@code maxCached}, those items are cached and every
 *       {@link #stream()} replays them from memory;</li>
 *   <li>if the delegate exceeds the bound (or is effectively infinite), nothing is cached and every
 *       {@link #stream()} re-reads the delegate lazily.</li>
 * </ul>
 * Either way memory stays bounded by {@code maxCached}, and an unbounded source is never
 * materialised.
 */
public final class CachingSource<T> implements Source<T> {

    /** Sane default bound when the caller does not specify one. */
    public static final int DEFAULT_MAX_CACHED = 10_000;

    private final Source<T> delegate;
    private final int maxCached;

    private boolean probed;
    private List<T> cache; // non-null only when the delegate fit within maxCached

    public CachingSource(Source<T> delegate, int maxCached) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        if (maxCached < 1) throw new IllegalArgumentException("maxCached must be >= 1, got: " + maxCached);
        this.delegate = delegate;
        this.maxCached = maxCached;
    }

    /** Caches up to {@link #DEFAULT_MAX_CACHED} items. */
    public static <T> CachingSource<T> of(Source<T> delegate) {
        return new CachingSource<>(delegate, DEFAULT_MAX_CACHED);
    }

    @Override
    public Stream<T> stream() {
        if (!probed) {
            probe();
        }
        return cache != null ? cache.stream() : delegate.stream();
    }

    private void probe() {
        List<T> head;
        try (Stream<T> s = delegate.stream()) {
            head = s.limit((long) maxCached + 1).toList();
        }
        if (head.size() <= maxCached) {
            cache = head;
        }
        probed = true;
    }
}
