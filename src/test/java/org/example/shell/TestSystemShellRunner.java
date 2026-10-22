package org.example.shell;

import org.example.io.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

class TestSystemShellRunner {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream capturedOut;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void redirectStreams() {
        capturedOut = new ByteArrayOutputStream();
        capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private Logger silentLogger() {
        return new Logger(Logger.Output.CONSOLE, false);
    }

    @Nested
    class OutputCapture {

        @Test
        void run_capturesStdout() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            String output = runner.run("echo hello");

            assertThat(output).isEqualTo("hello\n");
        }

        @Test
        void run_returnsMultilineOutput() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            String output = runner.run("printf 'line1\\nline2\\n'");

            assertThat(output).contains("line1").contains("line2");
        }

        @Test
        void run_returnsEmptyStringForCommandWithNoOutput() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            String output = runner.run("true");

            assertThat(output).isEmpty();
        }

        @Test
        void run_doesNotIncludeStderrInReturnValue() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            String output = runner.run("echo stdout; echo stderr >&2");

            assertThat(output).isEqualTo("stdout\n");
            assertThat(output).doesNotContain("stderr");
        }

        // Opinion: a non-zero exit code is not an exception — the runner still returns stdout.
        // This is intentional: nmcli/networksetup sometimes exit non-zero yet produce useful
        // output that the OutputClassifier needs to inspect.
        @Test
        void run_nonZeroExitCode_stillReturnsStdout() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            String output = runner.run("echo output; exit 1");

            assertThat(output).isEqualTo("output\n");
        }
    }

    @Nested
    class Timeouts {

        /** The soft watchdog (timeout-1s) stops a long command well before the hard limit. */
        @Test
        @Timeout(15)
        void stopsCommandExceedingTheLimit() {
            var runner = new SystemShellRunner(silentLogger());

            long startNanos = System.nanoTime();
            assertThatThrownBy(() -> runner.run("sleep 10", Duration.ofSeconds(2)))
                .isInstanceOf(TimeoutException.class);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

            assertThat(elapsedMillis).isLessThan(5_000); // soft-stopped near 1s, not after sleep 10
        }

        /** A command that ignores SIGTERM is hard-killed at the limit. */
        @Test
        @Timeout(15)
        void hardKillsCommandThatIgnoresTheSoftStop() {
            var runner = new SystemShellRunner(silentLogger());

            long startNanos = System.nanoTime();
            assertThatThrownBy(() -> runner.run("sh -c 'trap \"\" TERM; sleep 10'", Duration.ofSeconds(2)))
                .isInstanceOf(TimeoutException.class);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

            assertThat(elapsedMillis).isLessThan(6_000); // hard-killed near 2s, not after sleep 10
        }

        @Test
        @Timeout(15)
        void returnsOutputWhenCommandFinishesWithinTheLimit() throws Exception {
            var runner = new SystemShellRunner(silentLogger());

            assertThat(runner.run("echo quick", Duration.ofSeconds(5))).isEqualTo("quick\n");
        }
    }

    @Nested
    class Logging {

        @Test
        void run_logsDebugMessages() throws Exception {
            var runner = new SystemShellRunner(new Logger(Logger.Output.CONSOLE, true));
            capturedOut.reset();

            runner.run("echo test");

            assertThat(capturedOut.toString()).contains("[DEBUG]").contains("Executing");
        }

        @Test
        void run_writesStderrToErrorLog() throws Exception {
            var runner = new SystemShellRunner(new Logger(Logger.Output.CONSOLE, false));

            runner.run("echo error-output >&2");

            assertThat(capturedErr.toString()).contains("STDERR");
        }
    }
}
