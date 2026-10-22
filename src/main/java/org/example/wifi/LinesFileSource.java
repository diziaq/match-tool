package org.example.wifi;

import org.example.engine.Source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A {@link Source} that reads a text file one line per item, lazily. Each {@link #stream()} call
 * opens a fresh, lazily-read stream over the file, so the source is re-iterable and never holds the
 * whole file in memory — the file may be arbitrarily large.
 *
 * <p>This is the WiFi plugin's source of SSIDs and passwords, but it has no WiFi specifics: it is a
 * plain lines-from-a-file source.
 */
public record LinesFileSource(Path path) implements Source<String> {

    @Override
    public Stream<String> stream() {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }
}
