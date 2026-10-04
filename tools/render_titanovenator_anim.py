#!/usr/bin/env python3
"""Folhas de quadros das animações do Titanovenator (NumPy + Pillow).

Amostra o arquivo de animação gerado, aplica cada pose ao modelo local do passo 3 e
rasteriza com o renderizador de revisão. Saída em art/titanovenator/local/anim/ (ignorado).
Uso: python3 tools/render_titanovenator_anim.py [animação ...] [--frames N] [--view side|front]
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import render_titanovenator as r

ROOT = r.ROOT
ANIM = ROOT / 'art/titanovenator/animations/titanovenator.animation.json'
OUT = r.OUT / 'anim'
PREFIX = 'animation.titanovenator.'


def sample(frames, t):
    ts = sorted(frames, key=float)
    pts = [(float(k), frames[k]) for k in ts]
    if t <= pts[0][0]:
        return pts[0][1]
    for (t0, a), (t1, b) in zip(pts, pts[1:]):
        if t <= t1:
            x = (t - t0) / (t1 - t0) if t1 > t0 else 0
            return [a[i] + (b[i] - a[i]) * x for i in range(3)]
    return pts[-1][1]


def pose_at(clip, t):
    pose = {}
    for bone, chans in clip['bones'].items():
        rot = sample(chans['rotation'], t) if 'rotation' in chans else [0, 0, 0]
        pos = sample(chans['position'], t) if 'position' in chans else [0, 0, 0]
        scl = sample(chans['scale'], t) if 'scale' in chans else [1, 1, 1]
        pose[bone] = (rot, pos, scl)
    return pose


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('names', nargs='*')
    ap.add_argument('--frames', type=int, default=8)
    ap.add_argument('--view', default='side', choices=['side', 'front'])
    args = ap.parse_args()
    data = json.loads(ANIM.read_text())['animations']
    names = args.names or [n[len(PREFIX):] for n in data]
    OUT.mkdir(parents=True, exist_ok=True)
    model = r.OUT / 'titanovenator.bbmodel'
    base = r.scene(model)
    ref = np.concatenate([f[0] for f in base[0]])
    # Enquadramento fixo: o corpo inteiro em repouso, para o movimento ficar visível.
    for name in names:
        clip = data[PREFIX + name]
        L = clip['animation_length']
        cols = min(args.frames, 4)
        rows = -(-args.frames // cols)
        w, h = 640, 400
        sheet = Image.new('RGB', (cols * w, rows * h + 30), (243, 241, 235))
        d = ImageDraw.Draw(sheet)
        d.text((10, 8), f'{name}  {L:.2f}s', fill=(40, 40, 40))
        for i in range(args.frames):
            t = L * i / (args.frames if clip.get('loop') else args.frames - 1)
            frame = r.render(r.scene(model, pose=pose_at(clip, t)), ref, w, h)
            sheet.paste(frame, ((i % cols) * w, 30 + (i // cols) * h))
            d.text(((i % cols) * w + 8, 34 + (i // cols) * h), f't={t:.2f}', fill=(90, 90, 90))
        sheet.save(OUT / f'{name}.png')
        print(OUT / f'{name}.png')


if __name__ == '__main__':
    main()
