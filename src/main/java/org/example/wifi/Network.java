package org.example.wifi;

public record Network(String ssid, String signal) implements Comparable<Network> {
    @Override
    public int compareTo(Network other) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.ssid, other.ssid);
    }
}
