package org.example.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLog implements AutoCloseable {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
    private static final DateTimeFormatter LINE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final Path LOG_DIR = Path.of("logs");

    private final BufferedWriter writer;

    public FileLog(LocalDateTime timestamp, String label) {
        try {
            Files.createDirectories(LOG_DIR);
            String filename = timestamp.format(FILE_TIMESTAMP) + "_" + label + ".log";
            writer = Files.newBufferedWriter(LOG_DIR.resolve(filename), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log file", e);
        }
    }

    public void write(String message) {
        try {
            writer.write(LocalDateTime.now().format(LINE_TIMESTAMP) + " " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("FileLog write failed: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
