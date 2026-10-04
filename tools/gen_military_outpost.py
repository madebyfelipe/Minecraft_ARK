#!/usr/bin/env python3
"""Gera os templates dos postos militares (D52) a partir das builds do Felipe no BuildPaste.

    torre     -> data/iceagesurvival/structures/military_outpost.nbt          (/paste 2erEqlek38JmdkBn5xLG)
    complexo  -> data/iceagesurvival/structures/military_outpost_complex.nbt  (/paste gIS6gHHBNG7BFojwO3Yo)
    base      -> data/iceagesurvival/structures/military_base.nbt             (/paste OYZKoUYOVRxXrp49GM6b)

Cada build está como veio da API em tools/outpost/<nome>_buildpaste.json: "size" [x, y, z], "blocks" (índice na
tabela de blocos do BuildPaste 1.11, ou o id em texto quando o bloco é de outro mod), "data" (propriedades do estado,
"[a=b,c=d]" ou null) e "nbt" (SNBT dos blocos com entidade, pela posição), na ordem índice = (x * Y + y) * Z + z.

O script converte isso num template de estrutura do vanilla e acrescenta o que é do mod:

- torre: recorte da torre, o Terminal Militar e um baú no térreo de frente para a porta, e o perímetro (muro de pedra
  do mod em quadrado a 4 blocos da torre, com o Portão de Pedra Grande alinhado à porta e pilares dos lados dele);
- complexo: a build inteira; blocos de outros mods trocados por blocos do vanilla e do mod (a bancada de armas vira a
  Bancada de Armeiro, os baús do Lootr viram baús comuns), todos os baús com o saque dos postos, as placas em português
  (as originais citavam outro jogo) e o terminal ao lado do baú do térreo.

- base: o hangar inteiro; no centro do salão, o Núcleo da Contenção, com um Gerador do Campo em cada canto do salão;
  terminais da série "base" ao lado de oito baús, e todos os baús com o saque da base.

Na torre, o que não é bloco do posto fica fora do template (structure_void): o terreno do pátio fica como está. No
complexo o ar entra no template, para abrir o terreno no lugar dos prédios.

Uso: python3 tools/gen_military_outpost.py
"""
import gzip
import io
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / 'src/main/resources/data/iceagesurvival/structures'
DATA_VERSION = 3465  # 1.20.1
LOOT = 'iceagesurvival:chests/military_outpost'
BASE_LOOT = 'iceagesurvival:chests/military_base'
# O baú do térreo do complexo, junto do terminal: o saque dos postos com o Receptor de Sinal garantido.
COMMAND_LOOT = 'iceagesurvival:chests/military_outpost_command'

# Os índices da tabela de blocos do BuildPaste 1.11 que aparecem nas builds (a tabela fica dentro do jar do mod).
BUILDPASTE_IDS = {
    8: 'grass_block', 66: 'lapis_block', 70: 'cut_sandstone', 71: 'note_block', 110: 'gold_block', 111: 'iron_block',
    112: 'oak_slab', 119: 'sandstone_slab', 122: 'brick_slab', 124: 'nether_brick_slab', 152: 'crafting_table',
    185: 'glowstone', 187: 'oak_trapdoor', 207: 'glass_pane', 220: 'nether_bricks', 221: 'nether_brick_fence',
    222: 'nether_brick_stairs', 277: 'red_terracotta', 314: 'light_blue_stained_glass', 463: 'birch_door',
    0: 'air', 1: 'stone', 6: 'andesite', 7: 'polished_andesite', 9: 'dirt', 10: 'coarse_dirt', 12: 'cobblestone',
    14: 'spruce_planks', 18: 'dark_oak_planks', 46: 'stripped_birch_wood', 54: 'acacia_wood', 67: 'dispenser',
    76: 'grass', 81: 'piston', 89: 'gray_wool', 113: 'spruce_slab', 118: 'stone_slab', 121: 'cobblestone_slab',
    134: 'smooth_stone', 149: 'chest', 154: 'furnace', 155: 'ladder', 156: 'rail', 158: 'lever',
    159: 'stone_pressure_plate', 168: 'stone_button', 176: 'spruce_fence', 179: 'acacia_fence',
    188: 'spruce_trapdoor', 191: 'acacia_trapdoor', 199: 'stone_bricks', 202: 'chiseled_stone_bricks',
    206: 'iron_bars', 228: 'redstone_lamp', 232: 'tripwire_hook', 234: 'spruce_stairs', 239: 'cobblestone_wall',
    253: 'daylight_detector', 254: 'redstone_block', 256: 'hopper', 260: 'quartz_stairs', 272: 'cyan_terracotta',
    280: 'iron_trapdoor', 282: 'white_carpet', 289: 'gray_carpet', 290: 'light_gray_carpet', 299: 'coal_block',
    309: 'tall_grass', 316: 'lime_stained_glass', 318: 'gray_stained_glass', 331: 'yellow_stained_glass_pane',
    335: 'light_gray_stained_glass_pane', 349: 'sea_lantern', 359: 'bone_block', 361: 'observer',
    402: 'gray_concrete', 418: 'gray_concrete_powder', 448: 'tube_coral_fan', 452: 'horn_coral_fan',
    456: 'dead_fire_coral_fan', 460: 'iron_door', 462: 'spruce_door', 467: 'repeater', 475: 'cake',
    526: 'redstone_wire', 528: 'redstone_wall_torch', 536: 'tripwire', 546: 'potted_blue_orchid',
    548: 'potted_azure_bluet', 573: 'gray_wall_banner', 581: 'black_wall_banner', 602: 'smooth_stone_slab',
    612: 'andesite_wall', 623: 'stone_stairs', 625: 'smooth_quartz_stairs', 629: 'polished_andesite_stairs',
    642: 'polished_andesite_slab', 647: 'oak_sign', 655: 'barrel', 656: 'smoker', 657: 'blast_furnace',
    665: 'lantern', 672: 'spruce_wall_sign', 675: 'jungle_wall_sign', 676: 'dark_oak_wall_sign',
    707: 'polished_blackstone_pressure_plate', 716: 'chain', 724: 'polished_blackstone_wall',
    728: 'polished_blackstone_button', 732: 'warped_door', 761: 'warped_wall_sign', 778: 'candle',
    823: 'lightning_rod', 838: 'polished_deepslate', 839: 'polished_deepslate_slab',
    840: 'polished_deepslate_stairs', 852: 'tinted_glass', 853: 'tuff', 862: 'waxed_oxidized_copper',
    865: 'waxed_oxidized_cut_copper_stairs', 866: 'waxed_weathered_copper', 878: 'ochre_froglight',
    891: 'mangrove_roots', 893: 'mangrove_slab', 979: 'bamboo_trapdoor',
}

# Blocos de outros mods (e plantas de clima quente) -> (bloco, propriedades novas ou None para manter as do modelo).
REPLACE = {
    'dotf:tungsten_block': ('minecraft:iron_block', {}),
    'dotf:biomass_flood_rock_block': ('minecraft:mossy_cobblestone', {}),
    'dotf:green_flood_mossy_block': ('minecraft:moss_block', {}),
    'dotf:flood_biomass_infected_block': ('minecraft:mossy_cobblestone', {}),
    'dotf:flood_bloom': ('minecraft:air', {}),
    'dotf:health_pack': ('minecraft:air', {}),
    'dotf:gun_workbench': ('iceagesurvival:armory_bench', {}),
    'lootr:lootr_chest': ('minecraft:chest', None),
    'minecraft:grass': ('minecraft:air', {}),
    'minecraft:tall_grass': ('minecraft:air', {}),
}

# As placas do complexo, pelo texto original de cada linha.
SIGN_TEXT = {
    'UNSC': 'PROJETO LIMIAR', 'TEMPORARY OBS': 'OBS. TEMPORÁRIA', 'LAB': 'LABORATÓRIO',
    'RESTRICTED AREA': 'ÁREA RESTRITA', 'NO TRESPASSING!': 'ENTRADA PROIBIDA',
    'WARNING': 'PERIGO', 'BIOHAZARD': 'RISCO BIOLÓGICO', "'The Hive'": "'ALA INFERIOR'",
    'EXP. [REDACTED]': 'EXP. [SIGILO]', 'EXP [REDACTED]': 'EXP [SIGILO]', "'[Redacted]'": '[SIGILO]',
    "'ELITE FORM'": "'FORMA ADULTA'", "'[ERROR]'": "'[ERRO]'",
}


def parse_props(data):
    if not data:
        return {}
    return dict(p.split('=', 1) for p in data.strip('[]').split(',') if p)


# ---- SNBT (o "nbt" da API) ----

class Typed:
    """Número com o tipo NBT explícito (b, s, i, l, f, d)."""

    def __init__(self, kind, value):
        self.kind, self.value = kind, value


def parse_snbt(text):
    pos = 0

    def ws():
        nonlocal pos
        while pos < len(text) and text[pos] in ' \n\t':
            pos += 1

    def string():
        nonlocal pos
        quote = text[pos]
        pos += 1
        out = ''
        while text[pos] != quote:
            if text[pos] == '\\':
                pos += 1
            out += text[pos]
            pos += 1
        pos += 1
        return out

    def bare():
        nonlocal pos
        start = pos
        while pos < len(text) and re.match(r'[0-9A-Za-z_\-.+]', text[pos]):
            pos += 1
        return text[start:pos]

    def value():
        nonlocal pos
        ws()
        c = text[pos]
        if c == '{':
            pos += 1
            out = {}
            ws()
            while text[pos] != '}':
                key = string() if text[pos] in '"\'' else bare()
                ws()
                assert text[pos] == ':'
                pos += 1
                out[key] = value()
                ws()
                if text[pos] == ',':
                    pos += 1
                    ws()
            pos += 1
            return out
        if c == '[':
            pos += 1
            if re.match(r'[BIL];', text[pos:pos + 2]):
                kind = text[pos]
                pos += 2
                items = []
                ws()
                while text[pos] != ']':
                    items.append(int(bare().rstrip('bBlL')))
                    ws()
                    if text[pos] == ',':
                        pos += 1
                        ws()
                pos += 1
                return ('array', kind, items)
            out = []
            ws()
            while text[pos] != ']':
                out.append(value())
                ws()
                if text[pos] == ',':
                    pos += 1
                    ws()
            pos += 1
            return out
        if c in '"\'':
            return string()
        token = bare()
        m = re.fullmatch(r'(-?[0-9.]+(?:[eE][-+]?\d+)?)([bBsSlLfFdD]?)', token)
        if not m:
            return token if token not in ('true', 'false') else Typed('b', int(token == 'true'))
        num, suffix = m.groups()
        suffix = suffix.lower()
        if suffix in ('f', 'd') or '.' in num:
            return Typed(suffix or 'd', float(num))
        return Typed(suffix or 'i', int(num))

    return value()


# ---- NBT binário ----

def tag_string(s):
    raw = s.encode('utf-8')
    return struct.pack('>H', len(raw)) + raw


def payload(value):
    if isinstance(value, Typed):
        fmt = {'b': (1, '>b'), 's': (2, '>h'), 'i': (3, '>i'), 'l': (4, '>q'), 'f': (5, '>f'), 'd': (6, '>d')}
        tid, f = fmt[value.kind]
        return tid, struct.pack(f, value.value)
    if isinstance(value, tuple) and value[0] == 'array':
        _, kind, items = value
        tid, f = {'B': (7, '>b'), 'I': (11, '>i'), 'L': (12, '>q')}[kind]
        return tid, struct.pack('>i', len(items)) + b''.join(struct.pack(f, i) for i in items)
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
        parts = [payload(v) for v in value]
        tids = {p[0] for p in parts}
        assert len(tids) == 1, 'lista com tipos diferentes'
        return 9, struct.pack('>bi', tids.pop(), len(value)) + b''.join(p[1] for p in parts)
    raise TypeError(value)


def write_nbt(path, root):
    data = struct.pack('>b', 10) + tag_string('') + payload(root)[1]
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode='wb', mtime=0) as gz:
        gz.write(data)
    path.write_bytes(buf.getvalue())


# ---- Leitura da build ----

class Build:
    def __init__(self, name):
        self.raw = json.loads((ROOT / f'tools/outpost/{name}_buildpaste.json').read_text(encoding='utf-8'))
        self.sx, self.sy, self.sz = self.raw['size']

    def index(self, x, y, z):
        return (x * self.sy + y) * self.sz + z

    def block(self, x, y, z):
        """(nome com namespace, propriedades, SNBT ou None)."""
        i = self.index(x, y, z)
        raw = self.raw['blocks'][i]
        if isinstance(raw, str):
            name = raw
        elif raw in BUILDPASTE_IDS:
            name = 'minecraft:' + BUILDPASTE_IDS[raw]
        else:
            raise SystemExit(f'id do BuildPaste sem nome: {raw} em {(x, y, z)}')
        props = parse_props(self.raw['data'][i])
        if name in REPLACE:
            name, new_props = REPLACE[name]
            if new_props is not None:
                props = new_props
        if name == 'minecraft:air':
            props = {}
        return name, props, (self.raw.get('nbt') or {}).get(str(i))


def block_entity(name, snbt, loot=LOOT):
    """O NBT que vai no template: só o que importa, sem posição, dono nem dados de outros mods."""
    if name == 'minecraft:chest':
        return {'id': 'minecraft:chest', 'LootTable': loot}
    if snbt is None:
        return None
    tag = parse_snbt(snbt)
    if name.endswith('_sign'):
        out = {'id': 'minecraft:sign'}
        for side in ('front_text', 'back_text'):
            if side in tag:
                text = dict(tag[side])
                messages = []
                for line in text.get('messages', []):
                    # O BuildPaste exporta as linhas como compostos ({'text':...}); o jogo espera texto JSON.
                    original = line.get('text', '') if isinstance(line, dict) else json.loads(line).get('text', '')
                    content = {'text': SIGN_TEXT.get(original, original)}
                    if content['text'] == original and re.search('[A-Za-z]{3}', original) and not re.fullmatch(r'(BIO)?EXP \d+|\?+', original):
                        raise SystemExit(f'placa sem tradução: {original!r}')
                    messages.append(json.dumps(content, ensure_ascii=False))
                text['messages'] = messages
                out[side] = text
        if 'is_waxed' in tag:
            out['is_waxed'] = tag['is_waxed']
        return out
    if name.endswith('_banner'):
        out = {'id': 'minecraft:banner'}
        if 'Patterns' in tag:
            out['Patterns'] = tag['Patterns']
        return out
    return None


# ---- Montagem ----

def palette_and_blocks(blocks):
    palette, index, entries = [], {}, []
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
    return palette, entries


def save(name, size, blocks):
    palette, entries = palette_and_blocks(blocks)
    out = OUT_DIR / f'{name}.nbt'
    write_nbt(out, {'DataVersion': DATA_VERSION, 'size': list(size), 'palette': palette, 'blocks': entries,
                    'entities': []})
    print(f'{out.relative_to(ROOT)}: {list(size)}, {len(entries)} blocos, {len(palette)} estados')


# As paredes onde um terminal pode encostar (as da build da base).
WALL_BLOCKS = {'minecraft:stone_bricks', 'minecraft:iron_block', 'minecraft:chiseled_stone_bricks',
               'minecraft:polished_andesite'}
SIDES = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}
OPPOSITE = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}


def terminal_spot(blocks, near, offsets, taken=()):
    """Um lugar livre perto de {near}: chão embaixo, ar em cima, uma parede ao lado; o terminal fica de costas para
    ela. Devolve ((x, y, z), facing) ou None."""
    solid = lambda p: blocks.get(p, ('minecraft:air',))[0] != 'minecraft:air'
    for dx, dz in offsets:
        p = (near[0] + dx, near[1], near[2] + dz)
        if p in taken or solid(p) or solid((p[0], p[1] + 1, p[2])) or not solid((p[0], p[1] - 1, p[2])):
            continue
        walls = [side for side, (sx, sz) in SIDES.items() if solid((p[0] + sx, p[1], p[2] + sz))]
        if walls:
            return p, OPPOSITE[walls[0]]
    return None


def tower():
    """A torre de vigia com perímetro (veja o docstring do módulo)."""
    build = Build('torre')
    tower_x, tower_z = (4, 10), (5, 11)
    first_layer, gap, wall_height, gate, door_x = 1, 4, 2, 5, 6
    terminal, chest = (8, 10), (9, 10)

    blocks = {}

    def put(x, y, z, name, props=None, nbt=None):
        blocks[(x, y, z)] = (name, props or {}, nbt)

    size = tower_x[1] - tower_x[0] + 1 + 2 * gap + 2
    ox = oz = gap + 1
    height = 0
    for x in range(tower_x[0], tower_x[1] + 1):
        for z in range(tower_z[0], tower_z[1] + 1):
            for y in range(first_layer, build.sy):
                name, props, _ = build.block(x, y, z)
                ty = y - first_layer
                if name != 'minecraft:air':
                    height = max(height, ty + 1)
                put(x - tower_x[0] + ox, ty, z - tower_z[0] + oz, name, props)
    blocks = {p: b for p, b in blocks.items() if p[1] < height}

    def at(mx, my, mz):
        return mx - tower_x[0] + ox, my - first_layer, mz - tower_z[0] + oz

    floor = 2
    for (mx, mz), name, props in ((terminal, 'iceagesurvival:military_terminal', {'facing': 'north'}),
                                  (chest, 'minecraft:chest',
                                   {'facing': 'north', 'type': 'single', 'waterlogged': 'false'})):
        pos = at(mx, floor, mz)
        assert blocks[pos][0] == 'minecraft:air', f'{name} cairia sobre {blocks[pos][0]}'
        put(*pos, name, props, block_entity(name, None))

    door = at(door_x, floor, tower_z[0])[0]
    gate_from, gate_to, last = door - gate // 2, door + gate // 2, size - 1
    for a in range(size):
        for x, z in ((a, 0), (a, last), (0, a), (last, a)):
            put(x, 0, z, 'minecraft:cobblestone')
            if z == 0 and gate_from <= x <= gate_to:
                continue
            post = z == 0 and x in (gate_from - 1, gate_to + 1)
            for y in range(1, 1 + (gate if post else wall_height)):
                put(x, y, z, 'iceagesurvival:stone_wall')
    for column in range(gate):
        for row in range(gate):
            # facing=north: as colunas crescem para a direita de quem olha para o norte (leste, +x)
            put(gate_from + column, 1 + row, 0, 'iceagesurvival:large_stone_gate',
                {'facing': 'north', 'open': 'false', 'column': str(column), 'row': str(row)})
    save('military_outpost', (size, max(height, 1 + gate), size), blocks)


def complex_outpost():
    """O complexo inteiro (veja o docstring do módulo)."""
    build = Build('complexo')
    blocks = {}
    height = 0
    for x in range(build.sx):
        for y in range(build.sy):
            for z in range(build.sz):
                name, props, snbt = build.block(x, y, z)
                if name != 'minecraft:air':
                    height = max(height, y + 1)
                blocks[(x, y, z)] = (name, props, block_entity(name, snbt))
    blocks = {p: b for p, b in blocks.items() if p[1] < height}

    # O terminal: perto do baú do térreo, fora da frente dele, no chão, de costas para uma parede.
    chest = (16, 1, 24)
    assert blocks[chest][0] == 'minecraft:chest', blocks[chest]
    blocks[chest] = blocks[chest][:2] + (block_entity('minecraft:chest', None, COMMAND_LOOT),)
    found = terminal_spot(blocks, chest, ((-1, -2), (1, -2), (-2, -2), (2, -2), (-1, -3), (1, -3)))
    assert found, 'sem lugar para o terminal perto do baú'
    spot, facing = found
    blocks[spot] = ('iceagesurvival:military_terminal', {'facing': facing}, None)
    print(f'complexo: terminal em {spot} virado para {facing}')
    save('military_outpost_complex', (build.sx, height, build.sz), blocks)


def base():
    """O hangar da base com a contenção (veja o docstring do módulo)."""
    build = Build('base')
    blocks = {}
    height = 0
    for x in range(build.sx):
        for y in range(build.sy):
            for z in range(build.sz):
                name, props, snbt = build.block(x, y, z)
                if name != 'minecraft:air':
                    height = max(height, y + 1)
                blocks[(x, y, z)] = (name, props, block_entity(name, snbt, BASE_LOOT))
    blocks = {p: b for p, b in blocks.items() if p[1] < height}

    core = (40, 1, 18)
    generators = [(26, 1, 10), (53, 1, 10), (26, 1, 26), (53, 1, 26)]
    for pos in [core] + generators:
        assert blocks[pos][0] == 'minecraft:air', f'{pos} ocupado por {blocks[pos][0]}'
        assert blocks[(pos[0], 0, pos[2])][0] != 'minecraft:air', f'sem chão em {pos}'
    blocks[core] = ('iceagesurvival:containment_core', {}, None)
    for pos in generators:
        blocks[pos] = ('iceagesurvival:stasis_generator', {}, None)

    # Oito terminais da série "base" no térreo, encostados em paredes, fora do campo e o mais espalhados possível.
    solid = lambda p: blocks.get(p, ('minecraft:air',))[0] != 'minecraft:air'
    candidates = []
    for (x, y, z), (name, _, _) in blocks.items():
        if y != 1 or name != 'minecraft:air' or solid((x, 2, z)) or not solid((x, 0, z)):
            continue
        if (x - core[0]) ** 2 + (z - core[2]) ** 2 < 10 ** 2:
            continue
        walls = [side for side, (sx, sz) in SIDES.items()
                 if blocks.get((x + sx, 1, z + sz), ('minecraft:air',))[0] in WALL_BLOCKS]
        roofed = sum(solid((x, roof, z)) for roof in range(5, height)) >= 2  # o beiral sozinho não conta
        if walls and roofed:
            candidates.append(((x, 1, z), OPPOSITE[walls[0]]))
    candidates.sort()
    terminals = [candidates[0][0]]
    facings = {candidates[0][0]: candidates[0][1]}
    while len(terminals) < 8:
        pos, facing = max(candidates, key=lambda c: min((c[0][0] - t[0]) ** 2 + (c[0][2] - t[2]) ** 2
                                                       for t in terminals))
        terminals.append(pos)
        facings[pos] = facing
    for spot in terminals:
        blocks[spot] = ('iceagesurvival:military_terminal', {'facing': facings[spot]},
                        {'id': 'iceagesurvival:military_terminal', 'Series': 'base'})
    assert len(terminals) == 8, f'só {len(terminals)} terminais couberam'
    print(f'base: núcleo em {core}, geradores em {generators}, terminais em {terminals}')
    save('military_base', (build.sx, height, build.sz), blocks)


if __name__ == '__main__':
    tower()
    complex_outpost()
    base()
