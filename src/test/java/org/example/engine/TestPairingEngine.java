package org.example.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * The engine's contract: a lazy nested loop over two sources with configurable orientation and
 * inner-break policies. Evaluation order is logged as {@code "a|b"} strings so each scenario reads as
 * the exact sequence of pairs the engine tried. The Laziness group proves the engine never
 * materialises the outer source.
 */
class TestPairingEngine {

    @SafeVarargs
    private static <T> Source<T> src(T... items) {
        return () -> Stream.of(items);
    }

    /** Records each evaluated pair and returns {@code Mismatch} unless the pair is in {@code matches}. */
    private static final class RecordingEvaluator implements PairEvaluator<String, String> {
        final List<String> log = new ArrayList<>();
        private final List<String> matches;

        RecordingEvaluator(String... matches) {
            this.matches = List.of(matches);
        }

        @Override
        public Outcome evaluate(String a, String b) {
            log.add(a + "|" + b);
            return matches.contains(a + "|" + b) ? new Outcome.Match() : new Outcome.Mismatch();
        }
    }

    @Nested
    class BaseCases {

        @Test
        void evaluatesEveryOuterInnerPairOuterFirst() {
            var evaluator = new RecordingEvaluator();
            var engine = PairingEngine.<String, String>builder().build();

            engine.run(src("A", "B"), src("1", "2", "3"), evaluator, p -> {});

            assertThat(evaluator.log).containsExactly("A|1", "A|2", "A|3", "B|1", "B|2", "B|3");
        }

        @Test
        void reportsAcceptedPairToConsumer() {
            var evaluator = new RecordingEvaluator("A|2");
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().build();

            engine.run(src("A", "B"), src("1", "2"), evaluator, matches::add);

            assertThat(matches).containsExactly(new Pair<>("A", "2"));
        }

        @Test
        void reopensInnerSourceOncePerOuterItem() {
            AtomicInteger innerOpens = new AtomicInteger();
            Source<String> inner = () -> { innerOpens.incrementAndGet(); return Stream.of("1", "2"); };
            var engine = PairingEngine.<String, String>builder().build();

            engine.run(src("A", "B", "C"), inner, new RecordingEvaluator(), p -> {});

            assertThat(innerOpens).hasValue(3);
        }
    }

    @Nested
    class Orientation {

        @Test
        void swapIteratesSecondSourceAsOuter() {
            var evaluator = new RecordingEvaluator();
            var engine = PairingEngine.<String, String>builder().swap().build();

            engine.run(src("A", "B"), src("1", "2"), evaluator, p -> {});

            assertThat(evaluator.log).containsExactly("A|1", "B|1", "A|2", "B|2");
        }

        /** Swap changes traversal order only; the evaluator and the reported pair stay (a, b). */
        @Test
        void swapKeepsEvaluatorAndPairOrientation() {
            var evaluator = new RecordingEvaluator("B|2");
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().swap().build();

            engine.run(src("A", "B"), src("1", "2"), evaluator, matches::add);

            assertThat(matches).containsExactly(new Pair<>("B", "2"));
        }
    }

    @Nested
    class StopBehaviour {

        @Test
        void stopInnerOnMatchDropsRemainingCandidatesForThatOuter() {
            var evaluator = new RecordingEvaluator("N1|p2");
            var engine = PairingEngine.<String, String>builder().stopInnerOnMatch().build();

            engine.run(src("N1"), src("p1", "p2", "p3"), evaluator, p -> {});

            assertThat(evaluator.log).containsExactly("N1|p1", "N1|p2");
        }

        @Test
        void stopInnerOnMatchAdvancesToNextOuter() {
            var evaluator = new RecordingEvaluator("N1|p1", "N2|p1");
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().stopInnerOnMatch().build();

            engine.run(src("N1", "N2"), src("p1", "p2"), evaluator, matches::add);

            assertThat(evaluator.log).containsExactly("N1|p1", "N2|p1");
            assertThat(matches).containsExactly(new Pair<>("N1", "p1"), new Pair<>("N2", "p1"));
        }

        @Test
        void stopAllOnMatchEndsEntireRun() {
            var evaluator = new RecordingEvaluator("N1|p1");
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().stopAllOnMatch().build();

            engine.run(src("N1", "N2"), src("p1", "p2"), evaluator, matches::add);

            assertThat(evaluator.log).containsExactly("N1|p1");
            assertThat(matches).containsExactly(new Pair<>("N1", "p1"));
        }
    }

    @Nested
    class SkipBehaviour {

        /** Returns Skip for one configured pair, Mismatch otherwise. */
        private PairEvaluator<String, String> skipOn(List<String> log, String skipPair) {
            return (a, b) -> {
                log.add(a + "|" + b);
                return (a + "|" + b).equals(skipPair) ? new Outcome.Skip("dead end") : new Outcome.Mismatch();
            };
        }

        @Test
        void breakInnerOnSkipDropsRemainingCandidatesForThatOuter() {
            List<String> log = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().breakInnerOnSkip().build();

            engine.run(src("N1", "N2"), src("p1", "p2"), skipOn(log, "N1|p1"), p -> {});

            assertThat(log).containsExactly("N1|p1", "N2|p1", "N2|p2");
        }

        @Test
        void withoutPolicySkipContinuesInnerSweep() {
            List<String> log = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().build();

            engine.run(src("N1", "N2"), src("p1", "p2"), skipOn(log, "N1|p1"), p -> {});

            assertThat(log).containsExactly("N1|p1", "N1|p2", "N2|p1", "N2|p2");
        }
    }

    @Nested
    class FailureBehaviour {

        private PairEvaluator<String, String> alwaysFailing(List<String> log) {
            return (a, b) -> { log.add(a + "|" + b); return new Outcome.Failure("boom"); };
        }

        @Test
        void breakInnerAfterFailuresStopsAtThreshold() {
            List<String> log = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().breakInnerAfterFailures(2).build();

            engine.run(src("N1"), src("p1", "p2", "p3", "p4"), alwaysFailing(log), p -> {});

            assertThat(log).containsExactly("N1|p1", "N1|p2");
        }

        @Test
        void retriesOnFailureReevaluatesUntilNonFailure() {
            AtomicInteger calls = new AtomicInteger();
            PairEvaluator<String, String> flaky = (a, b) ->
                calls.incrementAndGet() < 3 ? new Outcome.Failure("transient") : new Outcome.Match();
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().retriesOnFailure(2).build();

            engine.run(src("N1"), src("p1"), flaky, matches::add);

            assertThat(calls).hasValue(3);
            assertThat(matches).containsExactly(new Pair<>("N1", "p1"));
        }

        @Test
        void retriesExhaustedLeavesPairUnmatched() {
            AtomicInteger calls = new AtomicInteger();
            PairEvaluator<String, String> failing = (a, b) -> {
                calls.incrementAndGet();
                return new Outcome.Failure("permanent");
            };
            List<Pair<String, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<String, String>builder().retriesOnFailure(1).build();

            engine.run(src("N1"), src("p1"), failing, matches::add);

            assertThat(calls).hasValue(2);
            assertThat(matches).isEmpty();
        }
    }

    @Nested
    class BuilderValidation {

        @Test
        void breakInnerAfterFailuresRejectsNonPositive() {
            assertThatThrownBy(() -> PairingEngine.builder().breakInnerAfterFailures(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 1");
        }

        @Test
        void retriesOnFailureRejectsNegative() {
            assertThatThrownBy(() -> PairingEngine.builder().retriesOnFailure(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 0");
        }
    }

    @Nested
    class Laziness {

        /** The outer source is iterated, never collected: stopAll must return on an infinite outer. */
        @Test
        @Timeout(5)
        void stopAllOnMatchReturnsOnInfiniteOuterSource() {
            Source<Integer> infiniteOuter = () -> Stream.iterate(0, n -> n + 1);
            PairEvaluator<Integer, String> alwaysMatch = (a, b) -> new Outcome.Match();
            List<Pair<Integer, String>> matches = new ArrayList<>();
            var engine = PairingEngine.<Integer, String>builder().stopAllOnMatch().build();

            engine.run(infiniteOuter, src("only"), alwaysMatch, matches::add);

            assertThat(matches).containsExactly(new Pair<>(0, "only"));
        }
    }

    @Nested
    class Budget {

        @Test
        void rejectsNonPositiveBudget() {
            assertThatThrownBy(() -> PairingEngine.builder().budget(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 1");
        }

        /**
         * A CyclicBarrier sized to the budget trips only when exactly {@code budget} evaluations run
         * at once, so a clean run proves the engine reaches the budget; the peak counter proves it
         * never exceeds it. No sleeps — the barrier does the synchronising.
         */
        @Test
        @Timeout(10)
        void runsExactlyBudgetEvaluationsConcurrently() {
            int budget = 3;
            CyclicBarrier barrier = new CyclicBarrier(budget);
            AtomicInteger current = new AtomicInteger();
            AtomicInteger peak = new AtomicInteger();
            PairEvaluator<String, String> barrierEvaluator = (a, b) -> {
                peak.accumulateAndGet(current.incrementAndGet(), Math::max);
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("budget not reached: barrier timed out", e);
                }
                current.decrementAndGet();
                return new Outcome.Mismatch();
            };
            var engine = PairingEngine.<String, String>builder().budget(budget).build();

            engine.run(src("N"), src("p1", "p2", "p3", "p4", "p5", "p6"), barrierEvaluator, p -> {});

            assertThat(peak).hasValue(budget);
        }

        @Test
        @Timeout(10)
        void concurrentPathReportsEveryMatch() {
            Queue<Pair<String, String>> matches = new ConcurrentLinkedQueue<>();
            PairEvaluator<String, String> matchEvens = (a, b) ->
                List.of("p2", "p4", "p6").contains(b) ? new Outcome.Match() : new Outcome.Mismatch();
            var engine = PairingEngine.<String, String>builder().budget(4).build();

            engine.run(src("N"), src("p1", "p2", "p3", "p4", "p5", "p6"), matchEvens, matches::add);

            assertThat(matches).containsExactlyInAnyOrder(
                new Pair<>("N", "p2"), new Pair<>("N", "p4"), new Pair<>("N", "p6"));
        }
    }
}
