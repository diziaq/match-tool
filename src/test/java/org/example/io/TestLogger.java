package org.example.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestLogger {

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

    @Nested
    class ConsoleOutput {

        @Test
        void info_writesToStdout() {
            new Logger(Logger.Output.CONSOLE, false).info("hello");

            assertThat(capturedOut.toString()).contains("hello");
            assertThat(capturedErr.toString()).isEmpty();
        }

        @Test
        void debug_writesToStdoutWhenEnabled() {
            new Logger(Logger.Output.CONSOLE, true).debug("detail");

            assertThat(capturedOut.toString()).contains("[DEBUG]").contains("detail");
        }

        @Test
        void debug_suppressedWhenDisabled() {
            new Logger(Logger.Output.CONSOLE, false).debug("hidden");

            assertThat(capturedOut.toString()).isEmpty();
        }

        @Test
        void error_writesToStderr() {
            new Logger(Logger.Output.CONSOLE, false).error("boom");

            assertThat(capturedErr.toString()).contains("boom");
            assertThat(capturedOut.toString()).isEmpty();
        }

        @Test
        void print_writesWithoutNewline() {
            new Logger(Logger.Output.CONSOLE, false).print("Enter: ");

            assertThat(capturedOut.toString()).isEqualTo("Enter: ");
        }

        @Test
        void info_doesNotWriteToStderr() {
            new Logger(Logger.Output.CONSOLE, false).info("msg");

            assertThat(capturedErr.toString()).isEmpty();
        }
    }

    @Nested
    class FileOutput {

        @TempDir Path tempDir;

        @Test
        void info_writesToFileWithTimestamp() throws Exception {
            Path logFile = tempDir.resolve("out.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("file-message");
            }

            String content = Files.readString(logFile);
            assertThat(content).contains("file-message");
            assertThat(content).matches("(?s).*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
        }

        @Test
        void info_doesNotWriteToConsole() throws Exception {
            Path logFile = tempDir.resolve("out.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("silent");
            }

            assertThat(capturedOut.toString()).isEmpty();
        }

        @Test
        void debug_writesToFileWhenEnabled() throws Exception {
            Path logFile = tempDir.resolve("debug.log");
            try (var logger = new Logger(Logger.Output.FILE, true, logFile)) {
                logger.debug("trace-detail");
            }

            String content = Files.readString(logFile);
            assertThat(content).contains("[DEBUG]").contains("trace-detail");
        }

        @Test
        void debug_notWrittenToFileWhenDisabled() throws Exception {
            Path logFile = tempDir.resolve("nodebug.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.debug("hidden");
            }

            assertThat(Files.readString(logFile)).isEmpty();
        }

        @Test
        void multipleWrites_eachOnOwnLine() throws Exception {
            Path logFile = tempDir.resolve("multi.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("line-one");
                logger.info("line-two");
            }

            List<String> lines = Files.readAllLines(logFile);
            assertThat(lines).hasSize(2);
            assertThat(lines.get(0)).contains("line-one");
            assertThat(lines.get(1)).contains("line-two");
        }

        @Test
        void createsParentDirectories() throws Exception {
            Path logFile = tempDir.resolve("nested/dir/out.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("deep");
            }

            assertThat(logFile).exists();
        }
    }

    @Nested
    class BothOutput {

        @TempDir Path tempDir;

        @Test
        void info_writesToBothConsoleAndFile() throws Exception {
            Path logFile = tempDir.resolve("both.log");
            try (var logger = new Logger(Logger.Output.BOTH, false, logFile)) {
                logger.info("dual");
            }

            assertThat(capturedOut.toString()).contains("dual");
            assertThat(Files.readString(logFile)).contains("dual");
        }

        @Test
        void error_writesToStderrAndFile() throws Exception {
            Path logFile = tempDir.resolve("err.log");
            try (var logger = new Logger(Logger.Output.BOTH, false, logFile)) {
                logger.error("failure");
            }

            assertThat(capturedErr.toString()).contains("failure");
            assertThat(Files.readString(logFile)).contains("[ERROR]").contains("failure");
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void throwsWhenFileOutputRequestedWithoutPath() {
            assertThatThrownBy(() -> new Logger(Logger.Output.FILE, false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void throwsWhenBothOutputRequestedWithoutPath() {
            assertThatThrownBy(() -> new Logger(Logger.Output.BOTH, false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void consoleConstructor_doesNotThrow() {
            assertThatCode(() -> new Logger(Logger.Output.CONSOLE, true))
                .doesNotThrowAnyException();
        }
    }
}
