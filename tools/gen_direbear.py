#!/usr/bin/env python3
"""Gera os assets originais do Direbear. Ver modelgen.py para as convenções."""

from modelgen import Bone, Cube, Gait, Palette, build

FUR = (104, 68, 40)
FUR_DARK = (66, 42, 28)
BELLY = (166, 126, 82)
MUZZLE = (126, 90, 58)
NOSE = (36, 26, 22)
EYE = (224, 174, 58)
PUPIL = (20, 14, 10)
EAR_INNER = (118, 76, 58)
CLAW = (42, 32, 28)

BONES = [
    Bone("root", None, (0, 0, 0)),
    Bone("body", "root", (0, 16, 0), [
        Cube("torso", (-7, 9, -10), (14, 12, 20)),
        Cube("shoulders", (-8, 15, -11), (16, 9, 9)),
        Cube("hump", (-6, 21, -7), (12, 4, 11)),
        Cube("rump", (-6, 11, 7), (12, 9, 7)),
    ]),
    Bone("neck", "body", (0, 19, -10), [
        Cube("neck", (-5, 14, -15), (10, 9, 7)),
    ]),
    Bone("head", "neck", (0, 19, -14), [
        Cube("skull", (-6, 15, -21), (12, 9, 8)),
        Cube("muzzle", (-5, 14, -26), (10, 6, 6)),
        Cube("ear", (4, 22, -18), (2, 3, 2)),
        Cube("ear", (-6, 22, -18), (2, 3, 2), mirror=True),
    ]),
    Bone("jaw", "head", (0, 15, -19), [
        Cube("jaw", (-4, 13, -26), (8, 2, 7)),
    ]),
    Bone("leg_front_left", "body", (6, 12, -7), [
        Cube("foreleg", (3, 0, -10), (5, 12, 6)),
        Cube("forepaw", (2, 0, -13), (6, 3, 8)),
    ]),
    Bone("leg_front_right", "body", (-6, 12, -7), [
        Cube("foreleg", (-8, 0, -10), (5, 12, 6), mirror=True),
        Cube("forepaw", (-8, 0, -13), (6, 3, 8), mirror=True),
    ]),
    Bone("leg_hind_left", "body", (5, 11, 8), [
        Cube("hindleg", (2, 0, 5), (6, 11, 6)),
        Cube("hindpaw", (2, 0, 3), (6, 3, 8)),
    ]),
    Bone("leg_hind_right", "body", (-5, 11, 8), [
        Cube("hindleg", (-8, 0, 5), (6, 11, 6), mirror=True),
        Cube("hindpaw", (-8, 0, 3), (6, 3, 8), mirror=True),
    ]),
    Bone("tail", "body", (0, 15, 14), [
        Cube("tail", (-2, 12, 13), (4, 4, 5)),
    ]),
]


def details(texture, name, faces):
    x0, y0, x1, y1 = faces["front"]
    if name == "skull":
        texture.pixel(x0 + 2, y0 + 2, EYE)
        texture.pixel(x1 - 3, y0 + 2, EYE)
        texture.pixel(x0 + 3, y0 + 2, PUPIL)
        texture.pixel(x1 - 4, y0 + 2, PUPIL)
    elif name == "muzzle":
        texture.fill((x0 + 3, y0, x1 - 3, y0 + 2), NOSE, noise=3)
        texture.fill((x0, y1 - 2, x1, y1), BELLY)
    elif name == "ear":
        texture.fill(faces["front"], EAR_INNER, noise=4)
    elif name in ("forepaw", "hindpaw"):
        texture.band_at_bottom(faces, 1, CLAW)
    elif name in ("torso", "shoulders", "hump"):
        for key in ("side_a", "side_b"):
            rx0, ry0, rx1, ry1 = faces[key]
            for x in range(rx0 + 1, rx1, 3):
                texture.fill((x, ry0, x + 1, ry1 - 3), FUR_DARK, noise=3)


if __name__ == "__main__":
    build(
        "direbear", BONES,
        Palette(fur=FUR, top=FUR_DARK, belly=BELLY, overrides={"muzzle": MUZZLE}),
        Gait(
            legs_a=("leg_front_left", "leg_hind_right"),
            legs_b=("leg_front_right", "leg_hind_left"),
            head="neck",
            tail="tail",
            jaw="jaw",
            leg_swing=20.0,
            walk_length=1.0,
            fallen_lift=8,
        ),
        scale=1.8,
        tex_size=(128, 128),
        details=details,
        seed=11,
    )
