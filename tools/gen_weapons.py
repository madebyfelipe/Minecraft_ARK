#!/usr/bin/env python3
"""Gera os modelos e as texturas das armas tranquilizantes.

- tranq_rifle e tranq_crossbow: modelos 3D de item (formato Java, com `elements`) e uma
  textura 64x64 por arma, montada com amostras de material (madeira, aço, aço oxidado, latão,
  vidro com narcótico, lente, borracha, corda, couro, pena).
- tranq_dart: sprite 16x16 em item/generated.

Convenções da geometria: a arma fica deitada no eixo X, com o cano (ou a ponta do dardo)
para +X, o topo para +Y e as laterais em z = 8 +- metade da largura. As transformações de
`display` são calculadas aqui a partir da posição da empunhadura, para que a mão segure a
arma no cabo em qualquer escala; os números saem arredondados no JSON.

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão.
Arte autoral; nada copiado de outros mods.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
ITEM_TEXTURES = ASSETS / "textures" / "item"
ITEM_MODELS = ASSETS / "models" / "item"

TEX = 64                 # lado da textura das armas, em texels
TEXEL = 16 / TEX         # um texel em unidades de UV (UV do formato Java vai de 0 a 16)

# Cor do narcótico no vidro do dardo e no cartucho do rifle. Uma constante só, para trocar fácil.
NARCOTIC = [(40, 20, 56), (72, 40, 98), (112, 70, 150), (170, 130, 210)]
GLASS_EDGE = (30, 16, 40)
GLASS_SHINE = (232, 214, 246)

# ---------------------------------------------------------------- texturas de material

# Cada material ocupa um retângulo (x, y, largura, altura) em texels da textura 64x64.
SWATCHES = {
    "wood":     (0, 0, 40, 12),
    "wood_v":   (40, 0, 12, 12),   # mesma madeira com o veio na vertical (peças ao longo de Z)
    "steel":    (0, 12, 40, 8),
    "gunmetal": (0, 20, 40, 8),
    "brass":    (40, 12, 12, 8),
    "glass":    (40, 20, 12, 8),
    "lens":     (52, 0, 6, 6),
    "feather":  (58, 0, 6, 6),
    "string":   (52, 6, 12, 4),
    "rubber":   (52, 12, 12, 8),
    "leather":  (52, 20, 12, 8),
    "wood_end": (0, 28, 12, 12),
}

WOOD = [(70, 44, 26), (98, 64, 38), (126, 84, 50), (152, 104, 64)]
STEEL = [(92, 98, 108), (128, 134, 144), (162, 168, 178), (206, 212, 220)]
GUNMETAL = [(30, 33, 40), (46, 51, 60), (64, 71, 82), (92, 101, 114)]
BRASS = [(112, 80, 28), (156, 116, 44), (196, 154, 66), (232, 200, 112)]
LENS = [(26, 42, 70), (52, 96, 146), (140, 192, 226)]
RUBBER = [(24, 22, 22), (36, 34, 33), (50, 47, 45)]
STRING = [(168, 158, 132), (204, 196, 172), (228, 222, 204)]
LEATHER = [(62, 38, 24), (88, 56, 34), (116, 78, 50)]
FEATHER = [(150, 40, 36), (198, 66, 54), (236, 228, 214)]


def paint_wood(rng, w, h, vertical=False):
    """Veio comprido de madeira: faixas de tom que ondulam devagar, riscos escuros e um nó."""
    out = {}
    length, across = (h, w) if vertical else (w, h)
    phase = [rng.random() * 6 for _ in range(across)]
    for a in range(across):
        base = 1 + (a // 2 + (a % 3 == 0)) % 2          # alterna médio/claro de 2 em 2 linhas
        for l in range(length):
            wobble = math.sin(l * 0.35 + phase[a])
            tone = base + (1 if wobble > 0.75 else 0) - (1 if wobble < -0.85 else 0)
            if (a * 7 + l // 5) % 9 == 0 and rng.random() < 0.6:
                tone = 0                                 # risco escuro do veio
            tone = max(0, min(3, tone))
            out[(a, l) if vertical else (l, a)] = WOOD[tone]
    kx, ky = rng.randrange(3, max(4, w - 3)), rng.randrange(2, max(3, h - 2))
    for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
        if (kx + dx, ky + dy) in out:
            out[(kx + dx, ky + dy)] = WOOD[0] if (dx, dy) == (0, 0) else WOOD[1]
    return out


def paint_wood_end(rng, w, h):
    """Topo de tora: anéis concêntricos."""
    out = {}
    cx, cy = w / 2 - 0.5, h / 2 - 0.5
    for y in range(h):
        for x in range(w):
            r = math.hypot(x - cx, y - cy) + rng.random() * 0.6
            out[(x, y)] = WOOD[[2, 1, 2, 3, 1][int(r) % 5]]
    return out


def paint_brushed(rng, w, h, tones):
    """Metal escovado: linhas horizontais de tom levemente diferente, pontos de brilho e de desgaste."""
    out = {}
    row = [1 + (rng.random() < 0.35) for _ in range(h)]
    for y in range(h):
        for x in range(w):
            tone = row[y]
            r = rng.random()
            if r < 0.04:
                tone = 3
            elif r < 0.09:
                tone = 0
            elif r < 0.25:
                tone = 2 if tone == 1 else 1
            out[(x, y)] = tones[tone]
    return out


def paint_glass(rng, w, h):
    """Vidro com narcótico: líquido verde em gradiente, bolhas e um reflexo diagonal."""
    out = {}
    for y in range(h):
        for x in range(w):
            tone = 2 if y < h // 2 else 1
            if y == h - 1:
                tone = 0
            if rng.random() < 0.08:
                tone = 3                                  # bolha
            out[(x, y)] = NARCOTIC[tone]
    for i in range(0, w, 5):                              # reflexo em diagonal, repetido
        for k in range(2):
            p = (i + k, 1 + k)
            if p in out:
                out[p] = GLASS_SHINE
    return out


def paint_flat(rng, w, h, tones, speck=0.15):
    out = {}
    for y in range(h):
        for x in range(w):
            r = rng.random()
            out[(x, y)] = tones[0] if r < speck else tones[2] if r > 1 - speck else tones[1]
    return out


def paint_lens(w, h):
    out = {}
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            out[(x, y)] = GUNMETAL[0] if edge else LENS[1]
    out[(1, 1)] = LENS[2]
    out[(2, 1)] = LENS[2]
    out[(1, 2)] = LENS[2]
    out[(w - 2, h - 2)] = LENS[0]
    return out


def paint_leather(w, h):
    """Couro enrolado no cabo: tiras diagonais com a borda escura."""
    out = {}
    for y in range(h):
        for x in range(w):
            k = (x + y) % 4
            out[(x, y)] = LEATHER[0] if k == 0 else LEATHER[2] if k == 1 else LEATHER[1]
    return out


def paint_feather(w, h):
    out = {}
    for y in range(h):
        for x in range(w):
            out[(x, y)] = FEATHER[2] if y == 0 or (x + y) % 5 == 0 else FEATHER[1] if (x + 2 * y) % 3 else FEATHER[0]
    return out


def weapon_texture(seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    painters = {
        "wood": lambda w, h: paint_wood(rng, w, h),
        "wood_v": lambda w, h: paint_wood(rng, w, h, vertical=True),
        "wood_end": lambda w, h: paint_wood_end(rng, w, h),
        "steel": lambda w, h: paint_brushed(rng, w, h, STEEL),
        "gunmetal": lambda w, h: paint_brushed(rng, w, h, GUNMETAL),
        "brass": lambda w, h: paint_brushed(rng, w, h, BRASS),
        "glass": lambda w, h: paint_glass(rng, w, h),
        "lens": paint_lens,
        "feather": paint_feather,
        "string": lambda w, h: paint_flat(rng, w, h, STRING),
        "rubber": lambda w, h: paint_flat(rng, w, h, RUBBER, 0.2),
        "leather": paint_leather,
    }
    for name, (sx, sy, w, h) in SWATCHES.items():
        for (x, y), color in painters[name](w, h).items():
            img.putpixel((sx + x, sy + y), color + (255,))
    return img


# ---------------------------------------------------------------- geometria

FACES = ("north", "south", "east", "west", "up", "down")


def face_size(frm, to, face):
    """Largura e altura da face em unidades de modelo (eixos u e v da textura)."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz)}[face]


class Model:
    def __init__(self, name):
        self.name = name
        self.elements = []

    def box(self, label, frm, to, material, faces=None, rotation=None):
        """Um cubo. `faces` troca o material de faces específicas; `rotation` = (eixo, ângulo, origem)."""
        faces = faces or {}
        element = {"name": label, "from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to]}
        if rotation:
            axis, angle, origin = rotation
            element["rotation"] = {"angle": angle, "axis": axis, "origin": [round(v, 4) for v in origin]}
        element["faces"] = {}
        for i, face in enumerate(FACES):
            mat = faces.get(face, material)
            sx, sy, sw, sh = SWATCHES[mat]
            w, h = face_size(frm, to, face)
            w, h = min(w, sw), min(h, sh)
            # Desloca cada face dentro da amostra para não repetir o mesmo recorte em todo cubo.
            seed = (len(self.elements) * 7 + i * 3)
            ox = (seed % max(1, int(sw - w) + 1)) if sw > w else 0
            oy = ((seed // 3) % max(1, int(sh - h) + 1)) if sh > h else 0
            u1, v1 = (sx + ox) * TEXEL, (sy + oy) * TEXEL
            element["faces"][face] = {
                "uv": [round(u1, 4), round(v1, 4), round(u1 + w * TEXEL, 4), round(v1 + h * TEXEL, 4)],
                "texture": "#0",
            }
        self.elements.append(element)


# ---------------------------------------------------------------- display (posição na mão, GUI etc.)

def rot_matrix(rx, ry, rz):
    """Matriz de rotation [x, y, z] do JSON: R = Rx * Ry * Rz (igual ao rotationXYZ do JOML)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    Rx = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    Ry = [[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]]
    Rz = [[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]]
    return matmul(Rx, matmul(Ry, Rz))


def matmul(A, B):
    return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(M, v):
    return [sum(M[i][k] * v[k] for k in range(3)) for i in range(3)]


def euler_xyz(R):
    """Inverso de rot_matrix: ângulos [x, y, z] em graus."""
    b = math.asin(max(-1.0, min(1.0, R[0][2])))
    c = math.atan2(-R[0][1], R[0][0])
    a = math.atan2(-R[1][2], R[2][2])
    return [math.degrees(a), math.degrees(b), math.degrees(c)]


def element_corners(element):
    """Os 8 cantos de cada elemento já com a rotação do próprio elemento aplicada."""
    pts = []
    for e in element:
        f, t = e["from"], e["to"]
        corners = [[x, y, z] for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])]
        if "rotation" in e:
            r = e["rotation"]
            ang = r["angle"]
            M = rot_matrix(*(ang if r["axis"] == ax else 0 for ax in "xyz"))
            o = r["origin"]
            corners = [[o[i] + d for i, d in enumerate(apply(M, [c[k] - o[k] for k in range(3)]))]
                       for c in corners]
        pts += corners
    return pts


def centered(elements, rotation, scale):
    """translation que põe o centro da caixa da arma girada no centro do slot/quadro."""
    R = rot_matrix(*rotation)
    pts = [apply(R, [(p[i] - 8) * scale for i in range(3)]) for p in element_corners(elements)]
    return [-(min(p[i] for p in pts) + max(p[i] for p in pts)) / 2 for i in range(3)]


def anchored(rotation, scale, grip, target):
    """translation que leva o ponto `grip` do modelo para `target` (unidades de 1/16 de bloco)."""
    R = rot_matrix(*rotation)
    g = apply(R, [(grip[i] - 8) * scale for i in range(3)])
    return [target[i] - g[i] for i in range(3)]


def left_of(rotation):
    """Rotação do JSON para a mão esquerda.

    O jogo espelha a entrada da mão esquerda (nega os ângulos Y e Z e a translação X). Para
    que a arma continue com o cano para a frente na outra mão, a entrada esquerda é a direita
    composta com meia-volta em Y em torno do centro (a arma é simétrica no plano z = 8).
    """
    R = matmul(rot_matrix(*rotation), rot_matrix(0, 180, 0))
    return euler_xyz(R)


def tidy(values, step=0.01):
    out = []
    for v in values:
        v = round(round(v / step) * step, 4)
        out.append(0.0 if v == 0 else v)
    return out


def display(elements, grip, *, gui_scale, hand_scale, fp_scale, ground_scale, fixed_scale,
            tp_target, fp_target, fp_yaw, gui_rot=(12, -18, 40)):
    d = {}
    # Terceira pessoa: cano para a frente da mão, topo para cima, empunhadura no punho.
    tp_rot = [0, 90, 90]
    tp_t = anchored(tp_rot, hand_scale, grip, tp_target)
    d["thirdperson_righthand"] = {"rotation": tp_rot, "translation": tp_t, "scale": [hand_scale] * 3}
    d["thirdperson_lefthand"] = {"rotation": left_of(tp_rot), "translation": tp_t, "scale": [hand_scale] * 3}
    # Primeira pessoa: cano para -Z da câmera (para longe do jogador), virado de leve para a mira.
    fp_rot = [0, 90 + fp_yaw, 3]
    fp_t = anchored(fp_rot, fp_scale, grip, fp_target)
    d["firstperson_righthand"] = {"rotation": fp_rot, "translation": fp_t, "scale": [fp_scale] * 3}
    d["firstperson_lefthand"] = {"rotation": left_of(fp_rot), "translation": fp_t, "scale": [fp_scale] * 3}
    # GUI: de lado, cano para cima à direita como as armas vanilla, com um pouco de volume.
    gui_rot = list(gui_rot)
    d["gui"] = {"rotation": gui_rot, "translation": centered(elements, gui_rot, gui_scale), "scale": [gui_scale] * 3}
    ground_rot = [0, 0, 0]
    d["ground"] = {"rotation": ground_rot, "translation": centered(elements, ground_rot, ground_scale),
                   "scale": [ground_scale] * 3}
    fixed_rot = [0, 180, 40]   # moldura: mostra a mesma lateral da GUI
    d["fixed"] = {"rotation": fixed_rot, "translation": centered(elements, fixed_rot, fixed_scale),
                  "scale": [fixed_scale] * 3}
    d["head"] = {"rotation": [0, 90, 0], "translation": centered(elements, [0, 90, 0], 0.6), "scale": [0.6] * 3}
    for entry in d.values():
        entry["rotation"] = tidy(entry["rotation"], 0.5)
        entry["translation"] = tidy(entry["translation"], 0.05)
        entry["scale"] = tidy(entry["scale"], 0.01)
    return d


# ---------------------------------------------------------------- rifle

RIFLE_GRIP = (4.2, 6.0, 8)


def rifle():
    m = Model("tranq_rifle")
    # Coronha de madeira com soleira de borracha e apoio de rosto.
    m.box("butt_pad", (-7.75, 4.25, 6.9), (-7, 9.75, 9.1), "rubber")
    m.box("butt", (-7, 4.5, 7), (-2, 9.5, 9), "wood", {"west": "wood_end"})
    m.box("comb", (-6, 9.5, 7.25), (-1, 10.25, 8.75), "wood")
    m.box("butt_toe", (-2, 5.4, 7.15), (1.5, 6.9, 8.85), "wood", rotation=("z", 22.5, (-2, 5.4, 8)))
    m.box("wrist", (-2, 6.25, 7.25), (3.5, 9.5, 8.75), "wood")
    m.box("pistol_grip", (2.5, 3.5, 7.4), (4.5, 7, 8.6), "wood", rotation=("z", -22.5, (3.5, 7, 8)))
    # Caixa da culatra em aço oxidado, com a câmara de vidro e o dardo verde à vista.
    m.box("receiver", (3.5, 7, 7), (11, 10, 9), "gunmetal")
    m.box("chamber", (5.5, 7.75, 6.75), (9.5, 9.5, 9.25), "glass")
    m.box("chamber_cap_rear", (5, 7.6, 6.85), (5.5, 9.65, 9.15), "brass")
    m.box("chamber_cap_front", (9.5, 7.6, 6.85), (10, 9.65, 9.15), "brass")
    # Guarda-mato e gatilho.
    m.box("guard_bottom", (4.25, 5.75, 7.6), (7.75, 6.25, 8.4), "steel")
    m.box("guard_front", (7.25, 6.25, 7.6), (7.75, 7, 8.4), "steel")
    m.box("trigger", (5.75, 6.25, 7.75), (6.25, 7, 8.25), "steel")
    # Ferrolho saindo pela direita.
    m.box("bolt", (8.9, 9.25, 9), (9.4, 9.75, 10.25), "steel")
    m.box("bolt_knob", (8.65, 9, 10.25), (9.65, 10, 11), "gunmetal")
    # Guarda-mão de madeira, cano comprido, abraçadeira de latão e boca do cano.
    m.box("forend", (11, 7.25, 7.25), (19, 9.25, 8.75), "wood", {"east": "wood_end"})
    m.box("barrel", (11, 9.25, 7.4), (25, 10.5, 8.6), "steel")
    m.box("barrel_band", (16.5, 7, 7.15), (17.25, 10.75, 8.85), "brass")
    m.box("muzzle", (24.5, 9, 7.2), (26, 10.75, 8.8), "gunmetal")
    m.box("front_sight", (24.75, 10.75, 7.85), (25.25, 11.5, 8.15), "steel")
    # Luneta sobre dois apoios, com lentes nas pontas.
    m.box("scope_mount_rear", (5.5, 10, 7.6), (6.5, 11, 8.4), "steel")
    m.box("scope_mount_front", (9.75, 10, 7.6), (10.75, 11, 8.4), "steel")
    m.box("scope_tube", (5, 11, 7.35), (11.5, 12.25, 8.65), "gunmetal")
    m.box("scope_eyepiece", (3.75, 10.75, 7.1), (5, 12.5, 8.9), "gunmetal", {"west": "lens"})
    m.box("scope_objective", (11.5, 10.6, 7), (13.5, 12.65, 9), "gunmetal", {"east": "lens"})
    m.box("scope_turret", (7.75, 12.25, 7.65), (8.75, 12.85, 8.35), "brass")
    return m


# ---------------------------------------------------------------- besta

CROSSBOW_GRIP = (3.8, 5.0, 8)


def crossbow():
    m = Model("tranq_crossbow")
    # Coronha de madeira com chapa na soleira e cabo de couro.
    m.box("butt_plate", (-5.5, 4.25, 7), (-5, 9, 9), "gunmetal")
    m.box("butt", (-5, 4.5, 7.1), (-1, 8.75, 8.9), "wood")
    m.box("stock", (-1, 6, 7.25), (6.5, 8.75, 8.75), "wood")
    m.box("grip", (2.75, 3, 7.4), (4.75, 6.5, 8.6), "leather", rotation=("z", -22.5, (3.75, 6.5, 8)))
    # Trilho de madeira com as guias de aço onde o dardo corre.
    m.box("tiller", (6.5, 7, 7.2), (16, 9, 8.8), "wood")
    m.box("rail_left", (7.5, 9, 7.2), (16, 9.3, 7.6), "steel")
    m.box("rail_right", (7.5, 9, 8.4), (16, 9.3, 8.8), "steel")
    m.box("lock_left", (8.5, 7.25, 6.95), (12, 9, 7.2), "steel")
    m.box("lock_right", (8.5, 7.25, 8.8), (12, 9, 9.05), "steel")
    m.box("trigger_lever", (8, 5.75, 7.75), (11, 6.25, 8.25), "steel")
    m.box("trigger_hanger", (10.5, 6.25, 7.75), (11, 7, 8.25), "steel")
    # Arco transversal: peça central de aço e dois braços de madeira com ponteiras de ferro,
    # puxados para trás.
    m.box("prod_center", (16, 7, 6), (17.25, 9.25, 10), "gunmetal")
    limb_y = (7.9, 9.15)
    root_n, root_s = (16.6, 8.5, 6), (16.6, 8.5, 10)
    m.box("limb_left", (16.1, limb_y[0], -0.5), (17.1, limb_y[1], 6), "wood_v",
          {"north": "wood_end", "south": "wood_end"}, rotation=("y", 22.5, root_n))
    m.box("limb_left_tip", (16, limb_y[0] - 0.1, -0.75), (17.2, limb_y[1] + 0.1, 0.5), "steel",
          rotation=("y", 22.5, root_n))
    m.box("limb_right", (16.1, limb_y[0], 10), (17.1, limb_y[1], 16.5), "wood_v",
          {"north": "wood_end", "south": "wood_end"}, rotation=("y", -22.5, root_s))
    m.box("limb_right_tip", (16, limb_y[0] - 0.1, 15.5), (17.2, limb_y[1] + 0.1, 16.75), "steel",
          rotation=("y", -22.5, root_s))
    # Corda: das costas da ponteira até a noz, a 22.5 graus (o formato só gira em passos de 22.5).
    s = math.sin(math.radians(22.5))
    c = math.cos(math.radians(22.5))
    tip_dx, tip_dz = -0.5, -6.25                       # costas da ponteira, relativo à raiz do braço
    tip_x = root_n[0] + tip_dx * c + tip_dz * s
    tip_z = root_n[2] - tip_dx * s + tip_dz * c
    dz = 8 - tip_z
    latch_x = tip_x - dz * math.tan(math.radians(22.5))
    length = dz / c
    sy = (8.95, 9.25)
    m.box("string_left", (latch_x - 0.15, sy[0], 8 - length), (latch_x + 0.15, sy[1], 8), "string",
          rotation=("y", -22.5, (latch_x, 9.1, 8)))
    m.box("string_right", (latch_x - 0.15, sy[0], 8), (latch_x + 0.15, sy[1], 8 + length), "string",
          rotation=("y", 22.5, (latch_x, 9.1, 8)))
    m.box("nut", (latch_x - 0.75, 9, 7.6), (latch_x - 0.1, 10.1, 8.4), "brass")
    # Dardo engatado: penas, haste, corpo de vidro com narcótico, colar e agulha.
    x0 = latch_x
    m.box("dart_fletch", (x0, 9.3, 7.7), (x0 + 1.5, 10.7, 8.3), "feather")
    m.box("dart_shaft", (x0 + 1.5, 9.45, 7.85), (x0 + 2.25, 9.85, 8.15), "steel")
    m.box("dart_body", (x0 + 2.25, 9.3, 7.65), (x0 + 6.25, 10.2, 8.35), "glass")
    m.box("dart_collar", (x0 + 6.25, 9.4, 7.72), (x0 + 6.75, 10.1, 8.28), "brass")
    m.box("dart_needle", (x0 + 6.75, 9.65, 7.92), (x0 + 9.5, 9.85, 8.08), "steel")
    # Estribo de aço na frente, para firmar o pé ao armar.
    m.box("stirrup_left", (17.25, 6.5, 7.0), (20, 7, 7.4), "steel")
    m.box("stirrup_right", (17.25, 6.5, 8.6), (20, 7, 9.0), "steel")
    m.box("stirrup_front", (20, 6.5, 7.0), (20.5, 7, 9.0), "steel")
    return m


# ---------------------------------------------------------------- dardo (sprite 16x16)

DART_METAL = [(84, 90, 100), (150, 156, 166), (214, 220, 228)]


# (distância da haste, a) de cada pixel de pena; a e b têm a mesma paridade na diagonal.
FLETCH = {(1, -14), (1, -12), (1, -10), (2, -13), (2, -11), (3, -12)}


def dart_sprite():
    """Dardo na diagonal, penas embaixo à esquerda e agulha em cima à direita.

    Coordenadas diagonais: a = x - y cresce rumo à ponta, b = x + y é a "linha" da diagonal
    (b menor = lado de cima/esquerda, iluminado).
    """
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            a, b = x - y - 2, x + y    # deslocado 1 px para cima e para a direita
            color = None
            if 8 <= a <= 13 and b == 15:
                color = DART_METAL[2] if a >= 11 else DART_METAL[1]            # agulha
            elif 8 <= a <= 9 and b == 16:
                color = DART_METAL[0]
            elif 5 <= a <= 7 and 14 <= b <= 16:
                color = BRASS[3] if b == 14 else BRASS[2] if b == 15 else BRASS[0]  # colar da agulha
            elif -5 <= a <= 4 and 13 <= b <= 17:
                if b in (13, 17) or a in (-5, 4):
                    color = GLASS_EDGE                                          # borda do vidro
                elif b == 14:
                    color = GLASS_SHINE if a in (1, 2, -2) else NARCOTIC[3]
                elif b == 15:
                    color = NARCOTIC[2]
                else:
                    color = NARCOTIC[1] if a % 3 else NARCOTIC[0]
            elif -7 <= a <= -6 and 14 <= b <= 16:
                color = BRASS[2] if b == 14 else BRASS[1] if b == 15 else BRASS[0]  # colar de trás
            elif a == -8 and b == 15:
                color = DART_METAL[1]                                           # haste
            elif -13 <= a <= -9 and b == 15:
                color = DART_METAL[0]                                           # haste entre as penas
            elif a <= -10 and (abs(b - 15), a) in FLETCH:
                # Duas penas, uma de cada lado da haste, mais largas atrás e com a ponta clara.
                d = abs(b - 15)
                color = FEATHER[2] if d == 3 or (d == 2 and a == -13) else FEATHER[1] if b < 15 else FEATHER[0]
            if color:
                img.putpixel((x, y), color + (255,))
    return img


# ---------------------------------------------------------------- saída e checagens

def check(model):
    """Limites do formato Java: coordenadas em -16..32 e rotação de elemento em passos de 22.5."""
    for e in model.elements:
        for v in e["from"] + e["to"]:
            assert -16 <= v <= 32, f"{model.name}/{e['name']}: coordenada {v} fora de -16..32"
        if "rotation" in e:
            assert e["rotation"]["angle"] in (-45, -22.5, 0, 22.5, 45), e
            assert e["rotation"]["axis"] in "xyz", e
    # Faces coplanares de mesmo sentido que se sobrepõem piscam (z-fighting) no jogo.
    plain = [e for e in model.elements if "rotation" not in e]
    for i, p in enumerate(plain):
        for q in plain[i + 1:]:
            for axis in range(3):
                o = [k for k in range(3) if k != axis]
                overlap = all(min(p["to"][k], q["to"][k]) - max(p["from"][k], q["from"][k]) > 1e-6 for k in o)
                if not overlap:
                    continue
                for side in ("from", "to"):
                    if abs(p[side][axis] - q[side][axis]) < 1e-6:
                        raise AssertionError(f"{model.name}: {p['name']} e {q['name']} piscam no eixo {axis}")


def write_model(model, display_):
    check(model)
    data = {
        "credit": "Ice Age Survival — gerado por tools/gen_weapons.py",
        "texture_size": [TEX, TEX],
        "textures": {"0": f"iceagesurvival:item/{model.name}", "particle": f"iceagesurvival:item/{model.name}"},
        "elements": model.elements,
        "gui_light": "side",
        "display": display_,
    }
    (ITEM_MODELS / f"{model.name}.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def main():
    ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)
    ITEM_MODELS.mkdir(parents=True, exist_ok=True)

    r = rifle()
    write_model(r, display(r.elements, RIFLE_GRIP, gui_scale=0.55, hand_scale=0.75, fp_scale=0.7,
                           ground_scale=0.4, fixed_scale=0.65, tp_target=(0, 1.5, 0.5),
                           fp_target=(2, 0, -3), fp_yaw=4))
    weapon_texture(11).save(ITEM_TEXTURES / "tranq_rifle.png")

    b = crossbow()
    write_model(b, display(b.elements, CROSSBOW_GRIP, gui_scale=0.6, hand_scale=0.8, fp_scale=0.75,
                           ground_scale=0.45, fixed_scale=0.7, tp_target=(0, 1.5, 0.5),
                           fp_target=(2, 0, -3), fp_yaw=4, gui_rot=(55, 42, 0)))
    weapon_texture(23).save(ITEM_TEXTURES / "tranq_crossbow.png")

    (ITEM_MODELS / "tranq_dart.json").write_text(json.dumps(
        {"parent": "minecraft:item/generated", "textures": {"layer0": "iceagesurvival:item/tranq_dart"}},
        indent=2) + "\n")
    dart_sprite().save(ITEM_TEXTURES / "tranq_dart.png")

    for name in ("tranq_rifle.json", "tranq_crossbow.json", "tranq_dart.json",
                 "tranq_rifle.png", "tranq_crossbow.png", "tranq_dart.png"):
        print(name)


if __name__ == "__main__":
    main()
