#!/usr/bin/env python3
"""Gera as texturas 16x16 das estações de trabalho (incubadora, mesa química), do ovo de
criatura e do estimulante.

O ovo sai em tons de cinza, em duas camadas (casca e pintas): o jogo tinge cada uma com as
cores do ovo gerador da espécie. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import random
from pathlib import Path

from PIL import Image

TEXTURES = (Path(__file__).resolve().parent.parent
            / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures")
SIZE = 16

IRON = [(150, 156, 160), (136, 142, 146), (164, 170, 174)]
IRON_DARK = (92, 98, 104)
RIVET = (196, 202, 206)
GLOW = [(255, 164, 72), (240, 132, 52), (255, 196, 112)]
WINDOW_DARK = (60, 34, 20)
WOOD = [(128, 92, 56), (116, 82, 48), (140, 102, 62)]
WOOD_DARK = (82, 58, 34)
GLASS = (182, 224, 230)
LIQUID = [(96, 200, 120), (120, 90, 200), (220, 90, 90)]


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), tuple(color) + (255,))


def plate(rng, shades, edge):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            put(img, x, y, edge if x in (0, 15) or y in (0, 15) else rng.choice(shades))
    return img


def incubator_side(rng, window):
    img = plate(rng, IRON, IRON_DARK)
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        put(img, x, y, RIVET)
    if window:
        # Janela redonda com o brilho quente de dentro.
        for x in range(4, 12):
            for y in range(4, 12):
                d = (x - 7.5) ** 2 + (y - 7.5) ** 2
                if d <= 13:
                    put(img, x, y, rng.choice(GLOW))
                elif d <= 18:
                    put(img, x, y, WINDOW_DARK)
    return img


def incubator_top(rng):
    img = plate(rng, IRON, IRON_DARK)
    for i in range(3, 13, 3):
        for x in range(3, 13):
            put(img, x, i, IRON_DARK)
    return img


def bench_top(rng):
    img = plate(rng, WOOD, WOOD_DARK)
    # Três frascos vistos de cima.
    for cx, cy, liquid in [(4, 4, LIQUID[0]), (11, 5, LIQUID[1]), (7, 11, LIQUID[2])]:
        for x in range(cx - 1, cx + 2):
            for y in range(cy - 1, cy + 2):
                put(img, x, y, GLASS)
        put(img, cx, cy, liquid)
    return img


def bench_side(rng):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if y < 4:
                put(img, x, y, rng.choice(WOOD))
            elif x in (1, 2, 13, 14):
                put(img, x, y, WOOD_DARK)
    for y in range(4, SIZE):
        for x in range(3, 13):
            if y in (9, 10):
                put(img, x, y, rng.choice(WOOD))
    return img


def egg_layers(rng):
    shell = canvas()
    spots = canvas()
    for y in range(2, 15):
        half = {2: 2, 3: 3, 4: 4, 5: 4, 13: 4, 14: 2}.get(y, 5)
        for x in range(8 - half, 8 + half):
            shade = 235 - (x - 3) * 6 - (y - 2) * 3
            if x == 8 - half or x == 7 + half:
                shade -= 40
            put(shell, x, y, (shade, shade, shade))
            if rng.random() < 0.16 and 3 < y < 14:
                put(spots, x, y, (200, 200, 200))
    put(shell, 6, 4, (255, 255, 255))
    return shell, spots


def stimulant():
    img = canvas()
    # Frasco com líquido verde-claro.
    for y in range(3, 6):
        put(img, 7, y, GLASS)
        put(img, 8, y, GLASS)
    put(img, 7, 2, WOOD_DARK)
    put(img, 8, 2, WOOD_DARK)
    for y in range(6, 14):
        for x in range(4, 12):
            edge = x in (4, 11) or y == 13
            put(img, x, y, GLASS if edge else ((128, 230, 96) if y > 8 else (180, 245, 150)))
    put(img, 6, 9, (230, 255, 220))
    return img


def main():
    rng = random.Random(31)
    shell, spots = egg_layers(rng)
    outputs = {
        "block/incubator_side.png": incubator_side(rng, False),
        "block/incubator_front.png": incubator_side(rng, True),
        "block/incubator_top.png": incubator_top(rng),
        "block/chemistry_bench_top.png": bench_top(rng),
        "block/chemistry_bench_side.png": bench_side(rng),
        "item/creature_egg.png": shell,
        "item/creature_egg_spots.png": spots,
        "item/stimulant.png": stimulant(),
    }
    for relative, img in outputs.items():
        path = TEXTURES / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    print(f"{len(outputs)} texturas geradas em {TEXTURES}")


if __name__ == "__main__":
    main()
