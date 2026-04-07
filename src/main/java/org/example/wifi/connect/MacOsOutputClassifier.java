package org.example.wifi.connect;

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
