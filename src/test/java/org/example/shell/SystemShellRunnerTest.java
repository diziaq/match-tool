package org.example.shell;

import org.example.io.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemShellRunnerTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void redirectStreams() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        capturedErr = new ByteArrayOutputStream();
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

        assertEquals("hello\n", output);
    }

    @Test
    void run_returnsMultilineOutput() throws Exception {
        var runner = new SystemShellRunner(silentLogger());

        String output = runner.run("printf 'line1\\nline2\\n'");

        assertTrue(output.contains("line1"));
        assertTrue(output.contains("line2"));
    }

    @Test
    void run_returnsEmptyStringForCommandWithNoOutput() throws Exception {
        var runner = new SystemShellRunner(silentLogger());

        String output = runner.run("true");

        assertEquals("", output);
    }

    @Test
    void run_logsDebugMessages() throws Exception {
        List<String> debugMessages = new ArrayList<>();
        Logger capturingLogger = new Logger(Logger.Output.CONSOLE, true) {
            // Use real console logger — debug messages will appear in captured output
        };
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));

        Logger logger = new Logger(Logger.Output.CONSOLE, true);
        var runner = new SystemShellRunner(logger);
        runner.run("echo test");

        String output = captured.toString();
        assertTrue(output.contains("[DEBUG]"));
        assertTrue(output.contains("Executing"));
    }

    @Test
    void run_writesStderrToErrorLog() throws Exception {
        var runner = new SystemShellRunner(new Logger(Logger.Output.CONSOLE, false));

        runner.run("echo error-output >&2");

        assertTrue(capturedErr.toString().contains("STDERR"));
    }

    @Test
    void run_doesNotIncludeStderrInReturnValue() throws Exception {
        var runner = new SystemShellRunner(silentLogger());

        String output = runner.run("echo stdout; echo stderr >&2");

        assertEquals("stdout\n", output);
        assertFalse(output.contains("stderr"));
    }
}
