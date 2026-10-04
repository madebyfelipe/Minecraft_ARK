#!/usr/bin/env python3
"""Restaura os pixels e UVs dos olhos anteriores no atlas local (Pillow)."""
import io
import json
from pathlib import Path
from PIL import Image
from study_titanovenator import to_bbmodel

OUT = Path(__file__).resolve().parents[1] / 'art/titanovenator/local'


def main():
    atlas = Image.open(OUT/'passo-3-atlas.png').convert('RGBA')
    original = Image.open(OUT/'passo-2.png').convert('RGBA')
    geometry = json.loads((OUT/'titanovenator.geo.json').read_text())
    previous = json.loads((OUT/'passo-2.geo.json').read_text())
    def head(g):
        return next(b for b in g['minecraft:geometry'][0]['bones'] if b['name']=='head')['cubes'][0]
    old, current = head(previous), head(geometry)
    # Espaço vazio do atlas; as demais regiões usadas permanecem byte a byte iguais.
    cursor = 300
    for face in ('east','west','north'):
        u,v = old['uv'][face]['uv']; w,h = old['uv'][face]['uv_size']
        w,h = int(w),int(h)
        tile = atlas.crop((915,285,1000,398)).resize((w*16,h*16),Image.Resampling.NEAREST)
        for y in range(h):
            for x in range(w):
                sx,sy=int(u+x),int(v+y)
                if sy in (6,7) and (3<=sx<=5 or 10<=sx<=12):
                    pixel=original.getpixel((sx,sy))
                    tile.paste(pixel,(x*16,y*16,(x+1)*16,(y+1)*16))
        atlas.paste(tile,(cursor,0))
        current['uv'][face]={'uv':[cursor*128/atlas.width,0],
                            'uv_size':[w*16*128/atlas.width,h*16*64/atlas.height]}
        cursor += w*16+4
    buffer=io.BytesIO();atlas.save(buffer,format='PNG');raw=buffer.getvalue()
    (OUT/'titanovenator.png').write_bytes(raw)
    model=to_bbmodel(geometry,raw)
    model['textures'][0].update(width=atlas.width,height=atlas.height)
    (OUT/'titanovenator.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
    (OUT/'titanovenator.geo.json').write_text(json.dumps(geometry,ensure_ascii=False,indent=2)+'\n')
    manifest=json.loads((OUT/'manifest.json').read_text())
    manifest['eyes']='Pixels verdes e pupila originais restaurados; posição e tamanho do passo 2'
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')


if __name__=='__main__': main()
