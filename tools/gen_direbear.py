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
    ]),
    Bone("jaw", "head", (0, 15, -19), [
        Cube("jaw", (-4, 13, -26), (8, 2, 7)),
    ]),
    Bone("ear_left", "head", (5, 22, -18), [
        Cube("ear", (4, 22, -18), (2, 3, 2)),
    ]),
    Bone("ear_right", "head", (-5, 22, -18), [
        Cube("ear", (-6, 22, -18), (2, 3, 2), mirror=True),
    ]),
    Bone("front_left_upper", "body", (6, 17, -7), [
        Cube("front_upper", (3, 10, -10), (5, 7, 6)),
    ]),
    Bone("front_left_lower", "front_left_upper", (6, 10, -7), [
        Cube("front_lower", (3, 3, -10), (5, 7, 6)),
    ]),
    Bone("front_left_paw", "front_left_lower", (6, 3, -7), [
        Cube("front_paw", (2, 0, -13), (6, 3, 8)),
    ]),
    Bone("front_right_upper", "body", (-6, 17, -7), [
        Cube("front_upper", (-8, 10, -10), (5, 7, 6), mirror=True),
    ]),
    Bone("front_right_lower", "front_right_upper", (-6, 10, -7), [
        Cube("front_lower", (-8, 3, -10), (5, 7, 6), mirror=True),
    ]),
    Bone("front_right_paw", "front_right_lower", (-6, 3, -7), [
        Cube("front_paw", (-8, 0, -13), (6, 3, 8), mirror=True),
    ]),
    Bone("hind_left_upper", "body", (5, 16, 8), [
        Cube("hind_upper", (2, 9, 5), (6, 7, 6)),
    ]),
    Bone("hind_left_lower", "hind_left_upper", (5, 9, 8), [
        Cube("hind_lower", (2, 3, 5), (6, 6, 6)),
    ]),
    Bone("hind_left_paw", "hind_left_lower", (5, 3, 8), [
        Cube("hind_paw", (2, 0, 3), (6, 3, 8)),
    ]),
    Bone("hind_right_upper", "body", (-5, 16, 8), [
        Cube("hind_upper", (-8, 9, 5), (6, 7, 6), mirror=True),
    ]),
    Bone("hind_right_lower", "hind_right_upper", (-5, 9, 8), [
        Cube("hind_lower", (-8, 3, 5), (6, 6, 6), mirror=True),
    ]),
    Bone("hind_right_paw", "hind_right_lower", (-5, 3, 8), [
        Cube("hind_paw", (-8, 0, 3), (6, 3, 8), mirror=True),
    ]),
    Bone("tail_base", "body", (0, 15, 14), [
        Cube("tail_base", (-2, 12, 13), (4, 4, 4)),
    ]),
    Bone("tail_tip", "tail_base", (0, 14, 17), [
        Cube("tail_tip", (-1.5, 12, 16), (3, 3, 5)),
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
    elif name in ("front_paw", "hind_paw"):
        texture.band_at_bottom(faces, 1, CLAW)
    elif name in ("torso", "shoulders", "hump"):
        for key in ("side_a", "side_b"):
            rx0, ry0, rx1, ry1 = faces[key]
            for x in range(rx0 + 1, rx1, 3):
                texture.fill((x, ry0, x + 1, ry1 - 3), FUR_DARK, noise=3)


def rotation(keys):
    return {"rotation": {str(time): values for time, values in keys.items()}}


def leg_cycle(upper, lower, paw, forward):
    return {
        upper: rotation({0.0: [forward, 0, 0], 0.25: [0, 0, 0], 0.5: [-forward, 0, 0],
                         0.75: [0, 0, 0], 1.0: [forward, 0, 0]}),
        lower: rotation({0.0: [-forward * 0.55, 0, 0], 0.25: [-forward * 0.8, 0, 0],
                         0.5: [forward * 0.45, 0, 0], 0.75: [forward * 0.7, 0, 0],
                         1.0: [-forward * 0.55, 0, 0]}),
        paw: rotation({0.0: [forward * 0.25, 0, 0], 0.25: [forward * 0.65, 0, 0],
                       0.5: [-forward * 0.2, 0, 0], 0.75: [-forward * 0.45, 0, 0],
                       1.0: [forward * 0.25, 0, 0]}),
    }


def bear_animations(species):
    prefix = f"animation.{species}."
    walk_bones = {
        "body": rotation({0.0: [0, -1.5, 0], 0.5: [0, 1.5, 0], 1.0: [0, -1.5, 0]}),
        "neck": rotation({0.0: [2, 0, 0], 0.5: [-2, 0, 0], 1.0: [2, 0, 0]}),
        "head": rotation({0.0: [-1, 0, 0], 0.5: [1, 0, 0], 1.0: [-1, 0, 0]}),
        "tail_base": rotation({0.0: [0, -4, 0], 0.5: [0, 4, 0], 1.0: [0, -4, 0]}),
        "tail_tip": rotation({0.0: [0, -7, 0], 0.5: [0, 7, 0], 1.0: [0, -7, 0]}),
    }
    for upper, lower, paw, forward in (
            ("front_left_upper", "front_left_lower", "front_left_paw", 22),
            ("front_right_upper", "front_right_lower", "front_right_paw", -22),
            ("hind_left_upper", "hind_left_lower", "hind_left_paw", -18),
            ("hind_right_upper", "hind_right_lower", "hind_right_paw", 18)):
        walk_bones.update(leg_cycle(upper, lower, paw, forward))

    return {
        "format_version": "1.8.0",
        "animations": {
            prefix + "idle": {
                "loop": True,
                "animation_length": 3.0,
                "bones": {
                    "body": rotation({0.0: [0, -0.8, 0], 1.5: [0, 0.8, 0], 3.0: [0, -0.8, 0]}),
                    "neck": rotation({0.0: [0, 0, 0], 1.5: [2, 0, 0], 3.0: [0, 0, 0]}),
                    "head": rotation({0.0: [0, 0, 0], 1.5: [-1.5, 0, 0], 3.0: [0, 0, 0]}),
                    "ear_left": rotation({0.0: [0, 0, 0], 1.1: [0, 0, 9], 1.4: [0, 0, 0]}),
                    "ear_right": rotation({0.0: [0, 0, 0], 1.8: [0, 0, -9], 2.1: [0, 0, 0]}),
                    "tail_base": rotation({0.0: [0, -3, 0], 1.5: [0, 3, 0], 3.0: [0, -3, 0]}),
                    "tail_tip": rotation({0.0: [0, -6, 0], 1.5: [0, 6, 0], 3.0: [0, -6, 0]}),
                },
            },
            prefix + "walk": {
                "loop": True,
                "animation_length": 1.0,
                "bones": walk_bones,
            },
            prefix + "attack": {
                "animation_length": 0.45,
                "bones": {
                    "body": rotation({0.0: [0, 0, 0], 0.12: [7, 0, 0], 0.3: [-5, 0, 0], 0.45: [0, 0, 0]}),
                    "neck": rotation({0.0: [0, 0, 0], 0.12: [-24, 0, 0], 0.3: [12, 0, 0], 0.45: [0, 0, 0]}),
                    "head": rotation({0.0: [0, 0, 0], 0.12: [-12, 0, 0], 0.3: [8, 0, 0], 0.45: [0, 0, 0]}),
                    "jaw": rotation({0.0: [0, 0, 0], 0.12: [28, 0, 0], 0.3: [0, 0, 0]}),
                    "front_left_upper": rotation({0.0: [0, 0, 0], 0.12: [-42, 0, 0], 0.3: [18, 0, 0], 0.45: [0, 0, 0]}),
                    "front_left_lower": rotation({0.0: [0, 0, 0], 0.12: [28, 0, 0], 0.3: [-16, 0, 0], 0.45: [0, 0, 0]}),
                    "front_right_upper": rotation({0.0: [0, 0, 0], 0.12: [-42, 0, 0], 0.3: [18, 0, 0], 0.45: [0, 0, 0]}),
                    "front_right_lower": rotation({0.0: [0, 0, 0], 0.12: [28, 0, 0], 0.3: [-16, 0, 0], 0.45: [0, 0, 0]}),
                },
            },
            prefix + "unconscious": {
                "loop": True,
                "animation_length": 1.0,
                "bones": {
                    "root": {"rotation": [0, 0, 90], "position": [0, 8, 0]},
                },
            },
        },
    }


if __name__ == "__main__":
    build(
        "direbear", BONES,
        Palette(fur=FUR, top=FUR_DARK, belly=BELLY, overrides={"muzzle": MUZZLE}),
        Gait(
            legs_a=("leg_front_left", "leg_hind_right"),
            legs_b=("leg_front_right", "leg_hind_left"),
            head="neck",
            tail="tail_base",
            jaw="jaw",
            leg_swing=20.0,
            walk_length=1.0,
            fallen_lift=8,
        ),
        scale=1.8,
        tex_size=(128, 128),
        details=details,
        seed=11,
        animation_factory=bear_animations,
    )
