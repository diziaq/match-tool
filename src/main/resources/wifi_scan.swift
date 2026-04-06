import CoreWLAN
let i = CWWiFiClient.shared().interface()!
let nets = try i.scanForNetworks(withSSID: nil)
for n in nets { print("\(n.ssid ?? "")|\(n.rssiValue)") }
