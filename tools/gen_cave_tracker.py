#!/usr/bin/env python3
"""Gera o ícone do Rastreador da Caverna (D47): um disco de osso com aro de couro escuro e uma agulha de dente, cor de
sangue seco com a ponta clara e a cauda pardacenta, que aponta para a boca da caverna da arena.

São 32 quadros 16x16 (`cave_tracker_00` a `_31`), a agulha girando 11,25° por quadro no sentido horário a partir do
topo, e os modelos do item: o `cave_tracker.json` troca de quadro pela propriedade `angle`
(`CompassItemPropertyFunction`), com os mesmos limiares da bússola vanilla — o quadro 00 é o alvo bem à frente.

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
import math
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
TEXTURES = ASSETS / "textures" / "item"
MODELS = ASSETS / "models" / "item"
SIZE = 16
FRAMES = 32

RIM_DARK = (52, 36, 26)
RIM = (86, 60, 40)
BONE_SHADE = (176, 164, 132)
BONE = (214, 204, 172)
BONE_LIGHT = (236, 230, 206)
NEEDLE = (128, 36, 30)
NEEDLE_TIP = (250, 244, 228)
TAIL_COLOR = (104, 90, 74)
PIN = (40, 30, 24)

CENTER = 7.5
TIP = 4.7
TAIL = 3.0


def face():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for x in range(SIZE):
        for y in range(SIZE):
            d = ((x - CENTER) ** 2 + (y - CENTER) ** 2) ** 0.5
            if d > 7.3:
                continue
            if d > 6.3:
                color = RIM_DARK
            elif d > 5.4:
                color = RIM
            else:
                # Osso com luz vindo de cima à esquerda.
                light = (CENTER - x) + (CENTER - y)
                color = BONE_LIGHT if light > 4 else BONE_SHADE if light < -3 else BONE
            img.putpixel((x, y), color + (255,))
    return img


def needle_pixels(degrees):
    """Os pixels da agulha, da cauda à ponta, para o ângulo de tela (0 no topo, horário)."""
    dx = math.sin(math.radians(degrees))
    dy = -math.cos(math.radians(degrees))
    pixels = []
    steps = 64
    for i in range(steps + 1):
        t = -TAIL + (TIP + TAIL) * i / steps
        px = math.floor(CENTER + dx * t + 0.5)
        py = math.floor(CENTER + dy * t + 0.5)
        if not pixels or pixels[-1][0] != (px, py):
            pixels.append(((px, py), t))
    return pixels, (dx, dy)


def frame(index):
    img = face()
    degrees = index * 360.0 / FRAMES
    pixels, _ = needle_pixels(degrees)
    for pos, t in pixels:
        img.putpixel(pos, (NEEDLE if t > 0 else TAIL_COLOR) + (255,))
    img.putpixel(pixels[-1][0], NEEDLE_TIP + (255,))
    pin = min(pixels, key=lambda p: abs(p[1]))[0]
    img.putpixel(pin, PIN + (255,))
    return img


def name(index):
    return f"cave_tracker_{index:02d}"


def overrides():
    """Os limiares da bússola vanilla: `angle` 0,5 é o alvo à frente (quadro 00)."""
    entries = [{"predicate": {"angle": 0.0}, "model": f"iceagesurvival:item/{name(FRAMES // 2)}"}]
    for j in range(1, FRAMES):
        entries.append({"predicate": {"angle": (2 * j - 1) / (2 * FRAMES)},
                        "model": f"iceagesurvival:item/{name((FRAMES // 2 + j) % FRAMES)}"})
    entries.append({"predicate": {"angle": (2 * FRAMES - 1) / (2 * FRAMES)},
                    "model": f"iceagesurvival:item/{name(FRAMES // 2)}"})
    return entries


def write_json(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    for index in range(FRAMES):
        frame(index).save(TEXTURES / f"{name(index)}.png")
        write_json(MODELS / f"{name(index)}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"iceagesurvival:item/{name(index)}"},
        })
    write_json(MODELS / "cave_tracker.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"iceagesurvival:item/{name(0)}"},
        "overrides": overrides(),
    })
    print(f"escritos {FRAMES} quadros em {TEXTURES} e os modelos em {MODELS}")


if __name__ == "__main__":
    main()
