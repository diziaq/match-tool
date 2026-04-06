package org.example.wifi.connect;

public interface NetworkConnector {
    ConnectOutcome tryConnect(String ssid, String password);
}
