#!/usr/bin/env python3
"""Gera os assets do Smilodon. Ver modelgen.py para as convenções."""

from modelgen import Bone, Cube, Gait, Palette, build

FUR = (196, 150, 84)
FUR_DARK = (158, 112, 58)
BELLY = (226, 200, 150)
NOSE = (60, 40, 34)
EYE = (232, 176, 40)
PUPIL = (24, 18, 14)
EAR_INNER = (120, 78, 60)

BONES = [
    Bone("root", None, (0, 0, 0)),
    Bone("body", "root", (0, 14, 0), [
        Cube("chest", (-5, 9, -12), (10, 10, 12)),
        Cube("hips", (-4, 9, 0), (8, 8, 11)),
        Cube("hump", (-4, 19, -11), (8, 2, 8)),
    ]),
    Bone("neck", "body", (0, 16, -12), [Cube("neck", (-3, 12, -16), (6, 7, 5))]),
    Bone("head", "neck", (0, 17, -15), [
        Cube("skull", (-4, 13, -22), (8, 7, 7)),
        Cube("snout", (-3, 13, -26), (6, 5, 4)),
        Cube("ear", (2, 20, -18), (2, 2, 1)),
        Cube("ear", (-4, 20, -18), (2, 2, 1), mirror=True),
        Cube("saber", (2, 8, -25), (1, 5, 1)),
        Cube("saber", (-3, 8, -25), (1, 5, 1), mirror=True),
    ]),
    Bone("jaw", "head", (0, 13, -19), [Cube("jaw", (-2, 11, -25), (4, 2, 6))]),
    Bone("leg_front_left", "body", (4, 11, -8), [Cube("front_leg", (2, 0, -10), (4, 11, 4))]),
    Bone("leg_front_right", "body", (-4, 11, -8), [Cube("front_leg", (-6, 0, -10), (4, 11, 4), mirror=True)]),
    Bone("leg_hind_left", "body", (3, 10, 8), [Cube("hind_leg", (1, 0, 6), (4, 10, 4))]),
    Bone("leg_hind_right", "body", (-3, 10, 8), [Cube("hind_leg", (-5, 0, 6), (4, 10, 4), mirror=True)]),
    Bone("tail", "body", (0, 15, 11), [Cube("tail", (-1, 14, 11), (2, 2, 4))]),
]


def details(texture, name, faces):
    x0, y0, x1, y1 = faces["front"]
    if name == "skull":
        # Olhos na segunda fileira; o focinho cobre a parte de baixo desta face.
        texture.pixel(x0 + 1, y0 + 1, EYE)
        texture.pixel(x1 - 2, y0 + 1, EYE)
        texture.pixel(x0 + 2, y0 + 1, PUPIL)
        texture.pixel(x1 - 3, y0 + 1, PUPIL)
    elif name == "snout":
        for nx in (x0 + 2, x0 + 3):
            texture.pixel(nx, y0, NOSE)
            texture.pixel(nx, y0 + 1, NOSE)
        texture.fill((x0, y1 - 2, x1, y1), BELLY)
    elif name == "ear":
        texture.fill(faces["front"], EAR_INNER, noise=5)
    elif name in ("front_leg", "hind_leg"):
        texture.band_at_bottom(faces, 2, FUR_DARK)
    elif name in ("chest", "hips"):
        for key in ("side_a", "side_b"):
            rx0, _, rx1, ry1 = faces[key]
            texture.fill((rx0, ry1 - 2, rx1, ry1), BELLY)


if __name__ == "__main__":
    build(
        "smilodon", BONES,
        Palette(fur=FUR, top=FUR_DARK, belly=BELLY, overrides={"saber": (240, 234, 214)}),
        Gait(legs_a=("leg_front_left", "leg_hind_right"), legs_b=("leg_front_right", "leg_hind_left"),
             head="neck", tail="tail", jaw="jaw", fallen_lift=5),
        tex_size=(128, 64), details=details)
