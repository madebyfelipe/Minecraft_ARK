#!/usr/bin/env python3
"""Gera os assets do Smilodon (geometria Bedrock, textura e animações) direto em
src/main/resources, nos caminhos que o GeckoLib espera.

Enquanto o modelo for mantido por aqui, este script é a fonte da verdade. Se um
arquivo for editado à mão no Blockbench, rodar o script de novo SOBRESCREVE a edição.

Convenções da geometria Bedrock: unidades de 1/16 de bloco, Y para cima,
a criatura olha para -Z, origem no chão entre as patas.
"""

import json
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
GEO = ASSETS / "geo" / "entity" / "smilodon.geo.json"
TEXTURE = ASSETS / "textures" / "entity" / "smilodon.png"
ANIMATIONS = ASSETS / "animations" / "entity" / "smilodon.animation.json"
TEX_W, TEX_H = 128, 64

FUR = (196, 150, 84)
FUR_DARK = (158, 112, 58)
BELLY = (226, 200, 150)
IVORY = (240, 234, 214)
NOSE = (60, 40, 34)
EYE = (232, 176, 40)
PUPIL = (24, 18, 14)
EAR_INNER = (120, 78, 60)

# (osso, pai, pivô, [cubos]); cubo = (nome, origem, tamanho, espelhado)
BONES = [
    ("root", None, (0, 0, 0), []),
    ("body", "root", (0, 14, 0), [
        ("chest", (-5, 9, -12), (10, 10, 12), False),
        ("hips", (-4, 9, 0), (8, 8, 11), False),
        ("hump", (-4, 19, -11), (8, 2, 8), False),
    ]),
    ("neck", "body", (0, 16, -12), [
        ("neck", (-3, 12, -16), (6, 7, 5), False),
    ]),
    ("head", "neck", (0, 17, -15), [
        ("skull", (-4, 13, -22), (8, 7, 7), False),
        ("snout", (-3, 13, -26), (6, 5, 4), False),
        ("ear", (2, 20, -18), (2, 2, 1), False),
        ("ear", (-4, 20, -18), (2, 2, 1), True),
        ("saber", (2, 8, -25), (1, 5, 1), False),
        ("saber", (-3, 8, -25), (1, 5, 1), True),
    ]),
    ("jaw", "head", (0, 13, -19), [
        ("jaw", (-2, 11, -25), (4, 2, 6), False),
    ]),
    ("leg_front_left", "body", (4, 11, -8), [("front_leg", (2, 0, -10), (4, 11, 4), False)]),
    ("leg_front_right", "body", (-4, 11, -8), [("front_leg", (-6, 0, -10), (4, 11, 4), True)]),
    ("leg_hind_left", "body", (3, 10, 8), [("hind_leg", (1, 0, 6), (4, 10, 4), False)]),
    ("leg_hind_right", "body", (-3, 10, 8), [("hind_leg", (-5, 0, 6), (4, 10, 4), True)]),
    ("tail", "body", (0, 15, 11), [
        ("tail", (-1, 14, 11), (2, 2, 4), False),
    ]),
]


def pack_uvs():
    """Empacota em prateleiras a região de box UV de cada cubo distinto (por nome)."""
    sizes = {}
    for _, _, _, cubes in BONES:
        for name, _, size, _ in cubes:
            sizes[name] = size
    # Maiores primeiro, para as prateleiras ficarem compactas.
    order = sorted(sizes, key=lambda n: -(sizes[n][2] + sizes[n][1]))
    uvs, x, y, shelf = {}, 0, 0, 0
    for name in order:
        w, h, d = sizes[name]
        rw, rh = 2 * (w + d), d + h
        if x + rw > TEX_W:
            x, y, shelf = 0, y + shelf, 0
        uvs[name] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    if y + shelf > TEX_H:
        raise SystemExit(f"UVs não cabem em {TEX_W}x{TEX_H}")
    return uvs, sizes


def faces(u, v, w, h, d):
    """Retângulos (x0, y0, x1, y1) de cada face no layout de box UV."""
    return {
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + d + 2 * w, v + d),
        "side_a": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "side_b": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def fill(img, rect, color, rng, noise=9):
    x0, y0, x1, y1 = rect
    for x in range(x0, x1):
        for y in range(y0, y1):
            delta = rng.randint(-noise, noise)
            img.putpixel((x, y), tuple(max(0, min(255, c + delta)) for c in color) + (255,))


def paint(uvs, sizes):
    rng = random.Random(1)
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    for name, (u, v) in uvs.items():
        w, h, d = sizes[name]
        f = faces(u, v, w, h, d)
        base = IVORY if name == "saber" else FUR
        for key, rect in f.items():
            color = base
            if name != "saber":
                if key == "up":
                    color = FUR_DARK
                elif key == "down":
                    color = BELLY
            fill(img, rect, color, rng, noise=4 if name == "saber" else 9)

        x0, y0, x1, y1 = f["front"]
        if name == "skull":
            # Olhos na segunda fileira; o focinho cobre a parte de baixo desta face.
            for ex in (x0 + 1, x1 - 2):
                img.putpixel((ex, y0 + 1), EYE + (255,))
            img.putpixel((x0 + 2, y0 + 1), PUPIL + (255,))
            img.putpixel((x1 - 3, y0 + 1), PUPIL + (255,))
        elif name == "snout":
            for nx in (x0 + 2, x0 + 3):
                img.putpixel((nx, y0), NOSE + (255,))
                img.putpixel((nx, y0 + 1), NOSE + (255,))
            fill(img, (x0, y1 - 2, x1, y1), BELLY, rng)
        elif name == "ear":
            fill(img, f["front"], EAR_INNER, rng, noise=5)
        elif name in ("front_leg", "hind_leg"):
            # Patas: faixa escura embaixo em todas as faces laterais.
            for key in ("side_a", "front", "side_b", "back"):
                rx0, _, rx1, ry1 = f[key]
                fill(img, (rx0, ry1 - 2, rx1, ry1), FUR_DARK, rng)
        elif name in ("chest", "hips"):
            # Barriga clara subindo pelas laterais.
            for key in ("side_a", "side_b"):
                rx0, _, rx1, ry1 = f[key]
                fill(img, (rx0, ry1 - 2, rx1, ry1), BELLY, rng)
    return img


def geometry(uvs):
    bones = []
    for name, parent, pivot, cubes in BONES:
        bone = {"name": name, "pivot": list(pivot)}
        if parent:
            bone["parent"] = parent
        if cubes:
            bone["cubes"] = []
            for cube_name, origin, size, mirror in cubes:
                cube = {"origin": list(origin), "size": list(size), "uv": list(uvs[cube_name])}
                if mirror:
                    cube["mirror"] = True
                bone["cubes"].append(cube)
        bones.append(bone)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.smilodon",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }],
    }


def swing(amplitude, length, phase=0.0):
    """Rotação em X que vai e volta uma vez ao longo da animação."""
    a = amplitude if phase == 0.0 else -amplitude
    return {"rotation": {"0.0": [a, 0, 0], str(length / 2): [-a, 0, 0], str(length): [a, 0, 0]}}


def animations():
    walk = 0.8
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.smilodon.idle": {
                "loop": True,
                "animation_length": 3.0,
                "bones": {
                    "neck": {"rotation": {"0.0": [0, 0, 0], "1.5": [3, 0, 0], "3.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, -10, 0], "1.5": [0, 10, 0], "3.0": [0, -10, 0]}},
                },
            },
            "animation.smilodon.walk": {
                "loop": True,
                "animation_length": walk,
                "bones": {
                    # Trote: patas em diagonal se movem juntas.
                    "leg_front_left": swing(28, walk),
                    "leg_hind_right": swing(28, walk),
                    "leg_front_right": swing(28, walk, phase=0.5),
                    "leg_hind_left": swing(28, walk, phase=0.5),
                    "body": {"position": {"0.0": [0, 0, 0], "0.2": [0, 0.5, 0], "0.4": [0, 0, 0],
                                          "0.6": [0, 0.5, 0], "0.8": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, -6, 0], "0.4": [0, 6, 0], "0.8": [0, -6, 0]}},
                },
            },
            "animation.smilodon.bite": {
                "animation_length": 0.4,
                "bones": {
                    "neck": {"rotation": {"0.0": [0, 0, 0], "0.1": [-18, 0, 0], "0.25": [16, 0, 0], "0.4": [0, 0, 0]}},
                    "jaw": {"rotation": {"0.0": [0, 0, 0], "0.1": [32, 0, 0], "0.25": [0, 0, 0]}},
                },
            },
            "animation.smilodon.unconscious": {
                "loop": True,
                "animation_length": 3.0,
                "bones": {
                    # Tombado de lado; o deslocamento compensa a rotação em torno da origem.
                    "root": {"rotation": [0, 0, 90], "position": [0, 5, 0]},
                    "body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.03, 1, 1], "3.0": [1, 1, 1]}},
                },
            },
        },
    }


def main():
    uvs, sizes = pack_uvs()
    for path in (GEO, TEXTURE, ANIMATIONS):
        path.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps(geometry(uvs), indent=2) + "\n")
    paint(uvs, sizes).save(TEXTURE)
    ANIMATIONS.write_text(json.dumps(animations(), indent=2) + "\n")
    print(f"Smilodon: {len(BONES)} ossos, {sum(len(b[3]) for b in BONES)} cubos, {len(animations()['animations'])} animações")


if __name__ == "__main__":
    main()
