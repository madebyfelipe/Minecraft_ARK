"""Biblioteca dos geradores de criatura: geometria Bedrock, textura e animações para o GeckoLib.

Cada espécie tem um script gen_<especie>.py que descreve ossos, cores e detalhes e chama
build(). Os arquivos saem direto em src/main/resources, nos caminhos que o GeckoLib espera.
Rodar um gerador de novo SOBRESCREVE edições feitas à mão no Blockbench.

Convenções da geometria Bedrock: unidades de 1/16 de bloco, Y para cima, a criatura olha
para -Z, origem no chão entre as patas. O tamanho final no jogo vem de body.model_scale
no JSON da espécie.
"""

import json
import random
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "iceagesurvival"


@dataclass
class Cube:
    """Um cubo de um osso. Cubos com o mesmo `name` dividem a mesma região da textura."""
    name: str
    origin: tuple
    size: tuple
    mirror: bool = False


@dataclass
class Bone:
    name: str
    parent: str | None
    pivot: tuple
    cubes: list = field(default_factory=list)


@dataclass
class Palette:
    fur: tuple
    top: tuple
    belly: tuple
    noise: int = 9
    # Cor base de cubos específicos (presas, cascos...), por nome de cubo.
    overrides: dict = field(default_factory=dict)


@dataclass
class Gait:
    """Parâmetros das animações padrão."""
    legs_a: tuple  # patas que se movem juntas (uma diagonal)
    legs_b: tuple  # a outra diagonal
    head: str  # osso que balança parado e avança no ataque
    tail: str | None = None
    jaw: str | None = None
    leg_swing: float = 28.0
    walk_length: float = 0.8
    # Quanto erguer o corpo tombado para não afundar no chão: meia largura do tronco.
    fallen_lift: float = 5.0


class Texture:
    """Tela de pintura com as regiões de box UV já calculadas."""

    def __init__(self, width, height, seed):
        self.image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def fill(self, rect, color, noise=9):
        x0, y0, x1, y1 = rect
        for x in range(x0, x1):
            for y in range(y0, y1):
                delta = self.rng.randint(-noise, noise)
                self.image.putpixel((x, y), tuple(max(0, min(255, c + delta)) for c in color) + (255,))

    def pixel(self, x, y, color):
        self.image.putpixel((x, y), tuple(color) + (255,))

    def band_at_bottom(self, faces, height, color):
        """Faixa na base das quatro faces laterais (patas escuras, barriga clara...)."""
        for key in ("side_a", "front", "side_b", "back"):
            x0, _, x1, y1 = faces[key]
            self.fill((x0, y1 - height, x1, y1), color)


def box_faces(u, v, w, h, d):
    """Retângulos (x0, y0, x1, y1) de cada face no layout de box UV. `front` é a face -Z."""
    return {
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + d + 2 * w, v + d),
        "side_a": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "side_b": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def pack_uvs(bones, tex_w, tex_h):
    """Empacota em prateleiras a região de box UV de cada cubo distinto (por nome)."""
    sizes = {}
    for bone in bones:
        for cube in bone.cubes:
            if sizes.setdefault(cube.name, cube.size) != cube.size:
                raise SystemExit(f"Cubos '{cube.name}' com tamanhos diferentes não podem dividir textura")
    order = sorted(sizes, key=lambda n: -(sizes[n][2] + sizes[n][1]))
    uvs, x, y, shelf = {}, 0, 0, 0
    for name in order:
        w, h, d = sizes[name]
        rw, rh = 2 * (w + d), d + h
        if rw > tex_w:
            raise SystemExit(f"Cubo '{name}' não cabe na largura {tex_w}")
        if x + rw > tex_w:
            x, y, shelf = 0, y + shelf, 0
        uvs[name] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    if y + shelf > tex_h:
        raise SystemExit(f"UVs não cabem em {tex_w}x{tex_h} (precisa de {y + shelf} de altura)")
    return uvs, sizes


def geometry(species, bones, uvs, tex_w, tex_h):
    out = []
    for bone in bones:
        entry = {"name": bone.name, "pivot": list(bone.pivot)}
        if bone.parent:
            entry["parent"] = bone.parent
        if bone.cubes:
            entry["cubes"] = []
            for cube in bone.cubes:
                data = {"origin": list(cube.origin), "size": list(cube.size), "uv": list(uvs[cube.name])}
                if cube.mirror:
                    data["mirror"] = True
                entry["cubes"].append(data)
        out.append(entry)
    top = max(c.origin[1] + c.size[1] for b in bones for c in b.cubes)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{species}",
                "texture_width": tex_w,
                "texture_height": tex_h,
                "visible_bounds_width": 6,
                "visible_bounds_height": max(3, round(top / 16) + 2),
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": out,
        }],
    }


def _swing(amplitude, length, opposite=False):
    a = -amplitude if opposite else amplitude
    return {"rotation": {"0.0": [a, 0, 0], str(length / 2): [-a, 0, 0], str(length): [a, 0, 0]}}


def animations(species, gait):
    """As quatro animações que o código do mod espera: idle, walk, attack, unconscious."""
    walk = gait.walk_length
    idle_bones = {gait.head: {"rotation": {"0.0": [0, 0, 0], "1.5": [3, 0, 0], "3.0": [0, 0, 0]}}}
    walk_bones = {}
    for leg in gait.legs_a:
        walk_bones[leg] = _swing(gait.leg_swing, walk)
    for leg in gait.legs_b:
        walk_bones[leg] = _swing(gait.leg_swing, walk, opposite=True)
    if gait.tail:
        idle_bones[gait.tail] = {"rotation": {"0.0": [0, -10, 0], "1.5": [0, 10, 0], "3.0": [0, -10, 0]}}
        walk_bones[gait.tail] = {"rotation": {"0.0": [0, -6, 0], str(walk / 2): [0, 6, 0], str(walk): [0, -6, 0]}}
    attack_bones = {gait.head: {"rotation": {"0.0": [0, 0, 0], "0.1": [-18, 0, 0], "0.25": [16, 0, 0], "0.4": [0, 0, 0]}}}
    if gait.jaw:
        attack_bones[gait.jaw] = {"rotation": {"0.0": [0, 0, 0], "0.1": [32, 0, 0], "0.25": [0, 0, 0]}}
    prefix = f"animation.{species}."
    return {
        "format_version": "1.8.0",
        "animations": {
            prefix + "idle": {"loop": True, "animation_length": 3.0, "bones": idle_bones},
            prefix + "walk": {"loop": True, "animation_length": walk, "bones": walk_bones},
            prefix + "attack": {"animation_length": 0.4, "bones": attack_bones},
            # Tombado de lado e imóvel; o deslocamento compensa a rotação em torno da origem.
            prefix + "unconscious": {"loop": True, "animation_length": 1.0, "bones": {
                "root": {"rotation": [0, 0, 90], "position": [0, gait.fallen_lift, 0]}}},
        },
    }


def build(species, bones, palette, gait, tex_size, details=None, seed=1):
    """Gera e grava geometria, textura e animações da espécie.

    `details(texture, cube_name, faces)` é chamado para cada cubo distinto depois da
    pintura base, para olhos, focinho, faixas etc.
    """
    tex_w, tex_h = tex_size
    uvs, sizes = pack_uvs(bones, tex_w, tex_h)
    texture = Texture(tex_w, tex_h, seed)
    for name, (u, v) in uvs.items():
        faces = box_faces(u, v, *sizes[name])
        override = palette.overrides.get(name)
        for key, rect in faces.items():
            if override:
                texture.fill(rect, override, noise=4)
            else:
                color = {"up": palette.top, "down": palette.belly}.get(key, palette.fur)
                texture.fill(rect, color, palette.noise)
        if details:
            details(texture, name, faces)

    paths = {
        "geo": ASSETS / "geo" / "entity" / f"{species}.geo.json",
        "texture": ASSETS / "textures" / "entity" / f"{species}.png",
        "animations": ASSETS / "animations" / "entity" / f"{species}.animation.json",
    }
    for path in paths.values():
        path.parent.mkdir(parents=True, exist_ok=True)
    paths["geo"].write_text(json.dumps(geometry(species, bones, uvs, tex_w, tex_h), indent=2) + "\n")
    texture.image.save(paths["texture"])
    paths["animations"].write_text(json.dumps(animations(species, gait), indent=2) + "\n")
    cubes = sum(len(b.cubes) for b in bones)
    print(f"{species}: {len(bones)} ossos, {cubes} cubos, textura {tex_w}x{tex_h}")
