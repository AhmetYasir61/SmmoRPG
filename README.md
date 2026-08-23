# SmmoRPG — RPG and Gore Layer for Epic Fight

A NeoForge mod for Minecraft 1.21.1 that turns an Epic Fight setup into an MMORPG: classes
chosen on first join, levels and stats, loot with holy and cursed affixes that roll onto
*any* weapon, a training arena that scales to absurdity — and, underneath all of it, wounds.
Cuts that land where the blade actually met the body, bleed, and slowly close.

**SmmoRPG does not implement combat.** It sits on top of mods that already do it better:

| Mod | What it owns here |
|---|---|
| **Epic Fight** (required) | Animations, movesets, colliders, skills, battle mode, mob combat |
| **ParCool** | Traversal — wall runs, wall jumps, rolls, vaults |
| **Epic Parcool** | Bridges ParCool's movement into Epic Fight's combat states |
| **Real Camera** | The first-person body and where the camera sits inside it |
| **Better Lock On** | Lock-on targeting |
| **Weapons of Miracles** | Weapons, which roll SmmoRPG affixes like anything else |
| **P1nero's Epic Fight Bow** | Bow animations |

What SmmoRPG adds on top:

- **Locational wounds.** Epic Fight knows exactly where its collider met the target;
  SmmoRPG turns that point into a cut on that body part, at that angle, of that depth.
- **60 impact sounds**, chosen by what the blade met and where — 8 materials × 7 body
  locations, plus parry, whiff, decapitate and dismember.
- **Bleeding, dismemberment, and wounds that close gradually**, so a regenerating mob
  visibly knits itself back together instead of snapping from shredded to clean.
- **Classes, levels, stats** and a skill tree.
- **Loot with rarity and holy/cursed affixes**, rolled onto any weapon in the game.
- **A training arena** at 1%–100000% difficulty with divine bands past 100%.
- **Camera shake and recoil** for hits, landings and sprinting.
- Server-pushed content, an auto-update flow, and a server browser.

---

## Installing

Put these in `mods/` alongside SmmoRPG. Epic Fight is required; the rest are optional but
the mod is designed around all of them:

```
epicfight            21.17.3.1      (required)
parcool              3.4.3.1
epicparcool          21.0.0
realcamera           0.7.8-beta     (+ cloth_config, optional but recommended)
betterlockon         2.0.8
wom                  2.0.176
p1nero_bow           21.16.1.0
```

## Online: accounts, ranked play and the store

One jar ships to everyone. What makes an install a ranked online server rather than a
singleplayer game is `config/smmorpg-server.toml` and nothing else — and because NeoForge
never sends a SERVER config to a client, that is also the only safe place for credentials.

**Clients configure nothing.** A client never contacts the account service at all: it asks
the server it is playing on, and the answer comes back as a packet. One secret, in one
place, held by the operator. The public service address is compiled into the mod
(`BackendEndpoints`), so installing the modpack is the whole setup; the config only exists
to point a server at a different service. The API key is deliberately *not* compiled in —
a secret inside a downloadable jar can be read out of the file in about ten seconds.

### Nothing is lost when the service is down

Every account write goes through an on-disk queue before it goes anywhere else:

1. The write is appended to `pending-writes.json` in the world folder and flushed immediately.
2. It is only dropped once the service has acknowledged it.
3. A server restarted mid-outage picks the queue back up on the next boot and keeps trying.

Alongside it, `accounts.json` is a full local mirror of every account the server has touched.
If the service is unreachable at boot, play continues from the mirror and the queue catches
the service up later. Reads never block on the network, so a player logging in during an
outage plays normally rather than staring at a loading screen.

Writes carry an id and are expected to be **idempotent** on the service's side — replaying
one that already landed has to be a no-op rather than a second grant.

### Ranked

Elo with two adjustments a game needs that chess does not: K falls as a player settles, so a
newcomer finds their level in a handful of matches and a Grandmaster's rating does not swing
on one bad night; and there is a floor, so a losing streak cannot bury someone below the
point where the ladder still has opponents for them.

Queues widen the longer you wait — a strict rating gap gives perfect matches and infinite
queues, no gap gives instant matches nobody enjoys. Widening makes the trade-off something
the player feels rather than a number an operator guessed once.

Modes are `duel` (1v1), `doubles` (2v2) and `friendly` (unrated). In 2v2 a side loses only
when every member is down, which is what makes it a team fight rather than two duels sharing
a floor. Leaving mid-match forfeits — a ladder where quitting before you lose costs nothing
is a ladder nobody trusts.

```
/smmorpg queue duel      /smmorpg queue doubles     /smmorpg queue friendly
/smmorpg leave           /smmorpg rank              /smmorpg status   (operators)
```

### The store

Real-money purchases go through **Tebex**. The mod never sees a card number, a billing
address or a payment token: checkout happens on your Tebex store in the player's browser,
and the server only asks Tebex which purchases are due, delivers them, and tells Tebex they
were delivered. That acknowledgement is per-command rather than batched, which is what stops
a network hiccup from granting the same package twice.

Tebex commands are **not** run as console commands. Only package types the mod understands
are honoured (`smmorpg:coins`, `smmorpg:premium`); anything else is logged and left
undelivered. Executing arbitrary commands handed over the network is a remote code execution
waiting to happen.

Set `tebexSecret` and `tebexStoreUrl` in the server config.

### Inventory, vault and wear

Loot stays on the ground until you **crouch** to take it. Walking through a room should not
fill your bag with things you never chose.

The vault stores damage explicitly, so a blade put away at half durability comes back at
half durability — otherwise repairing anything would be pointless. Anvil repairs keep rolled
affixes, which vanilla would discard.

## What is built and what is not

Built and working: accounts, the offline queue and local mirror, Elo, ranks, queues, 1v1 and
2v2 match flow with forfeits and timeouts, coin rewards, the Tebex delivery pipeline, manual
pickup, durability and repair rules, and the server↔client account sync.

**Not built yet:** the main-menu screens for the market, the vault, the leaderboard and the
queue. The systems underneath them all work and are reachable by command today; the screens
are a display layer on top of flows that already run, which is a much smaller piece of work
than building both at once. Also still to come: the vault's trash and deposit slots as an
actual container UI, and kit loadouts drawn from the vault.

## Settings this pack holds for you

SmmoRPG pins a couple of its dependencies' settings on every world join, because they are
pack-level decisions rather than taste:

- **Epic Fight's compute shader is turned off.** Epic Fight can skin its armatures on the
  GPU; on drivers where that misbehaves you get a mangled or invisible model rather than a
  crash, which is a miserable thing to diagnose.
- **Real Camera is kept on, and kept from switching itself off** while you sneak, swim or
  crawl. A first-person body that vanishes when you crouch is worse than none at all,
  because it teaches you not to trust what you are looking at.

Camera offsets, bind targets and smoothing are **not** touched — Real Camera's author tuned
those and this pack has no better numbers to put in their place. Nor is the classic/binding
mode choice, which stays yours.

All of it is under `[compatibility]` in `config/smmorpg-common.toml`, and
`enforceModSettings = false` stops SmmoRPG touching another mod's settings entirely.

## Licensing — read this before distributing

Epic Fight's code is **GPL-3.0**. SmmoRPG links against it, so a distributed build of the
two together is a combined work and is expected to be GPL-3.0 as well. This mod is
therefore licensed **GPL-3.0**, not MIT as it was before it depended on Epic Fight.

ParCool is LGPL-3.0, which linking does not affect. Real Camera is MIT. Better Lock On,
Weapons of Miracles, Epic Parcool and P1nero's Bow are **All Rights Reserved** — they are
dependencies you install, and none of them may be redistributed inside a pack or a jar
without their authors' permission. That is also why `libs/` is not committed.

## Building

The seven jars above go in `libs/`. They are `compileOnly` — never shaded into the output —
and declared as real mod dependencies in `neoforge.mods.toml`.

```bash
gradle build          # jar lands in build/libs/
```

---

## Where to put your weapon art

You said you would design the weapons yourself. Here is exactly where each file goes.

| What | Path |
|---|---|
| Weapon icon (item texture) | `src/main/resources/assets/smmorpg/textures/item/<name>.png` |
| Weapon item model | `src/main/resources/assets/smmorpg/models/item/<name>.json` |
| Weapon name (all languages) | `tools/lang_words.json` → `items` block, then run the lang script |

The ten built-in weapon ids are:

```
katana  odachi  dao  jian  dagger  tanto  spear  naginata  kanabo  yumi
```

So a katana you draw goes at `.../textures/item/katana.png`, and it replaces what is there
immediately — nothing else to change.

Accessories and consumables use the same folder: `ring`, `amulet`, `omamori`, `talisman`,
`bandage`, `cautery_iron`, `blood_vial`.

### The demo designs already in the repo

`tools/generate_weapon_textures.py` produces a 128×128 demo icon for all seventeen. They
are reference designs, not final art — they exist so the mod has something coherent to look
at now, and so every weapon that arrives later (loot drops, server-pushed weapons, whatever
the training arena hands out) has one silhouette language to follow.

That language is: **every weapon lies on the same bottom-left-to-top-right diagonal**,
pommel at the low corner and point at the high one, hilt and tip both drawn. A blade
standing straight up and down reads as a stick in the hand, so nothing here is vertical.
Each weapon is a curved spine plus a width profile plus a run of segments (pommel, grip,
guard, blade, tip), which is why a katana and a spear differ in their numbers rather than
in their drawing code.

```bash
python3 tools/generate_weapon_textures.py              # 128x128, the default
python3 tools/generate_weapon_textures.py --size 256   # same designs, bigger
```

Pure standard library — no Pillow, no numpy, nothing to install. When your own art lands,
just overwrite the PNGs; the mod reads the files, not the generator.

For a **handheld** weapon the model should stay `item/handheld` (already set), which is
what makes it sit in the fist correctly. Only change the parent if you author a full 3D
model.

Everything else visual — wound decals, blood, the HUD, the first-person body — is already
handled and needs nothing from you.

---

## Building

```bash
./gradlew build          # or: gradle build
```

The jar lands in `build/libs/`.

### The 60 impact sounds

The sounds are **synthesised**, not shipped as audio, because the repo should not carry
4 MB of binaries. Generate them once before your first run:

```bash
python3 tools/generate_impact_sounds.py
```

This needs `ffmpeg` (or `oggenc`) on your PATH, because Minecraft loads `.ogg` only. Without
it the script writes `.wav` files and tells you the one command to convert them.

The 60 are: every combination of 8 impact materials × 7 body locations (56), plus parry,
whiff, decapitate and dismember. A katana skimming a chainmail shoulder and the same katana
splitting a bare skull are genuinely different sounds.

### Languages

36 locales ship in `assets/smmorpg/lang/`. There is **no in-mod language menu on purpose**:
Minecraft already picks the file matching the client's own language setting, so changing
the language in Options changes the mod with it.

To edit or add a language, edit `tools/lang_words.json` and run:

```bash
python3 tools/generate_languages.py
```

Any locale that does not translate a given string falls back to English per key, so a
partial translation is still a valid, shippable file.

---

## What is in here

### Wounds — the cut lands where the blade did

`integration/EpicFightBridge.java` listens to Epic Fight's `DealDamageEvent.Post` and reads
`EpicFightDamageSource.getInitialPosition()` — the contact point of the collider that
actually connected. `HitboxResolver.at` decides which body part that point fell inside and
where on it, and the wound is opened there.

That is the whole point: cut a thigh low and you get a low gash across the thigh; follow
through on a shoulder and you get one at the angle you actually swung. Nothing is authored,
and because the position comes from the system that really swung the weapon, retuning an
Epic Fight animation moves the cuts with it for free.

When a blow has no collider behind it — a projectile, a skill — the bridge falls back to a
ray from the attacker's eyes, so no hit is ever left without a location.

### Wounds, blood and healing

- Wounds carry `u`/`v` inside the struck part, an angle, a length and a depth.
- Bleeding is per wound and per second; a serrated or cursed blade bleeds far harder.
- Wounds **close gradually**. A regenerating mob visibly knits itself back together rather
  than snapping from shredded to clean, and a closing cut darkens into a scar before it goes.
- Limbs come off when the blow is both severe enough and a cutting one. A severed part is
  hidden on the model and never closes.
- Bandages close a wound; the cautery iron closes it outright and burns you doing it.

The decals are drawn at high resolution over otherwise vanilla-looking models — the world
stays ordinary, the cuts are crisp.

### Feel

- `CameraShake` — two detuned oscillators plus noise, trauma-squared so light hits are
  subtle and heavy ones are violent, with a separate spring-returned recoil channel.
- Hit-stop freezes the frame briefly on a solid connect.
- Sprinting, landing and being hit all feed the same shake channel, so the whole game reads
  through one physical language.

### Training arena

A square **T** button beside Singleplayer on the title screen. Pick a difficulty from 1% to
100000% on a logarithmic slider (or type it exactly).

Pressed from inside a world it starts the arena where you stand. Pressed from the title
screen there is no server to ask, so a dedicated singleplayer world (`smmorpg_training`)
is created on the first visit and reused after, and the request is held until the player
is actually in it. Keep-inventory is on and the day, weather and natural spawns are frozen
— the arena is for practising fights, not for losing your gear to one.

The training world is a **superflat**: bedrock, a little stone, a smooth-stone surface, and
nothing else. Decoration, structures and lakes are all off, so there is no terrain to fight
around and **no water anywhere**.

Inside it the session lays down a **32×32 arena** — a polished floor with a grid line every
eight blocks so you can read your own footwork, a four-high wall so a bot cannot back you
into open ground without you knowing, lanterns above it for a session started at midnight,
and 24 blocks of cleared headroom for air jumps and wall runs. 32 across is deliberate:
small enough that a fight stays a fight rather than a chase, wide enough that a dash still
means something. Walking out past the wall ends the session.

### What comes at you

Opponents come from a tiered roster (`mob/MobRoster.java`), and two rules govern all of it.
**Nothing in it burns in daylight**, because a fight decided by the sunrise is a fight you
did not win. And **nothing in it removes itself** — no creepers, nothing that ends the
exchange by ceasing to exist, because there is nothing to learn from that.

The table climbs with the difficulty band, and the band decides what walks in rather than
just how much health it has:

| Tier | From band | What you fight |
|---|---|---|
| Mortal | 0 | Conscripts, soldiers, raiders, marauders |
| Veteran | 2 | Legionaries in plate, berserkers, brutes |
| Champion | 5 | Dread knights, siege beasts, wardens of ash |
| Ascendant | 9 | Iron colossi, revenant lords, shades, hollow kings |
| Divine | 14 | Wither sovereigns, deep wardens, god slayers |
| Primordial | 20 | Elder dragons, world enders, eternal sovereigns |

A band still fields the ranks below it, just half as often per step down — a wave of nothing
but Primordials is a spectacle, a wave of champions with a dragon in it is a fight.

Each opponent carries a **level** and a tier, shown in its name, and gear is most of what
makes a husk read as a soldier rather than a husk: leather and stone for conscripts,
chainmail and a shield for soldiers, netherite and an axe for warlords.

### Gluttony — monsters that grow by eating

A monster that kills another monster eats it, and what it eats it becomes. Devouring **its
own kind counts double**: a thing that turns on its own is the one that gets somewhere.
Enough of it and it levels; enough levels and it climbs a tier; past level 24 it becomes a
**Lord** — renamed, bolded, and a genuinely different fight from the thing that walked in.
Evolving heals proportionally rather than fully, so growing stronger mid-fight does not undo
the fight you have already won.

This runs everywhere, not only in the arena. Naturally spawned monsters are given a tier and
a level from how far out they spawned, so the map has a gradient instead of one flat threat.
Leave a field of monsters alone long enough and something in it will have eaten the rest by
the time you come back — the world does not wait politely at the difficulty you left it.

All three parts are switchable under `[monsters]` in the config: `enableDevouring`,
`levelWorldMobs` and `preventDaylightBurning`.

The arena is only ever built in that dedicated world — pressing the button inside your own
survival world will never rearrange it. 100% is a fair fight; past that every
100 points is another band, and damage compounds per band rather than adding, so each band
really is more lethal instead of just spongier.

Opponents use seven fighting styles and the **same** movement system you do — they wall
kick, dash, air jump and come at you from any angle. Reaction time, aggression, parry rate
and acrobatics all scale, and higher bands field more of them at once.

### Server-pushed content

`WeaponRegistrationApi` registers a weapon that lives entirely in server data — stats, item
model JSON and texture PNG. `ContentSync` ships it to every client on join and again the
moment the registry changes, so a weapon added to a live server appears in players' hands
with nobody reinstalling anything. Textures are uploaded as dynamic textures at runtime.

### Auto-update

`UpdateService` checks a manifest you point it at, downloads to a staging file, and
verifies SHA-256 before anything is installed. If you are already playing it posts a chat
note and lets you finish your fight; on the title screen it offers the choice directly.

The screen is two buttons and the player picks: **Update and Restart**, or **Wait**.
"Update and Restart" is one action — download, verify, relaunch — so nobody has to come back
and finish the job. "Wait" changes nothing; the update stays pending and a button appears in
the pause menu to apply it whenever they are ready.

The relaunch spawns a fresh process from this one's own command line, so it inherits the
exact JVM arguments and classpath the launcher used. Minecraft loads its mods once at launch,
so this restart is what makes the staged jar take effect — there is no hot-swap. If a
launcher or JVM will not expose its command line, the screen says so and offers a plain
quit instead; the staged jar still loads on the next start either way.

Configure it by setting `UpdateService.manifestUrl`. The manifest is:

```json
{ "version": "1.1.0", "url": "https://.../smmorpg-1.1.0.jar",
  "sha256": "…", "size": 1234567, "mandatory": false, "notes": "…" }
```

### Server browser

An **S** button on the title screen lists servers running this mod, fetched from a directory
endpoint (`ModServerListScreen.directoryUrl`). Empty by default — nothing is contacted until
you point it somewhere.

---

## API for future add-on mods

Nothing else needs to be integrated now, but the surface is in place so a companion mod can
be written later without touching internals.

- `api/SmmoRPGApi` — progression, skills, wounds, loot, combat info. Signatures frozen.
- `api/SmmoRPGEvents` — `PreHit`, `WoundInflicted`, `Dismember`, `LevelUp`, `Parry` on the
  NeoForge game bus, all cancellable where cancelling makes sense.
- `api/WeaponRegistrationApi` — data-driven weapons the server pushes to clients.

---

## Controls

| Key | Action |
|---|---|

| K | Character screen |
| N | Skill tree |

| T | Training arena |

---

## Status

Everything described above is implemented and compiles against NeoForge 21.1.72. The pieces
that are deliberately scaffolded rather than finished are called out where they are: the
stamina cost on movement (left out on purpose, per the design), and the update flow's
restart step (blocked by how Minecraft loads mods, explained above).

The weapon art is yours.
