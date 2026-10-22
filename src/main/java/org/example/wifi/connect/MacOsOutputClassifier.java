package org.example.wifi.connect;

import org.example.engine.Outcome;

/**
 * Classifies {@code networksetup} output into typed {@link Outcome} values.
 *
 * <ul>
 *   <li>Blank output → {@link Outcome.Match} (networksetup is silent on success)</li>
 *   <li>"Could not find network" → {@link Outcome.Skip} (network not visible — no password helps)</li>
 *   <li>"Failed to join network" + "apple80211API" → {@link Outcome.Failure}
 *       (security-type or capability mismatch — error -3912; no password will fix this)</li>
 *   <li>"Failed to join network" (other) → {@link Outcome.Mismatch}
 *       (wrong password — errors -3925, -3958, -3970, -528342014 with "tmpErr")</li>
 *   <li>Anything else → {@link Outcome.Failure}</li>
 * </ul>
 */
class MacOsOutputClassifier implements OutputClassifier {

    @Override
    public Outcome classify(String output) {
        if (output.isBlank())                          return new Outcome.Match();
        if (output.contains("Could not find network")) return new Outcome.Skip(output);
        if (output.contains("Failed to join network")) {
            if (output.contains("apple80211API"))      return new Outcome.Failure(output);
            return new Outcome.Mismatch();
        }
        return new Outcome.Failure(output);
    }
}
