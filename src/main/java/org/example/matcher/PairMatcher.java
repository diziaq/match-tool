package org.example.matcher;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PairMatcher<L, R> {

    public void match(
        Collection<L> lefts,
        Stream<R> rights,
        BiFunction<L, R, Boolean> predicate,
        Consumer<Match<L, R>> onMatch
    ) {
        rights.forEach(right ->
            lefts.forEach(left -> {
                if (predicate.apply(left, right)) {
                    onMatch.accept(new Match<>(left, right));
                }
            })
        );
    }
}
