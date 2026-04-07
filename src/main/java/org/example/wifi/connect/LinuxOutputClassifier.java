package org.example.wifi.connect;

class LinuxOutputClassifier implements OutputClassifier {

    @Override
    public ConnectOutcome classify(String output) {
        return output.toLowerCase().contains("successfully")
            ? new ConnectOutcome.Connected()
            : new ConnectOutcome.UnknownFailure(output);
    }
}
