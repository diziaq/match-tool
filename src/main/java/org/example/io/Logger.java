package org.example.io;

import java.io.BufferedWriter;
import java.io.IOException;
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
            throw new IllegalArgumentException("File path required for output mode: " + output);
        }
        this.output = output;
        this.debugEnabled = debugEnabled;
        this.fileWriter = null;
    }

    public Logger(Output output, boolean debugEnabled, Path filePath) {
        this.output = output;
        this.debugEnabled = debugEnabled;
        if (output == Output.FILE || output == Output.BOTH) {
            try {
                if (filePath.getParent() != null) Files.createDirectories(filePath.getParent());
                this.fileWriter = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open log file: " + filePath, e);
            }
        } else {
            this.fileWriter = null;
        }
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
}
