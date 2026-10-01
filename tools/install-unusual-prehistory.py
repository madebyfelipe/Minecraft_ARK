#!/usr/bin/env python3
"""Instala modelos do mod Unusual Prehistory (Peeko32213, branch main-final, Forge 1.20.1) como
assets do mod: geometria, textura, as quatro animações (idle, walk, attack, unconscious) e os
ajustes de renderização (escala, osso que olha, ossos escondidos no filhote).

ATENÇÃO — licença: o código do Unusual Prehistory é MIT, mas **texturas, geometria, animações e
música são "All Rights Reserved"** (arquivo LICENSE do repositório). Como os do Revival, ficam
no repositório privado por decisão do Felipe (2026-09-30) e vão dentro do jar: **antes de
distribuir o mod, substituir ou licenciar** (ver ASSET_LICENSES.md).

Os modelos do Unusual Prehistory são a primeira escolha a partir de 2026-09-30. Espécies que ele
não tem (espinossauro, lobo-terrível) continuam com o Revival ou com o gerador próprio. Os sons
continuam vindo de tools/install-revival-placeholders.py.

O script baixa o repositório para um cache fora do projeto. Rodar de novo **sobrescreve** edições
manuais nos arquivos destas espécies.
"""

import io
import json
import os
import shutil
import urllib.request
import zipfile
from pathlib import Path

ARCHIVE = "https://codeload." + "gi" + "thub.com/Peeko32213/Unusual-Prehistory/zip/refs/heads/main-final"
CACHE = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "iceagesurvival" / "unusual-prehistory"
SOURCE = CACHE / "assets" / "unusualprehistory"
PROJECT = Path(__file__).resolve().parent.parent
ASSETS = PROJECT / "src" / "main" / "resources" / "assets" / "iceagesurvival"
HAND_AUTHORED = PROJECT / "tools" / "hand_authored.txt"

# espécie nossa -> origem e ajustes.
#   geo/anim/texture: caminhos dentro de assets/unusualprehistory
#   anims: nossa animação -> nome da deles (o prefixo "animation.<x>." é achado sozinho)
#   scale: fator do modelo. Escolhido pelo corpo e pela colisão, não pela altura total: o
#          braquiossauro tem 25 blocos de altura, quase tudo pescoço
#   body: osso do tronco; o topo dele, na escala, é a altura do assento sugerida
#   look_bone: osso que o modelo deles gira para olhar
SPECIES = {
    "tyrannosaurus": dict(geo="geo/tyrannosaurus/tyrannosaurus_rex.geo.json",
                          anim="animations/tyrannosaurus/tyrannosaurus_rex.animation.json",
                          texture="textures/entity/tyrannosaurus/tyrannosaurus_rex.png",
                          anims={"idle": "idle", "walk": "walk", "attack": "bite_blend1", "unconscious": "knockout"},
                          scale=1.3, body="body", look_bone="neck_control"),
    "velociraptor": dict(geo="geo/velociraptor.geo.json", anim="animations/velociraptor.animation.json",
                         texture="textures/entity/velociraptor/velociraptor.png",
                         anims={"idle": "idle", "walk": "walk", "attack": "bite_blend", "unconscious": "sleep"},
                         scale=1.0, body="body", look_bone="head"),
    "smilodon": dict(geo="geo/smilodon.geo.json", anim="animations/smilodon.animation.json",
                     texture="textures/entity/smilodon/smilodon_cold.png",
                     anims={"idle": "idle", "walk": "move", "attack": "bite", "unconscious": "frozen"},
                     scale=1.0, body="Body", look_bone="Head"),
    "mammoth": dict(geo="geo/mammoth/mammoth.geo.json", anim="animations/mammoth/mammoth.animation.json",
                    texture="textures/entity/mammoth/mammoth.png",
                    anims={"idle": "idle", "walk": "move", "attack": "attack", "unconscious": "frozen"},
                    scale=1.0, body="Body", look_bone="Head", baby_hidden_bones=["Tusk1", "Tusk2"]),
    # Sem Brontossauro no Unusual Prehistory: o Braquiossauro, outro saurópode.
    "brontosaurus": dict(geo="geo/brachiosaurus.geo.json", anim="animations/brachiosaurus.animation.json",
                         texture="textures/entity/brachiosaurus.png",
                         anims={"idle": "idle", "walk": "walk", "attack": "stomp", "unconscious": "sleep"},
                         scale=0.7, body="body", look_bone="neck"),
    # Sem Carnotauro: o Majungassauro, abelissaurídeo como ele.
    "carnotaurus": dict(geo="geo/majungasaurus.geo.json", anim="animations/majungasaurus.animation.json",
                        texture="textures/entity/majungasaurus.png",
                        anims={"idle": "idle", "walk": "walk", "attack": "bite", "unconscious": "stunned"},
                        scale=1.81, body="Body", look_bone="Head"),
    # Sem Utahraptor: o Austroraptor, dromeossaurídeo grande.
    "utahraptor": dict(geo="geo/austroraptor.geo.json", anim="animations/austroraptor.animation.json",
                       texture="textures/entity/austroraptor.png",
                       anims={"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "idle"},
                       scale=1.2, body="Body", look_bone="Head"),
}


def fetch():
    if SOURCE.exists():
        return
    data = urllib.request.urlopen(ARCHIVE).read()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for name in archive.namelist():
            marker = "src/main/resources/"
            if marker in name and not name.endswith("/"):
                target = CACHE / name.split(marker, 1)[1]
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))


def first_value(channel):
    """Valor de um canal de animação no primeiro quadro-chave, como três números."""
    if isinstance(channel, dict) and "vector" not in channel:
        channel = channel[min(channel, key=float)]
    if isinstance(channel, dict):
        channel = channel.get("vector", channel.get("post", channel.get("pre", [0, 0, 0])))
    if not isinstance(channel, list):
        channel = [channel, channel, channel]
    numbers = []
    for component in channel:
        try:
            numbers.append(float(component))
        except (TypeError, ValueError):
            numbers.append(0.0)  # molang: a pose parada usa zero
    return numbers


def frozen(animation):
    """Pose estática do início da animação: a criatura inconsciente não pode se mexer."""
    bones = {bone: {name: first_value(value) for name, value in channels.items()
                    if name in ("rotation", "position", "scale")}
             for bone, channels in animation.get("bones", {}).items()}
    return {"loop": True, "animation_length": 1.0, "bones": bones}


def convert_animations(ours, mapping, source):
    theirs = json.loads(source.read_text())["animations"]
    by_name = {key.rsplit(".", 1)[-1]: value for key, value in theirs.items()}
    out = {}
    for our_key, their_key in mapping.items():
        animation = by_name[their_key]
        if our_key == "unconscious":
            animation = frozen(animation)
        elif our_key == "attack":
            animation = {k: v for k, v in animation.items() if k not in ("loop", "override_previous_animation")}
        else:
            animation = dict(animation, loop=True)
        out[f"animation.{ours}.{our_key}"] = animation
    return {"format_version": "1.8.0", "animations": out}


def body_top_blocks(geometry, body_bone, scale):
    bone = next(b for b in geometry["minecraft:geometry"][0]["bones"] if b["name"] == body_bone)
    return max(c["origin"][1] + c["size"][1] for c in bone["cubes"]) / 16.0 * scale


def mark_hand_authored(species):
    existing = set()
    if HAND_AUTHORED.exists():
        existing = {line.split("#")[0].strip() for line in HAND_AUTHORED.read_text().splitlines()}
        existing.discard("")
    HAND_AUTHORED.write_text(
        "# Espécies cujos assets NÃO vêm de tools/gen_<especie>.py e não devem ser regerados.\n"
        "# Preenchido por tools/install-revival-placeholders.py e tools/install-unusual-prehistory.py;\n"
        "# o guarda está em tools/modelgen.py. Arte All Rights Reserved — ver ASSET_LICENSES.md.\n"
        + "".join(f"{name}\n" for name in sorted(existing | set(species))))


def main():
    fetch()
    for sub in ("geo/entity", "textures/entity", "animations/entity", "creature_models"):
        (ASSETS / sub).mkdir(parents=True, exist_ok=True)
    for ours, spec in SPECIES.items():
        geometry = json.loads((SOURCE / spec["geo"]).read_text())
        geometry["minecraft:geometry"][0]["description"]["identifier"] = f"geometry.{ours}"
        (ASSETS / "geo" / "entity" / f"{ours}.geo.json").write_text(json.dumps(geometry, indent=2) + "\n")
        shutil.copyfile(SOURCE / spec["texture"], ASSETS / "textures" / "entity" / f"{ours}.png")
        animations = convert_animations(ours, spec["anims"], SOURCE / spec["anim"])
        (ASSETS / "animations" / "entity" / f"{ours}.animation.json").write_text(json.dumps(animations, indent=2) + "\n")
        settings = {"scale": spec["scale"], "look_bone": spec["look_bone"]}
        if spec.get("baby_hidden_bones"):
            settings["baby_hidden_bones"] = spec["baby_hidden_bones"]
        (ASSETS / "creature_models" / f"{ours}.json").write_text(json.dumps(settings, indent=2) + "\n")
        print(f"{ours}: Unusual Prehistory, escala {spec['scale']}, dorso a "
              f"{body_top_blocks(geometry, spec['body'], spec['scale']):.2f} blocos")
    mark_hand_authored(SPECIES)
    print("Arte All Rights Reserved: substituir antes de distribuir o mod (ver ASSET_LICENSES.md).")


if __name__ == "__main__":
    main()
