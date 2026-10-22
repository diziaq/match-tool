# input/ — your network and password lists

This directory holds the lists you feed to `wifi-tool` mode 2 (batch match) when you run the
built jar by hand. Each file is one value per line.

- `networks.sample.txt` — example SSID list. One network name per line.
- `passwords.sample.txt` — example password list. One password per line.

The sample files contain dummy names so `run.sh` and `run_v2.sh` run out of the box without
touching any real network. Replace them with your own lists to do real work:

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

The jar resolves a relative `--left` / `--right` against the directory that holds the jar
(`target/`), not against your shell's current directory. The run scripts build absolute paths
from their own location so the jar always gets a valid path. If you call the jar directly, pass
absolute paths too.
