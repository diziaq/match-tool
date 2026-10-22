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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Runs a {@link PairEvaluator} over every pair drawn from two {@link Source}s, as two nested loops.
 *
 * <p><b>Laziness.</b> The outer source is consumed once; the inner source is re-opened once per outer
 * item. Both are iterated lazily through an {@link Iterator} and closed after use — the engine never
 * materialises a whole source, so either source may be arbitrarily large or effectively infinite.
 * (To deliberately pre-load a small inner source, wrap it in {@link CachingSource}.)
 *
 * <p><b>Orientation.</b> By default the first source ({@code a}) is the outer loop and the second
 * ({@code b}) is the inner loop. {@link Builder#swap()} flips this without changing the evaluator's
 * {@code (a, b)} signature. The policies below always act on whichever source is the outer loop.
 *
 * <p><b>Parallelism budget.</b> {@link Builder#budget(int)} caps how many evaluations run at once;
 * it defaults to {@code 1}. Budget {@code 1} is the sequential, deterministic path. Budget greater
 * than {@code 1} evaluates an outer item's candidates on virtual threads gated by a semaphore, so at
 * most {@code budget} evaluations are ever in flight; it stays lazy and reports matches one at a time
 * (so the result consumer need not be thread-safe), but does not guarantee order.
 *
 * <p><b>Policies</b> (all default off), applied per outer item:
 * <ul>
 *   <li>{@code stopInnerOnMatch} — on {@link Outcome.Match}, stop this outer item's inner sweep.</li>
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

    private final boolean swap;
    private final int     budget;
    private final boolean stopInnerOnMatch;
    private final boolean stopAllOnMatch;
    private final boolean breakInnerOnSkip;
    private final int     breakInnerAfterFailures; // 0 = disabled
    private final int     retriesOnFailure;        // 0 = no retry

    private PairingEngine(Builder<A, B> b) {
        this.swap                    = b.swap;
        this.budget                  = b.budget;
        this.stopInnerOnMatch        = b.stopInnerOnMatch;
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
     * @param a         the first source (outer loop unless {@link Builder#swap()} was set)
     * @param b         the second source (inner loop unless {@link Builder#swap()} was set)
     * @param evaluator the pair action
     * @param onMatch   called for every accepted pair, always as {@code (a, b)}
     */
    public void run(Source<A> a, Source<B> b, PairEvaluator<A, B> evaluator, Consumer<Pair<A, B>> onMatch) {
        if (swap) {
            runLoop(b, a, (bi, ai) -> evaluator.evaluate(ai, bi), (bi, ai) -> new Pair<>(ai, bi), onMatch);
        } else {
            runLoop(a, b, evaluator::evaluate, Pair::new, onMatch);
        }
    }

    private <O, I> void runLoop(
        Source<O> outer, Source<I> inner,
        BiFunction<O, I, Outcome> act, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch
    ) {
        if (budget <= 1) {
            sequentialRun(outer, inner, act, pairOf, onMatch);
        } else {
            concurrentRun(outer, inner, act, pairOf, onMatch);
        }
    }

    // ── Sequential path (deterministic) ─────────────────────────────────────

    private <O, I> void sequentialRun(
        Source<O> outer, Source<I> inner,
        BiFunction<O, I, Outcome> act, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch
    ) {
        try (Stream<O> outerStream = outer.stream()) {
            Iterator<O> outerIt = outerStream.iterator();
            while (outerIt.hasNext()) {
                if (sweepInner(outerIt.next(), inner, act, pairOf, onMatch) == Control.STOP_ALL) {
                    return;
                }
            }
        }
    }

    private <O, I> Control sweepInner(
        O o, Source<I> inner,
        BiFunction<O, I, Outcome> act, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch
    ) {
        int failures = 0;
        try (Stream<I> innerStream = inner.stream()) {
            Iterator<I> innerIt = innerStream.iterator();
            while (innerIt.hasNext()) {
                I i = innerIt.next();
                Outcome outcome = withRetry(o, i, act);
                Control control = switch (outcome) {
                    case Outcome.Match ignored -> {
                        onMatch.accept(pairOf.apply(o, i));
                        if (stopAllOnMatch) yield Control.STOP_ALL;
                        yield stopInnerOnMatch ? Control.BREAK_INNER : Control.CONTINUE;
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

    private <O, I> void concurrentRun(
        Source<O> outer, Source<I> inner,
        BiFunction<O, I, Outcome> act, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch
    ) {
        AtomicBoolean stopAll = new AtomicBoolean(false);
        Semaphore permits = new Semaphore(budget);
        Object matchLock = new Object();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
             Stream<O> outerStream = outer.stream()) {
            Iterator<O> outerIt = outerStream.iterator();
            while (outerIt.hasNext() && !stopAll.get()) {
                sweepInnerConcurrent(outerIt.next(), inner, act, pairOf, onMatch, exec, permits, matchLock, stopAll);
            }
        }
    }

    private <O, I> void sweepInnerConcurrent(
        O o, Source<I> inner,
        BiFunction<O, I, Outcome> act, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch,
        ExecutorService exec, Semaphore permits, Object matchLock, AtomicBoolean stopAll
    ) {
        AtomicBoolean stopOuter = new AtomicBoolean(false);
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> inFlight = new ArrayList<>();
        try (Stream<I> innerStream = inner.stream()) {
            Iterator<I> innerIt = innerStream.iterator();
            while (innerIt.hasNext() && !stopOuter.get() && !stopAll.get()) {
                I i = innerIt.next();
                acquire(permits);
                if (stopOuter.get() || stopAll.get()) {
                    permits.release();
                    break;
                }
                inFlight.add(exec.submit(() -> {
                    try {
                        handleConcurrent(withRetry(o, i, act), o, i, pairOf, onMatch, matchLock, stopOuter, stopAll, failures);
                    } finally {
                        permits.release();
                    }
                }));
            }
        }
        awaitAll(inFlight);
    }

    private <O, I> void handleConcurrent(
        Outcome outcome, O o, I i, BiFunction<O, I, Pair<A, B>> pairOf, Consumer<Pair<A, B>> onMatch,
        Object matchLock, AtomicBoolean stopOuter, AtomicBoolean stopAll, AtomicInteger failures
    ) {
        switch (outcome) {
            case Outcome.Match ignored -> {
                synchronized (matchLock) {
                    onMatch.accept(pairOf.apply(o, i));
                }
                if (stopAllOnMatch)   stopAll.set(true);
                if (stopInnerOnMatch) stopOuter.set(true);
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

    // ── Shared ───────────────────────────────────────────────────────────────

    private <O, I> Outcome withRetry(O o, I i, BiFunction<O, I, Outcome> act) {
        Outcome outcome = act.apply(o, i);
        for (int attempt = 0; attempt < retriesOnFailure && outcome instanceof Outcome.Failure; attempt++) {
            outcome = act.apply(o, i);
        }
        return outcome;
    }

    /** Fluent builder for {@link PairingEngine}. All policies default to off / 0; budget defaults to 1. */
    public static final class Builder<A, B> {

        private boolean swap                    = false;
        private int     budget                  = 1;
        private boolean stopInnerOnMatch        = false;
        private boolean stopAllOnMatch          = false;
        private boolean breakInnerOnSkip        = false;
        private int     breakInnerAfterFailures = 0;
        private int     retriesOnFailure        = 0;

        private Builder() {}

        /** Iterate the second source as the outer loop and the first as the inner loop. */
        public Builder<A, B> swap() {
            this.swap = true;
            return this;
        }

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

        /** On {@link Outcome.Match}, stop the current outer item's inner sweep. */
        public Builder<A, B> stopInnerOnMatch() {
            this.stopInnerOnMatch = true;
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
