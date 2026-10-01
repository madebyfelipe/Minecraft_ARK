#!/usr/bin/env python3
"""Instala modelos do mod Fossils and Archeology: Revival como assets do mod, no repositório,
para servirem de placeholder enquanto não temos modelos definitivos.

ATENÇÃO — licença: o código do Revival é MIT, mas **modelos e texturas são "All Rights
Reserved"** e redistribuí-los exige permissão dos autores. Por decisão do Felipe
(2026-09-30) eles passaram a ficar no repositório, que é privado, para poderem ser abertos
e editados no Blockbench. Consequência que não dá para contornar: estando em
`src/main/resources/assets/`, **eles também vão dentro de todo jar compilado**. Antes de
qualquer distribuição do mod — tester, servidor de outra pessoa, release — os modelos das
espécies listadas em `tools/hand_authored.txt` precisam ser nossos ou licenciados.

O script baixa o Revival para um cache fora do projeto e escreve, por espécie, geometria,
textura, as quatro animações, a escala e os sons (ambiente, dano, morte e alerta). Rodar de novo **sobrescreve** edições manuais.
"""

import json
import os
import shutil
import subprocess
from pathlib import Path

REVIVAL_REPO = "https://github.com/TeamFossilsArcheology/FossilsArcheologyRevival"
CACHE = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "iceagesurvival" / "revival"
PROJECT = Path(__file__).resolve().parent.parent
ASSETS = PROJECT / "src" / "main" / "resources" / "assets" / "iceagesurvival"
# Espécies cujos assets não devem ser regerados por tools/gen_<especie>.py.
HAND_AUTHORED = PROJECT / "tools" / "hand_authored.txt"

# espécie nossa -> (nome no Revival, textura, animações deles para as nossas quatro, altura desejada em blocos)
SPECIES = {
    "smilodon": ("smilodon", "smilodon_male.png",
                 {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 2.5),
    "mammoth": ("mammoth", "mammoth_male.png",
                {"idle": "idle_1_90", "walk": "walk", "attack": "attack", "unconscious": "rest/sleep"}, 4.95),
    "tyrannosaurus": ("tyrannosaurus", "tyrannosaurus_male.png",
                      {"idle": "idle", "walk": "walk", "attack": "attack_normal_1", "unconscious": "sleep_1"}, 6.0),
    "velociraptor": ("velociraptor", "velociraptor_male.png", {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 1.3),
    # Sem Utahraptor no Revival: o Deinonychus, parente próximo, em tamanho de Utahraptor.
    "utahraptor": ("deinonychus", "deinonychus_male.png", {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 2.6),
    "spinosaurus": ("spinosaurus", "spinosaurus_male.png", {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 6.4),
    "allosaurus": ("allosaurus", "allosaurus_male.png", {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 3.8),
    # Sem Brontossauro no Revival: o Diplodoco, da mesma família.
    "brontosaurus": ("diplodocus", "diplodocus_male.png", {"idle": "idle", "walk": "walk", "attack": "attack", "unconscious": "sleep"}, 9.0),
}

# espécie nossa -> nosso som -> (evento de sounds.json do Revival, volume da espécie)
SOUNDS = {
    "smilodon": ({"ambient": "smilodon_ambient", "hurt": "smilodon_hurt", "death": "smilodon_death"}, 1.0),
    "mammoth": ({"ambient": "mammoth_ambient", "hurt": "mammoth_hurt", "death": "mammoth_death"}, 1.5),
    "tyrannosaurus": ({"ambient": "tyrannosaurus_ambient", "hurt": "tyrannosaurus_hurt",
                       "death": "tyrannosaurus_death", "alert": "tyrannosaurus_roar"}, 3.0),
    "velociraptor": ({"ambient": "velociraptor_ambient", "hurt": "velociraptor_hurt", "death": "velociraptor_death"}, 1.0),
    "utahraptor": ({"ambient": "deinonychus_ambient", "hurt": "deinonychus_hurt", "death": "deinonychus_death"}, 1.3),
    "spinosaurus": ({"ambient": "spinosaurus_ambient", "hurt": "spinosaurus_hurt", "death": "spinosaurus_death"}, 3.0),
    "allosaurus": ({"ambient": "allosaurus_ambient", "hurt": "allosaurus_hurt", "death": "allosaurus_death"}, 2.0),
    "brontosaurus": ({"ambient": "diplodocus_ambient", "hurt": "diplodocus_hurt", "death": "diplodocus_death"}, 4.0),
}
SPECIES_DATA = PROJECT / "src" / "main" / "resources" / "data" / "iceagesurvival" / "iceagesurvival" / "species"


def fetch_revival():
    if (CACHE / ".git").exists():
        return
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "clone", "--quiet", "--depth", "1", REVIVAL_REPO, str(CACHE)], check=True)


def adult_only(geometry):
    """Tira os ossos de filhote e de adolescente (babysail, teensail...), que o código do Revival esconde."""
    bones = geometry["minecraft:geometry"][0]["bones"]
    removed = {b["name"] for b in bones if b["name"].lower().startswith(("baby", "teen"))}
    changed = True
    while changed:
        changed = False
        for bone in bones:
            if bone.get("parent") in removed and bone["name"] not in removed:
                removed.add(bone["name"])
                changed = True
    geometry["minecraft:geometry"][0]["bones"] = [b for b in bones if b["name"] not in removed]
    return geometry


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


def install_sounds(source):
    """Copia os .ogg, escreve sounds.json e aponta o bloco "sounds" de cada espécie para eles."""
    theirs = json.loads((source / "sounds.json").read_text())
    events_file = ASSETS / "sounds.json"
    events = json.loads(events_file.read_text()) if events_file.exists() else {}
    for ours, (mapping, volume) in SOUNDS.items():
        folder = ASSETS / "sounds" / "entity" / ours
        if folder.exists():
            shutil.rmtree(folder)
        folder.mkdir(parents=True)
        block = {}
        for kind, their_event in mapping.items():
            files = []
            for sound in theirs[their_event]["sounds"]:
                name = (sound["name"] if isinstance(sound, dict) else sound).split(":", 1)[1]
                target = Path(name).name
                shutil.copyfile(source / "sounds" / f"{name}.ogg", folder / f"{target}.ogg")
                files.append(f"iceagesurvival:entity/{ours}/{target}")
            event = f"entity.{ours}.{kind}"
            events[event] = {"category": "neutral", "sounds": files,
                             "subtitle": f"subtitles.iceagesurvival.{event}"}
            block[kind] = f"iceagesurvival:{event}"
        block["volume"] = volume
        species_file = SPECIES_DATA / f"{ours}.json"
        species = json.loads(species_file.read_text())
        # Mescla: campos que não vêm do Revival (o som de ataque, vanilla) ficam.
        sounds = species.get("sounds", {})
        sounds.update(block)
        sounds["volume"] = sounds.pop("volume")
        species["sounds"] = sounds
        species_file.write_text(json.dumps(species, indent=2, ensure_ascii=False) + "\n")
        print(f"{ours}: sons {', '.join(mapping)}")
    events_file.write_text(json.dumps(dict(sorted(events.items())), indent=2) + "\n")


def mark_hand_authored(species):
    """Registra as espécies que o modelgen não deve sobrescrever."""
    existing = set()
    if HAND_AUTHORED.exists():
        existing = {line.split("#")[0].strip() for line in HAND_AUTHORED.read_text().splitlines()}
        existing.discard("")
    names = sorted(existing | set(species))
    HAND_AUTHORED.write_text(
        "# Espécies cujos assets NÃO vêm de tools/gen_<especie>.py e não devem ser regerados.\n"
        "# Preenchido por tools/install-revival-placeholders.py; o guarda está em tools/modelgen.py.\n"
        "# Arte do F&A Revival, All Rights Reserved — ver ASSET_LICENSES.md.\n"
        + "".join(f"{name}\n" for name in names))


def main():
    fetch_revival()
    source = CACHE / "common" / "src" / "main" / "resources" / "assets" / "fossil"
    for sub in ("geo/entity", "textures/entity", "animations/entity", "creature_models"):
        (ASSETS / sub).mkdir(parents=True, exist_ok=True)

    for ours, (theirs, texture, mapping, height) in SPECIES.items():
        geometry = adult_only(json.loads((source / "geo" / "entity" / f"{theirs}.geo.json").read_text()))
        # Indentado, ao contrário do pack local que isto substituiu: agora estes arquivos
        # vivem no git e são abertos à mão no Blockbench.
        (ASSETS / "geo" / "entity" / f"{ours}.geo.json").write_text(json.dumps(geometry, indent=2) + "\n")
        shutil.copyfile(source / "textures" / "entity" / theirs / texture,
                        ASSETS / "textures" / "entity" / f"{ours}.png")
        animations = convert_animations(ours, theirs, mapping, source / "animations" / f"{theirs}.animation.json")
        (ASSETS / "animations" / "entity" / f"{ours}.animation.json").write_text(
            json.dumps(animations, indent=2) + "\n")
        scale = round(height / model_height_blocks(geometry), 2)
        (ASSETS / "creature_models" / f"{ours}.json").write_text(json.dumps({"scale": scale}, indent=2) + "\n")
        print(f"{ours}: modelo do Revival, escala {scale}")

    install_sounds(source)
    mark_hand_authored(SPECIES)
    print(f"Assets em {ASSETS}")
    print("Arte All Rights Reserved: substituir antes de distribuir o mod (ver ASSET_LICENSES.md).")


if __name__ == "__main__":
    main()
