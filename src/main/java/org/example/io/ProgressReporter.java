package org.example.io;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Accumulates per-attempt timing for a long run and emits a one-line summary at most once per
 * interval, so an operator can tell a working run from a stuck one.
 *
 * <p>Reporting is <em>opportunity-based</em>: {@link #record} is called after each attempt and emits
 * a summary when at least the interval has passed since the last one. There is no background thread,
 * so a run that stops making attempts simply stops printing — which is itself the signal that it is
 * stuck. The summary carries the attempts and matches so far, the elapsed time, and the mean and
 * standard deviation of the per-attempt time.
 *
 * <p>The clock is injected ({@link System#nanoTime} in production) so the interval logic is testable.
 * Thread-safe: {@link #record} may be called from several evaluation threads under a budget.
 */
public final class ProgressReporter {

    private final long intervalNanos;
    private final Consumer<String> sink;
    private final LongSupplier nanoClock;
    private final long startNanos;

    private long lastReportNanos;
    private long attempts;
    private long matches;
    private double sumMillis;
    private double sumSquareMillis;

    public ProgressReporter(long intervalMillis, Consumer<String> sink, LongSupplier nanoClock) {
        if (intervalMillis < 1) throw new IllegalArgumentException("intervalMillis must be >= 1, got: " + intervalMillis);
        this.intervalNanos = intervalMillis * 1_000_000L;
        this.sink = sink;
        this.nanoClock = nanoClock;
        this.startNanos = nanoClock.getAsLong();
        this.lastReportNanos = startNanos;
    }

    /** Reports to {@code sink} using the system clock. */
    public static ProgressReporter every(long intervalMillis, Consumer<String> sink) {
        return new ProgressReporter(intervalMillis, sink, System::nanoTime);
    }

    /**
     * Records one completed attempt and emits a summary if the interval has elapsed since the last.
     *
     * @param durationNanos how long the attempt took
     * @param matched       whether the attempt produced a match
     */
    public synchronized void record(long durationNanos, boolean matched) {
        attempts++;
        if (matched) matches++;
        double millis = durationNanos / 1_000_000.0;
        sumMillis += millis;
        sumSquareMillis += millis * millis;

        long now = nanoClock.getAsLong();
        if (now - lastReportNanos >= intervalNanos) {
            sink.accept(summary(now));
            lastReportNanos = now;
        }
    }

    /** Emits a summary unconditionally, for the end of a run. No-op when nothing was recorded. */
    public synchronized void reportNow() {
        if (attempts > 0) {
            sink.accept(summary(nanoClock.getAsLong()));
        }
    }

    public synchronized long attempts() {
        return attempts;
    }

    public synchronized long matches() {
        return matches;
    }

    public synchronized double averageMillis() {
        return average();
    }

    public synchronized double deviationMillis() {
        return deviation();
    }

    private double average() {
        return attempts == 0 ? 0.0 : sumMillis / attempts;
    }

    private double deviation() {
        if (attempts == 0) return 0.0;
        double avg = average();
        double variance = Math.max(0.0, sumSquareMillis / attempts - avg * avg);
        return Math.sqrt(variance);
    }

    private String summary(long now) {
        double elapsedSeconds = (now - startNanos) / 1_000_000_000.0;
        return String.format(Locale.ROOT,
            "progress: %d attempts, %d matches, %.0fs elapsed, avg %.1f ms/attempt (sd %.1f ms)",
            attempts, matches, elapsedSeconds, average(), deviation());
    }
}
