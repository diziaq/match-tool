package org.example.matcher;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class TestPairMatcher {

    private final PairMatcher<String, String> matcher = new PairMatcher<>();

    private static MatchOutcome match()     { return new MatchOutcome.Match(); }
    private static MatchOutcome mismatch()  { return new MatchOutcome.Mismatch(); }

    @Nested
    class BaseCases {

        @Test
        void callsOnMatchForMatchingPair() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(
                List.of("a"),
                Stream.of("1"),
                (l, r) -> l.equals("a") && r.equals("1") ? match() : mismatch(),
                matched::add
            );

            assertThat(matched).hasSize(1);
            assertThat(matched.get(0).left()).isEqualTo("a");
            assertThat(matched.get(0).right()).isEqualTo("1");
        }

        @Test
        void doesNotCallOnMatchForMismatch() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of("a"), Stream.of("x"), (l, r) -> mismatch(), matched::add);

            assertThat(matched).isEmpty();
        }

        @Test
        void multipleMatchesAllReported() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of("a", "b"), Stream.of("1", "2"), (l, r) -> match(), matched::add);

            assertThat(matched).hasSize(4);
        }

        @Test
        void onMatchReceivesCorrectPair() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of("net"), Stream.of("pass"), (l, r) -> match(), matched::add);

            assertThat(matched.get(0)).isEqualTo(new Match<>("net", "pass"));
        }

        @Test
        void worksWithIntegerTypes() {
            List<Match<Integer, Integer>> matched = new ArrayList<>();
            PairMatcher<Integer, Integer> intMatcher = new PairMatcher<>();

            intMatcher.match(
                List.of(2, 3),
                Stream.of(4, 6, 9),
                (l, r) -> r % l == 0 ? match() : mismatch(),
                matched::add
            );

            assertThat(matched).hasSize(4);
        }
    }

    @Nested
    class IterationBehaviour {

        @Test
        void crossesAllLeftsWithAllRights() {
            List<String> attempted = new ArrayList<>();

            matcher.match(
                List.of("L1", "L2"),
                Stream.of("R1", "R2", "R3"),
                (l, r) -> { attempted.add(l + "+" + r); return mismatch(); },
                m -> {}
            );

            assertThat(attempted).hasSize(6)
                .contains("L1+R1", "L1+R2", "L1+R3", "L2+R1", "L2+R2", "L2+R3");
        }

        @Test
        void iteratesRightsOuterLoopLeftsInner() {
            List<String> order = new ArrayList<>();

            matcher.match(
                List.of("L1", "L2"),
                Stream.of("R1", "R2"),
                (l, r) -> { order.add(r + "-" + l); return mismatch(); },
                m -> {}
            );

            assertThat(order).containsExactly("R1-L1", "R1-L2", "R2-L1", "R2-L2");
        }

        // Opinion: pin the total invocation count to guard against short-circuit optimisations
        // being accidentally introduced — the batch spray mode must try all combinations.
        @Test
        void predicateCalledExactlyNTimesMTimes() {
            var count = new AtomicInteger();

            matcher.match(
                List.of("a", "b", "c"),
                Stream.of("1", "2", "3", "4"),
                (l, r) -> { count.incrementAndGet(); return mismatch(); },
                m -> {}
            );

            assertThat(count.get()).isEqualTo(3 * 4);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptyLefts_noMatchesAttempted() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of(), Stream.of("R1", "R2"), (l, r) -> match(), matched::add);

            assertThat(matched).isEmpty();
        }

        @Test
        void emptyRights_noMatchesAttempted() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of("L1"), Stream.empty(), (l, r) -> match(), matched::add);

            assertThat(matched).isEmpty();
        }

        // Only Match triggers onMatch; Mismatch, Unavailable and Failure are all non-match outcomes.
        @Test
        void onlyMatchOutcome_triggersOnMatch() {
            List<Match<String, String>> matched = new ArrayList<>();

            matcher.match(List.of("x"), Stream.of("1", "2", "3", "4"),
                (l, r) -> switch (r) {
                    case "1" -> new MatchOutcome.Match();
                    case "2" -> new MatchOutcome.Mismatch();
                    case "3" -> new MatchOutcome.Unavailable();
                    case "4" -> new MatchOutcome.Failure("err");
                    default  -> mismatch();
                },
                matched::add
            );

            assertThat(matched).hasSize(1);
            assertThat(matched.get(0).right()).isEqualTo("1");
        }
    }
}
