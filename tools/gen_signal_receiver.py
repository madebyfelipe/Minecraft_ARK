#!/usr/bin/env python3
"""Gera a arte do Receptor de Sinal: o ícone 2D do inventário e o modelo 3D animado da mão (GeckoLib).

Na lore, é o rádio de campo que os militares levavam nos postos: ainda capta o sinal de emergência que a base do Projeto
Limiar transmite desde o clarão. Carcaça verde-oliva de equipamento militar, com estêncil branco "PL-7"; na frente, uma
tela de radar âmbar rebaixada (a varredura gira, o ponto do sinal pisca), a grade do alto-falante e três teclas; no topo,
a antena de chicote com fita e ponta, o botão de sintonia e o LED vermelho; embaixo, a bateria mais escura com duas
travas; atrás, o clipe de cinto. Nada é copiado de jogo nenhum.

Escreve:

- `textures/item/signal_receiver.png` (16x16): o ícone da GUI.
- `geo/item/signal_receiver.geo.json`, `textures/item/signal_receiver_3d.png` (+ `_glowmask`: a tela, o ponto e o LED
  acesos no escuro) e `animations/item/signal_receiver.animation.json` (`signal`, com a base no alcance, e
  `no_signal`, chiado): o modelo de `client/item/SignalReceiverRenderer`.
- `models/item/signal_receiver.json`: o modelo 3D em todo contexto e o ícone na GUI.

Reaproveita de `gen_analyzer.py` o modelo Bedrock, as contas de posição (display) e a prévia.
Prévia: `python3 tools/gen_signal_receiver.py --preview <pasta>`.

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_analyzer as base  # noqa: E402

TEXTURES = base.TEXTURES
NAME = "signal_receiver"
SIZE = 16

OUTLINE = (20, 22, 16)
OLIVE_DARK = (56, 62, 38)
OLIVE = (86, 94, 58)
OLIVE_LIGHT = (118, 126, 82)
OLIVE_SHINE = (150, 156, 108)
BATTERY = (46, 50, 34)
RUBBER = (34, 36, 30)
METAL_DARK = (58, 60, 62)
METAL = (108, 110, 112)
METAL_LIGHT = (150, 152, 150)
SCREEN_DARK = (36, 20, 6)
SCREEN_GRID = (78, 46, 10)
SCREEN = (176, 104, 22)
AMBER_GLOW = (255, 196, 80)
RED = (206, 40, 32)
STENCIL = (214, 210, 186)
TAPE = (160, 132, 64)

# ---------------------------------------------------------------- ícone 2D (GUI)

PALETTE = {
    ".": None,
    "O": OUTLINE,
    "d": OLIVE_DARK,
    "o": OLIVE,
    "l": OLIVE_LIGHT,
    "h": OLIVE_SHINE,
    "b": BATTERY,
    "m": METAL,
    "s": SCREEN_DARK,
    "c": SCREEN,
    "g": AMBER_GLOW,
    "R": RED,
    "w": STENCIL,
}

# A antena sai do canto de cima à esquerda, inclinada; a tela âmbar em cima, a grade embaixo e a bateria no pé.
ICON = [
    "..g.............",
    "...m............",
    "...m............",
    "....m...........",
    "....m.....R.m...",
    "...OOOOOOOOOOO..",
    "...OhllllllloO..",
    "...OlssssscsdO..",
    "...OlsssscssdO..",
    "...OlsgsgsssdO..",
    "...OlsssssssdO..",
    "...OloOoOoOodO..",
    "...OlwwoomRodO..",
    "...ObbbbbbbbbO..",
    "...ObmbbbbbmbO..",
    "....OOOOOOOOO...",
]


def icon():
    assert len(ICON) == SIZE and all(len(row) == SIZE for row in ICON), "a grade precisa ser 16x16"
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(ICON):
        for x, key in enumerate(row):
            if PALETTE[key] is not None:
                img.putpixel((x, y), PALETTE[key] + (255,))
    return img


# ---------------------------------------------------------------- modelo 3D: textura

TEX = 64
REGIONS = {
    "front": (0, 0, 20, 36),        # 5 x 9 px
    "back": (20, 0, 20, 36),
    "side": (40, 0, 12, 36),        # 3 x 9 px
    "end": (0, 36, 20, 12),         # topo e fundo, 5 x 3 px
    "battery_face": (20, 36, 22, 8),  # 5,5 x 2 px
    "battery_side": (42, 36, 14, 8),  # 3,5 x 2 px
    "screen": (0, 48, 16, 15),      # 4 x 3,6 px
}
SWATCHES = {
    "outline": OUTLINE,
    "olive_dark": OLIVE_DARK,
    "olive": OLIVE,
    "battery": BATTERY,
    "rubber": RUBBER,
    "metal_dark": METAL_DARK,
    "metal": METAL,
    "metal_light": METAL_LIGHT,
    "red": RED,
    "glow": AMBER_GLOW,
    "tape": TAPE,
    "stencil": STENCIL,
    "dark_red": (110, 22, 18),
}
for i, name in enumerate(SWATCHES):
    REGIONS[name] = (16 + (i % 12) * 4, 48 + (i // 12) * 4, 4, 4)
# Peças que acendem no escuro.
GLOWING = ("screen", "glow", "red")


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color)


def paint_olive(img, rect, rng, color=OLIVE):
    """Chapa pintada de verde-oliva fosco: ruído fino, borda clara em cima/esquerda, escura embaixo/direita e uns
    arranhões que deixam ver o metal."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = shade(color, rng.randint(-4, 4))
            if y == 0 or x == 0:
                c = shade(color, 22)
            elif y == h - 1 or x == w - 1:
                c = shade(color, -26)
            img.putpixel((x0 + x, y0 + y), c + (255,))
    for _ in range(max(1, w * h // 120)):
        x, y = rng.randint(2, w - 3), rng.randint(2, h - 3)
        for k in range(rng.randint(1, 3)):
            if x + k < w - 1:
                img.putpixel((x0 + x + k, y0 + y), METAL + (255,))


def screw(img, x, y):
    img.putpixel((x, y), METAL_DARK + (255,))
    img.putpixel((x + 1, y), METAL_LIGHT + (255,))
    img.putpixel((x, y + 1), METAL_LIGHT + (255,))
    img.putpixel((x + 1, y + 1), OUTLINE + (255,))


# Letras de estêncil 3x5 (só as que o aparelho usa).
STENCIL_FONT = {
    "P": ["##.", "#.#", "##.", "#..", "#.."],
    "L": ["#..", "#..", "#..", "#..", "###"],
    "-": ["...", "...", "###", "...", "..."],
    "7": ["###", "..#", ".#.", ".#.", ".#."],
}


def stencil(img, x, y, text):
    """Escreve espelhado: o GeckoLib inverte o X da face, e o texto lê certo no modelo."""
    for ch in reversed(text):
        for row, line in enumerate(STENCIL_FONT[ch]):
            for col, mark in enumerate(reversed(line)):
                if mark == "#":
                    img.putpixel((x + col, y + row), STENCIL + (255,))
        x += 4


def paint_front(img, rng):
    rect = REGIONS["front"]
    paint_olive(img, rect, rng)
    x0, y0, w, h = rect
    for x in (1, w - 3):
        screw(img, x0 + x, y0 + 1)
    # A tela cobre o topo (do y 3 ao 18); embaixo, a grade do alto-falante: fendas horizontais.
    for row in range(3):
        y = y0 + 19 + row * 2
        for x in range(3, w - 3):
            img.putpixel((x0 + x, y), OUTLINE + (255,))
    # Estêncil de identificação embaixo da grade.
    stencil(img, x0 + 2, y0 + 25, "PL-7")


def paint_back(img, rng):
    rect = REGIONS["back"]
    paint_olive(img, rect, rng, OLIVE_DARK)
    x0, y0, w, h = rect
    # Placa de identificação rebitada.
    for x in range(4, w - 4):
        for y in range(6, 14):
            img.putpixel((x0 + x, y0 + y), shade(METAL, rng.randint(-6, 6)) + (255,))
    for x in range(6, w - 6, 2):
        img.putpixel((x0 + x, y0 + 9), METAL_DARK + (255,))
        img.putpixel((x0 + x, y0 + 11), METAL_DARK + (255,))
    for x, y in ((4, 6), (w - 5, 6), (4, 13), (w - 5, 13)):
        img.putpixel((x0 + x, y0 + y), OUTLINE + (255,))


def paint_side(img, rng):
    rect = REGIONS["side"]
    paint_olive(img, rect, rng, shade(OLIVE, -8))
    x0, y0, w, h = rect
    # Emenda da carcaça e frisos de pega.
    for y in range(1, h - 1):
        img.putpixel((x0 + w // 2, y0 + y), OLIVE_DARK + (255,))
    for y in range(20, 32, 3):
        for x in range(2, w - 2):
            img.putpixel((x0 + x, y0 + y), OUTLINE + (255,))


def paint_battery(img, rng, key):
    x0, y0, w, h = REGIONS[key]
    for y in range(h):
        for x in range(w):
            c = shade(BATTERY, rng.randint(-3, 3))
            if y == 0:
                c = OUTLINE
            img.putpixel((x0 + x, y0 + y), c + (255,))
    # Travas de metal nas pontas.
    for x in (1, w - 3):
        for y in range(2, h - 2):
            img.putpixel((x0 + x, y0 + y), METAL + (255,))
            img.putpixel((x0 + x + 1, y0 + y), METAL_DARK + (255,))


def paint_screen(img):
    """Tela de radar âmbar: fundo escuro, anéis e cruz de mira apagados; a varredura e o ponto são ossos à parte."""
    x0, y0, w, h = REGIONS["screen"]
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            c = SCREEN_DARK
            if abs(d - 3.0) < 0.5 or abs(d - 6.2) < 0.5 or x == round(cx) or y == round(cy):
                c = SCREEN_GRID
            img.putpixel((x0 + x, y0 + y), c + (255,))
    img.putpixel((x0 + round(cx), y0 + round(cy)), SCREEN + (255,))
    # Leitura de frequência no canto de baixo.
    for x in range(1, 6):
        img.putpixel((x0 + x, y0 + h - 2), SCREEN + (255,))
    for x in range(w):
        img.putpixel((x0 + x, y0), OUTLINE + (255,))
    for y in range(h):
        img.putpixel((x0, y0 + y), OUTLINE + (255,))


def model_textures():
    rng = random.Random(53)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    paint_front(img, rng)
    paint_back(img, rng)
    paint_side(img, rng)
    paint_olive(img, REGIONS["end"], rng, OLIVE_LIGHT)
    paint_battery(img, rng, "battery_face")
    paint_battery(img, rng, "battery_side")
    paint_screen(img)
    for name, color in SWATCHES.items():
        x0, y0, w, h = REGIONS[name]
        for y in range(h):
            for x in range(w):
                img.putpixel((x0 + x, y0 + y), shade(color, rng.randint(-2, 2)) + (255,))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for name in GLOWING:
        x0, y0, w, h = REGIONS[name]
        glow.paste(img.crop((x0, y0, x0 + w, y0 + h)), (x0, y0))
    # Na tela, só a grade e as leituras acendem; o fundo fica apagado.
    x0, y0, w, h = REGIONS["screen"]
    for y in range(h):
        for x in range(w):
            if img.getpixel((x0 + x, y0 + y))[:3] in (SCREEN_DARK, OUTLINE):
                glow.putpixel((x0 + x, y0 + y), (0, 0, 0, 0))
    return img, glow


# ---------------------------------------------------------------- modelo 3D: geometria

def build_model():
    base.REGIONS = REGIONS  # o Model do Analisador lê as regiões do módulo dele
    m = base.Model()
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 0, 0))
    # Carcaça: a tela e a grade na frente (+Z), a placa atrás.
    m.cube(body, (-2.5, 2, -1.5), (5, 9, 3), "side", {"south": "front", "north": "back", "up": "end", "down": "end"})
    # Bateria embaixo, um pouco mais larga, com as travas.
    m.cube(body, (-2.75, 0, -1.75), (5.5, 2, 3.5), "battery_side",
           {"south": "battery_face", "north": "battery_face", "up": "battery", "down": "battery"})
    # Clipe de cinto atrás.
    m.cube(body, (-1.0, 5, -2.0), (2.0, 4.5, 0.5), "metal_dark", {"north": "metal"})
    # Moldura da tela, saliente; a tela fica rebaixada.
    m.cube(body, (-2.3, 10.3, 1.5), (4.6, 0.35, 0.35), "outline")
    m.cube(body, (-2.3, 6.35, 1.5), (4.6, 0.35, 0.35), "outline")
    m.cube(body, (-2.3, 6.7, 1.5), (0.3, 3.6, 0.35), "outline")
    m.cube(body, (2.0, 6.7, 1.5), (0.3, 3.6, 0.35), "outline")
    m.cube(body, (-2.0, 6.7, 1.45), (4.0, 3.6, 0.1), "olive_dark", {"south": "screen"})
    # Teclas: duas de borracha e a vermelha de transmitir.
    m.cube(body, (-1.9, 2.6, 1.5), (0.9, 0.5, 0.3), "rubber")
    m.cube(body, (-0.45, 2.6, 1.5), (0.9, 0.5, 0.3), "rubber")
    m.cube(body, (1.0, 2.6, 1.5), (0.9, 0.5, 0.3), "red", {"north": "dark_red"})
    # Botão de sintonia no topo, à direita.
    m.cube(body, (0.6, 11, -0.7), (1.4, 0.7, 1.4), "metal_dark")
    m.cube(body, (0.85, 11.7, -0.45), (0.9, 0.5, 0.9), "metal", {"up": "metal_light"})

    # LED vermelho no topo, no meio; pisca pela escala.
    led = m.bone("led", "body", (-0.2, 11.2, 0.6))
    m.cube(led, (-0.6, 11, 0.2), (0.8, 0.45, 0.8), "red")

    # Antena de chicote: a base no canto esquerdo do topo, a haste com duas fitas e a ponta.
    antenna = m.bone("antenna", "body", (-1.7, 11, -0.4))
    m.cube(antenna, (-2.3, 11, -1.0), (1.2, 0.9, 1.2), "rubber")
    m.cube(antenna, (-1.9, 11.9, -0.6), (0.4, 8.5, 0.4), "metal_dark")
    for y in (13.0, 16.5):
        m.cube(antenna, (-1.98, y, -0.68), (0.56, 0.6, 0.56), "tape")
    m.cube(antenna, (-2.05, 20.4, -0.75), (0.7, 0.7, 0.7), "metal_dark")

    # Efeitos na tela, rentes à superfície: a varredura gira em volta do centro e o ponto do sinal pisca.
    center = (0, 8.5, 1.56)
    sweep = m.bone("sweep", "body", center)
    m.cube(sweep, (-0.08, 8.5, 1.55), (0.16, 1.7, 0.03), "glow")
    blip = m.bone("blip", "body", (0.9, 9.4, 1.57))
    m.cube(blip, (0.65, 9.15, 1.57), (0.5, 0.5, 0.02), "glow")
    # Chiado sem sinal: riscos que piscam em alturas diferentes.
    static = m.bone("static", "body", center)
    for y, x, w in ((9.6, -1.6, 1.8), (8.1, -0.4, 2.0), (7.2, -1.2, 1.4)):
        m.cube(static, (x, y, 1.56), (w, 0.15, 0.02), "glow")

    for bone in m.bones:
        if not bone["cubes"]:
            del bone["cubes"]
    return m


def geometry(model):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry." + NAME,
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 2,
                "visible_bounds_height": 2.5,
                "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": model.bones,
        }],
    }


# ---------------------------------------------------------------- animações

def spin(period, length):
    """A varredura dá uma volta inteira a cada `period` segundos (sentido horário visto de frente)."""
    keys, t = {}, 0.0
    while t < length - 1e-6:
        keys[f"{t:.3f}"] = [0, 0, 0]
        keys[f"{t + period - 0.001:.3f}"] = [0, 0, -359.9]
        t += period
    keys[f"{length:.3f}"] = [0, 0, 0]
    return keys


def animations():
    length = 4.0
    signal = {
        # Varredura girando, o ponto do sinal aceso logo depois que ela passa, LED lento e antena balançando pouco.
        "sweep": {"rotation": spin(2.0, length)},
        "blip": {"scale": base.blink(2.0, 0.45, length)},
        "static": {"scale": base.HIDDEN},
        "led": {"scale": base.blink(1.0, 0.5, length)},
        "antenna": {"rotation": {"0.000": [0, 0, 0], "1.000": [0, 0, 2], "3.000": [0, 0, -2], "4.000": [0, 0, 0]}},
    }
    no_signal = {
        # Sem a base no alcance: nada de varredura nem ponto, só o chiado piscando rápido e o LED apagado.
        "sweep": {"scale": base.HIDDEN},
        "blip": {"scale": base.HIDDEN},
        "static": {"scale": base.blink(0.2, 0.5, 1.0), "position": {"0.000": [0, 0, 0], "0.400": [0, -0.6, 0],
                                                                     "0.800": [0, 0.4, 0], "1.000": [0, 0, 0]}},
        "led": {"scale": base.HIDDEN},
    }
    return {
        "format_version": "1.8.0",
        "animations": {
            f"animation.{NAME}.signal": {"loop": True, "animation_length": length, "bones": signal},
            f"animation.{NAME}.no_signal": {"loop": True, "animation_length": 1.0, "bones": no_signal},
        },
    }


# ---------------------------------------------------------------- posições (display)

# Primeira pessoa: em pé, a tela virada para o jogador, um pouco inclinada; menor que o Analisador por causa da antena.
base.FP_ROT = [-24, -20, -8]
base.FP_SCALE = 0.5
base.FP_TRANSLATION = [-3.0, 2.5, -1.0]
# Terceira pessoa: em pé na bateria, a tela para a frente do jogador.
base.TP_ROT = [90, 180, 0]
base.TP_SCALE = 0.42
base.TP_TRANSLATION = [0, 1.5, -2.0]


def item_model(model):
    texture = "iceagesurvival:item/" + NAME
    return {
        "loader": "forge:separate_transforms",
        "gui_light": "front",
        "textures": {"particle": texture},
        "base": {"parent": "builtin/entity", "display": base.display(model)},
        "perspectives": {
            "gui": {"loader": "forge:item_layers", "textures": {"layer0": texture, "particle": texture}},
        },
    }


# ---------------------------------------------------------------- main

def main():
    base.TEX = TEX
    model = build_model()
    texture, glow = model_textures()
    if len(sys.argv) >= 3 and sys.argv[1] == "--preview":
        out = Path(sys.argv[2])
        base.preview(model, texture, out)
        for old, new in (("analyzer_3q.png", NAME + "_3q.png"), ("analyzer_firstperson.png", NAME + "_firstperson.png")):
            (out / old).replace(out / new)
        return
    TEXTURES.mkdir(parents=True, exist_ok=True)
    icon().save(TEXTURES / (NAME + ".png"))
    texture.save(TEXTURES / (NAME + "_3d.png"))
    glow.save(TEXTURES / (NAME + "_3d_glowmask.png"))
    base.write_json(base.GEO / (NAME + ".geo.json"), geometry(model))
    base.write_json(base.ANIMATIONS / (NAME + ".animation.json"), animations())
    base.write_json(base.MODELS / (NAME + ".json"), item_model(model))
    print(f"escritos o ícone, o modelo 3D (geo, textura, glowmask, animações) e models/item/{NAME}.json")


if __name__ == "__main__":
    main()
