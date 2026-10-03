#!/usr/bin/env python3
"""Gera a textura 16x16 do Rastreador da Caverna (D47): um disco de osso com aro de couro escuro e uma
agulha de dente serrilhado, cor de sangue seco, apontando para longe.

Arte autoral, desenhada pixel a pixel aqui. Rodar de novo SOBRESCREVE edições feitas à mão.
"""

from pathlib import Path

from PIL import Image

OUT = (Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"
       / "textures" / "item" / "cave_tracker.png")
SIZE = 16

RIM_DARK = (52, 36, 26)
RIM = (86, 60, 40)
BONE_SHADE = (176, 164, 132)
BONE = (214, 204, 172)
BONE_LIGHT = (236, 230, 206)
NEEDLE = (128, 36, 30)
NEEDLE_DARK = (82, 22, 20)
NEEDLE_TIP = (232, 226, 210)
PIN = (40, 30, 24)


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    for x in range(SIZE):
        for y in range(SIZE):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > 7.3:
                continue
            if d > 6.3:
                color = RIM_DARK
            elif d > 5.4:
                color = RIM
            else:
                # Osso com luz vindo de cima à esquerda.
                light = (cx - x) + (cy - y)
                color = BONE_LIGHT if light > 4 else BONE_SHADE if light < -3 else BONE
            img.putpixel((x, y), color + (255,))

    # Agulha diagonal: do canto de baixo à esquerda para cima à direita, com a ponta de dente clara.
    needle = [(4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5)]
    for i, (x, y) in enumerate(needle):
        img.putpixel((x, y), (NEEDLE_DARK if i < 3 else NEEDLE) + (255,))
        # Serrilha: um dente de cada lado, alternado.
        if i % 2 == 1 and i < 6:
            img.putpixel((x + 1, y + 1), NEEDLE_DARK + (255,))
    img.putpixel((11, 4), NEEDLE_TIP + (255,))
    img.putpixel((10, 4), NEEDLE + (255,))
    img.putpixel((11, 5), NEEDLE + (255,))
    img.putpixel((7, 7), PIN + (255,))
    img.putpixel((8, 8), PIN + (255,))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print(f"escrito {OUT}")


if __name__ == "__main__":
    main()
