package org.example.wifi.connect;

class ShellEscape {
    private ShellEscape() {}

    static String quoted(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
