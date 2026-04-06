package org.example.wifi.connect;

public interface NetworkConnector {
    boolean tryConnect(String ssid, String password);
}
