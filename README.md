# SmmoRPG — Ultra Realistic Fighting

A single NeoForge mod (Minecraft 1.21.1, Java 21) that turns Minecraft into an FPS-first
melee MMORPG: Sekiro-style classes, level and loot progression, holy and cursed gear,
locational wounds that come from where the blade actually went, 60 impact sounds, camera
shake and recoil you can feel, a training arena that scales to absurdity, and an operator
studio for retuning the whole feel while the server is running.

**No external mods. No third-party libraries.** Everything is built on Minecraft, NeoForge
and the JDK. The only "library" the mod uses is its own.

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

So a katana you draw goes at `.../textures/item/katana.png`, and it replaces the
placeholder immediately — nothing else to change. The placeholders currently in the repo
are 16×16 and deliberately plain; overwrite them with whatever resolution you like.

Accessories and consumables use the same folder: `ring`, `amulet`, `omamori`, `talisman`,
`bandage`, `cautery_iron`, `blood_vial`.

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

### Combat — the cut is never pre-planned

`combat/BladeTrace.java` samples the weapon edge's base and tip every tick while a swing is
live, forming a swept ribbon through the world. When that ribbon crosses a body part, the
crossing itself supplies the wound's position, its direction, its length and its depth.

That is the whole point: cut a thigh low and you get a low, shallow gash across the thigh;
follow through on a shoulder and you get a long, deep one at the angle you actually swung.
Neither is authored anywhere. Change the animation in the studio panel and the cuts change
with it, for free.

`HitboxResolver` remains as a fallback for blows with no tracked swing (a projectile, a mob
without a swing state, a dropped tick) so no hit is ever left without a location.

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

### FPS first, and the outside rig agrees

`PoseState` is authored **once**, on the owning client, from raw input. It drives the local
first-person body *and* is sent to every other client to drive the third-person rig. There
is no second animation set that only other people see, so what you feel and what they see
cannot drift apart.

`FirstPersonBodyRenderer` draws your real model in first person — the same model everyone
else sees, not a floating pair of hands.

### Feel

- `CameraShake` — two detuned oscillators plus noise, trauma-squared so light hits are
  subtle and heavy ones are violent, with a separate spring-returned recoil channel.
- Hit-stop freezes the frame briefly on a solid connect.
- Sprinting, landing and being hit all feed the same shake channel, so the whole game reads
  through one physical language.

### Movement

Wall kicks off **any** solid face (including a tree trunk), air jumps, directional dashes
and wall runs. Uncapped for now and priced only by cooldowns and contact conditions — a
stamina cost is deliberately left out until the feel is settled. Requests come from the
client, but the server re-checks every condition before granting them.

### Training arena

A square **T** button beside Singleplayer on the title screen. Pick a difficulty from 1% to
100000% on a logarithmic slider (or type it exactly). 100% is a fair fight; past that every
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

### Animation studio (operators only)

`F9` opens it if you have permission level 2. Every slider edits one field of one animation
profile, sends it to the server, and the server broadcasts it — visible on the next frame
for everyone. Duration, strike timing, arm swing, body twist, lean, recovery easing and
camera kick.

### Auto-update

`UpdateService` checks a manifest you point it at, downloads to a staging file, and
verifies SHA-256 before anything is installed. If you are already playing it posts a chat
note and lets you finish your fight; on the title screen it offers the choice directly.

**One honest limitation:** Minecraft loads its mods once, at launch. A jar cannot be swapped
under a running game — so an applied update is always staged for the next start, and the
screen offers "Quit to Apply" rather than pretending it can hot-swap or relaunch for you.
Everything up to that point is automatic.

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
| Left Alt | Parry |
| F5 | Toggle FPS / TPS |
| K | Character screen |
| N | Skill tree |
| Left Ctrl | Dash |
| R | Sheathe |
| F9 | Animation studio (operators) |
| Space (airborne) | Wall kick, or air jump if nothing to kick off |

---

## Status

Everything described above is implemented and compiles against NeoForge 21.1.72. The pieces
that are deliberately scaffolded rather than finished are called out where they are: the
stamina cost on movement (left out on purpose, per the design), and the update flow's
restart step (blocked by how Minecraft loads mods, explained above).

The weapon art is yours.
