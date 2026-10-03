#!/usr/bin/env python3
"""Gera as texturas do Anquilossauro que são do projeto:

- ``textures/item/dung.png`` (16x16): o esterco, um montinho marrom em três camadas, com luz de cima à
  esquerda, fibras de capim claras e fundo transparente;
- ``textures/mob_effect/broken_leg.png`` (18x18): o ícone do efeito Perna quebrada, um osso partido ao
  meio com a lasca vermelha.

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"

OUTLINE = (40, 26, 16)
DARK = (74, 48, 28)
MID = (104, 70, 40)
LIGHT = (138, 96, 56)
HIGHLIGHT = (166, 122, 74)
FIBER = (150, 140, 80)

# Mapa do montinho: '.' vazio, 'o' contorno, 'd' sombra, 'm' meio, 'l' luz, 'h' brilho, 'f' fibra.
DUNG = [
    "................",
    "................",
    "................",
    ".......oo.......",
    "......ohlo......",
    ".....ohlmdo.....",
    "......odddo.....",
    "....oohllmdoo...",
    "...ohllmmfmddo..",
    "...olmmmddddo...",
    "..oohlmmmdddoo..",
    ".ohlllmfmmmmddo.",
    ".olmmmmmmmdddddo",
    ".odmmmmddddddddo",
    "..oddddddddddoo.",
    "...oooooooooo...",
]

DUNG_COLORS = {"o": OUTLINE, "d": DARK, "m": MID, "l": LIGHT, "h": HIGHLIGHT, "f": FIBER}

BONE_DARK = (150, 140, 112)
BONE = (214, 204, 172)
BONE_LIGHT = (240, 234, 212)
CRACK = (170, 30, 26)
EDGE = (60, 44, 34)

# Osso partido em diagonal, 18x18.
BROKEN_LEG = [
    "..................",
    "..ee..............",
    ".eLLe.............",
    ".eLbbe............",
    "..ebbbe...........",
    "...ebbbe..........",
    "....ebbbe.........",
    ".....ebbcc........",
    "......ecc.........",
    ".........cce......",
    "........ccbbe.....",
    ".........ebbbe....",
    "..........ebbbe...",
    "...........ebbbe..",
    "............ebbbe.",
    ".............ebbe.",
    "..............ee..",
    "..................",
]

BONE_COLORS = {"e": EDGE, "b": BONE, "L": BONE_LIGHT, "c": CRACK, "D": BONE_DARK}


def paint(rows, colors, out):
    size = len(rows)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == size, (out.name, y, len(row))
        for x, ch in enumerate(row):
            if ch in colors:
                img.putpixel((x, y), colors[ch] + (255,))
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("gerado", out.relative_to(ASSETS))


def shade_bone(rows):
    """Sombra no lado de baixo/direito de cada trecho de osso, para dar volume."""
    out = [list(r) for r in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "b" and x + 1 < len(row) and row[x + 1] == "e":
                out[y][x] = "D"
    return ["".join(r) for r in out]


def main():
    paint(DUNG, DUNG_COLORS, ASSETS / "textures" / "item" / "dung.png")
    paint(shade_bone(BROKEN_LEG), BONE_COLORS, ASSETS / "textures" / "mob_effect" / "broken_leg.png")


if __name__ == "__main__":
    main()
