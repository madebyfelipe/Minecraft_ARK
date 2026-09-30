#!/usr/bin/env python3
"""Gera os assets do lobo-terrível. Ver modelgen.py para as convenções."""

from modelgen import Bone, Cube, Gait, Palette, build

FUR = (118, 118, 124)
FUR_DARK = (74, 74, 82)
BELLY = (190, 188, 184)
NOSE = (26, 22, 24)
EYE = (214, 178, 60)
EAR_INNER = (150, 110, 104)

BONES = [
    Bone("root", None, (0, 0, 0)),
    Bone("body", "root", (0, 10, 0), [
        Cube("chest", (-4, 6, -8), (8, 8, 7)),
        Cube("hips", (-3, 7, -1), (6, 6, 8)),
    ]),
    Bone("head", "body", (0, 12, -8), [
        Cube("skull", (-3, 9, -13), (6, 6, 5)),
        Cube("snout", (-2, 9, -17), (4, 3, 4)),
        Cube("ear", (1, 15, -11), (2, 2, 1)),
        Cube("ear", (-3, 15, -11), (2, 2, 1), mirror=True),
    ]),
    Bone("leg_front_left", "body", (2, 7, -5), [Cube("leg", (1, 0, -6), (2, 7, 2))]),
    Bone("leg_front_right", "body", (-2, 7, -5), [Cube("leg", (-3, 0, -6), (2, 7, 2), mirror=True)]),
    Bone("leg_hind_left", "body", (2, 7, 5), [Cube("leg", (1, 0, 4), (2, 7, 2))]),
    Bone("leg_hind_right", "body", (-2, 7, 5), [Cube("leg", (-3, 0, 4), (2, 7, 2), mirror=True)]),
    Bone("tail", "body", (0, 12, 7), [Cube("tail", (-1, 10, 7), (2, 2, 6))]),
]


def details(texture, name, faces):
    x0, y0, x1, y1 = faces["front"]
    if name == "skull":
        texture.pixel(x0 + 1, y0 + 1, EYE)
        texture.pixel(x1 - 2, y0 + 1, EYE)
    elif name == "snout":
        texture.pixel(x0 + 1, y0, NOSE)
        texture.pixel(x0 + 2, y0, NOSE)
        texture.fill((x0, y1 - 1, x1, y1), BELLY)
    elif name == "ear":
        texture.fill(faces["front"], EAR_INNER, noise=5)
    elif name == "tail":
        # Ponta da cauda clara (a face de trás do cubo).
        texture.fill(faces["back"], BELLY)


if __name__ == "__main__":
    build(
        "dire_wolf", BONES,
        Palette(fur=FUR, top=FUR_DARK, belly=BELLY),
        Gait(legs_a=("leg_front_left", "leg_hind_right"), legs_b=("leg_front_right", "leg_hind_left"),
             head="head", tail="tail", leg_swing=32.0, walk_length=0.6, fallen_lift=4),
        tex_size=(64, 64), details=details, seed=3)
