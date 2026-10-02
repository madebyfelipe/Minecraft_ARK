#!/usr/bin/env python3
"""Monta o modpack do Ice Age Survival no formato do Modrinth (.mrpack), que o Prism importa.

Os mods de terceiros não entram no arquivo: o pacote guarda o link oficial do Modrinth e os hashes,
e o launcher baixa na importação. Isso mantém o Revival (arte All Rights Reserved) fora de qualquer
coisa que o projeto distribua. Só o jar do Ice Age Survival vai dentro, em overrides/mods.

A lista de mods fica em tools/modpack.json (projeto do Modrinth + versão exata).

Uso: ./gradlew build && python3 tools/build_mrpack.py   -> build/modpack/IceAgeSurvival-<versão>.mrpack
"""
import json
import re
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = json.loads((ROOT / 'tools/modpack.json').read_text())
HEADERS = {'User-Agent': 'madebyfelipe/Minecraft_ARK modpack builder'}
version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M).group(1).strip()
jar = ROOT / f'build/libs/iceagesurvival-{version}.jar'
if not jar.exists():
    raise SystemExit(f'{jar.relative_to(ROOT)} não existe; rode ./gradlew build antes.')


def modrinth_file(project, wanted):
    query = urllib.parse.urlencode({'loaders': '["forge"]', 'game_versions': f'["{PACK["minecraft"]}"]'})
    request = urllib.request.Request(f'https://api.modrinth.com/v2/project/{project}/version?{query}', headers=HEADERS)
    for candidate in json.load(urllib.request.urlopen(request)):
        number = candidate['version_number']
        if number == wanted or re.search(rf'(^|[^\d.]){re.escape(wanted)}($|[^\d.])', number):
            return next(f for f in candidate['files'] if f['primary'])
    raise SystemExit(f'{project} {wanted} não encontrado no Modrinth para Forge {PACK["minecraft"]}')


ENV = {'both': {'client': 'required', 'server': 'required'},
       'client': {'client': 'required', 'server': 'unsupported'}}
files = []
for mod in PACK['mods']:
    found = modrinth_file(mod['project'], mod['version'])
    files.append({'path': f'mods/{found["filename"]}',
                  'hashes': {'sha1': found['hashes']['sha1'], 'sha512': found['hashes']['sha512']},
                  'env': ENV[mod['side']], 'downloads': [found['url']], 'fileSize': found['size']})
    print(f'  {found["filename"]}')

index = {'formatVersion': 1, 'game': 'minecraft', 'versionId': version, 'name': PACK['name'],
         'summary': PACK['summary'], 'files': files,
         'dependencies': {'minecraft': PACK['minecraft'], 'forge': PACK['forge']}}
out = ROOT / f'build/modpack/IceAgeSurvival-{version}.mrpack'
out.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as pack:
    pack.writestr('modrinth.index.json', json.dumps(index, indent=2, ensure_ascii=False))
    pack.write(jar, f'overrides/mods/{jar.name}')
print(f'{out.relative_to(ROOT)}: {len(files)} mods do Modrinth + {jar.name}')
