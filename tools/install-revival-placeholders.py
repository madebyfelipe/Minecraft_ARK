#!/usr/bin/env python3
"""Monta um resource pack LOCAL com modelos do mod Fossils and Archeology: Revival, para
servirem de placeholder na instância de teste enquanto não temos modelos definitivos.

ATENÇÃO — licença: o código do Revival é MIT, mas modelos e texturas são "All Rights
Reserved" e a redistribuição exige permissão dos autores. Por isso este script:

  - baixa o Revival para um cache fora do projeto;
  - escreve o pack dentro da instância do Prism, nunca no repositório;
  - não deve ter a sua saída copiada para src/, para o git ou para o jar do mod.

O pack sobrepõe só os assets de iceagesurvival (geometria, textura, animações e escala) das
espécies listadas em SPECIES. Remover o pack volta aos modelos gerados por tools/gen_*.py.
"""

import json
import os
import shutil
import subprocess
from pathlib import Path

REVIVAL_REPO = "https://github.com/TeamFossilsArcheology/FossilsArcheologyRevival"
CACHE = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "iceagesurvival" / "revival"
INSTANCE = Path(os.environ.get(
    "PRISM_INSTANCE_DIR", Path.home() / ".local/share/PrismLauncher/instances/IceAgeSurvival/minecraft"))
PACK_NAME = "revival_placeholders"
# Formato de resource pack do Minecraft 1.21.1.
PACK_FORMAT = 34

# espécie nossa -> (nome no Revival, textura, animações deles para as nossas quatro, altura desejada em blocos)
SPECIES = {
    "smilodon": ("smilodon", "smilodon_male.png",
                 {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 2.5),
    "mammoth": ("mammoth", "mammoth_male.png",
                {"idle": "idle_1_90", "walk": "walk", "attack": "attack", "unconscious": "rest/sleep"}, 3.3),
}


def fetch_revival():
    if (CACHE / ".git").exists():
        return
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "clone", "--quiet", "--depth", "1", REVIVAL_REPO, str(CACHE)], check=True)


def model_height_blocks(geometry):
    top = max(cube["origin"][1] + cube["size"][1]
              for bone in geometry["minecraft:geometry"][0]["bones"] for cube in bone.get("cubes", []))
    return top / 16.0


def first_value(channel):
    """Valor de um canal de animação no primeiro quadro-chave, como três números."""
    if isinstance(channel, dict) and "vector" not in channel:
        # Mapa de tempo -> quadro-chave.
        channel = channel[min(channel, key=float)]
    if isinstance(channel, dict):
        # Quadro-chave com metadados (easing do GeckoLib ou pre/post do Bedrock).
        channel = channel.get("vector", channel.get("post", channel.get("pre", [0, 0, 0])))
    if not isinstance(channel, list):
        channel = [channel, channel, channel]
    numbers = []
    for component in channel:
        try:
            numbers.append(float(component))
        except (TypeError, ValueError):
            # Expressão molang (depende do tempo): a pose parada usa zero.
            numbers.append(0.0)
    return numbers


def frozen(animation):
    """Pose estática do início da animação: a criatura inconsciente não pode se mexer."""
    bones = {}
    for bone, channels in animation.get("bones", {}).items():
        bones[bone] = {name: first_value(value) for name, value in channels.items()
                       if name in ("rotation", "position", "scale")}
    return {"loop": True, "animation_length": 1.0, "bones": bones}


def convert_animations(ours, theirs_name, mapping, source):
    theirs = json.loads(source.read_text())["animations"]
    out = {}
    for our_key, their_key in mapping.items():
        animation = theirs[f"animation.{theirs_name}.{their_key}"]
        if our_key == "unconscious":
            animation = frozen(animation)
        elif our_key == "attack":
            animation = {k: v for k, v in animation.items() if k != "loop"}
        else:
            animation = dict(animation, loop=True)
        out[f"animation.{ours}.{our_key}"] = animation
    return {"format_version": "1.8.0", "animations": out}


def main():
    fetch_revival()
    source = CACHE / "common" / "src" / "main" / "resources" / "assets" / "fossil"
    pack = INSTANCE / "resourcepacks" / PACK_NAME
    if pack.exists():
        shutil.rmtree(pack)
    assets = pack / "assets" / "iceagesurvival"
    for sub in ("geo/entity", "textures/entity", "animations/entity", "creature_models"):
        (assets / sub).mkdir(parents=True)

    (pack / "pack.mcmeta").write_text(json.dumps({"pack": {
        "pack_format": PACK_FORMAT,
        "description": "Placeholders locais do F&A Revival. Não redistribuir."}}, indent=2) + "\n")

    for ours, (theirs, texture, mapping, height) in SPECIES.items():
        geometry = json.loads((source / "geo" / "entity" / f"{theirs}.geo.json").read_text())
        (assets / "geo" / "entity" / f"{ours}.geo.json").write_text(json.dumps(geometry))
        shutil.copyfile(source / "textures" / "entity" / theirs / texture,
                        assets / "textures" / "entity" / f"{ours}.png")
        animations = convert_animations(ours, theirs, mapping, source / "animations" / f"{theirs}.animation.json")
        (assets / "animations" / "entity" / f"{ours}.animation.json").write_text(json.dumps(animations))
        scale = round(height / model_height_blocks(geometry), 2)
        (assets / "creature_models" / f"{ours}.json").write_text(json.dumps({"scale": scale}) + "\n")
        print(f"{ours}: modelo do Revival, escala {scale}")

    print(f"Pack em {pack}")
    print("Ative em Opções > Pacotes de Recursos dentro do jogo (ou reinicie se já estiver ativo).")


if __name__ == "__main__":
    main()
