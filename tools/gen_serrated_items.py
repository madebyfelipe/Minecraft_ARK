#!/usr/bin/env python3
"""Gera a textura 16x16 do dente serrilhado do Giganotosaurus (D47, mantido na D51; a espada saiu em 2026-10-04).

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão. Arte autoral, desenhada aqui pixel a pixel
(nada do Jurassic Reborn nem do Revival).
"""
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
ITEMS = ASSETS / "textures" / "item"
SIZE = 16

CLEAR = (0, 0, 0, 0)
OUTLINE = (40, 30, 26, 255)
ENAMEL = [(214, 204, 178), (232, 224, 200), (246, 240, 224)]
ROOT = (150, 128, 100)
BLOOD = [(96, 10, 10), (138, 3, 3), (176, 24, 20)]
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


def main():
    ITEMS.mkdir(parents=True, exist_ok=True)
    outputs = {
        ITEMS / "serrated_tooth.png": tooth(),
    }
    for path, img in outputs.items():
        img.save(path)
        print(path.name)


if __name__ == "__main__":
    main()
