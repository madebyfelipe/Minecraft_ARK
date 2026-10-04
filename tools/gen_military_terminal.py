#!/usr/bin/env python3
"""Gera a arte autoral do Terminal Militar dos postos (D51): texturas 16×16, blockstate e modelos.

    textures/block/military_terminal_front.png   tela verde-fósforo com linhas de texto e o cursor
    textures/block/military_terminal_side.png    chapa de aço com rebites e a grade de ventilação
    textures/block/military_terminal_top.png     tampa de aço com gelo nas bordas

Uso: python3 tools/gen_military_terminal.py
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets/iceagesurvival'
TEX = ASSETS / 'textures/block'

STEEL = (88, 96, 100)
STEEL_DARK = (58, 64, 68)
STEEL_LIGHT = (122, 132, 136)
RIVET = (150, 158, 160)
SCREEN = (10, 26, 18)
PHOSPHOR = (90, 230, 120)
PHOSPHOR_DIM = (40, 120, 64)
ICE = (196, 226, 240)


def steel(rng):
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            n = rng.randint(-6, 6)
            img.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in STEEL) + (255,))
    for i in range(16):  # moldura
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, STEEL_DARK + (255,))
        img.putpixel((i, 1), STEEL_LIGHT + (255,))
    return img


def front(rng):
    img = steel(rng)
    for x in range(2, 14):
        for y in range(2, 11):
            img.putpixel((x, y), SCREEN + (255,))
    for y, length in ((3, 8), (5, 10), (7, 6)):
        for x in range(3, 3 + length):
            img.putpixel((x, y), (PHOSPHOR if rng.random() > 0.25 else PHOSPHOR_DIM) + (255,))
    img.putpixel((3, 9), PHOSPHOR + (255,))
    img.putpixel((4, 9), PHOSPHOR + (255,))
    for x in range(3, 13, 2):  # teclado
        img.putpixel((x, 13), STEEL_LIGHT + (255,))
    img.putpixel((13, 13), (200, 60, 50, 255))  # LED
    return img


def side(rng):
    img = steel(rng)
    for y in range(5, 12, 2):
        for x in range(4, 12):
            img.putpixel((x, y), STEEL_DARK + (255,))
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel(p, RIVET + (255,))
    return img


def top(rng):
    img = steel(rng)
    for i in range(16):
        for p in ((i, 0), (0, i), (i, 15), (15, i)):
            if rng.random() < 0.6:
                img.putpixel(p, ICE + (255,))
    return img


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    for name, painter in (('front', front), ('side', side), ('top', top)):
        painter(random.Random(f'military_terminal_{name}')).save(TEX / f'military_terminal_{name}.png')
    write_json(ASSETS / 'models/block/military_terminal.json', {
        'parent': 'minecraft:block/orientable',
        'textures': {
            'front': 'iceagesurvival:block/military_terminal_front',
            'side': 'iceagesurvival:block/military_terminal_side',
            'top': 'iceagesurvival:block/military_terminal_top',
        },
    })
    write_json(ASSETS / 'models/item/military_terminal.json', {'parent': 'iceagesurvival:block/military_terminal'})
    write_json(ASSETS / 'blockstates/military_terminal.json', {'variants': {
        f'facing={facing}': {'model': 'iceagesurvival:block/military_terminal', **({'y': rot} if rot else {})}
        for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270))
    }})
    print('military_terminal: 3 texturas, blockstate e modelos')


if __name__ == '__main__':
    main()
