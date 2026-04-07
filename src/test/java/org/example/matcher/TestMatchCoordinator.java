package org.example.matcher;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class TestMatchCoordinator {

    // -------------------------------------------------------------------------
    // Outcome shorthands
    // -------------------------------------------------------------------------

    private static MatchOutcome match()       { return new MatchOutcome.Match(); }
    private static MatchOutcome mismatch()    { return new MatchOutcome.Mismatch(); }
    private static MatchOutcome unavailable() { return new MatchOutcome.Unavailable(); }
    private static MatchOutcome failure()     { return new MatchOutcome.Failure("err"); }

    // -------------------------------------------------------------------------
    // Stop behaviour
    // -------------------------------------------------------------------------

    @Nested
    class StopBehaviour {

        @Test
        void noPolicy_processesAllPairs() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder().build().match(
                List.of("A", "B"),
                Stream.of("1", "2"),
                (l, r) -> { seen.add(l + r); return mismatch(); },
                m -> {}
            );
            assertThat(seen).containsExactlyInAnyOrder("A1", "A2", "B1", "B2");
        }

        @Test
        void stopLeftOnFirstMatch_skipsFurtherRightsForMatchedLeft() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .stopLeftOnFirstMatch()
                .build()
                .match(
                    List.of("A", "B"),
                    Stream.of("1", "2", "3"),
                    (l, r) -> {
                        seen.add(l + r);
                        // A matches on right "2"; B never matches
                        return l.equals("A") && r.equals("2") ? match() : mismatch();
                    },
                    m -> {}
                );
            // A is excluded after matching "2", so A3 is never tried
            assertThat(seen).contains("A1", "A2", "B1", "B2", "B3")
                            .doesNotContain("A3");
        }

        @Test
        void stopLeftOnFirstMatch_reportedMatchIsCorrect() {
            List<Match<String, String>> matched = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .stopLeftOnFirstMatch()
                .build()
                .match(
                    List.of("net"),
                    Stream.of("wrong", "correct", "extra"),
                    (l, r) -> r.equals("correct") ? match() : mismatch(),
                    matched::add
                );
            assertThat(matched).hasSize(1);
            assertThat(matched.get(0)).isEqualTo(new Match<>("net", "correct"));
        }

        @Test
        void stopAllOnFirstMatch_stopsAfterFirstMatch() {
            List<String> seen = new ArrayList<>();
            List<Match<String, String>> matched = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .stopAllOnFirstMatch()
                .build()
                .match(
                    List.of("A", "B"),
                    Stream.of("1", "2", "3"),
                    (l, r) -> {
                        seen.add(l + r);
                        return l.equals("A") && r.equals("1") ? match() : mismatch();
                    },
                    matched::add
                );
            // A matches on right "1"; entire run stops; nothing else is tried
            assertThat(matched).hasSize(1);
            assertThat(seen).doesNotContain("A2", "A3", "B2", "B3");
        }

        @Test
        void stopAllOnFirstMatch_doesNotProcessRemainingLeftsForCurrentRight() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .stopAllOnFirstMatch()
                .build()
                .match(
                    List.of("A", "B", "C"),
                    Stream.of("x"),
                    (l, r) -> {
                        seen.add(l);
                        return l.equals("A") ? match() : mismatch();
                    },
                    m -> {}
                );
            // B and C are skipped within the same right once A matched
            assertThat(seen).containsExactly("A");
        }
    }

    // -------------------------------------------------------------------------
    // Exclusion behaviour
    // -------------------------------------------------------------------------

    @Nested
    class ExclusionBehaviour {

        @Test
        void excludeOnUnavailable_skipsLeftImmediately() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .excludeOnUnavailable()
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2", "3"),
                    (l, r) -> {
                        seen.add(l + r);
                        return r.equals("1") ? unavailable() : mismatch();
                    },
                    m -> {}
                );
            // A is excluded after Unavailable on "1"
            assertThat(seen).containsExactly("A1");
        }

        @Test
        void withoutExcludeOnUnavailable_leftIsNotExcluded() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder().build().match(
                List.of("A"),
                Stream.of("1", "2"),
                (l, r) -> { seen.add(l + r); return unavailable(); },
                m -> {}
            );
            // no exclusion policy — A is tried for both rights despite Unavailable
            assertThat(seen).containsExactly("A1", "A2");
        }

        @Test
        void excludeAfterNFailures_excludesLeftAtThreshold() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .excludeAfterFailures(2)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2", "3"),
                    (l, r) -> { seen.add(l + r); return failure(); },
                    m -> {}
                );
            // A fails on "1" (count=1) and "2" (count=2, excluded); "3" is skipped
            assertThat(seen).containsExactly("A1", "A2");
        }

        @Test
        void excludeAfterNFailures_doesNotExcludeBeforeThreshold() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .excludeAfterFailures(3)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2"),
                    (l, r) -> { seen.add(l + r); return failure(); },
                    m -> {}
                );
            // only 2 failures, threshold is 3 — A is never excluded
            assertThat(seen).containsExactly("A1", "A2");
        }

        @Test
        void excludeAfterNFailures_mismatchDoesNotCountTowardExclusion() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .excludeAfterFailures(2)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2", "3"),
                    (l, r) -> {
                        seen.add(l + r);
                        // "1" and "3" are Mismatch, only "2" is Failure
                        return r.equals("2") ? failure() : mismatch();
                    },
                    m -> {}
                );
            // only 1 failure total — never reaches threshold of 2
            assertThat(seen).containsExactly("A1", "A2", "A3");
        }

        @Test
        void failureCounterAccumulatesAcrossRights() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .excludeAfterFailures(3)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2", "3", "4"),
                    (l, r) -> { seen.add(l + r); return failure(); },
                    m -> {}
                );
            // fails on 1(→1), 2(→2), 3(→3=excluded); "4" never tried
            assertThat(seen).containsExactly("A1", "A2", "A3");
        }
    }

    // -------------------------------------------------------------------------
    // Retry behaviour
    // -------------------------------------------------------------------------

    @Nested
    class RetryBehaviour {

        @Test
        void retriesFailureUpToNTimes() {
            AtomicInteger calls = new AtomicInteger();
            MatchCoordinator.<String, String>builder()
                .retries(2)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1"),
                    (l, r) -> { calls.incrementAndGet(); return failure(); },
                    m -> {}
                );
            // initial + 2 retries = 3 total calls for the single pair
            assertThat(calls.get()).isEqualTo(3);
        }

        @Test
        void stopsRetryingWhenNonFailureReturned() {
            AtomicInteger calls = new AtomicInteger();
            List<Match<String, String>> matched = new ArrayList<>();
            MatchCoordinator.<String, String>builder()
                .retries(3)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1"),
                    (l, r) -> {
                        int n = calls.incrementAndGet();
                        return n < 3 ? failure() : match();  // succeeds on 3rd attempt
                    },
                    matched::add
                );
            assertThat(calls.get()).isEqualTo(3);
            assertThat(matched).hasSize(1);
        }

        @Test
        void successfulRetry_doesNotIncrementFailureCounter() {
            List<String> seen = new ArrayList<>();
            AtomicInteger predicateCalls = new AtomicInteger();
            List<Match<String, String>> matched = new ArrayList<>();

            MatchCoordinator.<String, String>builder()
                .retries(2)
                .excludeAfterFailures(1) // would exclude on first persistent failure
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1", "2"),
                    (l, r) -> {
                        seen.add(l + r + "#" + predicateCalls.incrementAndGet());
                        // On right "1": fails once then matches (so withRetry returns Match)
                        // On right "2": plain mismatch
                        if (r.equals("1") && predicateCalls.get() == 1) return failure();
                        if (r.equals("1") && predicateCalls.get() == 2) return match();
                        return mismatch();
                    },
                    matched::add
                );
            // A was NOT excluded — the failure on "1" was recovered by retry
            assertThat(matched).hasSize(1);
            // A2 was also processed
            assertThat(seen).anyMatch(s -> s.startsWith("A2"));
        }

        @Test
        void zeroRetries_callsPredicateOnce() {
            AtomicInteger calls = new AtomicInteger();
            MatchCoordinator.<String, String>builder()
                .retries(0)
                .build()
                .match(
                    List.of("A"),
                    Stream.of("1"),
                    (l, r) -> { calls.incrementAndGet(); return failure(); },
                    m -> {}
                );
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Nested
    class BuilderValidation {

        @Test
        void excludeAfterFailures_rejectsZero() {
            assertThatThrownBy(() -> MatchCoordinator.builder().excludeAfterFailures(0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void retries_rejectsNegative() {
            assertThatThrownBy(() -> MatchCoordinator.builder().retries(-1))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Effective preset
    // -------------------------------------------------------------------------

    @Nested
    class EffectivePreset {

        @Test
        void stopsLeftAfterFirstMatch() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>effective().match(
                List.of("net"),
                Stream.of("wrong", "correct", "extra"),
                (l, r) -> { seen.add(r); return r.equals("correct") ? match() : mismatch(); },
                m -> {}
            );
            assertThat(seen).containsExactly("wrong", "correct");
            assertThat(seen).doesNotContain("extra");
        }

        @Test
        void excludesUnavailableImmediately() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>effective().match(
                List.of("missing-net"),
                Stream.of("p1", "p2", "p3"),
                (l, r) -> { seen.add(r); return unavailable(); },
                m -> {}
            );
            assertThat(seen).containsExactly("p1");
        }

        @Test
        void retriesTransientFailure() {
            AtomicInteger calls = new AtomicInteger();
            List<Match<String, String>> matched = new ArrayList<>();
            MatchCoordinator.<String, String>effective().match(
                List.of("net"),
                Stream.of("pass"),
                (l, r) -> {
                    int n = calls.incrementAndGet();
                    // first 2 attempts fail, 3rd succeeds (within retries(2) budget = 3 total)
                    return n < 3 ? failure() : match();
                },
                matched::add
            );
            assertThat(matched).hasSize(1);
        }

        @Test
        void excludesAfterThreePersistentFailures() {
            List<String> seen = new ArrayList<>();
            MatchCoordinator.<String, String>effective().match(
                List.of("flaky-net"),
                Stream.of("p1", "p2", "p3", "p4"),
                (l, r) -> { seen.add(r); return failure(); },
                m -> {}
            );
            // excludeAfterFailures(3): excluded after p3's persistent failure; p4 is never tried
            // retries(2) means 3 predicate calls per right (initial + 2 retries), so each
            // right appears 3 times in seen — use distinct() to check which rights were attempted
            assertThat(seen.stream().distinct().toList()).containsExactly("p1", "p2", "p3");
            assertThat(seen).doesNotContain("p4");
        }

        @Test
        void doesNotStopAllOnFirstMatch_continuesOtherLefts() {
            List<Match<String, String>> matched = new ArrayList<>();
            MatchCoordinator.<String, String>effective().match(
                List.of("netA", "netB"),
                Stream.of("pass"),
                (l, r) -> match(),   // both match
                matched::add
            );
            // stopAllOnFirstMatch is NOT set in effective() — both lefts are reported
            assertThat(matched).hasSize(2);
        }
    }
}
