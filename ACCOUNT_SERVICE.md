# Account service contract

The mod does not ship with a backend. This file is the exact contract a server-side service
has to satisfy for `[account_service]` in `smmorpg-server.toml` to do anything.

**You do not need one.** Leaving `backendUrl` and `backendApiKey` empty is a complete,
supported setup: accounts live in `<world>/smmorpg/accounts.json`, nothing is contacted, and
everything except cross-server sync works. Build this only when you want one ladder and one
vault shared across several servers.

## Authentication

Every request carries:

```
Authorization: Bearer <backendApiKey>
```

Reject anything without a matching key. The key never reaches a client — it lives in the
SERVER config, which NeoForge does not send.

## Endpoints

### `GET /health`

Liveness probe. Return **200** with any JSON body (`{}` is fine). Anything else, or no
answer, and the server treats the service as down and keeps working from its local mirror.

### `GET /accounts/{uuid}`

Return that player's account as JSON, or **404** if you have never seen them.

### `POST /accounts/{uuid}`

Body is the same account JSON. Store it. Return **200**.

**This must be idempotent.** The mod queues writes to disk and replays them after an outage,
so the same body can arrive twice. Replaying a write that already landed has to be a no-op —
if it grants coins a second time, an outage becomes a duplication exploit.

Use the `revision` field to resolve conflicts: **if the incoming `revision` is lower than the
one you have stored, keep yours and return 200 anyway.** The mod does the same comparison on
its side when it pulls, so a server that played on through an outage is never overwritten by
a stale copy.

## Account JSON

```json
{
  "uuid": "8f4d0e2a-1c3b-4a55-9e11-77c0d2a4b901",
  "name": "Steve",
  "elo": 1000,
  "wins": 0,
  "losses": 0,
  "coins": 0,
  "premium": 0,
  "vault": [
    { "item": "minecraft:diamond_sword", "count": 1, "damage": 412, "gear": "" }
  ],
  "revision": 0
}
```

| Field | Type | Notes |
|---|---|---|
| `uuid` | string | Player UUID, dashed. Matches the path parameter. |
| `name` | string | Last known name. Refreshed on every join; do not key anything on it. |
| `elo` | int | Rating. Starts at 1000, floors at 100. |
| `wins` / `losses` | int | Rated matches only. |
| `coins` | long | In-game currency. |
| `premium` | long | Optional, defaults to 0. Granted by Tebex. |
| `vault` | array | Stored stacks. May be empty, never absent. |
| `revision` | long | Optional, defaults to 0. Increments on every mutation. |

Vault entries: `item` is a namespaced item id, `damage` is the durability already used
(optional, 0), `gear` is the mod's rolled-affix blob as a JSON string (optional, `""`).
Treat `gear` as opaque — store and return it byte for byte, never parse or rewrite it.

## Shared hosting? Use the PHP version

Apache runs PHP, not Node. On Hostinger, cPanel or Plesk, `service/php/` has a complete
implementation of everything below, plus the `.htaccess` needed to make path routing and
the Authorization header work. See `service/php/README.md`.

The Node version below is for a host where you can actually run a Node process.

## Minimal reference implementation

```js
// node, express, one JSON file. Enough to prove the contract; not a production service.
import express from "express";
import { readFileSync, writeFileSync, existsSync } from "fs";

const KEY = process.env.SMMORPG_KEY;
const FILE = "accounts.json";
const db = existsSync(FILE) ? JSON.parse(readFileSync(FILE, "utf8")) : {};
const save = () => writeFileSync(FILE, JSON.stringify(db));

const app = express();
app.use(express.json({ limit: "1mb" }));
app.use((req, res, next) =>
  req.get("authorization") === `Bearer ${KEY}` ? next() : res.sendStatus(401));

app.get("/health", (_, res) => res.json({}));

app.get("/accounts/:uuid", (req, res) => {
  const account = db[req.params.uuid];
  return account ? res.json(account) : res.sendStatus(404);
});

app.post("/accounts/:uuid", (req, res) => {
  const incoming = req.body;
  const existing = db[req.params.uuid];
  // Older revision: keep what we have. This is what makes a replayed write harmless.
  if (!existing || (incoming.revision ?? 0) >= (existing.revision ?? 0)) {
    db[req.params.uuid] = incoming;
    save();
  }
  res.sendStatus(200);
});

app.listen(8080);
```

Then, on each server:

```toml
[account_service]
    backendUrl = "https://api.example.com"
    backendApiKey = "a-long-random-string-you-generated"
    syncIntervalSeconds = 30
```

Put it behind HTTPS. The key is a bearer token in a header; over plain HTTP anyone on the
path can read it and write to any account.

## Checking it works

In game, as an operator:

```
/smmorpg status
```

It reports whether the service is reachable and how many writes are still queued. A queue
that keeps growing means writes are failing — check the key first, it is almost always the key.
