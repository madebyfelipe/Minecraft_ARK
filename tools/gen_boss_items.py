#!/usr/bin/env python3
"""Gera as texturas 16x16 do boss da arena (D47): dente serrilhado, espada serrilhada e o altar.

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão. Arte autoral, desenhada aqui pixel a pixel
(nada do Jurassic Reborn nem do Revival).
"""
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
ITEMS = ASSETS / "textures" / "item"
BLOCKS = ASSETS / "textures" / "block"
SIZE = 16

CLEAR = (0, 0, 0, 0)
OUTLINE = (40, 30, 26, 255)
ENAMEL = [(214, 204, 178), (232, 224, 200), (246, 240, 224)]
ROOT = (150, 128, 100)
BLOOD = [(96, 10, 10), (138, 3, 3), (176, 24, 20)]
DIAMOND = [(30, 120, 118), (52, 190, 182), (120, 232, 222), (210, 252, 246)]
HILT = [(60, 38, 22), (88, 58, 34)]
GUARD = (70, 70, 78)
STONE = [(48, 46, 50), (60, 58, 62), (72, 70, 74), (84, 82, 86)]
RUNE = [(120, 16, 14), (168, 30, 22)]
GLOW = [(220, 80, 30), (255, 150, 60)]


def tooth():
    """Um dente curvo de lâmina, com a serrilha na borda de trás e a ponta suja de sangue."""
    img = Image.new("RGBA", (SIZE, SIZE), CLEAR)
    # Coluna do dente: (y, x inicial, x final) — mais largo na raiz (embaixo), curvado para a direita na ponta.
    rows = [(1, 10, 11), (2, 9, 11), (3, 9, 11), (4, 8, 11), (5, 8, 11), (6, 7, 11), (7, 7, 11), (8, 6, 11),
            (9, 6, 11), (10, 5, 11), (11, 5, 11), (12, 5, 11), (13, 5, 11)]
    for y, x0, x1 in rows:
        for x in range(x0, x1 + 1):
            shade = ENAMEL[min(2, (x - x0) * 3 // max(1, x1 - x0 + 1))]
            img.putpixel((x, y), shade + (255,))
        img.putpixel((x0 - 1, y), OUTLINE)
        # Serrilha: dentículos alternados na borda de trás.
        img.putpixel((x1 + 1, y), OUTLINE if y % 2 else ENAMEL[0] + (255,))
        if y % 2 == 0:
            img.putpixel((min(SIZE - 1, x1 + 2), y), OUTLINE)
    img.putpixel((10, 0), OUTLINE)
    img.putpixel((11, 0), OUTLINE)
    # Raiz.
    for x in range(5, 12):
        img.putpixel((x, 14), ROOT + (255,))
        img.putpixel((x, 15), OUTLINE)
    img.putpixel((4, 14), OUTLINE)
    img.putpixel((12, 14), OUTLINE)
    # Sangue na ponta.
    for x, y in [(10, 1), (10, 2), (11, 2), (9, 3), (10, 4)]:
        img.putpixel((x, y), BLOOD[(x + y) % 3] + (255,))
    return img


def sword():
    """Espada na diagonal (cabo embaixo à esquerda), lâmina de diamante com o fio serrilhado de dente."""
    img = Image.new("RGBA", (SIZE, SIZE), CLEAR)
    # Lâmina: de (5,10) até (14,1).
    for i in range(10):
        x, y = 5 + i, 10 - i
        img.putpixel((x, y), DIAMOND[2] + (255,))
        img.putpixel((x - 1, y), DIAMOND[1] + (255,))
        img.putpixel((x, y - 1), DIAMOND[3] + (255,))
        # Fio serrilhado (de dente) num dos lados.
        if i % 2 == 0:
            img.putpixel((min(SIZE - 1, x + 1), y), ENAMEL[1] + (255,))
        else:
            img.putpixel((min(SIZE - 1, x + 1), y), OUTLINE)
        if x - 2 >= 0:
            img.putpixel((x - 2, y), OUTLINE)
    img.putpixel((15, 0), OUTLINE)
    img.putpixel((14, 0), DIAMOND[3] + (255,))
    # Sangue perto da ponta.
    img.putpixel((12, 3), BLOOD[1] + (255,))
    img.putpixel((13, 2), BLOOD[2] + (255,))
    # Guarda.
    for x, y in [(2, 9), (3, 10), (4, 11), (5, 12), (6, 13)]:
        img.putpixel((x, y), GUARD + (255,))
    # Cabo.
    for i in range(3):
        img.putpixel((3 - i, 12 + i), HILT[i % 2] + (255,))
        img.putpixel((2 - i, 12 + i), OUTLINE)
    img.putpixel((0, 15), ENAMEL[0] + (255,))
    return img


def stone(rng):
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            shade = STONE[(x // 4 + y // 4 + rng.randrange(2)) % len(STONE)]
            if x % 8 == 0 or y % 4 == 0:
                shade = STONE[0]
            img.putpixel((x, y), shade + (255,))
    return img


def altar_side(rng):
    img = stone(rng)
    # Fileira de dentes entalhada no meio, com runas de sangue.
    for x in range(4, 12):
        img.putpixel((x, 7), OUTLINE)
        if x % 2 == 0:
            img.putpixel((x, 8), ENAMEL[0] + (255,))
            img.putpixel((x, 9), ENAMEL[0] + (255,))
        else:
            img.putpixel((x, 8), ENAMEL[1] + (255,))
    for x, y in [(5, 5), (7, 4), (9, 5), (11, 4), (6, 11), (8, 10), (10, 11)]:
        img.putpixel((x, y), RUNE[(x + y) % 2] + (255,))
    return img


def altar_top(rng, lit):
    img = stone(rng)
    # Bacia no centro com o desenho de uma pegada de terópode (três dedos).
    basin = GLOW if lit else RUNE
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), STONE[0] + (255,))
    for x, y in [(7, 10), (8, 10), (7, 9), (8, 9), (7, 8), (8, 8),
                 (5, 5), (6, 6), (6, 7), (7, 4), (7, 5), (7, 6), (8, 4), (8, 5), (8, 6),
                 (10, 5), (9, 6), (9, 7)]:
        img.putpixel((x, y), basin[(x + y) % 2] + (255,))
    return img


def main():
    rng = random.Random(47)
    ITEMS.mkdir(parents=True, exist_ok=True)
    BLOCKS.mkdir(parents=True, exist_ok=True)
    outputs = {
        ITEMS / "serrated_tooth.png": tooth(),
        ITEMS / "serrated_sword.png": sword(),
        BLOCKS / "arena_altar_side.png": altar_side(rng),
        BLOCKS / "arena_altar_bottom.png": stone(rng),
        BLOCKS / "arena_altar_top.png": altar_top(rng, False),
        BLOCKS / "arena_altar_top_summoning.png": altar_top(rng, True),
    }
    for path, img in outputs.items():
        img.save(path)
        print(path.name)


if __name__ == "__main__":
    main()
