#!/usr/bin/env python3
"""
Synthesises the 60 impact sounds the mod indexes.

Every sound is built from a physical sketch of what actually happened: a modal ring for
metal, a damped noise burst for flesh, a sharp transient plus splintering for bone. The
struck location then shapes it -- a head hit is brighter and shorter than a leg hit,
because there is less soft tissue in the way.

Usage:
    python3 tools/generate_impact_sounds.py            # writes .wav, converts to .ogg if ffmpeg exists
    python3 tools/generate_impact_sounds.py --keep-wav

Minecraft only loads .ogg, so ffmpeg (or oggenc) must be on PATH for the output to be
usable in game. Without it the script still writes the .wav files and tells you what to run.
"""

import argparse
import math
import os
import random
import shutil
import struct
import subprocess
import sys
import wave

SAMPLE_RATE = 44100

MATERIALS = ["flesh", "bone", "leather", "chain", "plate", "shield", "stone", "undead"]
LOCATIONS = ["head", "neck", "torso", "left_arm", "right_arm", "left_leg", "right_leg"]
SPECIALS = ["parry", "whiff", "decapitate", "dismember"]

# Per-material voice: (modal partials in Hz, decay seconds, noise mix, transient hardness)
MATERIAL_VOICE = {
    "flesh":   ([90, 140, 210],            0.16, 0.85, 0.25),
    "bone":    ([420, 880, 1650, 2400],    0.10, 0.55, 0.95),
    "leather": ([160, 300, 520],           0.13, 0.70, 0.45),
    "chain":   ([700, 1300, 2100, 3300],   0.35, 0.60, 0.70),
    "plate":   ([520, 980, 1720, 2900],    0.55, 0.35, 0.90),
    "shield":  ([300, 610, 1150, 1900],    0.42, 0.45, 0.85),
    "stone":   ([220, 470, 900],           0.14, 0.75, 0.80),
    "undead":  ([70, 130, 260, 500],       0.22, 0.90, 0.35),
}

# Per-location shaping: (brightness, length scale, low-end weight)
LOCATION_SHAPE = {
    "head":      (1.35, 0.80, 0.70),
    "neck":      (1.15, 0.90, 0.85),
    "torso":     (0.80, 1.25, 1.35),
    "left_arm":  (1.05, 0.90, 0.85),
    "right_arm": (1.05, 0.90, 0.85),
    "left_leg":  (0.92, 1.05, 1.10),
    "right_leg": (0.92, 1.05, 1.10),
}


def _biquad_lowpass(samples, cutoff, q=0.707):
    """One-pole-per-stage RBJ lowpass, applied in place. Keeps the noise from hissing."""
    w0 = 2.0 * math.pi * cutoff / SAMPLE_RATE
    alpha = math.sin(w0) / (2.0 * q)
    cos_w0 = math.cos(w0)
    b0 = (1.0 - cos_w0) / 2.0
    b1 = 1.0 - cos_w0
    b2 = b0
    a0 = 1.0 + alpha
    a1 = -2.0 * cos_w0
    a2 = 1.0 - alpha
    b0, b1, b2, a1, a2 = b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0

    x1 = x2 = y1 = y2 = 0.0
    out = []
    for x0 in samples:
        y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1 = x1, x0
        y2, y1 = y1, y0
        out.append(y0)
    return out


def synth_impact(material, location, seed):
    rng = random.Random(seed)
    partials, decay, noise_mix, hardness = MATERIAL_VOICE[material]
    brightness, length_scale, low_weight = LOCATION_SHAPE[location]

    duration = decay * length_scale * 3.0 + 0.05
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # --- transient: the instant of contact ---
    click_len = int(SAMPLE_RATE * 0.004 * (2.0 - hardness))
    for i in range(min(click_len, n)):
        env = 1.0 - i / click_len
        out[i] += rng.uniform(-1.0, 1.0) * env * env * hardness

    # --- modal ring: the material's own voice ---
    for k, freq in enumerate(partials):
        f = freq * brightness * rng.uniform(0.97, 1.03)
        amp = (0.9 ** k) * (1.0 - noise_mix * 0.5)
        if f < 200:
            amp *= low_weight
        tau = decay * length_scale / (1.0 + k * 0.55)
        phase = rng.uniform(0, math.tau)
        two_pi_f = math.tau * f / SAMPLE_RATE
        for i in range(n):
            env = math.exp(-i / (SAMPLE_RATE * tau))
            if env < 1e-4:
                break
            out[i] += math.sin(two_pi_f * i + phase) * amp * env

    # --- body: the wet, non-pitched part of the hit ---
    noise = [rng.uniform(-1.0, 1.0) for _ in range(n)]
    noise = _biquad_lowpass(noise, max(300.0, 2600.0 * brightness * (1.0 - 0.4 * low_weight)))
    noise_tau = decay * length_scale * 0.55
    for i in range(n):
        env = math.exp(-i / (SAMPLE_RATE * noise_tau))
        out[i] += noise[i] * noise_mix * env * 1.4

    # --- soft-clip and normalise ---
    peak = max(1e-6, max(abs(s) for s in out))
    return [math.tanh(s / peak * 1.6) * 0.92 for s in out]


def synth_special(name, seed):
    rng = random.Random(seed)
    if name == "parry":
        base = synth_impact("plate", "torso", seed)
        # A parry is two blades: a bright scrape riding on the ring.
        n = len(base)
        for i in range(n):
            t = i / SAMPLE_RATE
            env = math.exp(-t / 0.22)
            base[i] += math.sin(math.tau * (3200 + 900 * math.sin(t * 40)) * t) * 0.35 * env
        return [math.tanh(s) * 0.95 for s in base]

    if name == "whiff":
        n = int(SAMPLE_RATE * 0.30)
        noise = [rng.uniform(-1.0, 1.0) for _ in range(n)]
        noise = _biquad_lowpass(noise, 1800.0)
        out = []
        for i, s in enumerate(noise):
            t = i / n
            # A swing through air swells then falls: that shape is the whole sound.
            env = math.sin(math.pi * t) ** 2
            out.append(s * env * 0.8)
        return out

    if name in ("decapitate", "dismember"):
        wet = synth_impact("flesh", "neck" if name == "decapitate" else "left_arm", seed)
        crack = synth_impact("bone", "neck", seed + 1)
        n = max(len(wet), len(crack))
        out = [0.0] * n
        for i in range(n):
            if i < len(crack):
                out[i] += crack[i] * 0.9
            if i < len(wet):
                out[i] += wet[i] * 1.1
        # Trailing spatter.
        tail = int(SAMPLE_RATE * 0.5)
        out.extend(0.0 for _ in range(tail))
        for i in range(tail):
            t = i / SAMPLE_RATE
            out[n + i] = rng.uniform(-1.0, 1.0) * math.exp(-t / 0.14) * 0.28
        peak = max(1e-6, max(abs(s) for s in out))
        return [math.tanh(s / peak * 1.5) * 0.94 for s in out]

    raise ValueError(name)


def write_wav(path, samples):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples)
        w.writeframes(frames)


def to_ogg(wav_path, ogg_path):
    if shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
                        "-c:a", "libvorbis", "-q:a", "5", ogg_path], check=True)
        return True
    if shutil.which("oggenc"):
        subprocess.run(["oggenc", "-Q", "-q", "5", "-o", ogg_path, wav_path], check=True)
        return True
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="src/main/resources/assets/smmorpg/sounds")
    ap.add_argument("--keep-wav", action="store_true")
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    jobs = []
    for mat in MATERIALS:
        for loc in LOCATIONS:
            jobs.append((f"impact_{mat}_{loc}", mat, loc))
    for name in SPECIALS:
        jobs.append((f"impact_{name}", None, None))

    assert len(jobs) == 60, f"expected 60 sounds, built {len(jobs)}"

    converted = True
    for index, (name, mat, loc) in enumerate(jobs):
        samples = synth_special(name.removeprefix("impact_"), index) if mat is None \
            else synth_impact(mat, loc, index)
        wav_path = os.path.join(args.out, name + ".wav")
        ogg_path = os.path.join(args.out, name + ".ogg")
        write_wav(wav_path, samples)
        if converted:
            converted = to_ogg(wav_path, ogg_path)
        if converted and not args.keep_wav:
            os.remove(wav_path)
        print(f"[{index + 1:2d}/60] {name}")

    if not converted:
        print("\nffmpeg / oggenc not found: only .wav was written.", file=sys.stderr)
        print("Minecraft loads .ogg only. Install ffmpeg and re-run, or convert with:",
              file=sys.stderr)
        print(f'  for f in {args.out}/*.wav; do ffmpeg -i "$f" "${{f%.wav}}.ogg"; done',
              file=sys.stderr)
        return 1
    print("\n60 impact sounds written to", args.out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
