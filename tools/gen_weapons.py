#!/usr/bin/env python3
"""Gera as texturas 16x16 das armas tranquilizantes: dardo sedativo, rifle e besta de dardos.

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão.
"""
from pathlib import Path

from PIL import Image

ITEMS = (Path(__file__).resolve().parent.parent
         / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures" / "item")
SIZE = 16

METAL = (150, 156, 164)
METAL_DARK = (88, 94, 104)
METAL_LIGHT = (206, 212, 218)
WOOD = (122, 84, 52)
WOOD_DARK = (84, 56, 34)
WOOD_LIGHT = (160, 116, 74)
DRUG = (120, 70, 170)
DRUG_LIGHT = (176, 128, 220)
FLETCH = (196, 60, 52)
STRING = (220, 214, 196)


def new():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), color + (255,))


def diagonal(img, x0, y0, length, color, shade=None):
    """Linha de baixo-esquerda para cima-direita, com sombra embaixo."""
    for i in range(length):
        put(img, x0 + i, y0 - i, color)
        if shade:
            put(img, x0 + i + 1, y0 - i, shade)


def dart():
    img = new()
    # Penas na cauda (baixo-esquerda), cápsula roxa no meio, agulha de metal na ponta.
    for x, y in ((2, 13), (3, 13), (2, 12), (3, 14), (4, 14)):
        put(img, x, y, FLETCH)
    diagonal(img, 4, 12, 3, METAL_DARK)
    diagonal(img, 6, 10, 4, DRUG, DRUG_LIGHT)
    diagonal(img, 10, 6, 2, METAL, METAL_DARK)
    diagonal(img, 12, 4, 2, METAL_LIGHT)
    put(img, 14, 2, METAL_LIGHT)
    return img


def rifle():
    img = new()
    # Coronha de madeira embaixo à esquerda.
    for x, y in ((1, 14), (2, 14), (1, 13), (2, 13), (3, 13), (2, 12), (3, 12), (4, 12), (3, 11), (4, 11)):
        put(img, x, y, WOOD)
    for x, y in ((1, 15), (2, 15), (3, 14), (4, 13), (5, 12)):
        put(img, x, y, WOOD_DARK)
    put(img, 3, 13, WOOD_LIGHT)
    # Gatilho e câmara com o dardo roxo.
    put(img, 6, 12, METAL_DARK)
    diagonal(img, 5, 10, 3, METAL, METAL_DARK)
    put(img, 6, 8, DRUG)
    put(img, 7, 8, DRUG_LIGHT)
    # Cano comprido até cima à direita.
    diagonal(img, 8, 7, 7, METAL_LIGHT, METAL_DARK)
    put(img, 15, 0, METAL_DARK)
    return img


def crossbow():
    img = new()
    # Coronha de madeira na diagonal.
    diagonal(img, 2, 13, 9, WOOD, WOOD_DARK)
    put(img, 1, 14, WOOD_DARK)
    put(img, 2, 14, WOOD_DARK)
    # Arco de metal atravessado (cima-esquerda a baixo-direita), com a corda.
    for i in range(8):
        put(img, 4 + i, 3 + i, METAL)
        put(img, 5 + i, 3 + i, METAL_DARK)
    for i in range(5):
        put(img, 5 + i, 6 + i // 2, STRING)
    # Dardo roxo encaixado na frente.
    diagonal(img, 9, 6, 3, DRUG, DRUG_LIGHT)
    put(img, 12, 3, METAL_LIGHT)
    return img


def main():
    ITEMS.mkdir(parents=True, exist_ok=True)
    for name, image in (("tranq_dart", dart()), ("tranq_rifle", rifle()), ("tranq_crossbow", crossbow())):
        image.save(ITEMS / f"{name}.png")
        print(f"{name}.png")


if __name__ == "__main__":
    main()
