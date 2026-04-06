package org.example.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger implements AutoCloseable {

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

    public void info(String message) {
        toConsole(message);
        toFile(message);
    }

    public void print(String message) {
        if (output == Output.CONSOLE || output == Output.BOTH) {
            System.out.print(message);
        }
    }

    public void debug(String message) {
        if (!debugEnabled) return;
        toConsole("[DEBUG] " + message);
        toFile("[DEBUG] " + message);
    }

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
