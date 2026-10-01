#!/usr/bin/env python3
"""Gera as texturas da armadura de pele, da pele (item) e da roupa e carne de dodô.

Das texturas de couro do Minecraft só se usa a **silhueta** (canal alfa): ela diz onde ficam
os pixels no layout UV da armadura e o contorno de cada ícone. A cor é toda nossa — pelagem
marrom com mechas e bordas claras. As texturas do Minecraft são lidas do jar de recursos que
o ForgeGradle baixa (`~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar`),
rodar `./gradlew build` antes.

Rodar de novo SOBRESCREVE edições feitas à mão.
"""

import io
import random
import zipfile
from pathlib import Path

from PIL import Image

PROJECT = Path(__file__).resolve().parent.parent
TEXTURES = PROJECT / "src" / "main" / "resources" / "assets" / "iceagesurvival" / "textures"
FUR = [(92, 66, 44), (104, 76, 50), (80, 57, 38), (116, 86, 58), (70, 50, 34)]
TRIM = [(222, 214, 196), (206, 196, 176), (236, 230, 214)]
OUTLINE = (46, 32, 22)
# Penugem cinza-amarronzada do dodô, com a ponta amarela clara das penas.
FEATHER = [(122, 116, 104), (140, 132, 118), (104, 98, 88), (158, 150, 134)]
FEATHER_TIP = [(214, 196, 128), (196, 178, 112)]
RAW_MEAT = [(232, 150, 140), (214, 122, 116), (244, 176, 164)]
COOKED_MEAT = [(168, 104, 58), (140, 82, 44), (190, 128, 72)]


def minecraft_jar():
    forge = Path.home() / ".gradle" / "caches" / "forge_gradle" / "minecraft_repo" / "versions" / "1.20.1" / "client-extra.jar"
    if forge.exists():
        return zipfile.ZipFile(forge)
    jars = sorted((PROJECT / "build" / "moddev" / "artifacts").glob("*client-extra*.jar"))
    if not jars:
        jars = sorted((PROJECT.parents[2] / "build" / "moddev" / "artifacts").glob("*client-extra*.jar"))
    if not jars:
        raise SystemExit("Jar de recursos do Minecraft não encontrado; rode ./gradlew build.")
    return zipfile.ZipFile(jars[-1])


def silhouette(jar, name):
    """Máscara do ícone; os itens de couro somam a camada {@code _overlay}, onde mora parte da forma."""
    image = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{name}.png"))).convert("RGBA")
    overlay = f"assets/minecraft/textures/{name}_overlay.png"
    if name.startswith("item/leather_") and overlay in jar.namelist():
        top = Image.open(io.BytesIO(jar.read(overlay))).convert("RGBA")
        image = Image.alpha_composite(image, top)
    return image


def fur_fill(mask, rng, trim_rows=()):
    """Pinta os pixels opacos da máscara com pelagem; mechas verticais e bordas claras."""
    out = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    width, height = mask.size
    streak = [rng.randrange(len(FUR)) for _ in range(width)]
    for x in range(width):
        for y in range(height):
            if mask.getpixel((x, y))[3] < 128:
                continue
            if y in trim_rows:
                color = rng.choice(TRIM)
            elif rng.random() < 0.55:
                color = FUR[streak[x]]
            else:
                color = rng.choice(FUR)
            out.putpixel((x, y), color + (255,))
    return out


def feather_fill(mask, rng):
    """Penas em fileiras: cada faixa de 3 linhas tem um tom, e a última linha é a ponta clara."""
    out = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    width, height = mask.size
    row_shade = [rng.randrange(len(FEATHER)) for _ in range(height // 3 + 1)]
    for x in range(width):
        for y in range(height):
            if mask.getpixel((x, y))[3] < 128:
                continue
            if y % 3 == 2 and (x + y // 3) % 2 == 0:
                color = rng.choice(FEATHER_TIP)
            elif rng.random() < 0.7:
                color = FEATHER[row_shade[y // 3]]
            else:
                color = rng.choice(FEATHER)
            out.putpixel((x, y), color + (255,))
    return out


def recolored(mask, palette, rng):
    """Silhueta do ícone pintada com a paleta, mantendo o sombreado do original pelo brilho."""
    out = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    width, height = mask.size
    for x in range(width):
        for y in range(height):
            r, g, b, a = mask.getpixel((x, y))
            if a < 128:
                continue
            light = (r + g + b) / 765
            base = palette[0] if light > 0.6 else palette[1] if light < 0.35 else rng.choice(palette)
            out.putpixel((x, y), base + (255,))
    return out


def outlined(icon):
    """Contorno escuro nos pixels da borda, para o ícone ler bem no inventário."""
    out = icon.copy()
    width, height = icon.size
    for x in range(width):
        for y in range(height):
            if icon.getpixel((x, y))[3] == 0:
                continue
            neighbours = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(not (0 <= nx < width and 0 <= ny < height) or icon.getpixel((nx, ny))[3] == 0
                   for nx, ny in neighbours):
                out.putpixel((x, y), OUTLINE + (255,))
    return out


def trim_rows(mask):
    """Primeira linha opaca do ícone: a gola/borda de pelo claro."""
    for y in range(mask.size[1]):
        if any(mask.getpixel((x, y))[3] >= 128 for x in range(mask.size[0])):
            return (y, y + 1)
    return ()


def main():
    jar = minecraft_jar()
    rng = random.Random(23)
    outputs = {}
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        mask = silhouette(jar, f"item/leather_{piece}")
        outputs[f"item/fur_{piece}.png"] = outlined(fur_fill(mask, rng, trim_rows(mask)))
    for layer in (1, 2):
        outputs[f"models/armor/fur_layer_{layer}.png"] = fur_fill(silhouette(jar, f"models/armor/leather_layer_{layer}"), rng)
    outputs["item/pelt.png"] = outlined(fur_fill(silhouette(jar, "item/leather"), rng))
    # Gerador próprio para a roupa de pena: as texturas de pele acima continuam idênticas.
    feather_rng = random.Random(41)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        outputs[f"item/feather_{piece}.png"] = outlined(feather_fill(silhouette(jar, f"item/leather_{piece}"), feather_rng))
    for layer in (1, 2):
        outputs[f"models/armor/feather_layer_{layer}.png"] = feather_fill(
            silhouette(jar, f"models/armor/leather_layer_{layer}"), feather_rng)
    outputs["item/dodo_feather.png"] = outlined(feather_fill(silhouette(jar, "item/feather"), feather_rng))
    outputs["item/dodo_meat.png"] = outlined(recolored(silhouette(jar, "item/chicken"), RAW_MEAT, feather_rng))
    outputs["item/cooked_dodo_meat.png"] = outlined(recolored(silhouette(jar, "item/cooked_chicken"), COOKED_MEAT, feather_rng))
    for relative, img in outputs.items():
        path = TEXTURES / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    print(f"{len(outputs)} texturas geradas em {TEXTURES}")


if __name__ == "__main__":
    main()
