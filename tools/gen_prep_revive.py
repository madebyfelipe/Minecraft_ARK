#!/usr/bin/env python3
"""Gera as texturas 16x16 da estação de preparação, da mesa de reviver, do implante e do charque.

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão.
"""
import random
from pathlib import Path

from PIL import Image

TEXTURES = (Path(__file__).resolve().parent.parent
            / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures")
SIZE = 16

WOOD = [(122, 84, 52), (110, 76, 46), (134, 94, 60)]
WOOD_DARK = (74, 50, 30)
STONE = [(118, 118, 122), (104, 104, 110), (132, 132, 136)]
METAL = [(168, 174, 182), (150, 156, 164), (186, 192, 198)]
METAL_DARK = (96, 102, 112)
MEAT = (168, 58, 52)
MEAT_LIGHT = (206, 104, 90)
MEAT_DARK = (110, 34, 32)
SALT = (232, 232, 224)
GLOW = (90, 210, 220)
GLOW_DARK = (40, 140, 160)
DIAMOND = (110, 230, 220)
JERKY = (124, 62, 38)
JERKY_DARK = (86, 40, 24)
JERKY_LIGHT = (166, 96, 62)


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), color + (255,))


def plate(rng, shades, edge):
    img = canvas()
    for y in range(SIZE):
        for x in range(SIZE):
            put(img, x, y, edge if x in (0, 15) or y in (0, 15) else rng.choice(shades))
    return img


def prep_top(rng):
    # Tábua de madeira com uma peça de carne e sal.
    img = plate(rng, WOOD, WOOD_DARK)
    for y in range(5, 11):
        for x in range(4, 12):
            put(img, x, y, MEAT if (x + y) % 4 else MEAT_LIGHT)
    for x in range(4, 12):
        put(img, x, 11, MEAT_DARK)
    for x, y in ((12, 3), (13, 4), (3, 12), (12, 12)):
        put(img, x, y, SALT)
    return img


def prep_side(rng):
    img = plate(rng, WOOD, WOOD_DARK)
    # Ganchos com carne pendurada.
    for x in (4, 11):
        put(img, x, 2, METAL_DARK)
        put(img, x, 3, METAL_DARK)
        for y in range(4, 9):
            put(img, x, y, MEAT)
            put(img, x + 1, y, MEAT_DARK)
    for x in range(1, 15):
        put(img, x, 12, WOOD_DARK)
    return img


def revive_top(rng):
    # Mesa de metal com o encaixe do implante brilhando e o diamante.
    img = plate(rng, METAL, METAL_DARK)
    for y in range(5, 11):
        for x in range(5, 11):
            put(img, x, y, GLOW if (x in (5, 10) or y in (5, 10)) else GLOW_DARK)
    put(img, 7, 7, DIAMOND)
    put(img, 8, 8, DIAMOND)
    put(img, 8, 7, (255, 255, 255))
    return img


def revive_side(rng):
    img = plate(rng, STONE, METAL_DARK)
    for x in range(1, 15):
        put(img, x, 2, METAL[0])
        put(img, x, 3, METAL_DARK)
    for x in range(3, 13):
        put(img, x, 8, GLOW if x % 2 else GLOW_DARK)
    return img


def implant():
    # Cápsula com núcleo luminoso.
    img = canvas()
    for y in range(4, 12):
        for x in range(5, 11):
            edge = x in (5, 10) or y in (4, 11)
            put(img, x, y, METAL_DARK if edge else METAL[0])
    for y in range(6, 10):
        put(img, 7, y, GLOW)
        put(img, 8, y, GLOW_DARK)
    for x, y in ((6, 3), (9, 3), (6, 12), (9, 12)):
        put(img, x, y, METAL_DARK)
    return img


def jerky():
    # Tiras de carne seca.
    img = canvas()
    for strip, (x0, y0) in enumerate(((3, 11), (6, 12), (9, 11))):
        for i in range(7):
            x, y = x0 + i // 3, y0 - i
            put(img, x, y, JERKY)
            put(img, x + 1, y, JERKY_DARK if i % 2 else JERKY_LIGHT)
    return img


def main():
    rng = random.Random(42)
    files = {
        "block/prep_station_top.png": prep_top(rng),
        "block/prep_station_side.png": prep_side(rng),
        "block/revive_table_top.png": revive_top(rng),
        "block/revive_table_side.png": revive_side(rng),
        "item/implant.png": implant(),
        "item/jerky.png": jerky(),
    }
    for name, image in files.items():
        path = TEXTURES / name
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)
        print(name)


if __name__ == "__main__":
    main()
