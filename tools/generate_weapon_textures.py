#!/usr/bin/env python3
"""
Generates the 128x128 demo weapon icons.

These are reference designs, not final art: they exist so the mod has something coherent
to look at now, and so every weapon that arrives later — loot drops, server-pushed
weapons, whatever the training arena hands out — has one silhouette language to follow.

The language is: every weapon lies on the same bottom-left-to-top-right diagonal, pommel
at the low corner and point at the high one, because a blade drawn straight up and down
reads as a stick in the hand. Each one is built from a curved spine with a width profile
and a run of segments (pommel, grip, guard, blade, tip), so a katana and a spear differ in
their numbers rather than in their drawing code.

Pure standard library — no Pillow, no numpy, nothing to install.

    python3 tools/generate_weapon_textures.py
    python3 tools/generate_weapon_textures.py --size 256      # same designs, bigger
"""

import argparse
import math
import os
import struct
import zlib

OUT = "src/main/resources/assets/smmorpg/textures/item"

# ---------------------------------------------------------------------------
# palette
# ---------------------------------------------------------------------------

STEEL       = (198, 206, 218)
STEEL_DARK  = (108, 118, 134)
STEEL_EDGE  = (240, 246, 255)
IRON        = (128, 132, 140)
IRON_DARK   = (74,  78,  86)
BRASS       = (198, 158,  72)
BRASS_DARK  = (126,  96,  36)
GOLD        = (232, 196, 104)
WOOD        = (124,  88,  54)
WOOD_DARK   = ( 78,  54,  32)
WRAP_BLACK  = ( 44,  44,  52)
WRAP_RED    = (140,  38,  42)
WRAP_BLUE   = ( 46,  70, 116)
WRAP_GREEN  = ( 58,  92,  64)
LEATHER     = ( 96,  70,  48)
BONE        = (226, 218, 196)


# ---------------------------------------------------------------------------
# png writing
# ---------------------------------------------------------------------------

def write_png(path, w, h, pixels):
    raw = b"".join(b"\x00" + bytes(v for x in range(w) for v in pixels[y * w + x])
                   for y in range(h))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(raw, 9))
    out += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(out)


# ---------------------------------------------------------------------------
# weapon description
# ---------------------------------------------------------------------------

class Segment:
    """One run along the spine: where it starts and ends, how wide, and what it is."""

    def __init__(self, t0, t1, half_width, kind, color, color_dark=None, edge=0):
        self.t0 = t0
        self.t1 = t1
        self.half_width = half_width      # callable(local 0..1) -> half width in px
        self.kind = kind                  # "blade" | "grip" | "metal" | "shaft"
        self.color = color
        self.color_dark = color_dark or scale(color, 0.62)
        self.edge = edge                  # +1 sharp on one side, -1 the other, 0 double


def scale(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)))


def const(v):
    return lambda u: v


def taper(a, b):
    """Linear width from a to b across the segment."""
    return lambda u: a + (b - a) * u


def leaf(peak, tip=0.6):
    """Wide in the middle, pointed at the end — a spear or naginata head."""
    return lambda u: tip + (peak - tip) * math.sin(math.pi * min(1.0, u * 0.92 + 0.04)) ** 0.7


def blade_taper(base, tip):
    """A sword blade: near-constant, then the last fifth runs out to the point."""
    def f(u):
        if u < 0.78:
            return base + (base * 0.9 - base) * (u / 0.78)
        k = (u - 0.78) / 0.22
        return base * 0.9 * (1.0 - k) + tip * k
    return f


class Weapon:
    def __init__(self, name, segments, curve=0.0, angle=45.0, length=0.86, wrap_pitch=0):
        self.name = name
        self.segments = segments
        self.curve = curve                # perpendicular bow, in pixels
        self.angle = angle                # degrees from horizontal
        self.length = length              # fraction of the canvas the weapon spans
        self.wrap_pitch = wrap_pitch      # grip wrap spacing in px, 0 for none


# ---------------------------------------------------------------------------
# rasteriser
# ---------------------------------------------------------------------------

def render(weapon, size):
    """
    Splats the spine into per-pixel (t, signed distance) buffers, then shades.

    Splatting rather than a per-pixel nearest-point search keeps this fast enough to be
    plain Python: the cost follows the weapon's area, not the canvas area.
    """
    ss = 4                                     # supersample factor
    dim = size * ss
    scale_px = size / 16.0 * ss                # design is authored at 16px units

    best_d = [1e9] * (dim * dim)
    best_t = [0.0] * (dim * dim)
    best_side = [0.0] * (dim * dim)

    # Spine endpoints, centred, along the requested diagonal.
    half = weapon.length * dim * 0.5
    a = math.radians(weapon.angle)
    cx = cy = dim * 0.5
    ax, ay = cx - math.cos(a) * half, cy + math.sin(a) * half     # pommel, low-left
    bx, by = cx + math.cos(a) * half, cy - math.sin(a) * half     # point, high-right

    dx, dy = bx - ax, by - ay
    spine_len = math.hypot(dx, dy)
    ux, uy = dx / spine_len, dy / spine_len
    nx, ny = -uy, ux                                             # left normal

    steps = int(spine_len * 2.2)
    for i in range(steps + 1):
        t = i / steps
        bow = weapon.curve * scale_px * math.sin(math.pi * t) * 0.5
        px = ax + dx * t + nx * bow
        py = ay + dy * t + ny * bow

        seg = segment_at(weapon, t)
        if seg is None:
            continue
        u = (t - seg.t0) / max(1e-6, seg.t1 - seg.t0)
        w = seg.half_width(u) * scale_px

        r = int(w) + 2
        for oy in range(-r, r + 1):
            iy = int(py) + oy
            if iy < 0 or iy >= dim:
                continue
            for ox in range(-r, r + 1):
                ix = int(px) + ox
                if ix < 0 or ix >= dim:
                    continue
                vx, vy = ix + 0.5 - px, iy + 0.5 - py
                # Distance across the spine only; along it is handled by the sweep.
                d = vx * nx + vy * ny
                if abs(d) > w:
                    continue
                idx = iy * dim + ix
                ad = abs(d) / max(1e-6, w)
                if ad < best_d[idx]:
                    best_d[idx] = ad
                    best_t[idx] = t
                    best_side[idx] = 1.0 if d >= 0 else -1.0

    # --- shade at supersample resolution ---
    hi = [(0, 0, 0, 0)] * (dim * dim)
    for idx in range(dim * dim):
        if best_d[idx] > 1.0:
            continue
        seg = segment_at(weapon, best_t[idx])
        if seg is None:
            continue
        hi[idx] = shade(weapon, seg, best_t[idx], best_d[idx], best_side[idx], scale_px)

    # --- box filter down; this is where the anti-aliasing comes from ---
    out = []
    for y in range(size):
        for x in range(size):
            r = g = b = al = 0
            for sy in range(ss):
                row = (y * ss + sy) * dim + x * ss
                for sx in range(ss):
                    pr, pg, pb, pa = hi[row + sx]
                    r += pr * pa
                    g += pg * pa
                    b += pb * pa
                    al += pa
            if al == 0:
                out.append((0, 0, 0, 0))
            else:
                n = ss * ss
                out.append((r // al, g // al, b // al, al // n))
    return out


def segment_at(weapon, t):
    for seg in weapon.segments:
        if seg.t0 <= t <= seg.t1:
            return seg
    return None


def shade(weapon, seg, t, ad, side, scale_px):
    """ad is 0 at the spine and 1 at the silhouette edge; side is which flank."""
    u = (t - seg.t0) / max(1e-6, seg.t1 - seg.t0)
    base, dark = seg.color, seg.color_dark

    if seg.kind == "blade":
        # One flank is the cutting edge and goes bright and thin; the other is the spine
        # and stays dark. That asymmetry is what stops a blade reading as a grey bar.
        sharp = (side == seg.edge) if seg.edge else True
        if sharp and ad > 0.55:
            k = (ad - 0.55) / 0.45
            c = mix(STEEL, STEEL_EDGE, k)
            a = 255
        elif not sharp and ad > 0.7:
            c = mix(base, dark, (ad - 0.7) / 0.3)
            a = 255
        else:
            # Central ridge catches the light along the whole length.
            ridge = 1.0 - min(1.0, abs(ad - 0.18) / 0.45)
            c = mix(base, STEEL_EDGE, ridge * 0.45)
            a = 255
        # A hint of a temper line a third of the way in from the edge.
        if seg.edge and sharp and 0.42 < ad < 0.52:
            c = mix(c, (232, 238, 248), 0.35)
        return (c[0], c[1], c[2], a)

    if seg.kind == "grip":
        c = mix(base, dark, ad ** 1.4)
        if weapon.wrap_pitch:
            # Diagonal wrap: the binding crosses the grip, so the band's phase has to
            # shift with the distance across it as well as along it.
            phase = (t * 1000.0 + ad * side * weapon.wrap_pitch * 0.45) % weapon.wrap_pitch
            band = phase / weapon.wrap_pitch
            if band < 0.38:
                c = mix(c, scale(base, 1.55), 0.75)
            elif band > 0.80:
                c = scale(c, 0.5)
        return (c[0], c[1], c[2], 255)

    if seg.kind == "studded":
        c = mix(base, dark, ad ** 1.2)
        # Rows of iron studs down the head. The row offsets alternate so they interlock
        # the way they do on the real weapon rather than lining up in a grid.
        row = int(t * 260.0)
        lane = (ad * side + 1.0) * 2.2 + (row % 2) * 0.5
        if abs(lane - round(lane)) < 0.22 and (row % 9) < 4:
            c = mix(IRON, IRON_DARK, ad * 0.6)
            if (row % 9) < 2:
                c = mix(c, (188, 192, 200), 0.5)
        return (c[0], c[1], c[2], 255)

    if seg.kind == "shaft":
        c = mix(base, dark, ad ** 1.2)
        # Grain: a couple of darker streaks running the length.
        if 0.30 < ad < 0.42 or 0.66 < ad < 0.74:
            c = scale(c, 0.86)
        return (c[0], c[1], c[2], 255)

    # metal: guards, pommels, collars
    c = mix(base, dark, ad ** 1.1)
    if ad < 0.3:
        c = mix(c, scale(base, 1.3), 0.4)
    return (c[0], c[1], c[2], 255)


def mix(a, b, k):
    k = max(0.0, min(1.0, k))
    return (int(a[0] + (b[0] - a[0]) * k),
            int(a[1] + (b[1] - a[1]) * k),
            int(a[2] + (b[2] - a[2]) * k))


# ---------------------------------------------------------------------------
# the ten weapons
# ---------------------------------------------------------------------------

def weapons():
    return [
        # --- katana: long curved blade, long wrapped grip, round tsuba ---
        Weapon("katana", [
            Segment(0.00, 0.05, const(0.78), "metal", IRON, IRON_DARK),           # kashira
            Segment(0.05, 0.28, const(0.66), "grip", WRAP_BLACK),                 # tsuka
            Segment(0.28, 0.31, taper(1.25, 1.12), "metal", BRASS, BRASS_DARK),   # tsuba
            Segment(0.31, 1.00, blade_taper(0.44, 0.05), "blade", STEEL, STEEL_DARK, edge=-1),
        ], curve=1.7, wrap_pitch=34),

        # --- odachi: everything longer and heavier ---
        Weapon("odachi", [
            Segment(0.00, 0.05, const(0.88), "metal", IRON, IRON_DARK),
            Segment(0.05, 0.32, const(0.74), "grip", WRAP_BLUE),
            Segment(0.32, 0.35, taper(1.42, 1.28), "metal", IRON, IRON_DARK),
            Segment(0.35, 1.00, blade_taper(0.60, 0.06), "blade", STEEL, STEEL_DARK, edge=-1),
        ], curve=2.1, length=0.94, wrap_pitch=30),

        # --- dao: broad single edge, flaring toward the tip, ring pommel ---
        Weapon("dao", [
            Segment(0.00, 0.06, const(0.80), "metal", BRASS, BRASS_DARK),
            Segment(0.06, 0.26, const(0.64), "grip", WRAP_RED),
            Segment(0.26, 0.29, taper(1.18, 1.02), "metal", BRASS, BRASS_DARK),
            Segment(0.29, 1.00, lambda u: 0.40 + 0.30 * u ** 1.6 if u < 0.82
                    else (0.40 + 0.30 * 0.82 ** 1.6) * (1.0 - (u - 0.82) / 0.18) + 0.05,
                    "blade", STEEL, STEEL_DARK, edge=-1),
        ], curve=1.1, wrap_pitch=28),

        # --- jian: straight, double edged, symmetric ---
        Weapon("jian", [
            Segment(0.00, 0.05, const(0.62), "metal", BRASS, BRASS_DARK),
            Segment(0.05, 0.21, const(0.54), "grip", WOOD, WOOD_DARK),
            Segment(0.21, 0.24, taper(1.12, 0.70), "metal", BRASS, BRASS_DARK),
            Segment(0.24, 1.00, blade_taper(0.48, 0.04), "blade", STEEL, STEEL_DARK, edge=0),
        ], curve=0.0, wrap_pitch=26),

        # --- dagger: short blade, most of the icon is grip ---
        Weapon("dagger", [
            Segment(0.00, 0.06, const(0.60), "metal", IRON, IRON_DARK),
            Segment(0.06, 0.30, const(0.52), "grip", LEATHER),
            Segment(0.30, 0.34, taper(1.00, 0.62), "metal", IRON, IRON_DARK),
            Segment(0.34, 1.00, blade_taper(0.44, 0.04), "blade", STEEL, STEEL_DARK, edge=0),
        ], curve=0.0, length=0.78, wrap_pitch=22),

        # --- tanto: no curve, chisel point, small tsuba ---
        Weapon("tanto", [
            Segment(0.00, 0.07, const(0.68), "metal", IRON, IRON_DARK),
            Segment(0.07, 0.40, const(0.60), "grip", WRAP_GREEN),
            Segment(0.40, 0.43, taper(1.02, 0.92), "metal", BRASS, BRASS_DARK),
            Segment(0.43, 1.00, blade_taper(0.40, 0.05), "blade", STEEL, STEEL_DARK, edge=-1),
        ], curve=0.5, length=0.74, wrap_pitch=32),

        # --- spear: long shaft, short leaf head, collar between them ---
        Weapon("spear", [
            Segment(0.00, 0.03, const(0.42), "metal", IRON, IRON_DARK),
            Segment(0.03, 0.34, const(0.40), "grip", LEATHER),
            Segment(0.34, 0.70, const(0.38), "shaft", WOOD, WOOD_DARK),
            Segment(0.70, 0.74, taper(0.52, 0.44), "metal", IRON, IRON_DARK),
            Segment(0.74, 1.00, leaf(0.62, 0.05), "blade", STEEL, STEEL_DARK, edge=0),
        ], curve=0.0, length=0.96),

        # --- naginata: shaft plus a long curved blade ---
        Weapon("naginata", [
            Segment(0.00, 0.03, const(0.44), "metal", IRON, IRON_DARK),
            Segment(0.03, 0.32, const(0.42), "grip", LEATHER),
            Segment(0.32, 0.56, const(0.40), "shaft", WOOD, WOOD_DARK),
            Segment(0.56, 0.60, taper(0.62, 0.52), "metal", BRASS, BRASS_DARK),
            Segment(0.60, 1.00, blade_taper(0.50, 0.05), "blade", STEEL, STEEL_DARK, edge=-1),
        ], curve=1.6, length=0.96),

        # --- kanabo: no edge at all, a studded club that widens to the head ---
        Weapon("kanabo", [
            Segment(0.00, 0.06, const(0.66), "metal", IRON, IRON_DARK),
            Segment(0.06, 0.36, const(0.56), "grip", LEATHER),
            Segment(0.36, 0.40, taper(0.80, 0.74), "metal", IRON, IRON_DARK),
            Segment(0.40, 1.00, taper(0.82, 1.15), "studded", WOOD, WOOD_DARK),
        ], curve=0.0, length=0.88),

        # --- yumi: an asymmetric bow, so the "spine" is the stave and the string is drawn on ---
        Weapon("yumi", [
            Segment(0.00, 0.04, const(0.28), "metal", IRON, IRON_DARK),
            Segment(0.04, 0.96, taper(0.34, 0.30), "shaft", WOOD, WOOD_DARK),
            Segment(0.96, 1.00, const(0.26), "metal", IRON, IRON_DARK),
        ], curve=6.5, length=0.94),
    ]


def draw_bowstring(pixels, size):
    """The yumi needs a string; it is a straight line the stave bows away from."""
    a = math.radians(45.0)
    half = 0.94 * size * 0.5
    cx = cy = size * 0.5
    x0, y0 = cx - math.cos(a) * half, cy + math.sin(a) * half
    x1, y1 = cx + math.cos(a) * half, cy - math.sin(a) * half

    steps = int(size * 2)
    for i in range(steps + 1):
        t = i / steps
        px = x0 + (x1 - x0) * t
        py = y0 + (y1 - y0) * t
        for oy in (-1, 0, 1):
            for ox in (-1, 0, 1):
                ix, iy = int(px) + ox, int(py) + oy
                if not (0 <= ix < size and 0 <= iy < size):
                    continue
                w = 1.0 - (abs(ox) + abs(oy)) * 0.4
                if w <= 0:
                    continue
                idx = iy * size + ix
                r, g, b, al = pixels[idx]
                a2 = int(220 * w)
                if a2 > al:
                    pixels[idx] = (226, 220, 200, a2)


# ---------------------------------------------------------------------------
# accessories and consumables
# ---------------------------------------------------------------------------

def render_trinket(kind, size):
    """
    The non-weapon icons, at the same resolution as the blades so nothing looks like it
    came from a different mod. Simple rings and vials on purpose — they are placeholders
    waiting for real art, and they should read as such rather than pretending otherwise.
    """
    ss = 3
    dim = size * ss
    hi = [(0, 0, 0, 0)] * (dim * dim)
    cx = cy = dim * 0.5
    unit = dim / 16.0

    for y in range(dim):
        for x in range(dim):
            px, py = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(px, py) / unit
            c = a = None

            if kind == "ring":
                if 4.4 < d < 6.2:
                    k = 1.0 - abs(d - 5.3) / 0.9
                    c, a = mix(BRASS_DARK, GOLD, k), 255
                elif d <= 2.0 and py < 0:
                    c, a = mix(GOLD, (255, 246, 210), 1.0 - d / 2.0), 255
            elif kind == "amulet":
                if 5.6 < d < 6.4 and py < 1.0:
                    c, a = LEATHER, 255                                # cord
                elif d < 3.4 and py > -1.0:
                    k = 1.0 - d / 3.4
                    c, a = mix(BRASS_DARK, GOLD, k), 255
            elif kind == "omamori":
                if abs(px) < 3.2 * unit and abs(py) < 4.6 * unit:
                    u = abs(px) / (3.2 * unit)
                    c = mix(WRAP_RED, scale(WRAP_RED, 1.5), 1.0 - u)
                    if abs(py) < 0.5 * unit:
                        c = mix(c, GOLD, 0.7)                          # the knot
                    a = 255
            elif kind == "talisman":
                if abs(px) < 2.4 * unit and abs(py) < 5.4 * unit:
                    c = mix((214, 206, 178), (240, 236, 220),
                            1.0 - abs(px) / (2.4 * unit))
                    row = int((py + 5.4 * unit) / unit)
                    if row % 2 == 1 and abs(px) < 1.4 * unit:
                        c = mix(c, (60, 30, 30), 0.6)                  # inked sigils
                    a = 255
            elif kind == "bandage":
                if 3.0 < d < 5.6:
                    band = (math.atan2(py, px) * 3.0) % 1.0
                    c = mix((236, 232, 220), (198, 192, 176), band)
                    a = 255
            elif kind == "cautery_iron":
                along = (px + py) / math.sqrt(2.0) / unit
                across = (px - py) / math.sqrt(2.0) / unit
                if abs(across) < 0.7 and -6.0 < along < 1.0:
                    c, a = mix(LEATHER, WOOD_DARK, abs(across) / 0.7), 255
                elif abs(across) < 1.6 and 1.0 <= along < 5.4:
                    heat = (along - 1.0) / 4.4
                    c, a = mix(IRON, (226, 96, 40), heat ** 1.6), 255
            elif kind == "blood_vial":
                if abs(px) < 2.6 * unit and -5.0 * unit < py < 5.2 * unit:
                    if py < -3.4 * unit:
                        c, a = mix(WOOD, WOOD_DARK, abs(px) / (2.6 * unit)), 255   # cork
                    else:
                        fill = mix((120, 16, 20), (176, 30, 34),
                                   1.0 - abs(px) / (2.6 * unit))
                        c, a = mix(fill, (232, 240, 248), 0.12), 255

            if c is not None:
                hi[y * dim + x] = (c[0], c[1], c[2], a)

    out = []
    for y in range(size):
        for x in range(size):
            r = g = b = al = 0
            for sy in range(ss):
                row = (y * ss + sy) * dim + x * ss
                for sx in range(ss):
                    pr, pg, pb, pa = hi[row + sx]
                    r += pr * pa
                    g += pg * pa
                    b += pb * pa
                    al += pa
            if al == 0:
                out.append((0, 0, 0, 0))
            else:
                n = ss * ss
                out.append((r // al, g // al, b // al, al // n))
    return out


TRINKETS = ["ring", "amulet", "omamori", "talisman", "bandage", "cautery_iron", "blood_vial"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--size", type=int, default=128)
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    for weapon in weapons():
        pixels = render(weapon, args.size)
        if weapon.name == "yumi":
            draw_bowstring(pixels, args.size)
        path = os.path.join(args.out, weapon.name + ".png")
        write_png(path, args.size, args.size, pixels)
        print(f"{weapon.name:10s} -> {path} ({args.size}x{args.size})")

    for kind in TRINKETS:
        pixels = render_trinket(kind, args.size)
        path = os.path.join(args.out, kind + ".png")
        write_png(path, args.size, args.size, pixels)
        print(f"{kind:10s} -> {path} ({args.size}x{args.size})")

    print(f"\n{len(weapons())} demo weapons and {len(TRINKETS)} trinkets written."
          f"\nOverwrite any of them with your own art; the mod reads the file,"
          f"\nnot the generator.")


if __name__ == "__main__":
    main()
