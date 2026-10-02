#!/usr/bin/env python3
"""Gera as texturas 16x16 das cabeças-troféu dos apex (T-Rex e Espinossauro).

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão.
"""
import random
from pathlib import Path

from PIL import Image

BLOCKS = (Path(__file__).resolve().parent.parent
          / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures" / "block")
SIZE = 16
TOOTH = (236, 230, 210)
GUM = (120, 40, 40)

PALETTES = {
    "tyrannosaurus_head": [(110, 82, 58), (96, 70, 48), (126, 94, 66), (84, 60, 42)],
    "spinosaurus_head": [(92, 104, 92), (80, 92, 82), (106, 118, 104), (70, 80, 72)],
}


def head(rng, shades):
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Escamas: um tom por célula 2x2, com uma pinta escura de vez em quando.
            shade = shades[(x // 2 + y // 2 * 3 + rng.randrange(2)) % len(shades)]
            if rng.random() < 0.06:
                shade = tuple(max(0, c - 30) for c in shade)
            img.putpixel((x, y), shade + (255,))
    # Fileira de dentes na faixa de baixo, sobre a gengiva.
    for x in range(SIZE):
        img.putpixel((x, 13), GUM + (255,))
        if x % 2 == 0:
            img.putpixel((x, 14), TOOTH + (255,))
    return img


def main():
    rng = random.Random(7)
    BLOCKS.mkdir(parents=True, exist_ok=True)
    for name, shades in PALETTES.items():
        head(rng, shades).save(BLOCKS / f"{name}.png")
        print(f"{name}.png")


if __name__ == "__main__":
    main()
