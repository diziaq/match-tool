package org.example.wifi.connect;

import org.example.matcher.MatchOutcome;

/**
 * Classifies {@code networksetup} output into typed {@link MatchOutcome} values.
 *
 * <ul>
 *   <li>Blank output → {@link MatchOutcome.Match} (networksetup is silent on success)</li>
 *   <li>"Could not find network" → {@link MatchOutcome.Unavailable} (network not visible)</li>
 *   <li>"Failed to join network" + "apple80211API" → {@link MatchOutcome.Failure}
 *       (security-type or capability mismatch — error -3912; no password will fix this)</li>
 *   <li>"Failed to join network" (other) → {@link MatchOutcome.Mismatch}
 *       (wrong password — errors -3925, -3958, -3970, -528342014 with "tmpErr")</li>
 *   <li>Anything else → {@link MatchOutcome.Failure}</li>
 * </ul>
 */
class MacOsOutputClassifier implements OutputClassifier {

    @Override
    public MatchOutcome classify(String output) {
        if (output.isBlank())                          return new MatchOutcome.Match();
        if (output.contains("Could not find network")) return new MatchOutcome.Unavailable();
        if (output.contains("Failed to join network")) {
            if (output.contains("apple80211API"))      return new MatchOutcome.Failure(output);
            return new MatchOutcome.Mismatch();
        }
        return new MatchOutcome.Failure(output);
    }
}
