package org.example.wifi;

/**
 * Domain object representing a WiFi network password.
 *
 * <p>A thin wrapper around a {@link String} value that makes credential handling explicit in
 * method signatures and prevents a raw password string from being accidentally swapped with
 * other string parameters such as an SSID.
 */
public record Password(String value) {}
