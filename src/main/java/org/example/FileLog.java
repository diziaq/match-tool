package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLog implements AutoCloseable {

    private static final DateTimeFormatter TS_FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
    private static final DateTimeFormatter TS_LINE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final Path LOG_DIR = Path.of("logs");

    private final BufferedWriter writer;
    private final Path path;

    public FileLog(LocalDateTime now, String label) {
        try {
            Files.createDirectories(LOG_DIR);
            String filename = now.format(TS_FILE) + "_" + label + ".log";
            path = LOG_DIR.resolve(filename);
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create FileLog", e);
        }
    }

    public void write(String message) {
        try {
            writer.write(LocalDateTime.now().format(TS_LINE) + " " + message);
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
