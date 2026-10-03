#!/usr/bin/env python3
"""Gera a arte do Analisador: o ícone 2D do inventário e o modelo 3D animado da mão (GeckoLib).

O aparelho é um rastreador de campo de mão, de carcaça metálica azul-acinzentada: na frente, uma tela verde-água
rebaixada numa moldura escura, com grade, onda de leitura e uma linha de varredura; embaixo da tela, três botões (um
vermelho); no topo, um LED âmbar de ligado e uma antena com travessa e ponta vermelha; de um lado, um botão de
sintonia; embaixo, a empunhadura emborrachada com frisos. A estética é de equipamento militar/científico de campo;
nada é copiado de jogo nenhum.

Escreve:

- `textures/item/analyzer.png` (16x16): o ícone, usado na GUI (inventário, barra, receitas), onde um sprite plano se
  lê melhor do que um modelo 3D de 16 pixels.
- `geo/item/analyzer.geo.json`, `textures/item/analyzer_3d.png` (+ `_glowmask`, a tela, o LED e a ponta da antena
  acesos no escuro) e `animations/item/analyzer.animation.json` (`idle` e `scan`): o modelo da mão, do chão e da
  moldura, desenhado por `client/item/AnalyzerRenderer`.
- `models/item/analyzer.json`: `forge:separate_transforms` com o modelo 3D (`builtin/entity`) em todo contexto e o
  ícone (`forge:item_layers`) na GUI. As posições de `display` são calculadas aqui.

Prévia: `python3 tools/gen_analyzer.py --preview <pasta>` grava PNGs do modelo (vista 3/4 e primeira pessoa).

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
TEXTURES = ASSETS / "textures" / "item"
MODELS = ASSETS / "models" / "item"
GEO = ASSETS / "geo" / "item"
ANIMATIONS = ASSETS / "animations" / "item"
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
RUBBER = (34, 38, 44)
RUBBER_LIGHT = (52, 58, 66)
GRID = (16, 62, 66)

# ---------------------------------------------------------------- ícone 2D (GUI)

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
ICON = [
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
    assert len(ICON) == SIZE and all(len(row) == SIZE for row in ICON), "a grade precisa ser 16x16"
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(ICON):
        for x, key in enumerate(row):
            color = PALETTE[key]
            if color is not None:
                img.putpixel((x, y), color + (255,))
    return img


# ---------------------------------------------------------------- modelo 3D: textura

TEX = 64            # lado da textura do modelo, em texels
DENSITY = 4         # texels por pixel de modelo nas faces grandes

# Regiões (x, y, largura, altura) da textura. As faces grandes têm desenho próprio; as peças pequenas usam amostras
# de cor lisa 4x4.
REGIONS = {
    "housing_front": (0, 0, 24, 28),    # 6 x 7 px
    "housing_back": (24, 0, 24, 28),
    "housing_side": (48, 0, 12, 28),    # 3 x 7 px
    "housing_end": (0, 28, 24, 12),     # topo e fundo, 6 x 3 px
    "grip_face": (24, 28, 20, 12),      # 5 x 3 px
    "grip_side": (44, 28, 10, 12),      # 2,5 x 3 px
    "screen": (0, 40, 18, 17),          # 4,4 x 4,2 px
}
SWATCHES = {
    "outline": OUTLINE,
    "metal_dark": METAL_DARK,
    "metal": METAL,
    "metal_light": METAL_LIGHT,
    "shine": METAL_SHINE,
    "rubber": RUBBER,
    "red": RED,
    "amber": AMBER,
    "glow": SCREEN_GLOW,
    "grey_button": (96, 104, 112),
    "dark_red": (120, 24, 22),
}
# As amostras ficam em fileiras de 4x4 a partir de (20, 40).
for i, name in enumerate(SWATCHES):
    REGIONS[name] = (20 + (i % 11) * 4, 40 + (i // 11) * 4, 4, 4)
# Peças que acendem no escuro (camada _glowmask do GeckoLib).
GLOWING = ("screen", "amber", "glow", "red")


def shade(color, amount):
    return tuple(max(0, min(255, c + amount)) for c in color)


def paint_metal(img, rect, rng, base=METAL, bevel=True):
    """Chapa metálica escovada: ruído fino em faixas horizontais, borda clara em cima/esquerda e escura embaixo/direita."""
    x0, y0, w, h = rect
    for y in range(h):
        streak = rng.randint(-4, 4)
        for x in range(w):
            c = shade(base, streak + rng.randint(-3, 3))
            if bevel:
                if y == 0 or x == 0:
                    c = shade(METAL_LIGHT, 10)
                elif y == h - 1 or x == w - 1:
                    c = METAL_DARK
            img.putpixel((x0 + x, y0 + y), c + (255,))


def screw(img, x, y):
    img.putpixel((x, y), METAL_DARK + (255,))
    img.putpixel((x + 1, y), METAL_SHINE + (255,))
    img.putpixel((x, y + 1), METAL_SHINE + (255,))
    img.putpixel((x + 1, y + 1), OUTLINE + (255,))


def paint_housing_front(img, rng):
    rect = REGIONS["housing_front"]
    paint_metal(img, rect, rng)
    x0, y0, w, h = rect
    # A moldura e a tela cobrem o meio; o que aparece são as bordas e o painel dos botões embaixo.
    for x in (2, w - 4):
        screw(img, x0 + x, y0 + 1)
        screw(img, x0 + x, y0 + h - 3)
    # Friso gravado entre a tela e os botões e outro abaixo dos botões.
    for x in range(2, w - 2):
        img.putpixel((x0 + x, y0 + 21), METAL_DARK + (255,))
        img.putpixel((x0 + x, y0 + 22), METAL_LIGHT + (255,))
        img.putpixel((x0 + x, y0 + h - 4), METAL_DARK + (255,))


def paint_housing_back(img, rng):
    rect = REGIONS["housing_back"]
    paint_metal(img, rect, rng)
    x0, y0, w, h = rect
    # Grade de ventilação: fendas horizontais em cima.
    for row in range(4):
        y = y0 + 3 + row * 3
        for x in range(4, w - 4):
            img.putpixel((x0 + x, y), OUTLINE + (255,))
            img.putpixel((x0 + x, y + 1), METAL_LIGHT + (255,))
    # Tampa da bateria: retângulo rebaixado com duas travas.
    for x in range(3, w - 3):
        img.putpixel((x0 + x, y0 + 16), METAL_DARK + (255,))
        img.putpixel((x0 + x, y0 + h - 3), METAL_LIGHT + (255,))
    for y in range(16, h - 2):
        img.putpixel((x0 + 3, y0 + y), METAL_DARK + (255,))
        img.putpixel((x0 + w - 4, y0 + y), METAL_LIGHT + (255,))
    for x in (w // 2 - 3, w // 2 + 2):
        img.putpixel((x0 + x, y0 + 19), METAL_SHINE + (255,))
        img.putpixel((x0 + x + 1, y0 + 19), METAL_SHINE + (255,))
    screw(img, x0 + 1, y0 + 1)
    screw(img, x0 + w - 3, y0 + 1)


def paint_housing_side(img, rng):
    rect = REGIONS["housing_side"]
    paint_metal(img, rect, rng, base=shade(METAL, -8))
    x0, y0, w, h = rect
    # Emenda das duas metades da carcaça e frisos de pega.
    for y in range(1, h - 1):
        img.putpixel((x0 + w // 2, y0 + y), METAL_DARK + (255,))
    for y in (h - 9, h - 6):
        for x in range(2, w - 2):
            img.putpixel((x0 + x, y0 + y), OUTLINE + (255,))


def paint_grip(img, rng, key):
    x0, y0, w, h = REGIONS[key]
    for y in range(h):
        for x in range(w):
            c = shade(RUBBER, rng.randint(-3, 3))
            if y % 3 == 0:
                c = RUBBER_LIGHT      # frisos do emborrachado
            img.putpixel((x0 + x, y0 + y), c + (255,))


def paint_screen(img):
    """Tela de radar: fundo verde-água escuro com grade, borda em sombra, uma onda de leitura e dois pontos."""
    x0, y0, w, h = REGIONS["screen"]
    for y in range(h):
        for x in range(w):
            c = SCREEN_DARK
            if x % 6 == 3 or y % 6 == 3:
                c = GRID
            img.putpixel((x0 + x, y0 + y), c + (255,))
    # Onda de leitura na metade de baixo.
    for x in range(1, w - 1):
        y = round(11 + 2.2 * math.sin(x * 0.8) * (0.4 + 0.6 * math.sin(x * 0.23)))
        img.putpixel((x0 + x, y0 + y), SCREEN_GLOW + (255,))
        img.putpixel((x0 + x, y0 + y + 1), SCREEN + (255,))
    # Barras de sinal no canto de cima.
    for i, height in enumerate((1, 2, 3)):
        for k in range(height):
            img.putpixel((x0 + 2 + i * 2, y0 + 5 - k), SCREEN_GLOW + (255,))
    for x, y in ((12, 4), (13, 4), (12, 5), (13, 5)):
        img.putpixel((x0 + x, y0 + y), SCREEN + (255,))
    # Sombra da moldura nas bordas (a tela é rebaixada).
    for x in range(w):
        img.putpixel((x0 + x, y0), OUTLINE + (255,))
    for y in range(h):
        img.putpixel((x0, y0 + y), OUTLINE + (255,))


def model_textures():
    rng = random.Random(49)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    paint_housing_front(img, rng)
    paint_housing_back(img, rng)
    paint_housing_side(img, rng)
    paint_metal(img, REGIONS["housing_end"], rng, base=METAL_LIGHT)
    paint_grip(img, rng, "grip_face")
    paint_grip(img, rng, "grip_side")
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
    # Na tela, só o que é luz de verdade acende: fundo e grade ficam apagados.
    x0, y0, w, h = REGIONS["screen"]
    for y in range(h):
        for x in range(w):
            if img.getpixel((x0 + x, y0 + y))[:3] in (SCREEN_DARK, OUTLINE):
                glow.putpixel((x0 + x, y0 + y), (0, 0, 0, 0))
    return img, glow


# ---------------------------------------------------------------- modelo 3D: geometria

FACES = ("north", "south", "east", "west", "up", "down")


class Model:
    """Ossos e cubos no formato Bedrock (unidades de 1/16 de bloco, Y para cima). A tela olha para +Z (sul)."""

    def __init__(self):
        self.bones = []

    def bone(self, name, parent, pivot):
        entry = {"name": name, "pivot": list(pivot), "cubes": []}
        if parent:
            entry["parent"] = parent
        self.bones.append(entry)
        return entry

    def cube(self, bone, origin, size, material, faces=None):
        """`material` cobre todas as faces; `faces` troca a região de faces específicas."""
        uv = {}
        for face in FACES:
            key = (faces or {}).get(face, material)
            x, y, w, h = REGIONS[key]
            uv[face] = {"uv": [x, y], "uv_size": [w, h]}
        bone["cubes"].append({"origin": [round(v, 4) for v in origin], "size": [round(v, 4) for v in size],
                              "uv": uv})


def build_model():
    m = Model()
    m.bone("root", None, (0, 0, 0))
    body = m.bone("body", "root", (0, 0, 0))
    # Carcaça: a tela e os botões na frente, a grade e a bateria atrás.
    m.cube(body, (-3, 3, -1.5), (6, 7, 3), "housing_side",
           {"south": "housing_front", "north": "housing_back", "up": "housing_end", "down": "housing_end"})
    # Empunhadura emborrachada, um pouco mais estreita, com dois frisos e a sapata de metal embaixo.
    m.cube(body, (-2.5, 0, -1.25), (5, 3, 2.5), "grip_face", {"east": "grip_side", "west": "grip_side",
                                                              "up": "rubber", "down": "rubber"})
    for y in (0.6, 1.8):
        m.cube(body, (-2.6, y, -1.35), (5.2, 0.4, 2.7), "metal_dark")
    m.cube(body, (-2.75, -0.3, -1.4), (5.5, 0.3, 2.8), "metal_dark")
    # Moldura da tela, saliente: a tela fica 0,3 px para dentro.
    m.cube(body, (-2.6, 9.2, 1.5), (5.2, 0.4, 0.35), "outline")
    m.cube(body, (-2.6, 4.6, 1.5), (5.2, 0.4, 0.35), "outline")
    m.cube(body, (-2.6, 5.0, 1.5), (0.4, 4.2, 0.35), "outline")
    m.cube(body, (2.2, 5.0, 1.5), (0.4, 4.2, 0.35), "outline")
    m.cube(body, (-2.2, 5.0, 1.45), (4.4, 4.2, 0.1), "metal_dark", {"south": "screen"})
    # Botões: um vermelho e dois cinza.
    m.cube(body, (-2.2, 3.5, 1.5), (1.0, 0.6, 0.3), "red", {"north": "dark_red"})
    m.cube(body, (-0.5, 3.5, 1.5), (1.0, 0.6, 0.3), "grey_button")
    m.cube(body, (1.2, 3.5, 1.5), (1.0, 0.6, 0.3), "grey_button")
    # Botão de sintonia na lateral.
    m.cube(body, (-3.5, 7.4, -0.6), (0.5, 1.2, 1.2), "metal_dark", {"west": "metal_light"})
    m.cube(body, (-3.7, 7.8, -0.2), (0.2, 0.4, 0.4), "shine")

    # LED de ligado, no topo; pisca pela escala.
    led = m.bone("led", "body", (1.3, 10.25, 0.6))
    m.cube(led, (0.8, 10, 0.1), (1.0, 0.5, 1.0), "amber")

    # Antena: base, haste e a cabeça (travessa e ponta vermelha) que gira no scan.
    antenna = m.bone("antenna", "body", (-1.8, 10, -0.4))
    m.cube(antenna, (-2.4, 10, -1.0), (1.2, 0.8, 1.2), "metal_dark")
    m.cube(antenna, (-2.05, 10.8, -0.65), (0.5, 4.5, 0.5), "metal_light")
    head = m.bone("antenna_head", "antenna", (-1.8, 15.3, -0.4))
    m.cube(head, (-3.0, 15.0, -0.55), (2.4, 0.3, 0.3), "metal")
    m.cube(head, (-2.3, 15.3, -0.9), (1.0, 1.0, 1.0), "red")

    # Efeitos na tela, rentes à superfície: a linha de varredura, o anel do ping e o ponto do alvo.
    sweep = m.bone("sweep", "body", (0, 8.9, 1.56))
    m.cube(sweep, (-2.1, 8.85, 1.55), (4.2, 0.2, 0.03), "glow")
    pulse = m.bone("pulse", "body", (0, 7.1, 1.56))
    for origin, size in (((-1.5, 8.45, 1.56), (3.0, 0.15, 0.02)), ((-1.5, 5.6, 1.56), (3.0, 0.15, 0.02)),
                         ((-1.5, 5.75, 1.56), (0.15, 2.7, 0.02)), ((1.35, 5.75, 1.56), (0.15, 2.7, 0.02))):
        m.cube(pulse, origin, size, "glow")
    blip = m.bone("blip", "body", (0.9, 7.8, 1.57))
    m.cube(blip, (0.6, 7.5, 1.57), (0.6, 0.6, 0.02), "amber")

    for bone in m.bones:
        if not bone["cubes"]:
            del bone["cubes"]
    return m


def geometry(model):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.analyzer",
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 2,
                "visible_bounds_height": 2,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": model.bones,
        }],
    }


# ---------------------------------------------------------------- animações

HIDDEN = [0.001, 0.001, 0.001]   # escala "apagada" (positiva: escala zero estraga as normais do PoseStack)
SHOWN = [1, 1, 1]


def blink(period, on_fraction, length):
    """Liga e desliga (escala) com período `period`, aceso na fração inicial de cada ciclo; troca quase instantânea."""
    keys, t = {}, 0.0
    while t < length - 1e-6:
        off = t + period * on_fraction
        keys[f"{t:.3f}"] = SHOWN
        keys[f"{off - 0.01:.3f}"] = SHOWN
        keys[f"{off:.3f}"] = HIDDEN
        keys[f"{t + period - 0.01:.3f}"] = HIDDEN
        t += period
    keys[f"{length:.3f}"] = SHOWN
    return keys


def sweep_down(period, length, distance=3.8):
    """A linha desce a tela e volta ao topo de uma vez, a cada `period` segundos."""
    keys, t = {}, 0.0
    while t < length - 1e-6:
        keys[f"{t:.3f}"] = [0, 0, 0]
        keys[f"{t + period - 0.01:.3f}"] = [0, -distance, 0]
        t += period
    keys[f"{length:.3f}"] = [0, 0, 0]
    return keys


def ping(period, length):
    """O anel do ping nasce pequeno no centro da tela e cresce até a borda, a cada `period` segundos."""
    keys, t = {}, 0.0
    while t < length - 1e-6:
        keys[f"{t:.3f}"] = [0.15, 0.15, 1]
        keys[f"{t + period - 0.01:.3f}"] = [1.25, 1.25, 1]
        t += period
    keys[f"{length:.3f}"] = [0.15, 0.15, 1]
    return keys


def animations():
    idle_len = 4.0
    scan_len = 1.0
    idle = {
        # LED piscando devagar e a varredura descendo calma; nada de ping nem alvo.
        "led": {"scale": blink(2.0, 0.5, idle_len)},
        "sweep": {"position": sweep_down(4.0, idle_len)},
        "pulse": {"scale": HIDDEN},
        "blip": {"scale": HIDDEN},
        "antenna": {"rotation": {"0.000": [0, 0, 0], "2.000": [0, 0, 1.5], "4.000": [0, 0, 0]}},
    }
    scan = {
        # LED rápido, antena oscilando e a cabeça dela girando, varredura e ping rápidos, alvo piscando.
        "led": {"scale": blink(0.25, 0.5, scan_len)},
        "sweep": {"position": sweep_down(0.5, scan_len)},
        "pulse": {"scale": ping(0.5, scan_len)},
        "blip": {"scale": blink(0.5, 0.6, scan_len)},
        "antenna": {"rotation": {"0.000": [0, 0, -8], "0.250": [0, 0, 8], "0.500": [0, 0, -8],
                                 "0.750": [0, 0, 8], "1.000": [0, 0, -8]}},
        "antenna_head": {"rotation": {"0.000": [0, 0, 0], "0.500": [0, 180, 0], "1.000": [0, 360, 0]}},
    }
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.analyzer.idle": {"loop": True, "animation_length": idle_len, "bones": idle},
            "animation.analyzer.scan": {"loop": True, "animation_length": scan_len, "bones": scan},
        },
    }


# ---------------------------------------------------------------- posições (display)
#
# O GeckoLib desenha o modelo com a origem (0, 0, 0) do Bedrock no ponto de apoio de cada contexto (o centro do
# "bloco" do item, mais 0,01 em Y) e espelha X. Na pose de cada contexto:
#   - primeira pessoa: X para a direita, Y para cima, Z para a câmera;
#   - terceira pessoa, braço caído: Y do item aponta para a frente do jogador e Z para cima;
#   - moldura: a frente do item (+Z) só fica para fora depois de meia-volta em Y, como no item/generated vanilla.
# O jogo espelha sozinho a mão esquerda (nega a rotação Y e Z e a translação X) quando falta a entrada dela.


def rot_matrix(rx, ry, rz):
    """Matriz de rotation [x, y, z] do JSON: R = Rx * Ry * Rz (o rotationXYZ do JOML)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    Rx = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    Ry = [[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]]
    Rz = [[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]]
    return matmul(Rx, matmul(Ry, Rz))


def matmul(A, B):
    return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(M, v):
    return [sum(M[i][k] * v[k] for k in range(3)) for i in range(3)]


def java_corners(model):
    """Os cantos de todos os cubos, já em coordenadas Java (X espelhado), em pixels."""
    pts = []
    for bone in model.bones:
        for c in bone.get("cubes", []):
            (ox, oy, oz), (sx, sy, sz) = c["origin"], c["size"]
            for x in (ox, ox + sx):
                for y in (oy, oy + sy):
                    for z in (oz, oz + sz):
                        pts.append([-x, y + 0.16, z])
    return pts


def centered(model, rotation, scale):
    """translation que põe o centro da caixa do modelo girado no ponto de apoio."""
    R = rot_matrix(*rotation)
    pts = [apply(R, [v * scale for v in p]) for p in java_corners(model)]
    return [-(min(p[i] for p in pts) + max(p[i] for p in pts)) / 2 for i in range(3)]


def tidy(values, step):
    out = []
    for v in values:
        v = round(round(v / step) * step, 4)
        out.append(0.0 if v == 0 else v)
    return out


# Primeira pessoa: em pé, a tela virada para o jogador, inclinada para trás (topo longe da câmera) e um pouco para
# dentro, embaixo à direita da vista.
FP_ROT = [-28, -22, -10]
FP_SCALE = 0.55
FP_TRANSLATION = [-2.0, 0.0, -1.0]
# Terceira pessoa: em pé no punho, tela para a frente do jogador; a empunhadura fica dentro da mão.
TP_ROT = [90, 180, 0]
TP_SCALE = 0.45
TP_TRANSLATION = [0, 1.5, -2.0]


def display(model):
    d = {
        "thirdperson_righthand": {"rotation": TP_ROT, "translation": TP_TRANSLATION, "scale": [TP_SCALE] * 3},
        "firstperson_righthand": {"rotation": FP_ROT, "translation": FP_TRANSLATION, "scale": [FP_SCALE] * 3},
    }
    for key, rotation, scale in (("ground", [0, 0, 0], 0.4), ("fixed", [0, 180, 0], 0.85),
                                 ("head", [0, 180, 0], 0.8)):
        d[key] = {"rotation": rotation, "translation": centered(model, rotation, scale), "scale": [scale] * 3}
    # No chão o ItemEntityRenderer ergue o ponto de apoio 0,25 bloco (a escala 1 que o modelo de transformações
    # separadas informa) mais o balanço de 0 a 0,2; desce o aparelho centrado até a base tocar o chão no ponto baixo.
    d["ground"]["translation"][1] -= 0.7
    # Na cabeça (só por comando), por cima do capacete.
    d["head"]["translation"][1] += 14
    for entry in d.values():
        entry["rotation"] = tidy(entry["rotation"], 0.5)
        entry["translation"] = tidy(entry["translation"], 0.05)
        entry["scale"] = tidy(entry["scale"], 0.01)
    return d


def item_model(model):
    """3D em todo contexto; o ícone plano na GUI. `gui_light: front` dá ao ícone a luz chapada dos itens comuns."""
    return {
        "loader": "forge:separate_transforms",
        "gui_light": "front",
        "textures": {"particle": "iceagesurvival:item/analyzer"},
        "base": {"parent": "builtin/entity", "display": display(model)},
        "perspectives": {
            "gui": {
                "loader": "forge:item_layers",
                "textures": {"layer0": "iceagesurvival:item/analyzer", "particle": "iceagesurvival:item/analyzer"},
            },
        },
    }


# ---------------------------------------------------------------- prévia (só para conferir; não vai para o jar)

def preview(model, texture, out_dir):
    """Desenha o modelo em repouso com a textura (pintor, um polígono por texel) em duas vistas."""
    out_dir.mkdir(parents=True, exist_ok=True)
    tex = texture.convert("RGBA")

    def faces_java():
        """(4 cantos Java em px, normal, região uv, cor-base) de cada face visível de cada cubo."""
        out = []
        for bone in model.bones:
            if bone["name"] in ("pulse", "blip"):
                continue  # escondidos no idle
            for c in bone.get("cubes", []):
                (ox, oy, oz), (sx, sy, sz) = c["origin"], c["size"]
                x0, x1, y0, y1, z0, z1 = ox, ox + sx, oy, oy + sy, oz, oz + sz
                # Cantos na ordem (topo-esq, topo-dir, baixo-dir, baixo-esq) olhando a face de fora (Bedrock).
                quads = {
                    "south": ([x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]),
                    "north": ([x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]),
                    "east": ([x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]),
                    "west": ([x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]),
                    "up": ([x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1]),
                    "down": ([x0, y0, z1], [x1, y0, z1], [x1, y0, z0], [x0, y0, z0]),
                }
                for face, corners in quads.items():
                    uv = c["uv"][face]
                    java = [[-p[0], p[1], p[2]] for p in corners]
                    out.append((java, uv["uv"], uv["uv_size"]))
        return out

    def render(transform, project, size, name, background=(40, 46, 56)):
        img = Image.new("RGB", size, background)
        draw = ImageDraw.Draw(img)
        polys = []
        light = [-0.4, 0.8, 0.45]
        for corners, (u0, v0), (w, h) in faces_java():
            pts = [transform(p) for p in corners]
            e1 = [pts[1][i] - pts[0][i] for i in range(3)]
            e2 = [pts[3][i] - pts[0][i] for i in range(3)]
            n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]]
            ln = math.sqrt(sum(v * v for v in n)) or 1
            n = [v / ln for v in n]
            # Os cantos vêm em sentido horário vistos de fora no Bedrock; espelhado o X, o produto vetorial aponta
            # para fora.
            center = [sum(p[i] for p in pts) / 4 for i in range(3)]
            to_cam = project(None, center)
            if sum(n[i] * to_cam[i] for i in range(3)) <= 0:
                continue
            bright = 0.55 + 0.45 * max(0.0, sum(n[i] * light[i] for i in range(3)))
            steps_u, steps_v = max(1, int(w)), max(1, int(h))
            for j in range(steps_v):
                for i in range(steps_u):
                    color = tex.getpixel((min(TEX - 1, int(u0) + i), min(TEX - 1, int(v0) + j)))
                    if color[3] == 0:
                        continue
                    a0, a1, b0, b1 = i / steps_u, (i + 1) / steps_u, j / steps_v, (j + 1) / steps_v
                    quad = []
                    for a, b in ((a0, b0), (a1, b0), (a1, b1), (a0, b1)):
                        top = [pts[0][k] + (pts[1][k] - pts[0][k]) * a for k in range(3)]
                        bottom = [pts[3][k] + (pts[2][k] - pts[3][k]) * a for k in range(3)]
                        quad.append([top[k] + (bottom[k] - top[k]) * b for k in range(3)])
                    depth = sum(q[2] for q in quad) / 4
                    polys.append((depth, [project(q, None) for q in quad],
                                  tuple(int(c * bright) for c in color[:3])))
        polys.sort(key=lambda p: p[0])
        for _, poly, color in polys:
            draw.polygon(poly, fill=color)
        img.save(out_dir / name)

    # Vista 3/4 do modelo solto, em pixels de modelo, ampliada.
    R = rot_matrix(-20, 35, 0)

    def iso_t(p):
        return apply(R, [p[0], p[1] - 8, p[2]])

    def iso_p(q, center):
        if center is not None:
            return [0, 0, 1]
        return (256 + q[0] * 26, 256 - q[1] * 26)

    render(iso_t, iso_p, (512, 512), "analyzer_3q.png")

    # Primeira pessoa, mão direita: a cadeia do ItemInHandRenderer + display + GeckoLib, câmera na origem olhando -Z.
    Rf = rot_matrix(*FP_ROT)

    def fp_t(p):
        local = apply(Rf, [p[0] / 16 * FP_SCALE, (p[1] / 16 + 0.01) * FP_SCALE, p[2] / 16 * FP_SCALE])
        return [0.56 + FP_TRANSLATION[0] / 16 + local[0], -0.52 + FP_TRANSLATION[1] / 16 + local[1],
                -0.72 + FP_TRANSLATION[2] / 16 + local[2]]

    W, H, fov = 854, 480, math.radians(70)
    f = (H / 2) / math.tan(fov / 2)

    def fp_p(q, center):
        if center is not None:
            ln = math.sqrt(sum(v * v for v in center))
            return [-v / ln for v in center]
        return (W / 2 + q[0] / -q[2] * f, H / 2 - q[1] / -q[2] * f)

    render(fp_t, fp_p, (W, H), "analyzer_firstperson.png", background=(120, 150, 190))
    print(f"prévias em {out_dir}")


# ---------------------------------------------------------------- main

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    model = build_model()
    texture, glow = model_textures()
    if len(sys.argv) >= 3 and sys.argv[1] == "--preview":
        preview(model, texture, Path(sys.argv[2]))
        return
    TEXTURES.mkdir(parents=True, exist_ok=True)
    icon().save(TEXTURES / "analyzer.png")
    texture.save(TEXTURES / "analyzer_3d.png")
    glow.save(TEXTURES / "analyzer_3d_glowmask.png")
    write_json(GEO / "analyzer.geo.json", geometry(model))
    write_json(ANIMATIONS / "analyzer.animation.json", animations())
    write_json(MODELS / "analyzer.json", item_model(model))
    print("escritos o ícone, o modelo 3D (geo, textura, glowmask, animações) e models/item/analyzer.json")


if __name__ == "__main__":
    main()
