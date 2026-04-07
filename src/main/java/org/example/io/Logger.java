package org.example.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight logger with three output modes: console-only, file-only, or both simultaneously.
 *
 * <p>Every entry written to a file is prefixed with an ISO-8601 timestamp
 * ({@code yyyy-MM-dd'T'HH:mm:ss.SSS}). File-backed instances implement {@link AutoCloseable} and
 * should be used in try-with-resources blocks to ensure the underlying writer is flushed and closed.
 *
 * <pre>{@code
 * try (var log = new Logger(Logger.Output.FILE, false, "logs/run.log")) {
 *     log.info("started");
 * }
 * }</pre>
 *
 * <p>Debug messages are suppressed unless {@code debugEnabled} is {@code true}. Error messages
 * always go to {@code stderr} (and to the file when a file is open).
 */
public class Logger implements AutoCloseable {

    /**
     * Controls where log output is directed.
     * {@code FILE} and {@code BOTH} require a file path at construction time.
     */
    public enum Output { CONSOLE, FILE, BOTH }

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final Output output;
    private final boolean debugEnabled;
    private final BufferedWriter fileWriter;

    public Logger(Output output, boolean debugEnabled) {
        if (output == Output.FILE || output == Output.BOTH) {
            throw new IllegalArgumentException("File name required for output mode: " + output);
        }
        this.output = output;
        this.debugEnabled = debugEnabled;
        this.fileWriter = null;
    }

    /** Creates a file-backed logger; {@code fileName} is resolved relative to the directory containing the application jar. */
    public Logger(Output output, boolean debugEnabled, String fileName) {
        this.output = output;
        this.debugEnabled = debugEnabled;
        this.fileWriter = openWriter(jarDir().resolve(fileName));
    }

    /** Package-private: used by tests to write to an explicit path. */
    Logger(Output output, boolean debugEnabled, Path filePath) {
        this.output = output;
        this.debugEnabled = debugEnabled;
        this.fileWriter = openWriter(filePath);
    }

    /** Writes {@code message} to the configured output(s), followed by a newline. */
    public void info(String message) {
        toConsole(message);
        toFile(message);
    }

    /** Writes {@code message} to the console <em>without</em> a trailing newline. No-op for file-only output. */
    public void print(String message) {
        if (output == Output.CONSOLE || output == Output.BOTH) {
            System.out.print(message);
        }
    }

    /** Writes a {@code [DEBUG]} prefixed message. Suppressed entirely when {@code debugEnabled} is {@code false}. */
    public void debug(String message) {
        if (!debugEnabled) return;
        toConsole("[DEBUG] " + message);
        toFile("[DEBUG] " + message);
    }

    /** Writes {@code message} to {@code stderr} and, with an {@code [ERROR]} prefix, to the file. */
    public void error(String message) {
        if (output == Output.CONSOLE || output == Output.BOTH) {
            System.err.println(message);
        }
        toFile("[ERROR] " + message);
    }

    private void toConsole(String message) {
        if (output == Output.CONSOLE || output == Output.BOTH) {
            System.out.println(message);
        }
    }

    private void toFile(String message) {
        if (fileWriter == null) return;
        try {
            fileWriter.write(LocalDateTime.now().format(TIMESTAMP) + " " + message);
            fileWriter.newLine();
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("Logger write failed: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        if (fileWriter != null) fileWriter.close();
    }

    private static Path jarDir() {
        try {
            var jar = Path.of(Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return jar.getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot determine jar location", e);
        }
    }

    private static BufferedWriter openWriter(Path filePath) {
        try {
            if (filePath.getParent() != null) Files.createDirectories(filePath.getParent());
            return Files.newBufferedWriter(filePath, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open log file: " + filePath, e);
        }
    }
}
