#!/usr/bin/env python3
"""Gera os assets do Tyrannosaurus rex. Ver modelgen.py para as convenções."""

from modelgen import Bone, Cube, Gait, Palette, build

HIDE = (88, 104, 66)
HIDE_DARK = (58, 74, 46)
BELLY = (178, 170, 128)
TEETH = (236, 230, 208)
EYE = (226, 168, 36)
PUPIL = (20, 16, 10)
NOSTRIL = (36, 44, 30)
CLAW = (50, 46, 38)

BONES = [
    Bone("root", None, (0, 0, 0)),
    Bone("body", "root", (0, 22, 0), [
        Cube("torso", (-6, 16, -6), (12, 14, 16)),
        Cube("chest", (-5, 18, -13), (10, 12, 7)),
    ]),
    Bone("neck", "body", (0, 28, -12), [Cube("neck", (-3, 26, -17), (6, 8, 6))]),
    Bone("head", "neck", (0, 32, -16), [
        Cube("skull", (-4, 30, -26), (8, 8, 10)),
        Cube("snout", (-3, 30, -32), (6, 5, 6)),
        Cube("teeth", (-3, 28, -32), (6, 2, 6)),
    ]),
    Bone("jaw", "head", (0, 28, -24), [Cube("jaw", (-3, 26, -32), (6, 2, 9))]),
    Bone("arm_left", "body", (3, 24, -12), [Cube("arm", (2, 18, -13), (2, 6, 2))]),
    Bone("arm_right", "body", (-3, 24, -12), [Cube("arm", (-4, 18, -13), (2, 6, 2), mirror=True)]),
    Bone("leg_left", "body", (4, 18, 2), [
        Cube("thigh", (1, 8, -1), (5, 10, 7)),
        Cube("shin", (2, 2, 0), (3, 7, 4)),
        Cube("foot", (1, 0, -5), (5, 2, 8)),
    ]),
    Bone("leg_right", "body", (-4, 18, 2), [
        Cube("thigh", (-6, 8, -1), (5, 10, 7), mirror=True),
        Cube("shin", (-5, 2, 0), (3, 7, 4), mirror=True),
        Cube("foot", (-6, 0, -5), (5, 2, 8), mirror=True),
    ]),
    Bone("tail", "body", (0, 24, 9), [Cube("tail_base", (-3, 19, 9), (6, 8, 8))]),
    Bone("tail_tip", "tail", (0, 23, 16), [Cube("tail_tip", (-2, 20, 16), (4, 5, 9))]),
]


def details(texture, name, faces):
    if name == "skull":
        # O olho fica nas laterais; a borda da frente de cada lateral encosta na face da frente.
        ax0, ay0, ax1, _ = faces["side_a"]
        bx0, by0, bx1, _ = faces["side_b"]
        for ex, ey in ((ax1 - 3, ay0 + 2), (bx0 + 2, by0 + 2)):
            texture.pixel(ex, ey, EYE)
            texture.pixel(ex + 1, ey, PUPIL)
    elif name == "snout":
        x0, y0, x1, _ = faces["front"]
        texture.pixel(x0 + 1, y0 + 1, NOSTRIL)
        texture.pixel(x1 - 2, y0 + 1, NOSTRIL)
    elif name == "teeth":
        # Serrilhado: pixels escuros alternados na base da fileira de dentes.
        for key in ("front", "side_a", "side_b"):
            x0, y0, x1, y1 = faces[key]
            for x in range(x0, x1, 2):
                texture.pixel(x, y1 - 1, HIDE_DARK)
    elif name in ("arm", "foot"):
        texture.band_at_bottom(faces, 1, CLAW)
    elif name == "torso":
        # Listras escuras descendo pelas laterais do dorso.
        for key in ("side_a", "side_b"):
            rx0, ry0, rx1, ry1 = faces[key]
            for x in range(rx0 + 1, rx1, 4):
                texture.fill((x, ry0, x + 1, ry1 - 4), HIDE_DARK, noise=4)


if __name__ == "__main__":
    build(
        "tyrannosaurus", BONES,
        Palette(fur=HIDE, top=HIDE_DARK, belly=BELLY, overrides={"teeth": TEETH, "jaw": BELLY}),
        Gait(legs_a=("leg_left",), legs_b=("leg_right",), head="neck", tail="tail", jaw="jaw",
             leg_swing=24.0, walk_length=1.0, fallen_lift=6.0),
        scale=1.8, tex_size=(128, 128), details=details, seed=3)
