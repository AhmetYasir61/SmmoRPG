# Account service on shared hosting (Hostinger, cPanel, Plesk)

## Why PHP and not the Node example

`public_html` is served by Apache. Apache runs PHP; it does **not** run Node. An `index.js`
uploaded there is not a service — it is either handed to whoever asks for it as plain text,
or refused with 403. Both of those you have probably already seen.

Shared hosting plans occasionally offer a "Node.js App" panel that runs outside `public_html`
on its own port. If yours has one, the Node version in `ACCOUNT_SERVICE.md` works there. If
it does not, use this.

## Upload

```
public_html/PmmoRPG_Api/index.php     <- from service/php/
public_html/PmmoRPG_Api/.htaccess     <- from service/php/
```

Delete the `index.js` that is there now. It cannot run, and leaving a readable script in the
web root is only a way to hand someone your configuration.

## Two things to change before you upload

**1. The key.** In `index.php`:

```php
const API_KEY = 'CHANGE-ME-to-a-long-random-string';
```

Make it 40+ random characters. The same string goes in every game server's
`smmorpg-server.toml` as `backendApiKey`. Anyone who learns it can read and rewrite every
account, so keep it out of screenshots and support messages.

**2. Where the data lives.** By default:

```php
const DATA_DIR = __DIR__ . '/../../smmorpg_data';
```

That is two levels above `PmmoRPG_Api`, which puts it **outside `public_html`** — so the
account files cannot be downloaded by anyone who guesses the URL. If your layout is
different, move it, but keep it out of the web root. The `.htaccess` rule that blocks
`.json` is a second line of defence, not the first one.

## Point the game servers at it

```toml
[account_service]
    backendUrl = "https://your-domain.tld/PmmoRPG_Api"
    backendApiKey = "the-same-long-random-string"
    syncIntervalSeconds = 30
```

No trailing slash on the URL.

## Check it before trusting it

```bash
# Should print {"ok":true} — no key needed, this is the liveness probe.
curl https://your-domain.tld/PmmoRPG_Api/health

# Should print {"error":"unauthorised"}.
curl https://your-domain.tld/PmmoRPG_Api/accounts/00000000-0000-0000-0000-000000000000

# Should print {"error":"no such account"} — which means the key works.
curl -H "Authorization: Bearer YOUR-KEY" \
     https://your-domain.tld/PmmoRPG_Api/accounts/00000000-0000-0000-0000-000000000000
```

If the third one still says `unauthorised`, the host is stripping the Authorization header
and the `.htaccess` did not take. Check that `.htaccess` uploaded (it starts with a dot and
some file managers hide it) and that the host allows `RewriteEngine`.

In game, as an operator: `/smmorpg status` reports whether the service is reachable and how
many writes are still queued.

## What this does and does not do

It stores one JSON file per player, writes atomically so an interrupted save cannot corrupt
an account, and compares revisions so a replayed write is a no-op and a stale copy never
overwrites a newer one. That is the whole contract the mod needs.

It is a flat-file store. It is fine for a few hundred players on one network. It is not a
database, there is no index, and `/leaderboard` would have to read every file — so if the
ladder grows to the point where that hurts, that is the moment to move to MySQL, which your
hosting already has.
