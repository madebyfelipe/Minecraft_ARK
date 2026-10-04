#!/usr/bin/env python3
"""Aplica um atlas local ao estudo sem alterar geometria (stdlib)."""
import argparse
import base64
import json
import shutil
import struct
from study_titanovenator import to_bbmodel
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / 'art/titanovenator/local'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('texture', type=Path)
    args = parser.parse_args()
    raw = args.texture.read_bytes()
    if raw[:8] != b'\x89PNG\r\n\x1a\n':
        parser.error('O atlas precisa ser PNG.')
    width, height = struct.unpack('>II', raw[16:24])
    if width != height * 2:
        parser.error('O atlas deve manter a proporção UV 2:1.')
    for suffix in ['bbmodel', 'geo.json', 'png']:
        saved = OUT / ('passo-2.' + suffix)
        if not saved.exists():
            shutil.copy2(OUT / ('titanovenator.' + suffix), saved)
    geometry = json.loads((OUT / 'passo-2.geo.json').read_text())
    # Regiões opacas conferidas no atlas gerado desta passagem, em pixels.
    # Remapeamento UV: não altera nem repinta a imagem produzida pelo ImageGen.
    regions = {
        'dark': (915, 285, 1000, 398),
        'flank': (1405, 150, 1640, 286),
        'gold': (1020, 272, 1100, 321),
        'eye': (83, 58, 166, 157),
        'muzzle': (1410, 157, 1590, 281),
        'mouth': (458, 166, 525, 292),
        'claws': (1320, 327, 1375, 398),
    }
    for bone in geometry['minecraft:geometry'][0]['bones']:
        name = bone['name']
        for cube in bone.get('cubes', []):
            for side in cube['uv']:
                region = 'dark'
                trunk = name in ('lowerBodyBreathing','upperBodyBreathing','neck') or name.startswith('tail')
                if trunk:
                    if side in ('east','west'): region = 'flank'
                    elif side == 'down': region = 'gold'
                if name == 'head':
                    if cube.get('name','').startswith('cheek'): region = 'muzzle'
                    elif side in ('east','west'): region = 'eye'
                    elif side == 'down': region = 'gold'
                if name in ('upperJaw','lowerJaw'):
                    region = 'gold' if name == 'lowerJaw' else 'dark'
                    if side in ('east','west','north'): region = 'muzzle'
                    if (name == 'lowerJaw' and side == 'up') or (name == 'upperJaw' and side == 'down'): region = 'mouth'
                    if '_lip_' in cube.get('name',''): region = 'gold'
                if name.endswith('Foot') and side == 'north': region = 'claws'
                x,y,X,Y = regions[region]
                if region in ('dark', 'flank', 'gold', 'muzzle'):
                    w,h,d = cube['size']
                    fw,fh = (d,h) if side in ('east','west') else ((w,d) if side in ('up','down') else (w,h))
                    ratio = fw/fh
                    if (X-x)/(Y-y) > ratio: X = x+(Y-y)*ratio
                    elif region not in ('flank','muzzle'): y = Y-(X-x)/ratio
                u,v,U,V = x*128/1774,y*64/887,X*128/1774,Y*64/887
                if side == 'west': u,U = U,u
                cube['uv'][side] = {'uv':[u,v], 'uv_size':[U-u,V-v]}
    model = to_bbmodel(geometry, raw)
    model['textures'][0].update(width=width, height=height)
    (OUT / 'titanovenator.bbmodel').write_text(json.dumps(model, ensure_ascii=False, indent=2)+'\n')
    (OUT / 'titanovenator.geo.json').write_text(json.dumps(geometry, ensure_ascii=False, indent=2)+'\n')
    (OUT / 'titanovenator.png').write_bytes(raw)
    manifest = json.loads((OUT / 'manifest.json').read_text())
    manifest.update(status='Passo 3 — textura seguindo referência do Felipe',
        texture='Carvão e ocre dourado, geração de imagem sobre atlas local',
        pending=['revisão da textura', 'cerdas discretas', 'animações e escala final'])
    (OUT / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
    print(OUT / 'titanovenator.bbmodel')


if __name__ == '__main__':
    main()
