package org.example.shell;

import org.example.io.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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

    @Test
    void run_doesNotIncludeStderrInReturnValue() throws Exception {
        var runner = new SystemShellRunner(silentLogger());

        String output = runner.run("echo stdout; echo stderr >&2");

        assertThat(output).isEqualTo("stdout\n");
        assertThat(output).doesNotContain("stderr");
    }
}
