package org.example.shell;

/**
 * Executes a shell command and returns its standard output.
 *
 * <p>Implementations are responsible for routing stderr to a log or discarding it; the return
 * value must contain only stdout. The checked exception propagates genuine I/O failures (e.g.
 * {@code /bin/sh} not found) but not non-zero exit codes — callers should inspect the returned
 * string to determine success.
 */
public interface ShellRunner {
    /** Runs {@code command} via the system shell and returns stdout. */
    String run(String command) throws Exception;
}
