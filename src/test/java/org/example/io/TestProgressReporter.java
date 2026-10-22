package org.example.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * The reporter is opportunity-based: it emits a summary on the first {@code record} after the
 * interval has passed, measured by an injected clock. These pin the throttling and the mean /
 * standard-deviation maths without any real time passing.
 */
class TestProgressReporter {

    private final AtomicLong clock = new AtomicLong(0);
    private final List<String> out = new ArrayList<>();

    private ProgressReporter reporter(long intervalMillis) {
        return new ProgressReporter(intervalMillis, out::add, clock::get);
    }

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }

    @Nested
    class Throttling {

        @Test
        void doesNotReportBeforeInterval() {
            var reporter = reporter(20_000);
            clock.set(ms(5_000));

            reporter.record(ms(10), false);

            assertThat(out).isEmpty();
        }

        @Test
        void reportsOnFirstRecordAfterInterval() {
            var reporter = reporter(20_000);
            clock.set(ms(25_000));

            reporter.record(ms(10), false);

            assertThat(out).singleElement().asString().contains("1 attempts").contains("0 matches");
        }

        @Test
        void reportsAgainOnlyAfterAnotherInterval() {
            var reporter = reporter(20_000);
            clock.set(ms(25_000));
            reporter.record(ms(10), false); // report 1
            clock.set(ms(30_000));
            reporter.record(ms(10), false); // only 5s later — no report
            clock.set(ms(46_000));
            reporter.record(ms(10), true);  // 21s after report 1 — report 2

            assertThat(out).hasSize(2);
            assertThat(out.get(1)).contains("3 attempts").contains("1 matches");
        }
    }

    @Nested
    class Metrics {

        @Test
        void computesAttemptsMatchesAverageAndDeviation() {
            var reporter = reporter(20_000);

            reporter.record(ms(10), false);
            reporter.record(ms(20), false);
            reporter.record(ms(30), true);

            assertThat(reporter.attempts()).isEqualTo(3);
            assertThat(reporter.matches()).isEqualTo(1);
            assertThat(reporter.averageMillis()).isCloseTo(20.0, within(1e-9));
            assertThat(reporter.deviationMillis()).isCloseTo(Math.sqrt(200.0 / 3.0), within(1e-9));
        }

        @Test
        void reportsZeroMetricsBeforeAnyRecord() {
            var reporter = reporter(20_000);

            assertThat(reporter.attempts()).isZero();
            assertThat(reporter.averageMillis()).isCloseTo(0.0, within(1e-9));
            assertThat(reporter.deviationMillis()).isCloseTo(0.0, within(1e-9));
        }
    }

    @Nested
    class FinalReport {

        @Test
        void reportNowEmitsAccumulatedSummary() {
            var reporter = reporter(20_000);
            reporter.record(ms(10), true);

            reporter.reportNow();

            assertThat(out).singleElement().asString().contains("1 attempts").contains("1 matches");
        }

        @Test
        void reportNowIsNoOpWhenNothingRecorded() {
            var reporter = reporter(20_000);

            reporter.reportNow();

            assertThat(out).isEmpty();
        }
    }

    @Nested
    class Validation {

        @Test
        void rejectsNonPositiveInterval() {
            assertThatThrownBy(() -> new ProgressReporter(0, out::add, clock::get))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 1");
        }
    }
}
