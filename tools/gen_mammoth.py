#!/usr/bin/env python3
"""Gera os assets do mamute-lanoso. Ver modelgen.py para as convenções."""

from modelgen import Bone, Cube, Gait, Palette, build

FUR = (96, 64, 42)
FUR_DARK = (70, 44, 28)
BELLY = (120, 86, 60)
IVORY = (236, 228, 204)
EYE = (20, 14, 10)
EAR_INNER = (128, 92, 74)
TOE = (60, 48, 40)

BONES = [
    Bone("root", None, (0, 0, 0)),
    Bone("body", "root", (0, 19, 0), [
        Cube("torso", (-7, 12, -10), (14, 14, 22)),
        Cube("hump", (-5, 26, -9), (10, 3, 9)),
    ]),
    Bone("head", "body", (0, 22, -10), [
        Cube("skull", (-5, 14, -18), (10, 12, 8)),
        Cube("dome", (-4, 26, -17), (8, 2, 6)),
        Cube("ear", (5, 17, -14), (1, 7, 4)),
        Cube("ear", (-6, 17, -14), (1, 7, 4), mirror=True),
        Cube("tusk_base", (3, 9, -20), (2, 5, 2)),
        Cube("tusk_base", (-5, 9, -20), (2, 5, 2), mirror=True),
        Cube("tusk", (3, 7, -27), (2, 2, 8)),
        Cube("tusk", (-5, 7, -27), (2, 2, 8), mirror=True),
    ]),
    Bone("trunk", "head", (0, 15, -18), [
        Cube("trunk_upper", (-2, 6, -21), (4, 9, 4)),
        Cube("trunk_lower", (-1, 0, -20), (2, 6, 2)),
    ]),
    Bone("leg_front_left", "body", (5, 12, -6), [Cube("leg", (3, 0, -9), (5, 12, 5))]),
    Bone("leg_front_right", "body", (-5, 12, -6), [Cube("leg", (-8, 0, -9), (5, 12, 5), mirror=True)]),
    Bone("leg_hind_left", "body", (5, 12, 8), [Cube("leg", (3, 0, 6), (5, 12, 5))]),
    Bone("leg_hind_right", "body", (-5, 12, 8), [Cube("leg", (-8, 0, 6), (5, 12, 5), mirror=True)]),
    Bone("tail", "body", (0, 24, 12), [Cube("tail", (-1, 15, 12), (2, 9, 2))]),
]


def details(texture, name, faces):
    x0, y0, x1, y1 = faces["front"]
    if name == "skull":
        # Olhos pequenos nas laterais da testa; a tromba cobre o centro de baixo.
        texture.pixel(x0 + 1, y0 + 3, EYE)
        texture.pixel(x1 - 2, y0 + 3, EYE)
    elif name == "ear":
        # A face larga da orelha é lateral (o cubo tem só 1 de largura).
        texture.fill(faces["side_a"], EAR_INNER, noise=5)
    elif name == "leg":
        texture.band_at_bottom(faces, 2, TOE)
    elif name == "torso":
        # Pelagem longa: franja mais escura descendo pelas laterais.
        for key in ("side_a", "side_b"):
            rx0, ry0, rx1, _ = faces[key]
            texture.fill((rx0, ry0, rx1, ry0 + 4), FUR_DARK)


if __name__ == "__main__":
    build(
        "mammoth", BONES,
        Palette(fur=FUR, top=FUR_DARK, belly=BELLY, overrides={"tusk": IVORY, "tusk_base": IVORY}),
        Gait(legs_a=("leg_front_left", "leg_hind_right"), legs_b=("leg_front_right", "leg_hind_left"),
             head="head", tail="tail", leg_swing=18.0, walk_length=1.2, fallen_lift=7),
        tex_size=(128, 128), details=details, seed=2)
