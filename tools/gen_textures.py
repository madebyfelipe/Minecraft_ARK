#!/usr/bin/env python3
"""Gera as texturas 16x16 de blocos e itens que não são criaturas.

Como em gen_smilodon.py: rodar de novo SOBRESCREVE edições feitas à mão.
"""

import random
from pathlib import Path

from PIL import Image

TEXTURES = (Path(__file__).resolve().parent.parent
            / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures")
SIZE = 16
CLEAR = (0, 0, 0, 0)

LEAF_SHADES = [(38, 66, 58), (48, 82, 70), (30, 52, 48), (58, 96, 80)]
FRUIT = (22, 16, 30)
FRUIT_SHADE = (12, 8, 18)
FRUIT_SHINE = (92, 78, 120)
STEM = (74, 96, 52)
PASTE = (52, 30, 66)
PASTE_DARK = (34, 18, 46)
PASTE_LIGHT = (96, 66, 118)
BOWL = (122, 88, 60)
BOWL_DARK = (88, 60, 40)
BARK = (86, 62, 44)
BARK_DARK = (60, 42, 30)


def new():
    return Image.new("RGBA", (SIZE, SIZE), CLEAR)


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), color + (255,))


def leaves(rng):
    img = new()
    for x in range(SIZE):
        for y in range(SIZE):
            # Buracos transparentes dão o aspecto de folhagem.
            if rng.random() < 0.18:
                continue
            put(img, x, y, rng.choice(LEAF_SHADES))
    return img


def berry(img, x, y):
    """Fruta de 2x2 com um ponto de brilho."""
    put(img, x, y, FRUIT_SHINE)
    put(img, x + 1, y, FRUIT)
    put(img, x, y + 1, FRUIT)
    put(img, x + 1, y + 1, FRUIT_SHADE)


def ripe_leaves(base):
    img = base.copy()
    for x, y in [(2, 3), (9, 1), (12, 7), (5, 9), (1, 12), (10, 12)]:
        berry(img, x, y)
    return img


def fruit_item():
    img = new()
    for x, y in [(8, 2), (9, 3), (8, 4)]:
        put(img, x, y, STEM)
    put(img, 10, 2, STEM)
    put(img, 11, 2, STEM)
    # Cacho de três frutas redondas.
    for cx, cy in [(5, 7), (9, 8), (7, 11)]:
        for dx in range(-2, 2):
            for dy in range(-2, 2):
                corner = dx in (-2, 1) and dy in (-2, 1)
                if not corner:
                    put(img, cx + dx, cy + dy, FRUIT_SHADE if dx == 1 or dy == 1 else FRUIT)
        put(img, cx - 1, cy - 1, FRUIT_SHINE)
    return img


def sapling(rng):
    """Muda: caule fino e três tufos da folhagem escura, com uma fruta."""
    img = new()
    for y in range(9, 16):
        put(img, 7 + (y < 12), y, BARK if y % 3 else BARK_DARK)
    for cx, cy, r in [(8, 5, 3), (4, 8, 2), (11, 9, 2)]:
        for x in range(cx - r, cx + r + 1):
            for y in range(cy - r, cy + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 1 and rng.random() > 0.12:
                    put(img, x, y, rng.choice(LEAF_SHADES))
    berry(img, 9, 4)
    return img


def narcotic_item():
    img = new()
    # Tigela vista de lado com uma pasta escura dentro.
    for x in range(2, 14):
        put(img, x, 9, BOWL)
    for y, (x0, x1) in {10: (3, 13), 11: (3, 13), 12: (4, 12), 13: (5, 11)}.items():
        for x in range(x0, x1):
            put(img, x, y, BOWL_DARK if y >= 12 else BOWL)
    for x in range(3, 13):
        put(img, x, 8, PASTE)
    for x in range(4, 12):
        put(img, x, 7, PASTE_DARK if x % 3 else PASTE)
    for x in range(6, 10):
        put(img, x, 6, PASTE)
    put(img, 5, 7, PASTE_LIGHT)
    put(img, 8, 6, PASTE_LIGHT)
    return img


def main():
    rng = random.Random(7)
    base = leaves(rng)
    outputs = {
        "block/black_fruit_leaves.png": base,
        "block/black_fruit_leaves_ripe.png": ripe_leaves(base),
        "item/black_fruit.png": fruit_item(),
        "item/narcotic.png": narcotic_item(),
        "block/black_fruit_sapling.png": sapling(random.Random(11)),
    }
    for relative, img in outputs.items():
        path = TEXTURES / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    print(f"{len(outputs)} texturas geradas em {TEXTURES}")


if __name__ == "__main__":
    main()
