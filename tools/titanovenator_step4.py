#!/usr/bin/env python3
"""Passo 4 do Titanovenator: cerdas na nuca/dorso/cauda e cicatrizes (Pillow).

Parte do passo 3 local (geo + atlas já com os olhos restaurados) e acrescenta, sem mexer
nos volumes existentes:
  - pernas: afastadas do tronco (1,0 para fora de cada lado) para o corpo parecer mais largo;
  - cerdas: cubos finos e escuros em fileira no meio do pescoço, do dorso e da base da
    cauda (dossiê §17: nuca, região dorsal e base da cauda). Ficam nos ossos que já existem,
    então acompanham respiração e animação;
  - cicatrizes: decalques de 0,08 de espessura sobre o flanco, o pescoço, o focinho e a
    base da cauda (dossiê §17 e §38: adultos acumulam cicatrizes). A pintura é nova e fica
    numa área livre do atlas local; os dentes não são tocados.

Só lê e grava em art/titanovenator/local/ (ignorado pelo Git). Idempotente: na primeira
execução guarda passo-3.* e sempre reconstrói a partir deles.
"""
from __future__ import annotations

import io
import json
import shutil
from copy import deepcopy
from pathlib import Path

from PIL import Image

from study_titanovenator import to_bbmodel

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'art/titanovenator/local'
CELL = 14                      # um texel do UV de 128x64 vale ~13,86 px no atlas; 14 mantém a grade
FREE = (1498, 490)             # canto de uma área transparente de 224x224 no atlas do passo 3
DARK = (940, 300, 968, 328)    # trecho carvão do atlas (o mesmo "dark" do passo 3)

PALE, EDGE, RAW = (212, 176, 150, 255), (86, 58, 54, 255), (132, 80, 70, 255)

# Pixel art das cicatrizes: p = tecido cicatrizado claro, d = sombra da borda, r = borda avermelhada.
SCARS = {
    'slash': (0, 0, [  # três garras paralelas, em diagonal
        'p.........',
        'dp.p......',
        '.dp.p.....',
        '..dp.p....',
        '...dp.p...',
        '....dp.p..',
        '.....dp.p.',
        '......dp.p',
        '.......dp.',
        '..........',
    ]),
    'bite': (10, 0, [  # duas fileiras de perfurações de dentes, com a borda inflamada
        '.dd.dd',
        'dprdpr',
        '.dd.dd',
        '......',
        '.dd.dd',
        'dprdpr',
        '.dd.dd',
    ]),
    'jaw': (0, 11, [  # corte longo e irregular atravessando o focinho
        '.....pp.........',
        '..ppppdp..ppp...',
        'ppdd..dppdd.dpp.',
        'd......dd....dp.',
    ]),
}
COLORS = {'p': PALE, 'd': EDGE, 'r': RAW}

# (nome do osso, índice do cubo, face, tipo, ponto inicial no plano, tamanho no plano)
# Face '+x'/'-x': plano é (z, y); '+z'/'-z' não é usado. Medidas em unidades do modelo.
SCAR_PLACEMENT = [
    ('upperBodyBreathing', 0, '+x', 'slash', (-10.5, 11.5), (6.0, 6.0)),
    ('upperBodyBreathing', 0, '-x', 'bite', (-12.0, 12.0), (4.5, 5.2)),
    ('upperJaw', 0, '+x', 'jaw', (-31.5, 17.3), (5.0, 1.6)),
    ('neck', 0, '+x', 'bite', (-19.0, 15.0), (4.5, 5.2)),
    ('tail2', 0, '+x', 'slash', (13.0, 15.0), (4.0, 4.0)),
]

# Cerdas: (osso, z de cada cerda, altura base). Largura e profundidade ficam finas e baixas.
BRISTLES = [
    ('neck', [-20.6, -19.3, -18.1, -16.6, -15.4, -14.0, -12.8], 1.25),
    ('upperBodyBreathing', [-12.9, -11.5, -10.0, -8.7, -7.2, -5.9], 1.45),
    ('lowerBodyBreathing', [-3.6, -2.0, -0.6, 1.0, 2.5, 4.0], 1.25),
    ('tail1', [5.5, 7.4, 9.3, 11.2], 0.95),
]


def bone_map(geometry):
    return {b['name']: b for b in geometry['minecraft:geometry'][0]['bones']}


def uv_rect(x, y, w, h, scale=(128 / 1774, 64 / 887)):
    return {'uv': [round(x * scale[0], 4), round(y * scale[1], 4)],
            'uv_size': [round(w * scale[0], 4), round(h * scale[1], 4)]}


EMPTY = uv_rect(1766, 881, 6, 4)  # canto do atlas sem pixels


def face_uv(rect):
    return {face: deepcopy(rect) for face in ('north', 'south', 'east', 'west', 'up', 'down')}


def paint_scars(atlas):
    """Pinta os decalques na área livre e devolve o retângulo UV de cada um."""
    rects = {}
    for name, (cx, cy, rows) in SCARS.items():
        w, h = len(rows[0]), len(rows)
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in COLORS:
                    x, y = FREE[0] + (cx + i) * CELL, FREE[1] + (cy + j) * CELL
                    atlas.paste(COLORS[ch], (x, y, x + CELL, y + CELL))
        rects[name] = uv_rect(FREE[0] + cx * CELL, FREE[1] + cy * CELL, w * CELL, h * CELL)
    return rects


def widen_stance(bones, amount=1.0):
    """Afasta as pernas do tronco (mesma regra do Java: coxa, canela e pé saem 1,0 para fora, com os pivôs)."""
    for side in ('left', 'right'):
        for part in ('Thigh', 'Leg', 'Foot'):
            bone = bones.get(side + part)
            if not bone or not bone.get('cubes'):
                continue
            first = bone['cubes'][0]
            center = first['origin'][0] + first['size'][0] / 2
            shift = amount if center > 0 else -amount
            for cube in bone['cubes']:
                cube['origin'][0] = round(cube['origin'][0] + shift, 5)
            if 'pivot' in bone:
                bone['pivot'][0] = round(bone['pivot'][0] + shift, 5)


def add_bristles(bones):
    dark = uv_rect(DARK[0], DARK[1], DARK[2] - DARK[0], DARK[3] - DARK[1])
    count = 0
    for bone, zs, base in BRISTLES:
        top = bones[bone]['cubes'][0]
        tx = top['origin'][0] + top['size'][0] / 2
        ty = top['origin'][1] + top['size'][1]
        for i, z in enumerate(zs):
            h = round(base * (0.85 + 0.3 * ((i * 7 + len(bone)) % 5) / 4), 3)
            bones[bone]['cubes'].append({
                'name': f'{bone}_bristle_{i}',
                'origin': [round(tx - 0.2 + 0.35 * ((i % 3) - 1), 3), round(ty - 0.25, 3), round(z - 0.2, 3)],
                'size': [0.4, h + 0.25, 0.4],
                # Inclina para trás (+z): as cerdas penteiam para a cauda.
                'rotation': [18 + 4 * (i % 3), 0, 3 * ((i % 3) - 1)],
                'pivot': [round(tx + 0.35 * ((i % 3) - 1), 3), round(ty - 0.25, 3), round(z, 3)],
                'uv': face_uv(dark)})
            count += 1
    return count


def add_scars(bones, rects):
    for bone, index, face, kind, (a, b), (da, db) in SCAR_PLACEMENT:
        cube = bones[bone]['cubes'][index]
        ox, oy, oz = cube['origin']
        sx = cube['size'][0]
        x = ox + sx + 0.03 if face == '+x' else ox - 0.03 - 0.08
        bones[bone]['cubes'].append({
            'name': f'{bone}_scar_{kind}',
            'origin': [round(x, 3), round(b, 3), round(a, 3)],
            'size': [0.08, db, da],
            # Só a face de fora leva a pintura; as outras apontam para um trecho vazio do atlas.
            'uv': {f: deepcopy(rects[kind] if f == ('east' if face == '+x' else 'west') else EMPTY)
                   for f in ('north', 'south', 'east', 'west', 'up', 'down')}})


def main():
    for suffix in ('geo.json', 'png'):
        saved = OUT / f'passo-3.{suffix}'
        if not saved.exists():
            shutil.copy2(OUT / f'titanovenator.{suffix}', saved)
    geometry = json.loads((OUT / 'passo-3.geo.json').read_text())
    atlas = Image.open(OUT / 'passo-3.png').convert('RGBA')
    rects = paint_scars(atlas)
    bones = bone_map(geometry)
    widen_stance(bones)
    n = add_bristles(bones)
    add_scars(bones, rects)
    buffer = io.BytesIO()
    atlas.save(buffer, format='PNG')
    raw = buffer.getvalue()
    model = to_bbmodel(geometry, raw)
    model['textures'][0].update(width=atlas.width, height=atlas.height)
    (OUT / 'titanovenator.png').write_bytes(raw)
    (OUT / 'titanovenator.geo.json').write_text(json.dumps(geometry, ensure_ascii=False, indent=2) + '\n')
    (OUT / 'titanovenator.bbmodel').write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n')
    manifest_path = OUT / 'manifest.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    manifest.update(status='Passo 4 — cerdas e cicatrizes para revisão',
                    pass_4={'bristles': n, 'scars': [p[3] + '@' + p[0] for p in SCAR_PLACEMENT],
                            'teeth': 'inalterados'})
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(f'{n} cerdas, {len(SCAR_PLACEMENT)} cicatrizes')


if __name__ == '__main__':
    main()
