package org.example.wifi.connect;

/**
 * Classifies {@code networksetup} output into typed {@link ConnectOutcome} values.
 *
 * <ul>
 *   <li>Blank output → {@link ConnectOutcome.Connected} (networksetup is silent on success)</li>
 *   <li>"Could not find network" → {@link ConnectOutcome.NetworkNotFound}</li>
 *   <li>"Failed to join network" + "apple80211API" → {@link ConnectOutcome.AssociationFailed}
 *       (security mismatch, e.g. wrong auth mode)</li>
 *   <li>"Failed to join network" (other) → {@link ConnectOutcome.WrongPassword}
 *       (e.g. errors -3925, -3958, -3970, -528342014 with "tmpErr")</li>
 *   <li>Anything else → {@link ConnectOutcome.UnknownFailure}</li>
 * </ul>
 */
class MacOsOutputClassifier implements OutputClassifier {

    @Override
    public ConnectOutcome classify(String output) {
        if (output.isBlank())                          return new ConnectOutcome.Connected();
        if (output.contains("Could not find network")) return new ConnectOutcome.NetworkNotFound();
        if (output.contains("Failed to join network")) {
            if (output.contains("apple80211API"))      return new ConnectOutcome.AssociationFailed();
            return new ConnectOutcome.WrongPassword();
        }
        return new ConnectOutcome.UnknownFailure(output);
    }
}
