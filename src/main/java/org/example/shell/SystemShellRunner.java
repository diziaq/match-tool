package org.example.shell;

import org.example.io.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ShellRunner} that delegates to {@code /bin/sh -c}. Stdout and stderr are drained; the return
 * value contains only stdout. Stderr is forwarded to {@link Logger#error}. A non-zero exit code is
 * not an error — callers inspect the returned stdout.
 *
 * <p>The timed variant {@link #run(String, Duration)} bounds a command two ways: a <em>soft</em>
 * timeout one second short of the limit, injected into the shell so the command kills itself and
 * frees the device, and a <em>hard</em> kill of the whole process tree at the limit if the soft stop
 * did not take. Either way the method throws {@link TimeoutException}, which callers turn into a
 * failure. The soft stop is signalled by exit code {@value #SOFT_TIMEOUT_EXIT} (the GNU
 * {@code timeout} convention), so a killed attempt is never mistaken for empty-output success.
 */
public class SystemShellRunner implements ShellRunner {

    /** Exit code the soft-timeout wrapper reports when it had to kill the command. */
    private static final int SOFT_TIMEOUT_EXIT = 124;

    private final Logger logger;

    public SystemShellRunner(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String run(String command) throws Exception {
        logger.debug("Executing: " + command);
        var process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        try (var br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) stdout.append(line).append("\n");
        }
        try (var br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) stderr.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        logDiagnostics(exitCode, stdout, stderr);
        return stdout.toString();
    }

    @Override
    public String run(String command, Duration timeout) throws Exception {
        long hardSeconds = Math.max(1, timeout.toSeconds());
        long softSeconds = Math.max(1, hardSeconds - 1);
        logger.debug("Executing (soft " + softSeconds + "s / hard " + hardSeconds + "s): " + command);

        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", withSoftTimeout(command, softSeconds)});
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        Thread outDrain = Thread.ofVirtual().start(() -> drain(process.getInputStream(), stdout));
        Thread errDrain = Thread.ofVirtual().start(() -> drain(process.getErrorStream(), stderr));

        boolean finished = process.waitFor(hardSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(); // wait for the tree to actually die, so the device is free for the next attempt
            outDrain.join();
            errDrain.join();
            throw new TimeoutException("command hard-killed after " + hardSeconds + "s: " + command);
        }
        outDrain.join();
        errDrain.join();

        int exitCode = process.exitValue();
        logDiagnostics(exitCode, stdout, stderr);
        if (exitCode == SOFT_TIMEOUT_EXIT) {
            throw new TimeoutException("command stopped at soft timeout " + softSeconds + "s: " + command);
        }
        return stdout.toString();
    }

    /**
     * Wraps {@code command} so it runs in the background with a watchdog that sends SIGTERM after
     * {@code softSeconds}. If the watchdog had to kill it (the command exits by signal, status &gt;
     * 128) the wrapper exits {@value #SOFT_TIMEOUT_EXIT}; otherwise it passes the command's own exit
     * status through.
     */
    private static String withSoftTimeout(String command, long softSeconds) {
        return command + " & __cmd=$!; "
            + "( sleep " + softSeconds + "; kill -TERM $__cmd 2>/dev/null ) & __wd=$!; "
            + "wait $__cmd 2>/dev/null; __st=$?; "
            + "kill -TERM $__wd 2>/dev/null; wait $__wd 2>/dev/null; "
            + "if [ $__st -gt 128 ]; then exit " + SOFT_TIMEOUT_EXIT + "; else exit $__st; fi";
    }

    private void logDiagnostics(int exitCode, StringBuilder stdout, StringBuilder stderr) {
        logger.debug("Exit code: " + exitCode);
        logger.debug("Stdout (" + stdout.length() + " chars): " + stdout.toString().replace('\n', '|'));
        if (!stderr.toString().isBlank()) {
            logger.error("STDERR: " + stderr.toString().trim());
        }
    }

    private static void drain(InputStream in, StringBuilder sink) {
        try (var br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) sink.append(line).append("\n");
        } catch (IOException ignored) {
            // the stream closed because the process was killed — stop draining
        }
    }
}
