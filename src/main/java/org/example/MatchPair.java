package org.example;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MatchPair {

    private final FileLog traceLog;
    private final FileLog successLog;

    MatchPair() {
        var time = LocalDateTime.now();
        this.traceLog = new FileLog(time, "trace");
        this.successLog = new FileLog(time, "success");
    }

    public record Pair(String left, String right) {
        @Override
        public String toString() {
            return left + " <-> " + right;
        }
    }

    public void match(
        List<String> items,
        Stream<String> candidates,
        BiFunction<String, String, Boolean> predicate,
        Consumer<Pair> onMatch
    ) {
        WiFiManager.debug("Trying items: " + items);
        candidates.forEach(candidate -> {
                for (var item : items) {
                    var result = predicate.apply(item, candidate);

                    traceLog.write("%s: %s @ %s".formatted(result, candidate, item));

                    if (result) {
                        successLog.write("TRUE: %s @ %s".formatted(candidate, item));
                        onMatch.accept(new Pair(item, candidate));
                    }
                }
            }
        );
    }
}
