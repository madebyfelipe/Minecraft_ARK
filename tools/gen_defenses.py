#!/usr/bin/env python3
"""Gera os assets autorais dos blocos de defesa: muros altos, portões, armadilhas e a cobertura de folhagem.

Saída (só os ids das defesas):
  assets/iceagesurvival/textures/block/<id>*.png   texturas 16x16
  assets/iceagesurvival/models/block/<id>*.json    modelos de bloco
  assets/iceagesurvival/models/item/<id>.json      modelos de item
  assets/iceagesurvival/blockstates/<id>.json      estados
  data/iceagesurvival/loot_tables/blocks/<id>.json cada bloco derruba a si mesmo

Nada copiado de outros mods nem do vanilla: os pixels saem daqui, de uma semente fixa. Como os outros gen_*.py,
rodar de novo SOBRESCREVE edições feitas à mão.
"""

import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / "iceagesurvival"
DATA = ROOT / "data" / "iceagesurvival"
SIZE = 16
NS = "iceagesurvival"

# Paletas
BARK = [(92, 64, 40), (82, 56, 34), (102, 72, 46)]
LOG = [(150, 112, 68), (138, 102, 60), (160, 122, 76)]
RING = (112, 80, 48)
GAP = (40, 28, 18)
PLANK = [(140, 104, 62), (128, 94, 56), (150, 112, 68)]
PLANK_DARK = (86, 60, 36)
STONE = [(128, 128, 124), (118, 118, 114), (138, 138, 132), (110, 110, 106)]
MORTAR = (78, 76, 72)
IRON = [(150, 154, 158), (136, 140, 144), (166, 170, 174)]
IRON_DARK = (82, 86, 92)
RIVET = (204, 208, 212)
SPIKE = [(176, 150, 112), (160, 134, 98)]
SPIKE_TIP = (206, 190, 160)
THORN = (196, 186, 150)
VINE = [(58, 84, 40), (48, 72, 32), (70, 98, 48)]
LEAF = [(70, 110, 44), (60, 98, 38), (82, 124, 52), (54, 88, 34)]
TWIG = (96, 70, 42)
DIRT = [(110, 80, 54), (98, 70, 46), (120, 88, 60)]


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), tuple(color) + (255,))


def darker(color, amount=24):
    return tuple(max(0, c - amount) for c in color)


# ---- Texturas ----

def wood_wall_side(rng):
    """Estacas de tronco em pé, lado a lado, com a fresta escura entre elas."""
    img = canvas()
    for x in range(SIZE):
        stake = x // 4
        inner = x % 4
        for y in range(SIZE):
            if inner == 3:
                put(img, x, y, GAP)
            elif inner == 0:
                put(img, x, y, darker(rng.choice(BARK), 10))
            else:
                put(img, x, y, rng.choice(BARK))
        # Nós e amarração de cipó na altura de cima e de baixo.
        if inner != 3:
            for y in (3, 12):
                put(img, x, y, VINE[(stake + y) % len(VINE)])
    for _ in range(6):
        put(img, rng.randrange(SIZE), rng.randrange(SIZE), darker(BARK[0], 30))
    return img


def wood_wall_top(rng):
    """As pontas das estacas vistas de cima: quatro anéis de tronco por fileira."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if x % 4 == 3:
                put(img, x, y, GAP)
                continue
            cx = (x // 4) * 4 + 1
            cy = (y // 4) * 4 + 1.5
            d = abs(x - cx) + abs(y - cy)
            if y % 4 == 3:
                put(img, x, y, rng.choice(BARK))
            elif d < 1.2:
                put(img, x, y, RING)
            else:
                put(img, x, y, rng.choice(LOG))
    return img


def stone_wall_side(rng):
    """Muralha: blocos de pedra assentados em fiadas desencontradas, com argamassa."""
    img = canvas()
    for y in range(SIZE):
        course = y // 4
        offset = 0 if course % 2 == 0 else 4
        for x in range(SIZE):
            if y % 4 == 3 or (x + offset) % 8 == 7:
                put(img, x, y, MORTAR)
            else:
                shade = rng.choice(STONE)
                if y % 4 == 0:
                    shade = tuple(min(255, c + 12) for c in shade)
                put(img, x, y, shade)
    return img


def stone_wall_top(rng):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if x in (0, 15) or y in (0, 15) or (x == 7 and 3 < y < 12):
                put(img, x, y, MORTAR)
            else:
                put(img, x, y, rng.choice(STONE))
    return img


def wood_gate(rng, large):
    """Tábuas em pé com travessas; no grande, cintas de ferro no lugar das travessas."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if x % 4 == 0:
                put(img, x, y, PLANK_DARK)
            else:
                put(img, x, y, rng.choice(PLANK))
    rows = (2, 13) if not large else (1, 2, 13, 14)
    for y in rows:
        for x in range(SIZE):
            put(img, x, y, rng.choice(IRON) if large else darker(PLANK[0], 16))
    if large:
        for x in (2, 6, 10, 14):
            for y in (1, 14):
                put(img, x, y, RIVET)
    else:
        # Mão-francesa em diagonal entre as travessas.
        for i in range(3, 13):
            put(img, i + 1, 15 - i, darker(PLANK[1], 20))
            put(img, i + 2, 15 - i, darker(PLANK[1], 20))
    return img


def stone_gate(rng, large):
    """Laje de pedra lavrada num quadro de ferro."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, rng.choice(IRON))
            elif y == 8 and large:
                put(img, x, y, IRON_DARK)
            elif (x + y) % 7 == 0 and rng.random() < 0.4:
                put(img, x, y, MORTAR)
            else:
                put(img, x, y, rng.choice(STONE))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, RIVET)
    return img


def edge(rng, shades, dark):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            put(img, x, y, dark if x in (0, 15) else rng.choice(shades))
    return img


def spike_trap_base(rng):
    """Tábua de base com terra batida nas frestas."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if y % 5 == 4:
                put(img, x, y, rng.choice(DIRT))
            else:
                put(img, x, y, rng.choice(PLANK))
    for x, y in ((3, 2), (8, 7), (12, 12), (5, 11), (13, 3)):
        put(img, x, y, GAP)
    return img


def spike_trap_spikes(rng):
    """Estacas apontadas, de alturas desencontradas, na metade de baixo (o resto é vazio)."""
    img = canvas()
    for i, x0 in enumerate(range(0, SIZE, 4)):
        top = 9 + (i % 2)
        for y in range(top, SIZE):
            for x in (x0 + 1, x0 + 2):
                put(img, x, y, SPIKE[(x + y) % 2])
        put(img, x0 + 1, top - 1, SPIKE_TIP)
        put(img, x0 + 2, top - 1, SPIKE_TIP)
        put(img, x0 + 1, top - 2, SPIKE_TIP)
    return img


def thorn_palisade_side(rng):
    """Estacas finas e apontadas trançadas de galho espinhento; frestas só entre as pontas de cima."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            inner = x % 3
            if y < 2 and inner != 1:
                continue
            put(img, x, y, rng.choice(BARK) if inner != 2 else darker(BARK[1], 16))
    for y in (5, 6, 11, 12):
        for x in range(SIZE):
            if (x + y) % 3 != 0:
                put(img, x, y, rng.choice(VINE))
    for _ in range(14):
        x, y = rng.randrange(SIZE), rng.randrange(3, SIZE)
        put(img, x, y, THORN)
    return img


def thorn_palisade_top(rng):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if x % 3 == 1 and y % 3 == 1:
                put(img, x, y, RING)
            elif rng.random() < 0.55:
                put(img, x, y, rng.choice(VINE))
            else:
                put(img, x, y, rng.choice(BARK))
    for _ in range(10):
        put(img, rng.randrange(SIZE), rng.randrange(SIZE), THORN)
    return img


def bear_trap_open(rng):
    """Vista de cima da armadilha armada: aro de ferro com dentes para dentro e o prato no meio."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if 5.6 <= d <= 7.4:
                put(img, x, y, rng.choice(IRON))
            elif 4.4 <= d < 5.6 and (x + y) % 2 == 0:
                put(img, x, y, RIVET)
            elif d < 2.2:
                put(img, x, y, IRON_DARK)
    # A mola atravessada.
    for x in range(1, 15):
        if abs(x - 7.5) > 2.2:
            put(img, x, 7, IRON_DARK)
            put(img, x, 8, IRON_DARK)
    return img


def bear_trap_jaw(rng):
    """As duas mandíbulas fechadas vistas de lado: dentes em serra em cima, aro embaixo."""
    img = canvas()
    for x in range(SIZE):
        for y in range(10, SIZE):
            put(img, x, y, rng.choice(IRON) if y < 14 else IRON_DARK)
        tooth = 2 if x % 3 == 1 else (1 if x % 3 != 0 else 0)
        for y in range(10 - tooth, 10):
            put(img, x, y, RIVET)
    return img


def foliage_top(rng):
    """Galhos cruzados cobertos de folhas: parece chão de mata."""
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            put(img, x, y, rng.choice(LEAF))
    for i in range(SIZE):
        put(img, i, (i * 3 // 4 + 3) % SIZE, TWIG)
        put(img, (i * 5 // 7 + 9) % SIZE, i, TWIG)
    for _ in range(10):
        put(img, rng.randrange(SIZE), rng.randrange(SIZE), darker(LEAF[0], 28))
    return img


def foliage_side(rng):
    img = canvas()
    for x in range(SIZE):
        for y in range(SIZE):
            if y < 4 + (x * 7 % 3):
                put(img, x, y, rng.choice(LEAF))
            else:
                put(img, x, y, rng.choice(DIRT))
    for _ in range(6):
        put(img, rng.randrange(SIZE), rng.randrange(5, SIZE), TWIG)
    return img


# ---- Modelos ----

def block_model(name):
    return f"{NS}:block/{name}"


def tex(name):
    return f"{NS}:block/{name}"


def face(texture, uv, cull=None):
    out = {"texture": texture, "uv": uv}
    if cull:
        out["cullface"] = cull
    return out


def gate_model(texture, edge_texture):
    """A folha do portão: atravessada no meio do bloco, ao longo do eixo X (virada para norte/sul)."""
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": tex(texture), "panel": tex(texture), "edge": tex(edge_texture)},
        "elements": [{
            "from": [0, 0, 6], "to": [16, 16, 10],
            "faces": {
                "north": face("#panel", [0, 0, 16, 16]),
                "south": face("#panel", [16, 0, 0, 16]),
                "east": face("#edge", [6, 0, 10, 16], "east"),
                "west": face("#edge", [6, 0, 10, 16], "west"),
                "up": face("#edge", [0, 6, 16, 10], "up"),
                "down": face("#edge", [0, 6, 16, 10], "down"),
            },
        }],
    }


def spike_trap_model():
    planes = []
    for angle in (45, -45):
        planes.append({
            "from": [0, 1, 8], "to": [16, 8, 8],
            "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": True},
            "shade": False,
            "faces": {
                "north": face("#spikes", [0, 9, 16, 16]),
                "south": face("#spikes", [0, 9, 16, 16]),
            },
        })
    # Uma fileira paralela a cada eixo, para o tapete de estacas não sumir de lado.
    for z in (3, 13):
        planes.append({
            "from": [0, 1, z], "to": [16, 8, z], "shade": False,
            "faces": {"north": face("#spikes", [0, 9, 16, 16]), "south": face("#spikes", [0, 9, 16, 16])},
        })
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": tex("spike_trap_base"), "base": tex("spike_trap_base"),
                     "spikes": tex("spike_trap_spikes")},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 1, 16],
            "faces": {
                "up": face("#base", [0, 0, 16, 16]),
                "down": face("#base", [0, 0, 16, 16], "down"),
                "north": face("#base", [0, 15, 16, 16]),
                "south": face("#base", [0, 15, 16, 16]),
                "east": face("#base", [0, 15, 16, 16]),
                "west": face("#base", [0, 15, 16, 16]),
            },
        }] + planes,
    }


def bear_trap_model():
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": tex("bear_trap"), "top": tex("bear_trap")},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 1, 16],
            "faces": {"up": face("#top", [0, 0, 16, 16]), "down": face("#top", [0, 16, 16, 0], "down")},
        }],
    }


def bear_trap_closed_model():
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": tex("bear_trap_jaw"), "top": tex("bear_trap"), "jaw": tex("bear_trap_jaw")},
        "elements": [
            {
                "from": [1, 0, 6.5], "to": [15, 6, 9.5],
                "faces": {
                    "north": face("#jaw", [1, 10, 15, 16]),
                    "south": face("#jaw", [1, 10, 15, 16]),
                    "east": face("#jaw", [6, 10, 9, 16]),
                    "west": face("#jaw", [6, 10, 9, 16]),
                    "up": face("#jaw", [1, 12, 15, 13]),
                    "down": face("#jaw", [1, 14, 15, 15], "down"),
                },
            },
            {
                # A mola, ainda no chão.
                "from": [2, 0, 2], "to": [14, 0.5, 14],
                "faces": {"up": face("#top", [2, 2, 14, 14]), "down": face("#top", [2, 14, 14, 2], "down")},
            },
        ],
    }


def main():
    rng = random.Random(1717)
    textures = {
        "wood_wall_side": wood_wall_side(rng),
        "wood_wall_top": wood_wall_top(rng),
        "stone_wall_side": stone_wall_side(rng),
        "stone_wall_top": stone_wall_top(rng),
        "wood_gate": wood_gate(rng, False),
        "wood_gate_edge": edge(rng, PLANK, PLANK_DARK),
        "large_wood_gate": wood_gate(rng, True),
        "stone_gate": stone_gate(rng, False),
        "stone_gate_edge": edge(rng, STONE, IRON_DARK),
        "large_stone_gate": stone_gate(rng, True),
        "spike_trap_base": spike_trap_base(rng),
        "spike_trap_spikes": spike_trap_spikes(rng),
        "thorn_palisade_side": thorn_palisade_side(rng),
        "thorn_palisade_top": thorn_palisade_top(rng),
        "bear_trap": bear_trap_open(rng),
        "bear_trap_jaw": bear_trap_jaw(rng),
        "foliage_cover_top": foliage_top(rng),
        "foliage_cover_side": foliage_side(rng),
    }

    models = {
        "wood_wall": {"parent": "minecraft:block/cube_column",
                      "textures": {"end": tex("wood_wall_top"), "side": tex("wood_wall_side")}},
        "stone_wall": {"parent": "minecraft:block/cube_column",
                       "textures": {"end": tex("stone_wall_top"), "side": tex("stone_wall_side")}},
        "wood_gate": gate_model("wood_gate", "wood_gate_edge"),
        "stone_gate": gate_model("stone_gate", "stone_gate_edge"),
        "large_wood_gate": gate_model("large_wood_gate", "wood_gate_edge"),
        "large_stone_gate": gate_model("large_stone_gate", "stone_gate_edge"),
        "spike_trap": spike_trap_model(),
        "thorn_palisade": {"parent": "minecraft:block/cube_bottom_top", "render_type": "minecraft:cutout",
                           "textures": {"top": tex("thorn_palisade_top"), "bottom": tex("thorn_palisade_top"),
                                        "side": tex("thorn_palisade_side")}},
        "bear_trap": bear_trap_model(),
        "bear_trap_closed": bear_trap_closed_model(),
        "foliage_cover": {"parent": "minecraft:block/cube_bottom_top",
                          "textures": {"top": tex("foliage_cover_top"), "bottom": tex("foliage_cover_side"),
                                       "side": tex("foliage_cover_side")}},
    }

    single = ["wood_wall", "stone_wall", "spike_trap", "thorn_palisade", "foliage_cover"]
    gates = ["wood_gate", "stone_gate", "large_wood_gate", "large_stone_gate"]
    blockstates = {name: {"variants": {"": {"model": block_model(name)}}} for name in single}
    rotation = {"north": 0, "east": 90, "south": 180, "west": 270}
    for gate in gates:
        variants = {}
        for facing, y in rotation.items():
            for open_ in (False, True):
                variant = {"model": block_model(gate)}
                angle = (y + (90 if open_ else 0)) % 360
                if angle:
                    variant["y"] = angle
                variants[f"facing={facing},open={str(open_).lower()}"] = variant
        blockstates[gate] = {"variants": variants}
    blockstates["bear_trap"] = {"variants": {
        "stage=armed": {"model": block_model("bear_trap")},
        "stage=holding": {"model": block_model("bear_trap_closed")},
        "stage=sprung": {"model": block_model("bear_trap_closed")},
    }}

    ids = single + gates + ["bear_trap"]
    items = {name: {"parent": block_model(name)} for name in ids}

    def loot(name):
        return {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
            "random_sequence": f"{NS}:blocks/{name}",
        }

    def write_json(path, value):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    for name, img in textures.items():
        path = ASSETS / "textures" / "block" / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    for name, model in models.items():
        write_json(ASSETS / "models" / "block" / f"{name}.json", model)
    for name, state in blockstates.items():
        write_json(ASSETS / "blockstates" / f"{name}.json", state)
    for name, model in items.items():
        write_json(ASSETS / "models" / "item" / f"{name}.json", model)
    for name in ids:
        write_json(DATA / "loot_tables" / "blocks" / f"{name}.json", loot(name))
    print(f"{len(textures)} texturas, {len(models)} modelos, {len(blockstates)} estados, "
          f"{len(items)} itens e {len(ids)} loot tables das defesas")


if __name__ == "__main__":
    main()
