# SmmoRPG co-op relay

A reverse tunnel that lets somebody host a singleplayer world for friends without opening
a port. It is plumbing: it never reads the game protocol, holds no account data, and talks
to nothing else.

## What it needs

A machine with a real, reachable TCP port. Any small VPS does — this is a few megabytes of
RAM and almost no CPU, because all it does is copy bytes.

**Shared PHP hosting cannot run this.** It needs a long-lived process listening on a TCP
port, which is exactly what shared hosting does not give you. That is a different product
from the account service, not a smaller version of it.

## Install

```sh
# Node 18 or newer
node --version

mkdir -p /opt/smmorpg-relay
cp relay.js /opt/smmorpg-relay/
```

Run it as a service so it comes back after a reboot:

```ini
# /etc/systemd/system/smmorpg-relay.service
[Unit]
Description=SmmoRPG co-op relay
After=network.target

[Service]
ExecStart=/usr/bin/node /opt/smmorpg-relay/relay.js
Restart=always
RestartSec=5
User=nobody
Environment=RELAY_PORT=25599

[Install]
WantedBy=multi-user.target
```

```sh
systemctl daemon-reload
systemctl enable --now smmorpg-relay
systemctl status smmorpg-relay
```

Open the port in the firewall:

```sh
ufw allow 25599/tcp
```

## Point the mod at it

Set `DEFAULT_HOST` in `com/smmorpg/coop/RelayEndpoints.java` to your relay's hostname and
rebuild the mod. Nothing secret goes in there — the relay authenticates nobody, and Mojang's
session servers do the real vouching at both ends — so it is safe to compile in, and it
means nobody has to type an address to play with a friend.

To test against a relay before you commit to it, start the client with:

```
-Dsmmorpg.relay=your.host:25599
```

## How a session goes

1. The host opens a world, pauses, and picks **Play with friends**. The game publishes to
   `127.0.0.1` only, and the mod dials the relay and keeps one control connection open.
2. The relay hands back a six-character code. The host reads it out.
3. A friend types the code. Their mod opens a door on their own loopback address, dials the
   relay, and says the code.
4. The relay tells the host, the host dials out a second time, and the two sockets are
   stapled together. From then on it is an ordinary Minecraft connection that happens to be
   travelling through a pipe.

Nothing listens on either player's public address at any point, which is why this works
behind carrier-grade NAT where port forwarding cannot.

## Limits and behaviour

- 17 connections per session (sixteen players and one spare, so a reconnect never bounces).
- A session closes 90 seconds after the host stops answering.
- A waiting guest is dropped after 15 seconds if the host never dials back.
- Codes avoid `O`, `0`, `I` and `1`, because they are read aloud.

## What the code is

A password to somebody's game. Anyone holding it can join while the session is open; the
host stops that by closing the session, which issues a fresh code next time. There is no
ban list here — if you need one, it belongs on the host's own server, where the game
already knows who everyone is.
