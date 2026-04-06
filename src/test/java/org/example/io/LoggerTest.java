package org.example.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

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

            assertTrue(capturedOut.toString().contains("hello"));
            assertTrue(capturedErr.toString().isEmpty());
        }

        @Test
        void debug_writesToStdoutWhenEnabled() {
            new Logger(Logger.Output.CONSOLE, true).debug("detail");

            assertTrue(capturedOut.toString().contains("[DEBUG]"));
            assertTrue(capturedOut.toString().contains("detail"));
        }

        @Test
        void debug_suppressedWhenDisabled() {
            new Logger(Logger.Output.CONSOLE, false).debug("hidden");

            assertTrue(capturedOut.toString().isEmpty());
        }

        @Test
        void error_writesToStderr() {
            new Logger(Logger.Output.CONSOLE, false).error("boom");

            assertTrue(capturedErr.toString().contains("boom"));
            assertTrue(capturedOut.toString().isEmpty());
        }

        @Test
        void print_writesWithoutNewline() {
            new Logger(Logger.Output.CONSOLE, false).print("Enter: ");

            assertEquals("Enter: ", capturedOut.toString());
        }

        @Test
        void info_doesNotWriteToStderr() {
            new Logger(Logger.Output.CONSOLE, false).info("msg");

            assertTrue(capturedErr.toString().isEmpty());
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
            assertTrue(content.contains("file-message"));
            assertTrue(content.matches(".*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"));
        }

        @Test
        void info_doesNotWriteToConsole() throws Exception {
            Path logFile = tempDir.resolve("out.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("silent");
            }

            assertTrue(capturedOut.toString().isEmpty());
        }

        @Test
        void debug_writesToFileWhenEnabled() throws Exception {
            Path logFile = tempDir.resolve("debug.log");
            try (var logger = new Logger(Logger.Output.FILE, true, logFile)) {
                logger.debug("trace-detail");
            }

            assertTrue(Files.readString(logFile).contains("[DEBUG]"));
            assertTrue(Files.readString(logFile).contains("trace-detail"));
        }

        @Test
        void debug_notWrittenToFileWhenDisabled() throws Exception {
            Path logFile = tempDir.resolve("nodebug.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.debug("hidden");
            }

            assertEquals("", Files.readString(logFile));
        }

        @Test
        void multipleWrites_eachOnOwnLine() throws Exception {
            Path logFile = tempDir.resolve("multi.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("line-one");
                logger.info("line-two");
            }

            List<String> lines = Files.readAllLines(logFile);
            assertEquals(2, lines.size());
            assertTrue(lines.get(0).contains("line-one"));
            assertTrue(lines.get(1).contains("line-two"));
        }

        @Test
        void createsParentDirectories() throws Exception {
            Path logFile = tempDir.resolve("nested/dir/out.log");
            try (var logger = new Logger(Logger.Output.FILE, false, logFile)) {
                logger.info("deep");
            }

            assertTrue(Files.exists(logFile));
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

            assertTrue(capturedOut.toString().contains("dual"));
            assertTrue(Files.readString(logFile).contains("dual"));
        }

        @Test
        void error_writesToStderrAndFile() throws Exception {
            Path logFile = tempDir.resolve("err.log");
            try (var logger = new Logger(Logger.Output.BOTH, false, logFile)) {
                logger.error("failure");
            }

            assertTrue(capturedErr.toString().contains("failure"));
            assertTrue(Files.readString(logFile).contains("[ERROR]"));
            assertTrue(Files.readString(logFile).contains("failure"));
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void throwsWhenFileOutputRequestedWithoutPath() {
            assertThrows(IllegalArgumentException.class,
                () -> new Logger(Logger.Output.FILE, false));
        }

        @Test
        void throwsWhenBothOutputRequestedWithoutPath() {
            assertThrows(IllegalArgumentException.class,
                () -> new Logger(Logger.Output.BOTH, false));
        }

        @Test
        void consoleConstructor_doesNotThrow() {
            assertDoesNotThrow(() -> new Logger(Logger.Output.CONSOLE, true));
        }
    }
}
