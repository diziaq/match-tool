package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

@FunctionalInterface
public interface OutputClassifier extends PlatformDependent {
    ConnectOutcome classify(String output);

    static OutputClassifier of(Platform platform) {
        return switch (platform) {
            case MACOS -> OutputClassifier::classifyMac;
            case LINUX -> output -> output.toLowerCase().contains("successfully")
                ? new ConnectOutcome.Connected()
                : new ConnectOutcome.UnknownFailure(output);
        };
    }

    private static ConnectOutcome classifyMac(String output) {
        if (output.isBlank())                          return new ConnectOutcome.Connected();
        if (output.contains("Could not find network")) return new ConnectOutcome.NetworkNotFound();
        if (output.contains("Failed to join network")) {
            if (output.contains("apple80211API"))      return new ConnectOutcome.AssociationFailed();
            return new ConnectOutcome.WrongPassword();
        }
        return new ConnectOutcome.UnknownFailure(output);
    }
}
