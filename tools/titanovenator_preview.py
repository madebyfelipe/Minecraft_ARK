#!/usr/bin/env python3
"""Empacota a prévia WebGL local; execute da raiz com python3 tools/titanovenator_preview.py."""
import base64
import json
from pathlib import Path


def main():
    root = Path(__file__).resolve().parents[1]
    folder = root / 'art/titanovenator/local'
    sources = {
        '__GEOMETRY_ORIGINAL__': folder / 'rex-original.geo.json',
        '__GEOMETRY_PREVIOUS__': folder / 'passo-1.geo.json',
        '__GEOMETRY_HEAD__': (folder / 'passo-2.geo.json') if (folder / 'passo-2.geo.json').exists() else folder / 'titanovenator.geo.json',
        '__GEOMETRY_MODIFIED__': folder / 'titanovenator.geo.json',
    }
    texture = base64.b64encode((folder / 'titanovenator.png').read_bytes()).decode()
    template = Path(__file__).with_suffix('.html').read_text()
    result = template
    for placeholder, path in sources.items():
        geometry = json.loads(path.read_text())
        if not geometry.get('minecraft:geometry'):
            raise ValueError(f'Geometria Bedrock ausente: {path}')
        result = result.replace(placeholder, json.dumps(geometry, ensure_ascii=True).replace('</', '<\\/'))
    result = result.replace('__TEXTURE_DATA__', 'data:image/png;base64,' + texture)
    original = json.loads((folder / 'rex-original.bbmodel').read_text())['textures'][0]['source']
    result = result.replace('__TEXTURE_ORIGINAL__', original)
    output = folder / 'preview.html'
    output.write_text(result)
    print(output)


if __name__ == '__main__':
    main()
