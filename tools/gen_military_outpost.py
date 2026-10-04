#!/usr/bin/env python3
"""Gera o template do posto militar (D52): data/iceagesurvival/structures/military_outpost.nbt.

A torre de vigia é o modelo do Felipe no BuildPaste (/paste 2erEqlek38JmdkBn5xLG), baixado como veio da API em
tools/outpost/torre_buildpaste.json: "size" [x, y, z], "blocks" (índice na tabela de blocos do BuildPaste) e "data"
(propriedades do estado, "[a=b,c=d]" ou null), na ordem índice = (x * Y + y) * Z + z. O script converte isso num
template de estrutura do vanilla e acrescenta o que é do mod:

- o Terminal Militar e um baú com o saque (loot table chests/military_outpost) no térreo, de frente para a porta;
- o perímetro: muro de pedra do mod (2 de altura) em quadrado a 4 blocos da torre, sobre uma fiada de pedregulho, com o
  Portão de Pedra Grande (5 × 5) alinhado à porta da torre e um pilar de muro de cada lado até a altura do portão.

O que não é bloco do posto fica fora do template (equivale a structure_void): o terreno do pátio fica como está. Dentro
da torre o ar é gravado, para limpar o terreno.

Uso: python3 tools/gen_military_outpost.py
"""
import gzip
import io
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / 'tools/outpost/torre_buildpaste.json'
OUT = ROOT / 'src/main/resources/data/iceagesurvival/structures/military_outpost.nbt'
DATA_VERSION = 3465  # 1.20.1

# Os índices da tabela de blocos do BuildPaste 1.11 que aparecem no modelo.
BUILDPASTE_IDS = {
    0: 'air', 1: 'stone', 6: 'andesite', 12: 'cobblestone', 54: 'acacia_wood', 118: 'stone_slab',
    155: 'ladder', 191: 'acacia_trapdoor', 206: 'iron_bars', 280: 'iron_trapdoor', 289: 'gray_carpet',
    602: 'smooth_stone_slab', 623: 'stone_stairs', 732: 'warped_door', 853: 'tuff',
}

# Recorte da torre dentro do modelo (o resto do volume é vazio), em coordenadas do modelo.
TOWER_X = (4, 10)
TOWER_Z = (5, 11)
FIRST_LAYER = 1          # a camada 0 do modelo é uma segunda fiada de alicerce: a 1 vira o chão do template
GAP = 4                  # pátio entre o muro e a torre
WALL_HEIGHT = 2
GATE = 5                 # portão grande 5 × 5
DOOR_X = 6               # a porta da torre (face norte, z = TOWER_Z[0]) no modelo

TERMINAL = (8, 10)       # térreo, encostado na parede do fundo, no modelo
CHEST = (9, 10)


def parse_props(data):
    if not data:
        return {}
    return dict(p.split('=', 1) for p in data.strip('[]').split(',') if p)


# ---- NBT mínimo ----

def tag_string(s):
    raw = s.encode('utf-8')
    return struct.pack('>H', len(raw)) + raw


def payload(value):
    """(id do tipo, bytes do valor) para int, str, dict (compound) e list."""
    if isinstance(value, bool):
        return 1, struct.pack('>b', int(value))
    if isinstance(value, int):
        return 3, struct.pack('>i', value)
    if isinstance(value, str):
        return 8, tag_string(value)
    if isinstance(value, dict):
        out = b''
        for key, item in value.items():
            tid, body = payload(item)
            out += struct.pack('>b', tid) + tag_string(key) + body
        return 10, out + b'\x00'
    if isinstance(value, list):
        if not value:
            return 9, struct.pack('>bi', 0, 0)
        tids = {payload(v)[0] for v in value}
        assert len(tids) == 1, 'lista com tipos diferentes'
        tid = tids.pop()
        return 9, struct.pack('>bi', tid, len(value)) + b''.join(payload(v)[1] for v in value)
    raise TypeError(value)


def write_nbt(path, root):
    data = struct.pack('>b', 10) + tag_string('') + payload(root)[1]
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode='wb', mtime=0) as gz:
        gz.write(data)
    path.write_bytes(buf.getvalue())


# ---- Montagem ----

def main():
    build = json.loads(SOURCE.read_text(encoding='utf-8'))
    sx, sy, sz = build['size']
    blocks = {}  # (x, y, z) do template -> (nome, props, nbt)

    def put(x, y, z, name, props=None, nbt=None):
        blocks[(x, y, z)] = (name, props or {}, nbt)

    tower_w = TOWER_X[1] - TOWER_X[0] + 1
    size = tower_w + 2 * GAP + 2
    ox = oz = GAP + 1        # canto da torre no template
    height = 0
    for x in range(TOWER_X[0], TOWER_X[1] + 1):
        for z in range(TOWER_Z[0], TOWER_Z[1] + 1):
            for y in range(FIRST_LAYER, sy):
                i = (x * sy + y) * sz + z
                block_id = build['blocks'][i]
                if block_id not in BUILDPASTE_IDS:
                    raise SystemExit(f'id do BuildPaste sem nome: {block_id} em {(x, y, z)}')
                name = BUILDPASTE_IDS[block_id]
                ty = y - FIRST_LAYER
                if name != 'air':
                    height = max(height, ty + 1)
                put(x - TOWER_X[0] + ox, ty, z - TOWER_Z[0] + oz, 'minecraft:' + name, parse_props(build['data'][i]))
    # o ar acima do topo da torre não precisa ir no template
    blocks = {p: b for p, b in blocks.items() if p[1] < height}

    def tower(mx, my, mz):
        return mx - TOWER_X[0] + ox, my - FIRST_LAYER, mz - TOWER_Z[0] + oz

    floor = 2  # primeira camada livre dentro da torre, no modelo
    for (mx, mz), name, props, nbt in (
            (TERMINAL, 'iceagesurvival:military_terminal', {'facing': 'north'}, None),
            (CHEST, 'minecraft:chest', {'facing': 'north', 'type': 'single', 'waterlogged': 'false'},
             {'LootTable': 'iceagesurvival:chests/military_outpost'})):
        pos = tower(mx, floor, mz)
        assert blocks[pos][0] == 'minecraft:air', f'{name} cairia sobre {blocks[pos][0]}'
        put(*pos, name, props, nbt)

    # Perímetro: fiada de pedregulho no chão (y 0), muro em cima; portão grande na frente (norte, z = 0).
    door_x = tower(DOOR_X, floor, TOWER_Z[0])[0]
    gate_from = door_x - GATE // 2
    gate_to = door_x + GATE // 2
    last = size - 1
    for a in range(size):
        for x, z in ((a, 0), (a, last), (0, a), (last, a)):
            put(x, 0, z, 'minecraft:cobblestone')
            if z == 0 and gate_from <= x <= gate_to:
                continue
            post = z == 0 and x in (gate_from - 1, gate_to + 1)
            for y in range(1, 1 + (GATE if post else WALL_HEIGHT)):
                put(x, y, z, 'iceagesurvival:stone_wall')
    for column in range(GATE):
        for row in range(GATE):
            # facing=north: as colunas crescem para a direita de quem olha para o norte (leste, +x)
            props = {'facing': 'north', 'open': 'false', 'column': str(column), 'row': str(row)}
            put(gate_from + column, 1 + row, 0, 'iceagesurvival:large_stone_gate', props)

    palette, index = [], {}
    entries = []
    for (x, y, z), (name, props, nbt) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {'Name': name}
            if props:
                entry['Properties'] = dict(sorted(props.items()))
            palette.append(entry)
        block = {'pos': [x, y, z], 'state': index[key]}
        if nbt:
            block['nbt'] = nbt
        entries.append(block)

    root = {
        'DataVersion': DATA_VERSION,
        'size': [size, max(height, 1 + GATE), size],
        'palette': palette,
        'blocks': entries,
        'entities': [],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    write_nbt(OUT, root)
    print(f'{OUT.relative_to(ROOT)}: {root["size"]}, {len(entries)} blocos, {len(palette)} estados; '
          f'porta da torre em x={door_x}, portão x={gate_from}..{gate_to}')


if __name__ == '__main__':
    main()
