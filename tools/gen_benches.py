#!/usr/bin/env python3
"""Gera as texturas 16x16, os modelos e os blockstates das bancadas de Construção e de Armeiro.

Construção: tampo de tábuas com serrote e martelo, laterais de tora com uma prancha de medidas.
Armeiro: tampo com torno de ferro, frente com o rifle pendurado, laterais com ferramentas.
Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
import random
from pathlib import Path

from PIL import Image

ASSETS = (Path(__file__).resolve().parent.parent
          / "src" / "main" / "resources" / "assets" / "iceagesurvival")
TEXTURES = ASSETS / "textures" / "block"
SIZE = 16

WOOD = [(150, 112, 70), (138, 102, 62), (160, 122, 78)]
WOOD_DARK = (92, 66, 40)
BARK = [(98, 74, 48), (88, 66, 42), (108, 82, 52)]
IRON = [(150, 156, 160), (136, 142, 146), (164, 170, 174)]
IRON_DARK = (82, 88, 94)
HANDLE = (120, 80, 44)
STONE = [(128, 128, 128), (116, 116, 116), (140, 140, 140)]
ROPE = (196, 170, 120)
SHADOW = [(52, 38, 24), (46, 34, 22)]


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), tuple(color) + (255,))


def planks(rng, shades=WOOD):
    """Tábuas horizontais de 4 px com as juntas escuras."""
    img = canvas()
    for y in range(SIZE):
        for x in range(SIZE):
            joint = y % 4 == 3 or (x == (5 + 7 * (y // 4)) % SIZE)
            put(img, x, y, WOOD_DARK if joint else rng.choice(shades))
    return img


def side_frame(rng):
    """Lateral de bancada: tampo de tábua, pernas de tora, travessa no meio e a sombra de baixo (opaca)."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            put(img, x, y, rng.choice(SHADOW))
            if y < 4:
                put(img, x, y, WOOD_DARK if y == 3 else rng.choice(WOOD))
            elif x in (1, 2, 13, 14):
                put(img, x, y, rng.choice(BARK))
            elif y in (10, 11):
                put(img, x, y, rng.choice(WOOD))
    return img


def construction_top(rng):
    img = planks(rng)
    # Serrote: lâmina de ferro em diagonal com o cabo de madeira.
    for i in range(7):
        put(img, 3 + i, 10 - i, IRON[0])
        put(img, 4 + i, 10 - i, IRON_DARK if i % 2 else IRON[2])
    for x, y in [(2, 11), (2, 12), (3, 12), (1, 12)]:
        put(img, x, y, HANDLE)
    # Martelo de pedra.
    for y in range(8, 14):
        put(img, 12, y, HANDLE)
    for x in range(10, 15):
        put(img, x, 7, STONE[1])
        put(img, x, 8, STONE[0])
    return img


def construction_side(rng):
    img = side_frame(rng)
    # Prancha de medidas com marcas, presa na travessa.
    for x in range(4, 12):
        for y in range(5, 9):
            put(img, x, y, rng.choice(WOOD))
    for x in range(4, 12, 2):
        put(img, x, 5, WOOD_DARK)
    return img


def construction_front(rng):
    img = side_frame(rng)
    # Rolo de corda e uma estaca afiada encostada.
    for x, y in [(5, 5), (6, 5), (7, 5), (4, 6), (8, 6), (4, 7), (8, 7), (5, 8), (6, 8), (7, 8)]:
        put(img, x, y, ROPE)
    for y in range(5, 15):
        put(img, 11, y, rng.choice(BARK))
    put(img, 11, 4, WOOD[2])
    return img


def armory_top(rng):
    img = planks(rng)
    # Torno de ferro no canto, com o parafuso.
    for x in range(2, 8):
        for y in range(2, 6):
            put(img, x, y, IRON_DARK if x in (2, 7) or y in (2, 5) else rng.choice(IRON))
    for x in range(8, 11):
        put(img, x, 4, IRON[2])
    # Dardos espalhados.
    for cx, cy in [(10, 10), (12, 12), (6, 12)]:
        put(img, cx, cy, IRON[2])
        put(img, cx + 1, cy + 1, (90, 200, 120))
        put(img, cx + 2, cy + 2, (230, 230, 230))
    return img


def armory_side(rng):
    img = side_frame(rng)
    # Placa de ferro com rebites (o armeiro reforça a bancada).
    for x in range(4, 12):
        for y in range(5, 9):
            put(img, x, y, rng.choice(IRON))
    for x, y in [(4, 5), (11, 5), (4, 8), (11, 8)]:
        put(img, x, y, IRON_DARK)
    return img


def armory_front(rng):
    img = side_frame(rng)
    # Rifle pendurado na frente: cano de ferro e coronha de madeira.
    for x in range(3, 13):
        put(img, x, 6, IRON_DARK if x < 9 else IRON[0])
    for x in range(3, 7):
        put(img, x, 7, HANDLE)
    for x in range(3, 5):
        put(img, x, 8, HANDLE)
    put(img, 8, 7, IRON_DARK)
    return img


def block_model(name):
    # Como a mesa de trabalho: frente no norte e no oeste, lateral no sul e no leste.
    return {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": f"iceagesurvival:block/{name}_front",
            "north": f"iceagesurvival:block/{name}_front",
            "west": f"iceagesurvival:block/{name}_front",
            "south": f"iceagesurvival:block/{name}_side",
            "east": f"iceagesurvival:block/{name}_side",
            "up": f"iceagesurvival:block/{name}_top",
            "down": "minecraft:block/spruce_planks",
        },
    }


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def main():
    rng = random.Random(53)
    textures = {
        "construction_bench_top": construction_top(rng),
        "construction_bench_side": construction_side(rng),
        "construction_bench_front": construction_front(rng),
        "armory_bench_top": armory_top(rng),
        "armory_bench_side": armory_side(rng),
        "armory_bench_front": armory_front(rng),
    }
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, img in textures.items():
        img.save(TEXTURES / f"{name}.png")
    for name in ("construction_bench", "armory_bench"):
        write_json(ASSETS / "blockstates" / f"{name}.json",
                   {"variants": {"": {"model": f"iceagesurvival:block/{name}"}}})
        write_json(ASSETS / "models" / "block" / f"{name}.json", block_model(name))
        write_json(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"iceagesurvival:block/{name}"})
    print(f"{len(textures)} texturas e 2 bancadas geradas em {ASSETS}")


if __name__ == "__main__":
    main()
