package org.example.engine;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A re-iterable supplier of items for the {@link PairingEngine}.
 *
 * <p>{@link #stream()} returns a <em>fresh</em> stream each time it is called, so a source can be
 * traversed more than once — the engine re-opens the inner source once per outer item. The stream
 * is consumed lazily and may be arbitrarily large or effectively infinite; callers must close it
 * (the engine does, through try-with-resources). Nothing in the engine materialises a whole source;
 * see {@link CachingSource} for the only, bounded, opt-in pre-load.
 *
 * <p>This is a functional interface, so a lazy file source is simply
 * {@code Source<String> s = () -> Files.lines(path)}.
 *
 * @param <T> the item type
 */
@FunctionalInterface
public interface Source<T> {

    /** Returns a fresh, lazily-consumed, closeable stream of this source's items. */
    Stream<T> stream();

    /**
     * Returns a source whose items are {@code f} applied to this source's items. The mapping is
     * lazy and preserves re-iterability: each {@link #stream()} call maps a fresh underlying stream.
     */
    default <R> Source<R> map(Function<? super T, ? extends R> f) {
        return () -> stream().map(f);
    }
}
