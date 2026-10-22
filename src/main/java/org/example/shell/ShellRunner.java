package org.example.shell;

import java.time.Duration;

/**
 * Executes a shell command and returns its standard output.
 *
 * <p>Implementations are responsible for routing stderr to a log or discarding it; the return
 * value must contain only stdout. The checked exception propagates genuine I/O failures (e.g.
 * {@code /bin/sh} not found) but not non-zero exit codes — callers should inspect the returned
 * string to determine success.
 */
@FunctionalInterface
public interface ShellRunner {
    /** Runs {@code command} via the system shell and returns stdout. */
    String run(String command) throws Exception;

    /**
     * Runs {@code command} with a hard time limit. If it does not finish within {@code timeout}, the
     * implementation terminates the process (and its children) and throws, so the command cannot
     * block the caller indefinitely. The default ignores the timeout — only a real process-backed
     * runner can honour it; test doubles return immediately anyway.
     *
     * @throws java.util.concurrent.TimeoutException if the command exceeds {@code timeout}
     */
    default String run(String command, Duration timeout) throws Exception {
        return run(command);
    }
}
