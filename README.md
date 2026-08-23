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
— the arena is for practising fights, not for losing your gear to one. 100% is a fair fight; past that every
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
