# input/ — your network and password lists

This directory holds the lists you feed to `wifi-tool` mode 2 (batch match) when you run the
built jar by hand. Each file is one value per line.

- `networks.sample.txt` — example SSID list. One network name per line.
- `passwords.sample.txt` — example password list. One password per line.

The sample files contain dummy names so `run.sh` and `run_v2.sh` run out of the box without
touching any real network. Replace them with your own lists to do real work.

## Config file

The command line takes only the plugin name and a config file — everything else lives in a
`.properties` config:

```
java -jar target/wifi-tool.jar --plugin wifi --config input/wifi.properties
```

Copy `wifi.sample.properties` to `wifi.properties` and edit it:

```properties
mode=2                                   # 1 = scan, 2 = batch match
networks=/abs/path/to/input/networks.txt
passwords=/abs/path/to/input/passwords.txt
skip=0
debug=false
```

The plugin validates the config (required keys, that the files exist); the harness only checks the
config file itself is present and readable.

## Discovering what is nearby

To build your own list, run the helper (macOS) to dump every visible network with its signal and
characteristics to a TSV, then delete the rows that are out of scope:

```
./scan_networks.sh                 # writes input/networks_scan.tsv (SSID, Signal, Channel, Band, Security)
# edit input/networks_scan.tsv, keep only the networks you own or may test, then:
cut -f1 input/networks_scan.tsv | tail -n +2 > input/networks.txt
```


```
input/networks.txt     # SSIDs you want to test
input/passwords.txt    # passwords to try
```

Then point a config at them (see above) and run `./run_v2.sh input/wifi.properties`, or let
`run.sh` / `run_v2.sh` generate a config for the samples automatically.

## Authorisation

Only run the batch match against networks you own or are explicitly authorised to test, for
example your own access point, a home lab, or a CTF range. Do not run it against networks that
belong to other people.

## Why absolute paths

The plugin resolves a relative `networks` / `passwords` value against the directory that holds the
jar (`target/`), not against your shell's current directory. Use absolute paths in the config so the
plugin always finds the files. (The `--config` path itself is resolved normally, relative to where
you run the command.)
