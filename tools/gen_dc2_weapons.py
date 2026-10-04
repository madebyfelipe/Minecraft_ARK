#!/usr/bin/env python3
"""Gera a arte das armas de fogo no estilo do Dino Crisis 2 (D58): modelo 3D GeckoLib (geometria, textura, glowmask
e animações de tiro e recarga), o modelo de item com as transformações de exibição e os ícones das munições.

Armas (o desenho é autoral, inspirado no ar militar-futurista de 2009 do DC2; nada copiado do jogo):

- `handgun`: pistola grande de dois tons, ferrolho de aço serrilhado, armação de polímero, compensador com saídas
  de gás, miras de trítio verdes e a lanterna/laser sob o trilho.
- `shotgun`, `submachine_gun`, `heavy_machine_gun`, `solid_cannon`, `anti_tank_rifle`: ver cada `build_*`.

Escreve, por arma, `geo/item/<arma>.geo.json`, `textures/item/<arma>_3d.png` (+ `_glowmask`),
`animations/item/<arma>.animation.json` (`idle`, `fire`, `reload`) e `models/item/<arma>.json`; e os ícones 16x16 das
munições em `textures/item/` com os modelos `item/generated`.

Prévia (não vai para o jar): `python3 tools/gen_dc2_weapons.py --preview <pasta> [arma...]`.
Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import geckoitem as gi  # noqa: E402
from geckoitem import Material, Model, Animation  # noqa: E402

# ---------------------------------------------------------------- paleta

STEEL = (150, 156, 164)
STEEL_DARK = (96, 102, 110)
GUNMETAL = (54, 60, 68)
GUNMETAL_DARK = (36, 40, 46)
POLYMER = (34, 36, 40)
POLYMER_LIGHT = (52, 55, 60)
OLIVE = (84, 90, 58)
OLIVE_DARK = (60, 65, 42)
TAN = (156, 134, 96)
RUBBER = (28, 28, 30)
BORE = (12, 12, 14)
BRASS = (196, 150, 64)
COPPER = (176, 102, 60)
RED_SHELL = (164, 40, 32)
WHITE_ARMOR = (204, 208, 210)
ORANGE = (226, 124, 34)
YELLOW = (226, 186, 40)
TRITIUM = (120, 255, 150)
LASER = (255, 70, 60)
CYAN = (86, 236, 230)
FLASH = (255, 226, 150)


def mat(base, noise=4, bevel=16, **kw):
    return Material(base, noise=noise, bevel=bevel, **kw)


M = {
    "steel": mat(STEEL, 5, 20),
    "steel_serrated": mat(STEEL, 5, 20, pattern=gi.pattern_serrations(2, -40)),
    "steel_dark": mat(STEEL_DARK, 5, 18),
    "gunmetal": mat(GUNMETAL),
    "gunmetal_ribs": mat(GUNMETAL, pattern=gi.pattern_ribs(2, True, -22)),
    "gunmetal_vribs": mat(GUNMETAL, pattern=gi.pattern_ribs(2, False, -22)),
    "gunmetal_holes": mat(GUNMETAL, 3, 12, pattern=gi.pattern_holes(4, 2)),
    "gunmetal_dark": mat(GUNMETAL_DARK, 3, 12),
    "polymer": mat(POLYMER, 3, 14),
    "polymer_light": mat(POLYMER_LIGHT, 3, 14),
    "polymer_ribs": mat(POLYMER, 3, 14, pattern=gi.pattern_ribs(2, False, 18)),
    "grip": mat(RUBBER, 3, 12, pattern=gi.pattern_stipple(0.7)),
    "rubber": mat(RUBBER, 2, 8),
    "rail": mat(GUNMETAL_DARK, 3, 10, pattern=gi.pattern_ribs(2, False, 26)),
    "olive": mat(OLIVE, 5, 16, speckle=0.04),
    "olive_dark": mat(OLIVE_DARK, 4, 14),
    "olive_ribs": mat(OLIVE, 5, 16, pattern=gi.pattern_ribs(3, True, -20)),
    "tan": mat(TAN, 5, 16, speckle=0.05),
    "bore": mat(BORE, 1, 0),
    "brass": mat(BRASS, 6, 22),
    "copper": mat(COPPER, 6, 20),
    "red_shell": mat(RED_SHELL, 5, 18, pattern=gi.pattern_ribs(2, False, -16)),
    "armor": mat(WHITE_ARMOR, 4, 16, speckle=0.03),
    "armor_vents": mat(WHITE_ARMOR, 4, 16, pattern=gi.pattern_holes(3, 1, (40, 44, 48))),
    "orange": mat(ORANGE, 4, 16),
    "hazard": mat(YELLOW, 3, 10, pattern=gi.pattern_hazard(3)),
    "tritium": Material(TRITIUM, noise=0, bevel=0, glow=True),
    "laser": Material(LASER, noise=0, bevel=0, glow=True, pattern=gi.pattern_glow_core()),
    "cyan": Material(CYAN, noise=6, bevel=0, glow=True, pattern=gi.pattern_glow_core()),
    "cyan_dim": Material(gi.shade(CYAN, -60), noise=6, bevel=10, glow=True),
    "lens": mat((30, 60, 80), 2, 0, pattern=gi.pattern_lens()),
    "lens_glow": Material((90, 200, 255), noise=4, bevel=0, glow=True, pattern=gi.pattern_lens()),
    "flash": Material(FLASH, noise=10, bevel=0, glow=True, pattern=gi.pattern_glow_core()),
}

# ---------------------------------------------------------------- estêncil

FONT = {
    "P": ["111", "101", "111", "100", "100"], "L": ["100", "100", "100", "100", "111"],
    "-": ["000", "000", "111", "000", "000"], "9": ["111", "101", "111", "001", "111"],
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "111", "001", "111"],
    "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "111", "001", "111"],
    "6": ["111", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"],
    "8": ["111", "101", "111", "101", "111"], "A": ["010", "101", "111", "101", "101"],
    "T": ["111", "010", "010", "010", "010"], "S": ["111", "100", "111", "001", "111"],
    "M": ["101", "111", "111", "101", "101"], "G": ["111", "100", "101", "101", "111"],
    "C": ["111", "100", "100", "100", "111"], "H": ["101", "101", "111", "101", "101"],
    "X": ["101", "101", "010", "101", "101"], " ": ["000", "000", "000", "000", "000"],
    ".": ["000", "000", "000", "000", "010"], "V": ["101", "101", "101", "101", "010"],
}


def stencil(text, color, at=(0.5, 0.5)):
    """Decalque de texto 3x5 centrado em `at` (fração da região)."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        width = len(text) * 4 - 1
        sx = x0 + int(round(w * at[0] - width / 2))
        sy = y0 + int(round(h * at[1] - 2.5))
        for k, ch in enumerate(text):
            for j, row in enumerate(FONT[ch]):
                for i, bit in enumerate(row):
                    x, y = sx + k * 4 + i, sy + j
                    if bit == "1" and x0 <= x < x0 + w and y0 <= y < y0 + h:
                        img.putpixel((x, y), color + (255,))
    return draw


def dark_rect(fx0, fy0, fx1, fy1, color=(14, 14, 16), rim=10):
    """Recorte (janela de ejeção, fenda, furo), em frações da região."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        a0, a1 = x0 + int(round(w * fx0)), x0 + max(int(round(w * fx0)) + 1, int(round(w * fx1)))
        b0, b1 = y0 + int(round(h * fy0)), y0 + max(int(round(h * fy0)) + 1, int(round(h * fy1)))
        for y in range(b0, min(b1, y0 + h)):
            for x in range(a0, min(a1, x0 + w)):
                edge = y == b0 or x == a0
                img.putpixel((x, y), gi.shade(color, rim if edge else 0) + (255,))
    return draw


def slots(count, fy0=0.25, fy1=0.75, margin=0.12, color=(14, 14, 16)):
    """Fendas verticais repetidas (saídas de gás do freio de boca, ventilação)."""
    def draw(img, rect, face, rng, density):
        step = (1 - 2 * margin) / count
        for k in range(count):
            a = margin + k * step + step * 0.25
            dark_rect(a, fy0, a + step * 0.5, fy1, color)(img, rect, face, rng, density)
    return draw


def band(fy0, fy1, color, horizontal=True):
    """Faixa de cor (anel do cano, ponta pintada)."""
    def draw(img, rect, face, rng, density):
        x0, y0, w, h = rect
        for j in range(h):
            for i in range(w):
                f = (j + 0.5) / h if horizontal else (i + 0.5) / w
                if fy0 <= f < fy1:
                    img.putpixel((x0 + i, y0 + j), gi.shade(color, rng.randint(-4, 4)) + (255,))
    return draw


def combine(*decals):
    def draw(img, rect, face, rng, density):
        for d in decals:
            d(img, rect, face, rng, density)
    return draw


def muzzle_flash(m, parent, x, y, z, size):
    """O clarão: três lâminas acesas cruzadas à frente da boca (−Z), escondidas fora do tiro."""
    flash = m.bone("flash", parent, pivot=(x, y, z))
    m.box(flash, (x - size * 0.6, y - 0.05, z - size * 1.6), (x + size * 0.6, y + 0.05, z), M["flash"])
    m.box(flash, (x - 0.05, y - size * 0.6, z - size * 1.6), (x + 0.05, y + size * 0.6, z), M["flash"])
    m.box(flash, (x - size * 0.3, y - size * 0.3, z - size * 0.5), (x + size * 0.3, y + size * 0.3, z), M["flash"])
    return flash


def idle_hiding_flash(extra=None):
    idle = Animation(2.0, loop=True)
    idle.bone("flash").size(0.0, gi.HIDDEN).size(2.0, gi.HIDDEN)
    if extra:
        extra(idle)
    return idle


def flash_on(anim, duration=0.05):
    anim.bone("flash").size(0.0, gi.SHOWN).size(duration, gi.SHOWN).size(duration + 0.01, gi.HIDDEN)


def kick(anim, bone, pitch, back, t_up, t_back):
    """Coice do corpo da arma: sobe o cano e recua, e volta suave."""
    anim.bone(bone).tween("turn", 0.0, t_up, (0, 0, 0), (pitch, 0, 0), gi.ease_out, 2) \
        .tween("turn", t_up, t_back, (pitch, 0, 0), (0, 0, 0), gi.ease_in_out, 5) \
        .tween("move", 0.0, t_up, (0, 0, 0), (0, 0, back), gi.ease_out, 2) \
        .tween("move", t_up, t_back, (0, 0, back), (0, 0, 0), gi.ease_in_out, 5)


def tilt_for_reload(anim, bone, length, roll=-14, pitch=6):
    """A arma inteira inclina de lado enquanto troca o pente e volta no fim."""
    anim.bone(bone).tween("turn", 0.0, 0.25, (0, 0, 0), (pitch, 0, roll), gi.ease_in_out, 4) \
        .turn(length - 0.35, (pitch, 0, roll)) \
        .tween("turn", length - 0.35, length - 0.05, (pitch, 0, roll), (0, 0, 0), gi.ease_in_out, 4)


def swap_part(anim, bone, out, t_out0, t_out1, t_in0, t_in1):
    """A peça sai até `out` (posição Java, em px), some, reaparece lá e volta ao lugar."""
    anim.bone(bone).move(0.0, (0, 0, 0)).move(t_out0, (0, 0, 0)) \
        .tween("move", t_out0, t_out1, (0, 0, 0), out, gi.ease_in_out, 5) \
        .size(0.0, gi.SHOWN).size(t_out1, gi.SHOWN).size(t_out1 + 0.01, gi.HIDDEN) \
        .size(t_in0 - 0.01, gi.HIDDEN).size(t_in0, gi.SHOWN) \
        .tween("move", t_in0, t_in1, out, (0, 0, 0), gi.ease_out, 5)


# ---------------------------------------------------------------- pistola

def build_handgun():
    """Pistola da Regina: grande, dois tons. O cano aponta para −Z; ~12 px da boca do compensador ao cão."""
    m = Model("handgun", density=6, seed=11)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 3, 1))
    slide = m.bone("slide", "body", pivot=(0, 4.6, 0))
    # Ferrolho de aço: serrilhado atrás e na frente, janela de ejeção (com o estojo de latão) no lado direito,
    # "PL-9" no esquerdo; no alto, os três rebaixos de alívio.
    m.box(slide, (-0.9, 3.6, -6.4), (0.9, 5.6, 3.2), M["steel"],
          decals={"east": combine(dark_rect(0.46, 0.08, 0.66, 0.55), dark_rect(0.52, 0.2, 0.6, 0.45, BRASS, 30),
                                  stencil("PL-9", (58, 62, 68), (0.44, 0.75))),
                  "west": stencil("PL-9", (58, 62, 68), (0.56, 0.75))})
    for z0, z1 in ((1.3, 3.0), (-6.1, -4.9)):
        m.box(slide, (-0.93, 3.75, z0), (0.93, 5.45, z1), M["steel_serrated"], hide=("north", "south", "up", "down"))
    m.box(slide, (-0.62, 5.6, -6.2), (0.62, 5.85, 3.0), M["steel_dark"],
          decals={"up": combine(dark_rect(0.25, 0.12, 0.75, 0.24), dark_rect(0.25, 0.3, 0.75, 0.42),
                                dark_rect(0.25, 0.48, 0.75, 0.6))})
    # Miras de trítio: a da frente e as duas de trás, com o ponto verde virado para o atirador.
    m.box(slide, (-0.25, 5.85, -6.0), (0.25, 6.45, -5.4), M["gunmetal_dark"], faces={"south": M["tritium"]})
    m.box(slide, (-0.62, 5.85, 2.3), (0.62, 6.1, 2.95), M["gunmetal_dark"])
    for x in (-0.6, 0.2):
        m.box(slide, (x, 6.1, 2.3), (x + 0.4, 6.45, 2.95), M["gunmetal_dark"], faces={"south": M["tritium"]})
    m.box(slide, (-0.9, 5.4, 3.2), (0.9, 5.6, 3.45), M["steel_dark"])
    # Cano e compensador, com as saídas de gás em cima e dos lados e a boca.
    m.octagon(body, "z", (0, 4.6), -6.9, -6.3, 1.0, M["steel_dark"])
    m.box(body, (-0.95, 3.7, -8.6), (0.95, 5.5, -6.7), M["gunmetal"],
          faces={"up": mat(GUNMETAL, 3, 12, pattern=gi.pattern_holes(3, 1))},
          decals={"north": dark_rect(0.33, 0.3, 0.67, 0.7, BORE, 0), "east": slots(2, 0.2, 0.55),
                  "west": slots(2, 0.2, 0.55)})
    m.box(body, (-0.97, 3.65, -7.1), (0.97, 5.55, -6.8), M["steel"])
    # Armação de polímero com o trilho, a lanterna/laser e o retém.
    m.box(body, (-0.85, 2.6, -6.2), (0.85, 3.6, 2.9), M["polymer"],
          decals={"west": stencil("PL", (88, 92, 98), (0.2, 0.5)), "east": stencil("PL", (88, 92, 98), (0.8, 0.5))})
    m.box(body, (-0.62, 2.25, -6.1), (0.62, 2.6, -2.7), M["rail"])
    m.box(body, (-0.72, 1.5, -6.05), (0.72, 2.25, -3.3), M["polymer_light"], faces={"north": M["laser"]})
    m.box(body, (-0.74, 1.8, -4.1), (0.74, 1.95, -3.55), M["gunmetal_dark"])
    m.box(body, (0.85, 3.25, -1.6), (1.02, 3.7, 0.5), M["gunmetal_dark"])
    m.box(body, (0.85, 2.7, 0.8), (1.0, 3.15, 1.3), mat(ORANGE, 2, 10))
    # Guarda-mato e gatilho.
    m.box(body, (-0.32, 1.05, -2.95), (0.32, 2.6, -2.5), M["polymer"])
    m.box(body, (-0.32, 0.75, -2.95), (0.32, 1.15, 0.3), M["polymer"])
    m.box(body, (-0.18, 1.35, -1.3), (0.18, 2.6, -0.9), M["steel"], rotation=(-12, 0, 0), pivot=(0, 2.6, -1.1))
    # Cão esqueletizado e a cauda de castor.
    m.box(body, (-0.32, 4.25, 3.2), (0.32, 5.15, 3.8), M["steel_dark"], decals={"east": dark_rect(0.3, 0.3, 0.7, 0.6),
                                                                              "west": dark_rect(0.3, 0.3, 0.7, 0.6)})
    m.box(body, (-0.86, 2.6, 2.8), (0.86, 3.45, 4.0), M["polymer"])
    # Empunhadura inclinada, com textura, apoio dos dedos na frente e a boca do pente alargada embaixo.
    grip = m.bone("grip", "body", pivot=(0, 2.6, 1.4), rotation=(-14, 0, 0))
    m.box(grip, (-1.0, -2.7, -0.2), (1.0, 2.7, 3.0), M["grip"], faces={"up": M["polymer"]})
    m.box(grip, (-1.04, -1.7, 0.5), (1.04, 1.9, 2.6), M["polymer_light"], hide=("north", "south", "up", "down"),
          decals={"east": gi.pattern_stipple(0.9), "west": gi.pattern_stipple(0.9)})
    for y in (-1.6, -0.2, 1.2):
        m.box(grip, (-0.9, y, -0.45), (0.9, y + 0.8, -0.15), M["polymer"])
    m.box(grip, (-1.12, -2.9, -0.4), (1.12, -2.3, 3.2), M["polymer"])
    # O pente (osso próprio: cai pelo pé da empunhadura na recarga), com a base de aço e a ponta de latão.
    mag = m.bone("magazine", "grip", pivot=(0, 0, 1.4))
    m.box(mag, (-0.7, -2.6, 0.3), (0.7, 2.4, 2.5), M["gunmetal"], faces={"up": M["brass"]})
    m.box(mag, (-1.08, -3.4, -0.35), (1.08, -2.9, 3.05), M["steel"], decals={"down": stencil("9", (40, 40, 44))})
    muzzle_flash(m, "body", 0, 4.6, -8.6, 1.4)
    return m


def handgun_animations():
    fire = Animation(0.25)
    flash_on(fire)
    fire.bone("slide").tween("move", 0.0, 0.04, (0, 0, 0), (0, 0, 2.2), gi.ease_out, 2) \
        .tween("move", 0.04, 0.16, (0, 0, 2.2), (0, 0, 0), gi.ease_in_out, 4)
    kick(fire, "body", 8, 0.6, 0.04, 0.22)

    length = 1.5
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    # O pente cai ao longo da empunhadura (inclinada 14° para trás), some e o novo sobe.
    swap_part(reload, "magazine", (0, -6.5, 1.6), 0.15, 0.45, 0.65, 0.95)
    # O ferrolho, travado atrás com o pente vazio, solta no fim.
    reload.bone("slide").move(0.0, (0, 0, 1.8)).move(1.05, (0, 0, 1.8)) \
        .tween("move", 1.05, 1.15, (0, 0, 1.8), (0, 0, 0), gi.ease_out, 3)
    tilt_for_reload(reload, "body", length)
    return {"idle": idle_hiding_flash(), "fire": fire, "reload": reload}


# ---------------------------------------------------------------- escopeta

def build_shotgun():
    """Escopeta do Dylan: semiautomática de guarda-mão de bomba, escudo térmico furado, coronha esqueletizada e o
    porta-cartuchos vermelho no lado esquerdo. ~34 px da boca à soleira."""
    m = Model("shotgun", density=5, seed=23)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 4))
    # Caixa da culatra, com a janela de ejeção à direita e o porta-cartuchos à esquerda.
    m.box(body, (-1.1, 3.4, -2.2), (1.1, 6.3, 6.2), M["gunmetal"],
          decals={"east": combine(dark_rect(0.3, 0.12, 0.62, 0.5), stencil("PL-12", (110, 116, 124), (0.5, 0.78))),
                  "west": stencil("PL-12", (110, 116, 124), (0.72, 0.78))})
    m.box(body, (-1.15, 3.3, 1.0), (1.15, 3.6, 6.2), M["gunmetal_dark"])
    m.box(body, (-0.6, 6.3, -1.8), (0.6, 6.7, 5.6), M["rail"])
    m.box(body, (-0.7, 6.7, 4.6), (0.7, 7.5, 5.4), M["gunmetal_dark"], decals={"south": dark_rect(0.35, 0.2, 0.65, 0.6)})
    for k in range(4):
        z = -0.8 + k * 1.15
        m.box(body, (-1.75, 3.9, z), (-1.1, 5.0, z + 0.9), M["red_shell"], faces={"west": M["red_shell"]})
        m.box(body, (-1.8, 3.8, z + 0.65), (-1.1, 5.1, z + 0.95), M["brass"])
    m.box(body, (-1.25, 3.7, -1.0), (-1.1, 5.2, 3.8), M["gunmetal_dark"])
    # Cano, escudo térmico furado, tubo do carregador, abraçadeira e o estrangulador da boca.
    m.octagon(body, "z", (0, 5.35), -22.0, -2.2, 1.2, M["steel_dark"])
    m.box(body, (-0.85, 5.0, -15.5), (0.85, 6.4, -2.3), M["gunmetal_holes"], hide=("south",))
    m.octagon(body, "z", (0, 3.85), -17.2, -2.2, 1.1, M["gunmetal"])
    m.box(body, (-0.75, 3.1, -17.6), (0.75, 6.1, -16.6), M["gunmetal_dark"])
    m.octagon(body, "z", (0, 5.35), -23.2, -21.6, 1.55, M["gunmetal"], cap=M["gunmetal"])
    m.box(body, (-0.4, 5.3, -23.21), (0.4, 5.4, -23.2), M["bore"])
    m.box(body, (-0.2, 6.0, -21.4), (0.2, 6.7, -20.8), M["gunmetal_dark"], faces={"south": M["tritium"]})
    # Guarda-mão de bomba (osso próprio: recua no tiro e na recarga).
    pump = m.bone("pump", "body", pivot=(0, 3.9, -9))
    m.box(pump, (-1.25, 2.85, -13.4), (1.25, 4.9, -6.6), M["olive_dark"], faces={"down": M["olive_dark"]})
    m.box(pump, (-1.3, 3.3, -13.0), (1.3, 4.5, -7.0), M["olive"], hide=("north", "south", "up", "down"),
          decals={"east": gi.pattern_ribs(2, False, -24), "west": gi.pattern_ribs(2, False, -24)})
    # Guarda-mato, gatilho e a empunhadura de pistola.
    m.box(body, (-0.32, 1.6, 1.4), (0.32, 3.4, 1.85), M["gunmetal"])
    m.box(body, (-0.32, 1.3, 1.4), (0.32, 1.7, 4.6), M["gunmetal"])
    m.box(body, (-0.18, 2.0, 2.4), (0.18, 3.4, 2.8), M["steel"], rotation=(-10, 0, 0), pivot=(0, 3.4, 2.6))
    grip = m.bone("grip", "body", pivot=(0, 3.4, 5.0), rotation=(-18, 0, 0))
    m.box(grip, (-0.95, -1.8, 4.0), (0.95, 3.4, 6.4), M["grip"], faces={"up": M["polymer"]})
    m.box(grip, (-1.05, -2.1, 3.8), (1.05, -1.6, 6.6), M["polymer"])
    # Coronha esqueletizada: barra de cima, braço de baixo, soleira de borracha e o gancho.
    m.box(body, (-0.5, 4.5, 6.2), (0.5, 5.6, 15.0), M["gunmetal_dark"])
    m.box(body, (-0.45, 0.9, 7.4), (0.45, 1.7, 15.0), M["gunmetal_dark"])
    m.box(body, (-0.4, 1.7, 7.4), (0.4, 4.5, 8.2), M["gunmetal_dark"], rotation=(-20, 0, 0), pivot=(0, 1.7, 7.8))
    m.box(body, (-1.2, 0.4, 15.0), (1.2, 6.0, 16.2), M["rubber"], faces={"south": mat(RUBBER, 2, 6,
          pattern=gi.pattern_ribs(2, True, 14))})
    m.box(body, (-0.4, 2.0, 14.2), (0.4, 4.5, 15.0), M["gunmetal_dark"])
    # O cartucho da recarga (osso próprio: entra pelo fundo da culatra, escondido fora dela).
    shell = m.bone("shell", "body", pivot=(0, 3.0, 1.0))
    m.box(shell, (-0.45, 2.6, -1.0), (0.45, 3.5, 1.4), M["red_shell"], faces={"south": M["brass"]})
    muzzle_flash(m, "body", 0, 5.35, -23.2, 2.4)
    return m


def shotgun_animations():
    fire = Animation(0.6)
    flash_on(fire, 0.06)
    kick(fire, "body", 14, 1.2, 0.05, 0.35)
    # A bomba recua e volta, para o próximo cartucho.
    fire.bone("pump").move(0.0, (0, 0, 0)).move(0.18, (0, 0, 0)) \
        .tween("move", 0.18, 0.32, (0, 0, 0), (0, 0, 3.0), gi.ease_out, 3) \
        .tween("move", 0.32, 0.48, (0, 0, 3.0), (0, 0, 0), gi.ease_in_out, 3)
    fire.bone("shell").size(0.0, gi.HIDDEN)

    length = 2.5
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    tilt_for_reload(reload, "body", length, roll=-28, pitch=4)
    # Quatro cartuchos entram pelo fundo, um atrás do outro; depois a bomba recua e volta.
    shell = reload.bone("shell")
    shell.size(0.0, gi.HIDDEN)
    for k in range(4):
        t0 = 0.3 + k * 0.38
        shell.move(t0, (0, -3.0, 1.5)).size(t0 - 0.01, gi.HIDDEN).size(t0, gi.SHOWN) \
            .tween("move", t0, t0 + 0.22, (0, -3.0, 1.5), (0, 0, -1.6), gi.ease_in_out, 3) \
            .size(t0 + 0.22, gi.SHOWN).size(t0 + 0.23, gi.HIDDEN)
    reload.bone("pump").move(0.0, (0, 0, 0)).move(1.85, (0, 0, 0)) \
        .tween("move", 1.85, 2.0, (0, 0, 0), (0, 0, 3.0), gi.ease_out, 3) \
        .tween("move", 2.0, 2.15, (0, 0, 3.0), (0, 0, 0), gi.ease_in_out, 3)
    idle = idle_hiding_flash(lambda a: a.bone("shell").size(0.0, gi.HIDDEN).size(2.0, gi.HIDDEN))
    return {"idle": idle, "fire": fire, "reload": reload}


# ---------------------------------------------------------------- submetralhadora

def build_submachine_gun():
    """Submetralhadora da Regina: compacta, polímero preto com painéis verde-oliva, mira de ponto vermelho, punho
    vertical, pente comprido à frente do gatilho e a coronha recolhida. ~21 px."""
    m = Model("submachine_gun", density=6, seed=37)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 2))
    # Corpo: parte de cima de polímero, de baixo com os painéis oliva; janela de ejeção à direita.
    m.box(body, (-1.0, 4.2, -6.2), (1.0, 6.4, 5.2), M["polymer"],
          decals={"east": combine(dark_rect(0.38, 0.15, 0.6, 0.5), stencil("PL-5", (86, 92, 70), (0.75, 0.72))),
                  "west": stencil("PL-5", (86, 92, 70), (0.25, 0.72))})
    m.box(body, (-1.02, 3.2, -5.6), (1.02, 4.2, 4.0), M["olive"], decals={
        "east": dark_rect(0.1, 0.35, 0.3, 0.65), "west": dark_rect(0.7, 0.35, 0.9, 0.65)})
    m.box(body, (-0.62, 6.4, -5.8), (0.62, 6.8, 4.6), M["rail"])
    # Cano com a capa furada e o quebra-chamas.
    m.box(body, (-0.82, 4.4, -10.4), (0.82, 6.1, -6.2), M["gunmetal_holes"], hide=("south",))
    m.octagon(body, "z", (0, 5.25), -12.4, -10.4, 0.95, M["gunmetal_dark"], cap=M["gunmetal_dark"])
    m.box(body, (-0.25, 5.15, -12.41), (0.25, 5.35, -12.4), M["bore"])
    m.octagon(body, "z", (0, 5.25), -11.2, -10.6, 1.25, M["gunmetal"])
    # Mira de ponto vermelho: corpo, as duas janelas e o ponto aceso virado para o atirador.
    m.box(body, (-0.75, 6.8, -1.6), (0.75, 7.0, 1.6), M["gunmetal_dark"])
    m.box(body, (-0.75, 7.0, -1.4), (-0.55, 8.4, 1.4), M["gunmetal"])
    m.box(body, (0.55, 7.0, -1.4), (0.75, 8.4, 1.4), M["gunmetal"])
    m.box(body, (-0.75, 8.4, -1.4), (0.75, 8.6, 1.4), M["gunmetal"])
    m.box(body, (-0.55, 7.0, -0.1), (0.55, 8.4, 0.0), mat((60, 20, 20), 2, 0), faces={"south": M["laser"]})
    m.box(body, (-0.12, 7.6, 0.0), (0.12, 7.84, 0.02), M["laser"])
    # Punho vertical, gatilho, empunhadura e o seletor.
    m.box(body, (-0.55, 0.8, -5.6), (0.55, 3.2, -4.3), M["grip"], rotation=(6, 0, 0), pivot=(0, 3.2, -5.0))
    m.box(body, (-0.3, 1.9, -0.2), (0.3, 3.2, 0.25), M["polymer"])
    m.box(body, (-0.3, 1.6, -0.2), (0.3, 2.0, 2.8), M["polymer"])
    m.box(body, (-0.16, 2.2, 0.9), (0.16, 3.2, 1.3), M["steel"], rotation=(-10, 0, 0), pivot=(0, 3.2, 1.1))
    m.box(body, (1.0, 4.5, 2.6), (1.14, 4.9, 3.2), mat(LASER, 3, 10))
    grip = m.bone("grip", "body", pivot=(0, 3.2, 3.0), rotation=(-14, 0, 0))
    m.box(grip, (-0.9, -0.9, 2.0), (0.9, 3.2, 4.3), M["grip"], faces={"up": M["polymer"]})
    m.box(grip, (-1.0, -1.2, 1.9), (1.0, -0.8, 4.5), M["polymer"])
    # Pente comprido à frente do gatilho (osso próprio), levemente curvo para a frente.
    mag = m.bone("magazine", "body", pivot=(0, 3.2, -2.2), rotation=(8, 0, 0))
    m.box(mag, (-0.55, -2.8, -3.0), (0.55, 3.3, -1.4), M["gunmetal"], faces={"up": M["brass"]},
          decals={"east": gi.pattern_ribs(3, True, -16), "west": gi.pattern_ribs(3, True, -16)})
    m.box(mag, (-0.68, -3.3, -3.15), (0.68, -2.8, -1.25), M["polymer_light"])
    # Alavanca de manejo (osso próprio: puxada no fim da recarga) e a coronha recolhida.
    handle = m.bone("charging", "body", pivot=(0, 6.0, 4.5))
    m.box(handle, (-1.25, 5.6, 4.2), (1.25, 6.2, 4.9), M["gunmetal_dark"])
    for y in (4.4, 5.7):
        m.box(body, (-0.85, y, 5.2), (-0.55, y + 0.35, 8.8), M["steel_dark"])
        m.box(body, (0.55, y, 5.2), (0.85, y + 0.35, 8.8), M["steel_dark"])
    m.box(body, (-0.95, 3.6, 8.8), (0.95, 6.6, 9.7), M["rubber"])
    muzzle_flash(m, "body", 0, 5.25, -12.4, 1.6)
    return m


def submachine_gun_animations():
    fire = Animation(0.1)
    flash_on(fire, 0.04)
    kick(fire, "body", 3, 0.4, 0.02, 0.09)
    fire.bone("charging").tween("move", 0.0, 0.03, (0, 0, 0), (0, 0, 0.8), gi.ease_out, 1) \
        .tween("move", 0.03, 0.08, (0, 0, 0.8), (0, 0, 0), gi.ease_in_out, 2)

    length = 2.25
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    tilt_for_reload(reload, "body", length, roll=-18, pitch=8)
    swap_part(reload, "magazine", (0, -7.0, -1.0), 0.25, 0.6, 0.95, 1.35)
    reload.bone("charging").move(0.0, (0, 0, 0)).move(1.5, (0, 0, 0)) \
        .tween("move", 1.5, 1.65, (0, 0, 0), (0, 0, 2.2), gi.ease_out, 3) \
        .tween("move", 1.7, 1.8, (0, 0, 2.2), (0, 0, 0), gi.ease_out, 3)
    return {"idle": idle_hiding_flash(), "fire": fire, "reload": reload}


# ---------------------------------------------------------------- metralhadora pesada

def build_heavy_machine_gun():
    """Metralhadora pesada da Regina: caixa de munição embaixo com a fita de latão, alça de transporte, capa do cano
    furada, bipé recolhido, punho dianteiro e coronha cheia oliva. ~42 px."""
    m = Model("heavy_machine_gun", density=4, seed=41)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 6))
    m.box(body, (-1.45, 3.0, -4.5), (1.45, 6.8, 8.4), M["gunmetal"],
          decals={"east": combine(dark_rect(0.15, 0.18, 0.42, 0.5), stencil("PL-60", (116, 122, 130), (0.65, 0.78))),
                  "west": stencil("PL-60", (116, 122, 130), (0.4, 0.78))})
    m.box(body, (-1.5, 2.8, 1.0), (1.5, 3.2, 8.4), M["gunmetal_dark"])
    # Tampa de alimentação (osso próprio: abre na recarga), com o trilho e a mira de trás.
    cover = m.bone("cover", "body", pivot=(0, 6.8, -2.6))
    m.box(cover, (-1.35, 6.8, -2.6), (1.35, 7.7, 5.6), M["olive"], decals={"up": stencil("PL", (60, 64, 42))})
    m.box(cover, (-0.6, 7.7, -2.2), (0.6, 8.1, 5.0), M["rail"])
    m.box(cover, (-0.65, 8.1, 3.8), (0.65, 8.9, 4.6), M["gunmetal_dark"], decals={"south": dark_rect(0.35, 0.2, 0.65,
                                                                                               0.6)})
    # Alça de transporte sobre o cano.
    for z in (-9.5, -5.5):
        m.box(body, (-0.35, 6.6, z), (0.35, 8.6, z + 0.7), M["olive_dark"])
    m.box(body, (-0.45, 8.6, -9.6), (0.45, 9.3, -4.7), M["olive"])
    # Cano, capa furada, tubo de gás, quebra-chamas com fendas e a mira da frente.
    m.octagon(body, "z", (0, 5.0), -26.0, -4.5, 1.35, M["steel_dark"])
    m.box(body, (-1.15, 3.8, -17.0), (1.15, 6.4, -4.5), M["gunmetal_holes"], hide=("south",))
    m.octagon(body, "z", (0, 3.4), -19.0, -4.5, 0.85, M["gunmetal_dark"])
    m.box(body, (-0.6, 2.9, -19.6), (0.6, 5.7, -18.8), M["gunmetal"])
    m.octagon(body, "z", (0, 5.0), -28.4, -25.6, 1.75, M["gunmetal"], cap=M["gunmetal"])
    m.box(body, (-0.88, 4.4, -28.0), (0.88, 5.6, -26.0), M["gunmetal_dark"], hide=("north", "south", "up", "down"),
          decals={"east": slots(3, 0.1, 0.9), "west": slots(3, 0.1, 0.9)})
    m.box(body, (-0.45, 4.8, -28.41), (0.45, 5.2, -28.4), M["bore"])
    m.box(body, (-0.2, 5.8, -24.8), (0.2, 7.0, -24.2), M["gunmetal_dark"], faces={"south": M["tritium"]})
    # Bipé recolhido sob a capa.
    for x in (-0.9, 0.6):
        m.box(body, (x, 3.3, -19.0), (x + 0.3, 3.7, -9.0), M["steel_dark"])
    m.box(body, (-1.0, 3.2, -19.6), (1.0, 3.8, -18.9), M["gunmetal_dark"])
    # Punho dianteiro, guarda-mato, gatilho e a empunhadura.
    m.box(body, (-0.6, 0.0, -12.0), (0.6, 3.8, -10.6), M["grip"], rotation=(8, 0, 0), pivot=(0, 3.8, -11.3))
    m.box(body, (-0.32, 1.2, 3.0), (0.32, 3.0, 3.45), M["gunmetal"])
    m.box(body, (-0.32, 0.9, 3.0), (0.32, 1.3, 6.4), M["gunmetal"])
    m.box(body, (-0.18, 1.6, 3.9), (0.18, 3.0, 4.3), M["steel"], rotation=(-10, 0, 0), pivot=(0, 3.0, 4.1))
    grip = m.bone("grip", "body", pivot=(0, 3.0, 6.8), rotation=(-16, 0, 0))
    m.box(grip, (-1.0, -2.0, 5.6), (1.0, 3.0, 8.0), M["grip"], faces={"up": M["polymer"]})
    # Coronha cheia oliva e a soleira.
    m.box(body, (-1.25, 2.4, 8.4), (1.25, 6.4, 13.0), M["olive"])
    m.box(body, (-1.2, 1.4, 13.0), (1.2, 6.4, 18.0), M["olive"], rotation=(4, 0, 0), pivot=(0, 6.4, 13.0),
          decals={"east": dark_rect(0.2, 0.45, 0.8, 0.6), "west": dark_rect(0.2, 0.45, 0.8, 0.6)})
    m.box(body, (-1.3, 1.1, 18.0), (1.3, 6.6, 19.2), M["rubber"], rotation=(4, 0, 0), pivot=(0, 6.4, 13.0))
    # Caixa de munição (osso próprio: sai e entra na recarga) e a fita de latão que sobe para a culatra.
    box = m.bone("ammo_box", "body", pivot=(0, 1.5, -1.0))
    m.box(box, (-1.8, -1.6, -4.0), (1.4, 3.0, 1.6), M["olive"], decals={
        "east": stencil("PL-60", (40, 44, 30), (0.5, 0.35)), "west": stencil("PL-60", (40, 44, 30), (0.5, 0.35))})
    m.box(box, (-1.9, -0.3, -4.1), (1.5, 0.1, 1.7), M["olive_dark"])
    m.box(box, (-0.6, 3.0, -2.8), (0.6, 3.4, 0.4), M["olive_dark"])
    belt = m.bone("belt", "body", pivot=(-1.6, 4.0, -1.0))
    for k in range(5):
        z = -3.2 + k * 0.85
        m.box(belt, (-2.2, 2.9, z), (-1.45, 5.0, z + 0.55), M["brass"], faces={"up": M["copper"]})
        m.box(belt, (-2.25, 3.5, z - 0.15), (-1.45, 3.8, z + 0.7), M["steel_dark"])
    muzzle_flash(m, "body", 0, 5.0, -28.4, 2.2)
    return m


def heavy_machine_gun_animations():
    fire = Animation(0.15)
    flash_on(fire, 0.05)
    kick(fire, "body", 3.5, 0.5, 0.03, 0.13)
    fire.bone("belt").tween("move", 0.0, 0.05, (0, 0, 0), (0, 0, 0.6), gi.ease_out, 1) \
        .tween("move", 0.05, 0.12, (0, 0, 0.6), (0, 0, 0), gi.ease_in_out, 2)

    length = 4.0
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    tilt_for_reload(reload, "body", length, roll=-16, pitch=6)
    # A tampa abre, a caixa e a fita saem, a caixa nova entra, a fita sobe, a tampa fecha.
    reload.bone("cover").tween("turn", 0.2, 0.55, (0, 0, 0), (-70, 0, 0), gi.ease_out, 4).turn(3.05, (-70, 0, 0)) \
        .tween("turn", 3.05, 3.3, (-70, 0, 0), (0, 0, 0), gi.ease_in_out, 4)
    swap_part(reload, "ammo_box", (-1.0, -9.0, 0), 0.7, 1.2, 1.8, 2.4)
    swap_part(reload, "belt", (-1.5, -6.0, 0), 0.7, 1.1, 2.4, 2.9)
    return {"idle": idle_hiding_flash(), "fire": fire, "reload": reload}


# ---------------------------------------------------------------- canhão sólido

def build_solid_cannon():
    """Canhão sólido do Dylan: carcaça branca blindada com faixas laranja e de alerta, três bobinas que giram em volta
    do núcleo ciano aceso, o prato emissor na frente e a célula de energia brilhando no berço de cima. ~33 px."""
    m = Model("solid_cannon", density=4, seed=53)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 4, 4))
    # Carcaça: o bloco principal, a faixa laranja, a ventilação e o painel de alerta.
    m.box(body, (-2.2, 2.4, -6.4), (2.2, 7.6, 8.4), M["armor"],
          decals={"east": combine(band(0.62, 0.72, ORANGE), stencil("PL-SC", (70, 74, 78), (0.6, 0.35))),
                  "west": combine(band(0.62, 0.72, ORANGE), stencil("PL-SC", (70, 74, 78), (0.4, 0.35))),
                  "up": band(0.0, 0.08, ORANGE)})
    m.box(body, (-2.25, 3.0, 1.6), (2.25, 5.2, 6.8), M["armor_vents"], hide=("north", "south", "up", "down"))
    m.box(body, (-2.3, 6.0, -6.0), (2.3, 6.5, -2.0), M["hazard"], hide=("north", "south"))
    m.box(body, (-1.6, 1.6, -5.6), (1.6, 2.4, 7.6), M["gunmetal_dark"])
    m.box(body, (-1.9, 3.0, 8.4), (1.9, 7.0, 9.6), M["gunmetal"], faces={"south": mat(GUNMETAL, 3, 12)},
          decals={"south": combine(dark_rect(0.15, 0.2, 0.3, 0.32, CYAN, 0), dark_rect(0.4, 0.2, 0.55, 0.32, CYAN, 0),
                                   dark_rect(0.65, 0.2, 0.8, 0.32, ORANGE, 0))})
    # Núcleo aceso, as barras que seguram as bobinas e o prato emissor com o miolo aceso.
    m.octagon(body, "z", (0, 5.0), -17.5, -6.4, 1.3, M["cyan"], cap=M["cyan"])
    for x, y in ((-1.7, 6.6), (1.2, 6.6), (-1.7, 2.9), (1.2, 2.9)):
        m.box(body, (x, y, -17.0), (x + 0.5, y + 0.5, -6.4), M["gunmetal"])
    m.octagon(body, "z", (0, 5.0), -18.6, -17.4, 4.2, M["gunmetal"], cap=M["gunmetal_dark"])
    m.octagon(body, "z", (0, 5.0), -18.65, -18.0, 2.0, M["cyan"], cap=M["cyan"])
    # As três bobinas (osso próprio: giram em volta do núcleo).
    coils = m.bone("coils", "body", pivot=(0, 5.0, -12.0))
    for z in (-8.4, -11.7, -15.0):
        m.octagon(coils, "z", (0, 5.0), z - 0.9, z, 4.0, M["orange"], cap=M["gunmetal"])
        m.octagon(coils, "z", (0, 5.0), z - 1.1, z + 0.2, 2.8, M["cyan_dim"], cap=M["cyan_dim"])
    # Berço da célula em cima, com a alça.
    for z in (0.4, 5.8):
        m.box(body, (-1.3, 7.6, z), (1.3, 8.0, z + 0.8), M["gunmetal"])
        for x in (-1.3, 0.9):
            m.box(body, (x, 8.0, z), (x + 0.4, 9.8, z + 0.8), M["gunmetal"])
    m.box(body, (-1.3, 9.8, 0.4), (1.3, 10.4, 6.6), M["armor"], decals={"up": band(0.4, 0.6, ORANGE, False)})
    m.box(body, (-1.9, 7.6, -6.0), (1.9, 8.0, 8.0), M["armor"], hide=("down",))
    cell = m.bone("cell", "body", pivot=(0, 8.6, 3.5))
    m.octagon(cell, "z", (0, 8.6), 1.0, 6.0, 1.6, M["cyan"], cap=M["gunmetal"])
    m.octagon(cell, "z", (0, 8.6), 0.8, 1.4, 1.9, M["gunmetal"], cap=M["gunmetal"])
    m.octagon(cell, "z", (0, 8.6), 5.6, 6.2, 1.9, M["gunmetal"], cap=M["gunmetal"])
    # Punho dianteiro, gatilho, empunhadura.
    m.box(body, (-0.6, -1.4, -4.6), (0.6, 2.4, -3.2), M["grip"], rotation=(6, 0, 0), pivot=(0, 2.4, -3.9))
    m.box(body, (-0.32, 0.4, 2.2), (0.32, 1.6, 2.65), M["gunmetal"])
    m.box(body, (-0.32, 0.1, 2.2), (0.32, 0.5, 5.4), M["gunmetal"])
    m.box(body, (-0.18, 0.8, 3.0), (0.18, 1.6, 3.4), M["steel"])
    grip = m.bone("grip", "body", pivot=(0, 1.6, 5.4), rotation=(-12, 0, 0))
    m.box(grip, (-1.0, -3.0, 4.2), (1.0, 1.6, 6.6), M["grip"], faces={"up": M["polymer"]})
    muzzle_flash(m, "body", 0, 5.0, -18.7, 2.6)
    return m


def solid_cannon_animations():
    def spin(anim, length, turns):
        coils = anim.bone("coils")
        steps = max(4, int(turns * 8))
        for k in range(steps + 1):
            coils.turn(length * k / steps, (0, 0, -360.0 * turns * k / steps))

    idle = idle_hiding_flash(lambda a: spin(a, 2.0, 0.5))
    fire = Animation(0.5)
    flash_on(fire, 0.08)
    kick(fire, "body", 10, 1.4, 0.05, 0.4)
    spin(fire, 0.5, 1.5)

    length = 2.5
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    tilt_for_reload(reload, "body", length, roll=-12, pitch=-6)
    # A célula gasta sobe e sai pela traseira; a nova desce no berço e as bobinas voltam a girar.
    swap_part(reload, "cell", (0, 5.0, 6.0), 0.25, 0.7, 1.2, 1.7)
    spin(reload, length, 0.75)
    return {"idle": idle, "fire": fire, "reload": reload}


# ---------------------------------------------------------------- rifle antitanque

def build_anti_tank_rifle():
    """Rifle antitanque do Dylan: cano grosso e longo com o freio de boca de fendas, luneta com a lente acesa, ferrolho
    de alavanca à direita, carregador de caixa com o projetor pesado, bipé recolhido e coronha esqueletizada com
    apoio de rosto. ~58 px."""
    m = Model("anti_tank_rifle", density=4, seed=67)
    m.bone("root", pivot=(0, 0, 0))
    body = m.bone("body", "root", pivot=(0, 5, 6))
    m.box(body, (-1.35, 3.8, -6.5), (1.35, 7.2, 8.4), M["gunmetal"],
          decals={"east": combine(dark_rect(0.42, 0.15, 0.7, 0.5), stencil("PL-AT", (124, 130, 138), (0.25, 0.75))),
                  "west": stencil("PL-AT", (124, 130, 138), (0.75, 0.75))})
    m.box(body, (-1.4, 3.5, -5.0), (1.4, 3.9, 8.0), M["gunmetal_dark"])
    m.box(body, (-0.7, 7.2, -5.6), (0.7, 7.6, 6.6), M["rail"])
    # Cano grosso, a camisa de trás e o freio de boca com três fendas de cada lado.
    barrel = m.bone("barrel", "body", pivot=(0, 5.5, -6.5))
    m.octagon(barrel, "z", (0, 5.5), -38.0, -6.5, 1.9, M["steel_dark"])
    m.octagon(barrel, "z", (0, 5.5), -13.5, -6.5, 2.5, M["gunmetal_vribs"])
    m.box(barrel, (-1.0, 4.4, -29.0), (1.0, 6.6, -27.6), M["hazard"], hide=("north", "south"))
    m.box(barrel, (-1.7, 4.0, -43.0), (1.7, 7.0, -37.6), M["gunmetal"],
          decals={"east": slots(3, 0.18, 0.82), "west": slots(3, 0.18, 0.82),
                  "north": dark_rect(0.36, 0.3, 0.64, 0.7, BORE, 0), "up": slots(2, 0.3, 0.7)})
    # Bipé recolhido junto ao cano.
    for x in (-1.0, 0.7):
        m.box(barrel, (x, 3.6, -27.0), (x + 0.3, 4.0, -15.0), M["steel_dark"])
    m.box(barrel, (-1.15, 3.5, -16.0), (1.15, 4.6, -14.6), M["gunmetal_dark"])
    # Luneta: tubo, campânula da objetiva, ocular, torres e anéis; as lentes acesas.
    m.octagon(body, "z", (0, 9.5), -6.0, 6.0, 1.6, M["gunmetal_dark"])
    m.octagon(body, "z", (0, 9.5), -9.0, -5.6, 2.5, M["gunmetal"], cap=M["lens_glow"])
    m.octagon(body, "z", (0, 9.5), 5.6, 8.2, 2.1, M["gunmetal"], cap=M["lens"])
    m.box(body, (-0.5, 10.3, -1.0), (0.5, 11.2, 0.4), M["gunmetal"])
    m.box(body, (0.8, 9.0, -1.0), (1.7, 10.0, 0.4), M["gunmetal"])
    for z in (-3.6, 2.6):
        m.box(body, (-0.9, 7.6, z), (0.9, 9.0, z + 1.0), M["gunmetal_dark"])
    # Ferrolho (osso próprio: a alavanca sobe, recua e volta na recarga).
    bolt = m.bone("bolt", "body", pivot=(1.35, 6.2, 4.5))
    m.box(bolt, (1.35, 5.9, 4.1), (2.9, 6.5, 4.8), M["steel"])
    m.box(bolt, (2.6, 5.6, 3.95), (3.5, 6.8, 5.0), M["steel_dark"])
    # Carregador de caixa (osso próprio) com o projetil de latão de ponta pintada em cima.
    mag = m.bone("magazine", "body", pivot=(0, 2.0, -1.0))
    m.box(mag, (-1.05, -0.4, -3.6), (1.05, 3.6, 1.6), M["gunmetal"], decals={
        "east": band(0.0, 0.12, YELLOW), "west": band(0.0, 0.12, YELLOW)})
    m.box(mag, (-0.55, 3.6, -3.2), (0.55, 3.9, 1.2), M["brass"])
    # Guarda-mato, gatilho e a empunhadura.
    m.box(body, (-0.32, 1.6, 2.6), (0.32, 3.5, 3.05), M["gunmetal"])
    m.box(body, (-0.32, 1.3, 2.6), (0.32, 1.7, 6.2), M["gunmetal"])
    m.box(body, (-0.18, 2.0, 3.6), (0.18, 3.5, 4.0), M["steel"], rotation=(-10, 0, 0), pivot=(0, 3.5, 3.8))
    grip = m.bone("grip", "body", pivot=(0, 3.5, 6.6), rotation=(-16, 0, 0))
    m.box(grip, (-1.0, -1.6, 5.4), (1.0, 3.5, 7.8), M["grip"], faces={"up": M["polymer"]})
    # Coronha esqueletizada com o apoio de rosto, a soleira e o monopé.
    m.box(body, (-1.1, 5.2, 8.4), (1.1, 7.4, 18.6), M["tan"])
    m.box(body, (-1.0, 7.4, 10.0), (1.0, 8.2, 16.0), M["tan"], decals={"up": gi.pattern_ribs(2, True, -14)})
    m.box(body, (-0.55, 1.4, 9.0), (0.55, 2.4, 18.6), M["gunmetal_dark"], rotation=(-10, 0, 0), pivot=(0, 1.9, 18.6))
    m.box(body, (-0.5, 2.4, 17.6), (0.5, 5.2, 18.6), M["gunmetal_dark"])
    m.box(body, (-1.45, 0.8, 18.6), (1.45, 8.0, 20.0), M["rubber"], faces={"south": mat(RUBBER, 2, 6,
          pattern=gi.pattern_ribs(2, True, 14))})
    m.box(body, (-0.3, -1.4, 15.6), (0.3, 1.6, 16.2), M["steel_dark"])
    muzzle_flash(m, "barrel", 0, 5.5, -43.0, 3.4)
    return m


def anti_tank_rifle_animations():
    fire = Animation(0.8)
    flash_on(fire, 0.08)
    kick(fire, "body", 18, 2.4, 0.06, 0.6)
    fire.bone("barrel").tween("move", 0.0, 0.04, (0, 0, 0), (0, 0, 2.0), gi.ease_out, 1) \
        .tween("move", 0.04, 0.3, (0, 0, 2.0), (0, 0, 0), gi.ease_in_out, 4)

    length = 3.0
    reload = Animation(length)
    reload.bone("flash").size(0.0, gi.HIDDEN)
    tilt_for_reload(reload, "body", length, roll=-14, pitch=5)
    # A alavanca sobe e recua, o carregador sai e o novo entra, a alavanca volta e desce.
    reload.bone("bolt").tween("turn", 0.15, 0.35, (0, 0, 0), (0, 0, 70), gi.ease_out, 3) \
        .move(0.0, (0, 0, 0)).move(0.35, (0, 0, 0)) \
        .tween("move", 0.35, 0.55, (0, 0, 0), (0, 0, 2.6), gi.ease_out, 3).move(2.2, (0, 0, 2.6)) \
        .tween("move", 2.2, 2.4, (0, 0, 2.6), (0, 0, 0), gi.ease_in_out, 3).turn(2.4, (0, 0, 70)) \
        .tween("turn", 2.4, 2.6, (0, 0, 70), (0, 0, 0), gi.ease_out, 3)
    swap_part(reload, "magazine", (0, -7.0, 0.6), 0.7, 1.1, 1.5, 1.95)
    return {"idle": idle_hiding_flash(), "fire": fire, "reload": reload}


# ---------------------------------------------------------------- exibição

# Onde a mão da primeira pessoa fica no espaço da câmera (o vanilla, sem balanço), em px.
FP_HAND = (0.56 * 16, -0.52 * 16, -0.72 * 16)
# Terceira pessoa: a origem da exibição fica na face de cima da mão (o braço erguido na mira) e na ponta dela; o ponto
# da empunhadura desce 0,6 px para dentro do punho e recua 1,8 px, e o resto da arma fica por cima da mão.
TP_FIST = (0.0, -0.6, 1.8)


def bounds(model):
    """A caixa do modelo em repouso (px), sem o clarão e o cartucho da recarga, que ficam escondidos."""
    if not model.tex_w:
        model.pack()
    quads = gi.posed_quads(model, hidden_bones=("flash", "shell"))
    pts = np.vstack([q[0] for q in quads]) * 16 - np.array([0, 0.16, 0])
    return pts.min(axis=0), pts.max(axis=0)


def centered(model, rotation, scale):
    lo, hi = bounds(model)
    return gi.anchored(rotation, scale, (lo + hi) / 2, (0, 0, 0))


def display(model, cfg):
    """As transformações de exibição: primeira e terceira pessoa ancoradas na empunhadura; GUI, chão, moldura e
    cabeça centradas na caixa do modelo."""
    grip = cfg["grip"]
    fp_rot, fp_scale = cfg["fp_rotation"], cfg["fp_scale"]
    fp_target = [cfg["fp_at"][i] - FP_HAND[i] for i in range(3)]
    tp_rot, tp_scale = cfg.get("tp_rotation", [0, 0, 0]), cfg["tp_scale"]
    gui_rot, gui_scale = cfg.get("gui_rotation", [0, -90, 0]), cfg["gui_scale"]
    d = {
        "firstperson_righthand": {"rotation": fp_rot, "translation": gi.anchored(fp_rot, fp_scale, grip, fp_target),
                                  "scale": [fp_scale] * 3},
        "thirdperson_righthand": {"rotation": tp_rot, "translation": gi.anchored(tp_rot, tp_scale, grip, TP_FIST),
                                  "scale": [tp_scale] * 3},
        "gui": {"rotation": gui_rot, "translation": centered(model, gui_rot, gui_scale), "scale": [gui_scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": centered(model, [0, 0, 0], cfg["ground_scale"]),
                   "scale": [cfg["ground_scale"]] * 3},
        "fixed": {"rotation": gui_rot, "translation": centered(model, gui_rot, gui_scale * 1.1),
                  "scale": [round(gui_scale * 1.1, 3)] * 3},
        "head": {"rotation": [0, 0, 0], "translation": centered(model, [0, 0, 0], 0.8), "scale": [0.8] * 3},
    }
    # No chão o ItemEntityRenderer ergue o ponto de apoio 0,25 bloco e o balanço vai até 0,2: desce até o pé tocar.
    lo, hi = bounds(model)
    d["ground"]["translation"][1] -= (hi[1] - lo[1]) / 2 * cfg["ground_scale"] - 1.0
    d["head"]["translation"][1] += 14
    for entry in d.values():
        entry["translation"] = [round(v, 2) for v in entry["translation"]]
    return d


def item_model(name, display_entries):
    return {
        "loader": "forge:separate_transforms",
        "gui_light": "side",
        "textures": {"particle": f"iceagesurvival:item/{name}_3d"},
        "base": {"parent": "builtin/entity", "display": display_entries},
    }


GUNS = {
    "handgun": {"build": build_handgun, "animations": handgun_animations, "grip": (0, 2.2, 1.4),
                "fp_rotation": [0, 4, 0], "fp_scale": 1.1, "fp_at": (7.6, -7.0, -17.0),
                "tp_scale": 0.5, "gui_scale": 0.95, "ground_scale": 0.5},
    "shotgun": {"build": build_shotgun, "animations": shotgun_animations, "grip": (0, 2.6, 5.2),
                "fp_rotation": [2, 10, 0], "fp_scale": 0.85, "fp_at": (9.6, -8.4, -17.0),
                "tp_scale": 0.5, "gui_scale": 0.62, "gui_rotation": [90, -65, 90], "ground_scale": 0.45},
    "submachine_gun": {"build": build_submachine_gun, "animations": submachine_gun_animations, "grip": (0, 2.4, 3.2),
                       "fp_rotation": [1, 8, 0], "fp_scale": 1.0, "fp_at": (8.4, -7.6, -17.0),
                       "tp_scale": 0.5, "gui_scale": 0.72, "ground_scale": 0.5},
    "heavy_machine_gun": {"build": build_heavy_machine_gun, "animations": heavy_machine_gun_animations,
                          "grip": (0, 2.2, 6.9), "fp_rotation": [2, 10, 0], "fp_scale": 0.75,
                          "fp_at": (10.0, -9.2, -17.0), "tp_scale": 0.5, "gui_scale": 0.5,
                          "gui_rotation": [90, -65, 90], "ground_scale": 0.4},
    "solid_cannon": {"build": build_solid_cannon, "animations": solid_cannon_animations, "grip": (0, 0.6, 5.4),
                     "fp_rotation": [2, 10, 0], "fp_scale": 0.8, "fp_at": (9.4, -8.6, -17.0),
                     "tp_scale": 0.5, "gui_scale": 0.55, "gui_rotation": [90, -70, 90], "ground_scale": 0.42},
    "anti_tank_rifle": {"build": build_anti_tank_rifle, "animations": anti_tank_rifle_animations,
                        "grip": (0, 2.6, 6.6), "fp_rotation": [2, 10, 0], "fp_scale": 0.7,
                        "fp_at": (10.0, -8.6, -17.0), "tp_scale": 0.45, "gui_scale": 0.4,
                        "gui_rotation": [90, -60, 90], "ground_scale": 0.35},
}


# ---------------------------------------------------------------- prévia

def arm_quads(arm):
    """O braço direito do jogador (caixa 4x12x4 a partir de -3,-2,-2), no espaço do modelo do jogador."""
    model = Model("arm", density=2)
    bone = model.bone("arm")
    model.box(bone, (-3, -2, -2), (1, 10, 2), Material((198, 150, 116), noise=4, bevel=10))
    model.box(bone, (-3.2, -2.2, -2.2), (1.2, 4, 2.2), Material((40, 120, 140), noise=4, bevel=10))
    tex, _ = model.textures()
    quads = [(np.array([gi.apply4(arm @ gi.tr(0, -0.01, 0), p) for p in q[0]]), q[1], q[2])
             for q in gi.posed_quads(model)]
    return quads, tex


def preview(name, out_dir):
    cfg = GUNS[name]
    model = cfg["build"]()
    tex, glow = model.textures()
    anims = cfg["animations"]()
    disp = display(model, cfg)
    out_dir.mkdir(parents=True, exist_ok=True)
    idle = anims["idle"]

    def model_view(anim, t, yaw, pitch, size=(640, 400)):
        quads = gi.posed_quads(model, anim, t)
        R = gi.rx(math.radians(pitch)) @ gi.ry(math.radians(yaw))
        pts = np.vstack([q[0] for q in quads])
        center = pts.mean(axis=0)
        cam = np.array([gi.apply4(R, p - center) for p in pts])
        lo, hi = cam.min(axis=0), cam.max(axis=0)
        s = 0.88 * min(size[0] / max(1e-6, hi[0] - lo[0]), size[1] / max(1e-6, hi[1] - lo[1]))
        mid = ((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2)
        to_cam = lambda p: gi.apply4(R, p - center)  # noqa: E731
        return gi.rasterize(quads, tex, glow, to_cam, gi.orthographic(size[0], size[1], s, mid), size,
                            supersample=2)

    views = [model_view(idle, 0, -90, 0), model_view(idle, 0, 90, 0), model_view(idle, 0, -40, 22),
             model_view(idle, 0, 150, -18)]
    W, H = views[0].size
    grid = Image.new("RGB", (W * 2, H * 2))
    for k, img in enumerate(views):
        grid.paste(img, ((k % 2) * W, (k // 2) * H))
    grid.save(out_dir / f"{name}_model.png")

    # Primeira pessoa: câmera na origem olhando −Z, FOV vertical 70, 16:9; repouso, tiro e dois quadros da recarga.
    W, H = 854, 480
    fp = gi.first_person_chain(disp["firstperson_righthand"])
    reload_len = anims["reload"].length
    frames = []
    for anim, t in ((idle, 0.0), (anims["fire"], 0.03), (anims["reload"], reload_len * 0.33),
                    (anims["reload"], reload_len * 0.7)):
        quads = gi.posed_quads(model, anim, t)
        img = gi.rasterize(quads, tex, glow, lambda p: gi.apply4(fp, p), gi.perspective(W, H), (W, H),
                           background=(128, 160, 196), supersample=2)
        d = ImageDraw.Draw(img)
        d.line((W / 2 - 6, H / 2, W / 2 + 6, H / 2), fill=(255, 255, 255))
        d.line((W / 2, H / 2 - 6, W / 2, H / 2 + 6), fill=(255, 255, 255))
        frames.append(img)
    grid = Image.new("RGB", (W * 2, H * 2))
    for k, img in enumerate(frames):
        grid.paste(img, ((k % 2) * W, (k // 2) * H))
    grid.save(out_dir / f"{name}_firstperson.png")

    # Terceira pessoa: o braço direito na pose de mira, com um braço de referência; de lado e de 3/4.
    item, arm = gi.third_person_chain(disp["thirdperson_righthand"])
    a_quads, a_tex = arm_quads(arm)
    g_quads = [(np.array([gi.apply4(item, p) for p in q[0]]), q[1], q[2]) for q in gi.posed_quads(model, idle, 0)]
    views = []
    for yaw, pitch in ((90, 0), (35, 15)):
        R = gi.rx(math.radians(pitch)) @ gi.ry(math.radians(yaw)) @ gi.sc(-1, -1, 1)
        img_g = gi.rasterize(g_quads, tex, glow, lambda p: gi.apply4(R, p), gi.orthographic(500, 400, 420,
                             (-0.2, -0.05)), (500, 400), supersample=2, mirrored=True)
        # O braço por cima em segundo passe seria errado na profundidade: junta as duas listas com texturas próprias.
        views.append(composite(g_quads, tex, glow, a_quads, a_tex, R))
    grid = Image.new("RGB", (1000, 400))
    for k, img in enumerate(views):
        grid.paste(img, (k * 500, 0))
    grid.save(out_dir / f"{name}_thirdperson.png")

    # GUI: o item no quadrado de 16 px (ampliado 4x).
    gui = gi.display_matrix(disp["gui"])
    quads = gi.posed_quads(model, idle, 0)
    img = gi.rasterize(quads, tex, glow, lambda p: gi.apply4(gui, p), gi.orthographic(64, 64, 64), (64, 64),
                       background=(139, 139, 139), supersample=4)
    img.resize((192, 192), Image.NEAREST).save(out_dir / f"{name}_gui.png")
    tex.resize((tex.width * 3, tex.height * 3), Image.NEAREST).save(out_dir / f"{name}_texture.png")
    print(f"prévias de {name} em {out_dir}")


def composite(quads_a, tex_a, glow_a, quads_b, tex_b, R, size=(500, 400)):
    """Duas malhas com texturas diferentes na mesma cena: junta as texturas lado a lado e desloca os UV da segunda."""
    w = tex_a.width
    atlas = Image.new("RGBA", (w + tex_b.width, max(tex_a.height, tex_b.height)), (0, 0, 0, 0))
    atlas.paste(tex_a, (0, 0))
    atlas.paste(tex_b, (w, 0))
    glow = Image.new("RGBA", atlas.size, (0, 0, 0, 0))
    glow.paste(glow_a, (0, 0))
    moved = [(p, [(u + w, v) for u, v in uvs], f) for p, uvs, f in quads_b]
    return gi.rasterize(quads_a + moved, atlas, glow, lambda p: gi.apply4(R, p),
                        gi.orthographic(size[0], size[1], 420, (-0.2, -0.05)), size, supersample=2, mirrored=True)


# ---------------------------------------------------------------- ícones das munições (16x16, item/generated)

AMMO_PALETTE = {
    ".": None,
    "o": (24, 22, 20),      # contorno
    "b": (150, 108, 40),    # latão escuro
    "B": (206, 160, 70),    # latão
    "Y": (246, 214, 130),   # brilho do latão
    "c": (150, 82, 46),     # cobre
    "C": (200, 120, 70),    # ponta de cobre
    "r": (110, 26, 22),     # cartucho vermelho escuro
    "R": (178, 46, 36),     # cartucho
    "P": (222, 96, 80),     # brilho do cartucho
    "g": (44, 48, 54),      # metal escuro
    "m": (98, 104, 112),    # metal
    "M": (160, 166, 174),   # brilho do metal
    "t": (36, 120, 122),    # célula funda
    "T": (86, 236, 230),    # célula
    "W": (214, 255, 252),   # miolo aceso
    "k": (30, 32, 34),      # ponta preta
    "y": (226, 186, 40),    # faixa amarela
}

AMMO_ICONS = {
    # Três balas em pé, de latão com ponta de cobre, na bandeja de metal.
    "light_rounds": [
        "................",
        "....o...o...o...",
        "...oCo.oCo.oCo..",
        "...oCo.oCo.oCo..",
        "..ocCCocCCocCCo.",
        "..oBYBoBYBoBYBo.",
        "..oBYBoBYBoBYBo.",
        "..oBYBoBYBoBYBo.",
        "..obYBobYBobYBo.",
        "..obBBobBBobBBo.",
        "..obBBobBBobBBo.",
        "..obBBobBBobBBo.",
        "..ooooooooooooo.",
        ".oMMMMMMMMMMMMMo",
        ".ommmmmmmmmmmmmo",
        "..ooooooooooooo.",
    ],
    # Dois cartuchos vermelhos deitados, com a base de latão à esquerda e o fecho estrelado à direita.
    "shotgun_shells": [
        "................",
        "................",
        "...oooooooooooo.",
        "..oYBoPPPPPPPPro",
        "..oBBoRRRRRRRRro",
        "..oBBoRRRRRRRRro",
        "..obboRrrrrrrrro",
        "...oooooooooooo.",
        ".oooooooooooo...",
        "oYBoPPPPPPPPro..",
        "oBBoRRRRRRRRro..",
        "oBBoRRRRRRRRro..",
        "obboRrrrrrrrro..",
        ".oooooooooooo...",
        "................",
        "................",
    ],
    # A célula do canhão: cápsula ciano acesa com as tampas de metal.
    "solid_cell": [
        "................",
        "......oooo......",
        ".....oMmmgo.....",
        ".....oggggo.....",
        "......otto......",
        ".....otTTto.....",
        ".....otWTto.....",
        ".....otWTto.....",
        ".....otWTto.....",
        ".....otTTto.....",
        ".....otTTto.....",
        "......otto......",
        ".....oMmmgo.....",
        ".....oggggo.....",
        "......oooo......",
        "................",
    ],
    # O projétil antitanque: latão grosso com a ponta preta e a faixa amarela, na diagonal.
    "heavy_rounds": [
        "................",
        "............oo..",
        "...........okko.",
        "..........okkko.",
        ".........okkkko.",
        "........oyyyko..",
        ".......oBYyyo...",
        "......oBYBBo....",
        ".....oBYBBo.....",
        "....oBYBBo......",
        "...obYBBo.......",
        "..obbBBo........",
        "..ogbbo.........",
        "..oggo..........",
        "...oo...........",
        "................",
    ],
}


def ammo_icon(grid):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, row
        for x, key in enumerate(row):
            if AMMO_PALETTE[key] is not None:
                img.putpixel((x, y), AMMO_PALETTE[key] + (255,))
    return img


# ---------------------------------------------------------------- saída

def write(name):
    cfg = GUNS[name]
    model = cfg["build"]()
    tex, glow = model.textures()
    gi.TEXTURES.mkdir(parents=True, exist_ok=True)
    tex.save(gi.TEXTURES / f"{name}_3d.png")
    glow.save(gi.TEXTURES / f"{name}_3d_glowmask.png")
    gi.write_json(gi.GEO / f"{name}.geo.json", model.geometry())
    gi.write_animations(name, cfg["animations"]())
    gi.write_json(gi.MODELS / f"{name}.json", item_model(name, display(model, cfg)))
    print(f"{name}: textura {model.tex_w}x{model.tex_h}, {sum(len(b.cubes) for b in model.bones)} cubos")


def main():
    args = sys.argv[1:]
    if args and args[0] == "--preview":
        out = Path(args[1])
        for name in args[2:] or GUNS:
            preview(name, out)
        return
    for name in GUNS:
        write(name)
    for name, grid in AMMO_ICONS.items():
        ammo_icon(grid).save(gi.TEXTURES / f"{name}.png")
        gi.write_json(gi.MODELS / f"{name}.json", {"parent": "minecraft:item/generated",
                                                    "textures": {"layer0": f"iceagesurvival:item/{name}"}})
    print("ícones das munições escritos")


if __name__ == "__main__":
    main()
