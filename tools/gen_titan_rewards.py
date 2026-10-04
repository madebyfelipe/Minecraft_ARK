#!/usr/bin/env python3
"""Gera as texturas 16x16 das recompensas do Titanovenator: o soro (frasco com o sangue dele) e o projetor de êxtase.

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão. Arte autoral, desenhada aqui pixel a pixel
num mapa de caracteres (nada do Revival).
"""
from pathlib import Path

from PIL import Image

ITEMS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures" / "item"

# Frasco de vidro com tampa de metal; o sangue escuro brilha em âmbar perto do vidro (a cor do ventre dele).
SERUM_PALETTE = {
    ".": None,
    "o": (34, 28, 30),      # contorno
    "m": (92, 98, 106),     # tampa
    "M": (150, 158, 166),   # brilho da tampa
    "g": (168, 206, 214),   # vidro
    "G": (232, 248, 252),   # reflexo
    "r": (88, 8, 10),       # sangue fundo
    "R": (140, 18, 14),     # sangue
    "a": (214, 132, 36),    # brilho âmbar
    "c": (46, 40, 38),      # rótulo carvão
}
SERUM = [
    "................",
    "......oooo......",
    ".....oMMmmo.....",
    ".....ommmmo.....",
    "......oggo......",
    "......oGgo......",
    ".....oGgggo.....",
    "....oGgRRggo....",
    "....oGRRRRgo....",
    "....ogRacRRo....",
    "....oGRccRRo....",
    "....oGRRRaro....",
    "....ogRRRRro....",
    "....ogrRRrro....",
    ".....orrrro.....",
    "......oooo......",
]

# Aparelho de mão: corpo de metal escuro com a lente ciano na frente e o cabo embaixo (na diagonal, como as armas).
PROJECTOR_PALETTE = {
    ".": None,
    "o": (20, 24, 28),      # contorno
    "d": (42, 52, 60),      # metal escuro
    "m": (74, 90, 100),     # metal
    "M": (128, 146, 156),   # brilho
    "c": (56, 198, 217),    # ciano do campo
    "C": (168, 244, 255),   # lente acesa
    "y": (255, 178, 30),    # luz âmbar
    "h": (60, 44, 34),      # cabo
}
PROJECTOR = [
    "................",
    "...........ooo..",
    "..........oCCco.",
    ".........ocCCCo.",
    "........omMccco.",
    ".......omMMmoo..",
    "......omMmmdo...",
    ".....omMmydo....",
    "....omMmmdo.....",
    "...omddddo......",
    "..ohoddo.o......",
    ".ohhoo..........",
    "ohhho...........",
    "ohho............",
    ".oo.............",
    "................",
]


def draw(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, row
        for x, key in enumerate(row):
            color = palette[key]
            if color is not None:
                img.putpixel((x, y), color + (255,))
    return img


def main():
    ITEMS.mkdir(parents=True, exist_ok=True)
    outputs = {
        ITEMS / "titan_serum.png": draw(SERUM, SERUM_PALETTE),
        ITEMS / "stasis_projector.png": draw(PROJECTOR, PROJECTOR_PALETTE),
    }
    for path, img in outputs.items():
        img.save(path)
        print(path.name)


if __name__ == "__main__":
    main()
