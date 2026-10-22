# input/ — your network and password lists

This directory holds the lists you feed to `wifi-tool` mode 2 (batch match) when you run the
built jar by hand. Each file is one value per line.

- `networks.sample.txt` — example SSID list. One network name per line.
- `passwords.sample.txt` — example password list. One password per line.

The sample files contain dummy names so `run.sh` and `run_v2.sh` run out of the box without
touching any real network. Replace them with your own lists to do real work:

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

Then point a run at them with full paths:

```
./run_v2.sh /absolute/path/to/input/networks.txt /absolute/path/to/input/passwords.txt
```

## Authorisation

Only run the batch match against networks you own or are explicitly authorised to test, for
example your own access point, a home lab, or a CTF range. Do not run it against networks that
belong to other people.

## Why full paths

The jar resolves a relative `--networks` / `--passwords` against the directory that holds the jar
(`target/`), not against your shell's current directory. The run scripts build absolute paths
from their own location so the jar always gets a valid path. If you call the jar directly, pass
absolute paths too, for example:

```
java -jar target/wifi-tool.jar --plugin wifi --mode 2 \
     --networks /abs/path/to/input/networks.txt \
     --passwords /abs/path/to/input/passwords.txt
```
