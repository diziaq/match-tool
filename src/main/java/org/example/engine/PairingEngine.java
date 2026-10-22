package org.example.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Runs a {@link PairEvaluator} over every pair drawn from two {@link Source}s, as two nested loops.
 *
 * <p><b>Laziness.</b> The first source is the outer loop and is consumed once; the second source is
 * the inner loop and is re-opened once per outer item. Both are iterated lazily through an
 * {@link Iterator} and closed after use — the engine never materialises a whole source, so either
 * source may be arbitrarily large or effectively infinite. (To deliberately pre-load a small inner
 * source, wrap it in {@link CachingSource}.) The caller — a plugin — is responsible for passing the
 * sources in the right order for its intent (for WiFi: networks outer, passwords inner).
 *
 * <p><b>Parallelism budget.</b> {@link Builder#budget(int)} caps how many evaluations run at once;
 * it defaults to {@code 1}. Budget {@code 1} is the sequential, deterministic path. Budget greater
 * than {@code 1} evaluates an outer item's candidates on virtual threads gated by a semaphore, so at
 * most {@code budget} evaluations are ever in flight; it stays lazy and reports matches one at a time
 * (so the result consumer need not be thread-safe), but does not guarantee order.
 *
 * <p><b>Completion.</b> Each outer item is independent. It is complete when its inner sweep is
 * exhausted, or when it reaches {@code maxMatchesPerOuter} matches — whichever comes first. Reaching
 * the match cap stops that outer item's inner loop only; other outer items keep going. The cap
 * defaults to {@link Integer#MAX_VALUE} (scan every candidate and report every match); set it to 1 to
 * stop an outer item at its first match (what the WiFi plugin wants — one working password per
 * network). {@code stopAllOnMatch} is the separate, stronger policy that ends the entire run.
 *
 * <p><b>Policies</b> (defaults: cap unbounded, the rest off), applied per outer item:
 * <ul>
 *   <li>{@code maxMatchesPerOuter(n)} — complete an outer item (stop its inner sweep) after {@code n}
 *       matches; other outer items continue. Default {@link Integer#MAX_VALUE}.</li>
 *   <li>{@code stopAllOnMatch} — on {@link Outcome.Match}, end the whole run.</li>
 *   <li>{@code breakInnerOnSkip} — on {@link Outcome.Skip}, stop this outer item's inner sweep.</li>
 *   <li>{@code breakInnerAfterFailures(n)} — stop this outer item's inner sweep once it has
 *       accumulated {@code n} {@link Outcome.Failure} outcomes (after retries).</li>
 *   <li>{@code retriesOnFailure(n)} — re-evaluate a pair up to {@code n} more times while the outcome
 *       is {@link Outcome.Failure}.</li>
 * </ul>
 * In the concurrent path the stop/break policies are best-effort early-stop: no further candidates
 * are scheduled once the condition is met, but evaluations already in flight finish.
 */
public final class PairingEngine<A, B> {

    private enum Control { CONTINUE, BREAK_INNER, STOP_ALL }

    private final int     budget;
    private final int     maxMatchesPerOuter;
    private final boolean stopAllOnMatch;
    private final boolean breakInnerOnSkip;
    private final int     breakInnerAfterFailures; // 0 = disabled
    private final int     retriesOnFailure;        // 0 = no retry

    private PairingEngine(Builder<A, B> b) {
        this.budget                  = b.budget;
        this.maxMatchesPerOuter      = b.maxMatchesPerOuter;
        this.stopAllOnMatch          = b.stopAllOnMatch;
        this.breakInnerOnSkip        = b.breakInnerOnSkip;
        this.breakInnerAfterFailures = b.breakInnerAfterFailures;
        this.retriesOnFailure        = b.retriesOnFailure;
    }

    public static <A, B> Builder<A, B> builder() {
        return new Builder<>();
    }

    /**
     * Runs the pairing. Iterates the outer source once and the inner source once per outer item,
     * calling {@code evaluator} for each pair and {@code onMatch} for each {@link Outcome.Match}.
     *
     * @param outer     the outer-loop source (consumed once)
     * @param inner     the inner-loop source (re-opened once per outer item)
     * @param evaluator the pair action
     * @param onMatch   called for every accepted pair
     */
    public void run(Source<A> outer, Source<B> inner, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch) {
        if (budget <= 1) {
            sequentialRun(outer, inner, evaluator, onMatch);
        } else {
            concurrentRun(outer, inner, evaluator, onMatch);
        }
    }

    // ── Sequential path (deterministic) ─────────────────────────────────────

    private void sequentialRun(Source<A> outer, Source<B> inner, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch) {
        try (Stream<A> outerStream = outer.stream()) {
            Iterator<A> outerIt = outerStream.iterator();
            while (outerIt.hasNext()) {
                if (sweepInner(outerIt.next(), inner, evaluator, onMatch) == Control.STOP_ALL) {
                    return;
                }
            }
        }
    }

    private Control sweepInner(A a, Source<B> inner, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch) {
        int failures = 0;
        int matches = 0;
        try (Stream<B> innerStream = inner.stream()) {
            Iterator<B> innerIt = innerStream.iterator();
            while (innerIt.hasNext()) {
                B b = innerIt.next();
                Outcome outcome = withRetry(a, b, evaluator);
                Control control = switch (outcome) {
                    case Outcome.Match ignored -> {
                        onMatch.accept(new Pair<>(a, b));
                        if (stopAllOnMatch) yield Control.STOP_ALL;
                        yield ++matches >= maxMatchesPerOuter ? Control.BREAK_INNER : Control.CONTINUE;
                    }
                    case Outcome.Skip ignored ->
                        breakInnerOnSkip ? Control.BREAK_INNER : Control.CONTINUE;
                    case Outcome.Failure ignored -> {
                        failures++;
                        yield breakInnerAfterFailures > 0 && failures >= breakInnerAfterFailures
                            ? Control.BREAK_INNER : Control.CONTINUE;
                    }
                    case Outcome.Mismatch ignored -> Control.CONTINUE;
                };
                if (control == Control.STOP_ALL)    return Control.STOP_ALL;
                if (control == Control.BREAK_INNER) return Control.CONTINUE;
            }
        }
        return Control.CONTINUE;
    }

    // ── Concurrent path (budget > 1, bounded by a semaphore) ─────────────────

    private void concurrentRun(Source<A> outer, Source<B> inner, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch) {
        AtomicBoolean stopAll = new AtomicBoolean(false);
        Semaphore permits = new Semaphore(budget);
        Object matchLock = new Object();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
             Stream<A> outerStream = outer.stream()) {
            Iterator<A> outerIt = outerStream.iterator();
            while (outerIt.hasNext() && !stopAll.get()) {
                sweepInnerConcurrent(outerIt.next(), inner, evaluator, onMatch, exec, permits, matchLock, stopAll);
            }
        }
    }

    private void sweepInnerConcurrent(
        A a, Source<B> inner, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch,
        ExecutorService exec, Semaphore permits, Object matchLock, AtomicBoolean stopAll
    ) {
        AtomicBoolean stopOuter = new AtomicBoolean(false);
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger matches = new AtomicInteger();
        List<Future<?>> inFlight = new ArrayList<>();
        try (Stream<B> innerStream = inner.stream()) {
            Iterator<B> innerIt = innerStream.iterator();
            while (innerIt.hasNext() && !stopOuter.get() && !stopAll.get()) {
                B b = innerIt.next();
                acquire(permits);
                if (stopOuter.get() || stopAll.get()) {
                    permits.release();
                    break;
                }
                inFlight.add(exec.submit(() -> {
                    try {
                        handleConcurrent(withRetry(a, b, evaluator), a, b, onMatch, matchLock, stopOuter, stopAll, failures, matches);
                    } finally {
                        permits.release();
                    }
                }));
            }
        }
        awaitAll(inFlight);
    }

    private void handleConcurrent(
        Outcome outcome, A a, B b, Consumer<Pair<A, B>> onMatch,
        Object matchLock, AtomicBoolean stopOuter, AtomicBoolean stopAll, AtomicInteger failures, AtomicInteger matches
    ) {
        switch (outcome) {
            case Outcome.Match ignored -> {
                synchronized (matchLock) {
                    onMatch.accept(new Pair<>(a, b));
                }
                if (stopAllOnMatch) stopAll.set(true);
                if (matches.incrementAndGet() >= maxMatchesPerOuter) stopOuter.set(true);
            }
            case Outcome.Skip ignored -> {
                if (breakInnerOnSkip) stopOuter.set(true);
            }
            case Outcome.Failure ignored -> {
                if (breakInnerAfterFailures > 0 && failures.incrementAndGet() >= breakInnerAfterFailures) {
                    stopOuter.set(true);
                }
            }
            case Outcome.Mismatch ignored -> { /* try the next candidate */ }
        }
    }

    private static void acquire(Semaphore permits) {
        try {
            permits.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for a parallelism permit", e);
        }
    }

    private static void awaitAll(List<Future<?>> futures) {
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while awaiting evaluations", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("a pair evaluation failed", e.getCause());
            }
        }
    }

    private Outcome withRetry(A a, B b, PairEvaluator<A, B> evaluator) {
        Outcome outcome = evaluator.evaluate(a, b);
        for (int attempt = 0; attempt < retriesOnFailure && outcome instanceof Outcome.Failure; attempt++) {
            outcome = evaluator.evaluate(a, b);
        }
        return outcome;
    }

    /** Fluent builder for {@link PairingEngine}. All policies default to off / 0; budget defaults to 1. */
    public static final class Builder<A, B> {

        private int     budget                  = 1;
        private int     maxMatchesPerOuter      = Integer.MAX_VALUE;
        private boolean stopAllOnMatch          = false;
        private boolean breakInnerOnSkip        = false;
        private int     breakInnerAfterFailures = 0;
        private int     retriesOnFailure        = 0;

        private Builder() {}

        /**
         * Cap how many evaluations run at once. {@code 1} (the default) is the sequential,
         * deterministic path.
         *
         * @param n must be &gt;= 1
         */
        public Builder<A, B> budget(int n) {
            if (n < 1) throw new IllegalArgumentException("budget must be >= 1, got: " + n);
            this.budget = n;
            return this;
        }

        /**
         * Complete an outer item once it reaches {@code n} matches: its inner sweep stops, other
         * outer items keep going. An outer item that never reaches {@code n} is completed by
         * exhausting its inner source. The default is {@link Integer#MAX_VALUE} — report every match
         * and scan every candidate. The WiFi plugin sets {@code 1}: one working password per network.
         *
         * @param n must be in {@code [1, Integer.MAX_VALUE]}
         */
        public Builder<A, B> maxMatchesPerOuter(int n) {
            if (n < 1) throw new IllegalArgumentException("maxMatchesPerOuter must be >= 1, got: " + n);
            this.maxMatchesPerOuter = n;
            return this;
        }

        /** On {@link Outcome.Match}, end the whole run. */
        public Builder<A, B> stopAllOnMatch() {
            this.stopAllOnMatch = true;
            return this;
        }

        /** On {@link Outcome.Skip}, stop the current outer item's inner sweep. */
        public Builder<A, B> breakInnerOnSkip() {
            this.breakInnerOnSkip = true;
            return this;
        }

        /**
         * Stop the current outer item's inner sweep after {@code n} {@link Outcome.Failure} outcomes.
         *
         * @param n must be &gt;= 1
         */
        public Builder<A, B> breakInnerAfterFailures(int n) {
            if (n < 1) throw new IllegalArgumentException("n must be >= 1, got: " + n);
            this.breakInnerAfterFailures = n;
            return this;
        }

        /**
         * Re-evaluate a pair up to {@code n} more times while the outcome is {@link Outcome.Failure}.
         *
         * @param n must be &gt;= 0
         */
        public Builder<A, B> retriesOnFailure(int n) {
            if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
            this.retriesOnFailure = n;
            return this;
        }

        public PairingEngine<A, B> build() {
            return new PairingEngine<>(this);
        }
    }
}
