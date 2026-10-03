#!/usr/bin/env python3
"""Gera o ícone do Analisador: um aparelho de campo de mão, em pé e visto de frente, de carcaça metálica
azul-acinzentada com borda escura. Na metade de cima, uma tela verde-água rebaixada mostra uma onda de leitura; no
topo, um LED âmbar de ligado e uma antena curta de ponta vermelha; embaixo da tela, um botão vermelho e dois frisos
de pega.

Escreve `textures/item/analyzer.png` (16x16) e o modelo `models/item/analyzer.json` (`minecraft:item/generated`).
A estética é de equipamento militar/científico de campo; nada é copiado de jogo nenhum.

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
TEXTURES = ASSETS / "textures" / "item"
MODELS = ASSETS / "models" / "item"
SIZE = 16

OUTLINE = (18, 22, 30)
METAL_DARK = (50, 62, 80)
METAL = (82, 98, 118)
METAL_LIGHT = (118, 136, 154)
METAL_SHINE = (160, 178, 192)
SCREEN_DARK = (10, 44, 50)
SCREEN = (26, 112, 112)
SCREEN_GLOW = (150, 255, 228)
AMBER = (255, 178, 44)
RED = (204, 42, 36)

# Luz vindo de cima à esquerda: a faixa de cima e a coluna da esquerda claras, a coluna da direita escura; a tela é
# rebaixada, então a fileira de cima dela fica na sombra.
PALETTE = {
    ".": None,
    "O": OUTLINE,
    "D": METAL_DARK,
    "M": METAL,
    "L": METAL_LIGHT,
    "H": METAL_SHINE,
    "s": SCREEN_DARK,
    "c": SCREEN,
    "g": SCREEN_GLOW,
    "A": AMBER,
    "R": RED,
}

# Uma letra por pixel, fileira por fileira (y de cima para baixo).
GRID = [
    "..........R.....",
    "..........O.....",
    "....OOOOOOOO....",
    "...OHAHHHHHMO...",
    "...OLssssssDO...",
    "...OLccccccDO...",
    "...OLcggcccDO...",
    "...OLgccgccDO...",
    "...OLccccggDO...",
    "...OLccccccDO...",
    "...OLMMMMMMDO...",
    "...OLMMMRRMDO...",
    "...ODDDDDDDDO...",
    "...OLMMMMMMDO...",
    "...ODDDDDDDDO...",
    "....OOOOOOOO....",
]


def icon():
    assert len(GRID) == SIZE and all(len(row) == SIZE for row in GRID), "a grade precisa ser 16x16"
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(GRID):
        for x, key in enumerate(row):
            color = PALETTE[key]
            if color is not None:
                img.putpixel((x, y), color + (255,))
    return img


def write_json(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    icon().save(TEXTURES / "analyzer.png")
    write_json(MODELS / "analyzer.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "iceagesurvival:item/analyzer"},
    })
    print(f"escritos {TEXTURES / 'analyzer.png'} e {MODELS / 'analyzer.json'}")


if __name__ == "__main__":
    main()
