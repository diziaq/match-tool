package org.example.wifi.connect;

/**
 * POSIX single-quote shell escaping for command arguments.
 *
 * <p>Single-quoting a value in POSIX shells is the safest way to pass arbitrary strings as
 * arguments without the shell interpreting special characters. The only character that requires
 * special handling inside single quotes is the single quote itself, escaped as {@code '\''}.
 */
class ShellEscape {
    private ShellEscape() {}

    /**
     * Wraps {@code s} in single quotes and escapes any embedded single quote as {@code '\''}.
     * Example: {@code O'Brien} → {@code 'O'\''Brien'}.
     */
    static String quoted(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
