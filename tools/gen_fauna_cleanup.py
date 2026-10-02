#!/usr/bin/env python3
"""Gera os dados que desligam o conteúdo próprio do Jurassic Reborn e do Prehistoric Fauna (D22).

Os dois mods entram no modpack só como fornecedores de modelos, texturas, animações e sons (D21).
Tudo o que eles colocam no mundo é desligado por dados, no mesmo caminho dos arquivos deles; o nosso
pacote carrega depois (ordering="AFTER" no mods.toml), então o nosso arquivo vence:

- biome modifiers (data/<ns>/forge/biome_modifier/**)  -> {"type": "forge:none"}
  (o Prehistoric Fauna também traz cópias em data/forge/biome_modifier/, que o Forge não lê: a pasta
  dos biome modifiers é <ns>/forge/biome_modifier. Ficam como estão.)
- structure_set (data/<ns>/worldgen/structure_set/**)   -> mesmo placement, "structures": []
- global loot modifiers citados em data/forge/loot_modifiers/global_loot_modifiers.json
                                                        -> {"type": "iceagesurvival:none"}
  (a lista compartilhada em si NÃO é sobrescrita: ela é de todos os mods)
- tags vanilla de profissão de aldeão e de world preset -> "remove" do Forge, com entradas opcionais

Nada dos jars é copiado: só os caminhos (ids) e o placement dos structure_set.

Uso, com todos os jars de uma vez (o script é dono das pastas que gera e as recria do zero):

    python3 tools/gen_fauna_cleanup.py [jar ...] [--mesclar-tags]

Sem jars, procura o Jurassic Reborn e o Prehistoric Fauna no cache do Gradle (maven.modrinth), que o
build.gradle baixa como runtimeOnly. Para atualizar um mod: troque a versão no build.gradle, rode
./gradlew test (que baixa as dependências de runtime) e rode este script de novo.

Tag que já existe no nosso pacote (ex.: world_preset/normal.json, que lista o preset Era do Gelo) só é
alterada com --mesclar-tags, que preserva o que já está lá e troca só o "remove".
"""
import argparse
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data'
GRADLE_MODRINTH = Path.home() / '.gradle/caches/modules-2/files-2.1/maven.modrinth'
DEFAULT_PROJECTS = ('jurassic-reborn', 'prehistoric-fauna')

BIOME_MODIFIER = re.compile(r'^data/([^/]+)/forge/biome_modifier/(.+)\.json$')
STRUCTURE_SET = re.compile(r'^data/([^/]+)/worldgen/structure_set/(.+)\.json$')
GLM_LIST = 'data/forge/loot_modifiers/global_loot_modifiers.json'
# Tags vanilla em que os mods se põem e que mudam o jogo: aldeão que vira profissão deles e tipo de
# mundo na tela de criação. As outras (logs, planks, mineable...) só descrevem os próprios blocos.
STRIPPED_TAGS = ('point_of_interest_type/acquirable_job_site', 'worldgen/world_preset/normal',
                 'worldgen/world_preset/extended', 'painting_variant/placeable')

NONE_BIOME_MODIFIER = {'type': 'forge:none'}
NONE_LOOT_MODIFIER = {'type': 'iceagesurvival:none'}


def find_default_jars():
    jars = []
    for project in DEFAULT_PROJECTS:
        found = sorted((GRADLE_MODRINTH / project).glob('*/*/*.jar'))
        found = [j for j in found if not j.name.endswith(('-sources.jar', '-javadoc.jar'))]
        if not found:
            sys.exit(f'{project}: jar não achado em {GRADLE_MODRINTH}; rode ./gradlew test (baixa as dependências de runtime) ou passe o jar.')
        if len(found) > 1:
            sys.exit(f'{project}: mais de uma versão no cache ({", ".join(str(j) for j in found)}); passe o jar.')
        jars.append(found[0])
    return jars


def mod_ids(jar):
    toml = jar.read('META-INF/mods.toml').decode('utf-8')
    return re.findall(r'^\s*modId\s*=\s*"([^"]+)"', toml.split('[[dependencies')[0], re.M)


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def tag_entry_id(entry):
    return entry if isinstance(entry, str) else entry['id']


def collect(jar_paths):
    biome_modifiers, structure_sets, loot_modifiers, tags, namespaces = {}, {}, {}, {}, set()
    for jar_path in jar_paths:
        with zipfile.ZipFile(jar_path) as jar:
            ids = mod_ids(jar)
            namespaces.update(ids)
            names = set(jar.namelist())
            print(f'{Path(jar_path).name}: modId {", ".join(ids)}')
            for name in sorted(names):
                if BIOME_MODIFIER.match(name):
                    biome_modifiers[name] = jar_path
                elif STRUCTURE_SET.match(name):
                    original = json.loads(jar.read(name))
                    structure_sets[name] = {'placement': original['placement'], 'structures': []}
            if GLM_LIST in names:
                for entry in json.loads(jar.read(GLM_LIST))['entries']:
                    namespace, path = entry.split(':', 1)
                    file = f'data/{namespace}/loot_modifiers/{path}.json'
                    if file not in names:
                        sys.exit(f'{jar_path}: {entry} está na lista de loot modifiers mas não no jar')
                    loot_modifiers[file] = jar_path
            for tag in STRIPPED_TAGS:
                file = f'data/minecraft/tags/{tag}.json'
                if file in names:
                    values = [tag_entry_id(v) for v in json.loads(jar.read(file)).get('values', [])]
                    tags.setdefault(file, [])
                    tags[file] += [v for v in values if v not in tags[file]]
    return biome_modifiers, structure_sets, loot_modifiers, tags, namespaces


def owned_dirs(namespaces):
    """Pastas que este script gera inteiras: recriadas do zero a cada execução."""
    dirs = []
    for namespace in sorted(namespaces):
        dirs += [DATA / namespace / 'forge/biome_modifier', DATA / namespace / 'worldgen/structure_set',
                 DATA / namespace / 'loot_modifiers']
    return dirs


def write_tag(file, removed, merge):
    target = ROOT / 'src/main/resources' / file
    remove = [{'id': value, 'required': False} for value in removed]
    if not target.exists():
        dump(target, {'replace': False, 'values': [], 'remove': remove})
        return 'criada'
    current = json.loads(target.read_text(encoding='utf-8'))
    if current.get('remove') == remove:
        return 'já atualizada'
    if not merge:
        print(f'  ATENÇÃO: {file} é nosso; acrescente (ou rode com --mesclar-tags):\n'
              f'    "remove": {json.dumps(remove, ensure_ascii=False)}')
        return 'pendente'
    current['remove'] = remove
    dump(target, current)
    return 'mesclada'


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('jars', nargs='*', type=Path)
    parser.add_argument('--mesclar-tags', action='store_true',
                        help='também altera tags que já existem no nosso pacote (preserva values/replace)')
    args = parser.parse_args()
    jar_paths = args.jars or find_default_jars()

    biome_modifiers, structure_sets, loot_modifiers, tags, namespaces = collect(jar_paths)
    if not namespaces.isdisjoint({'minecraft', 'forge', 'iceagesurvival'}):
        sys.exit(f'modId inesperado: {namespaces}')
    for directory in owned_dirs(namespaces):
        if directory.exists():
            shutil.rmtree(directory)

    resources = ROOT / 'src/main/resources'
    for file in biome_modifiers:
        dump(resources / file, NONE_BIOME_MODIFIER)
    for file, emptied in structure_sets.items():
        dump(resources / file, emptied)
    for file in loot_modifiers:
        dump(resources / file, NONE_LOOT_MODIFIER)

    outside = [f for f in [*biome_modifiers, *structure_sets, *loot_modifiers]
               if not any((resources / f).is_relative_to(d) for d in owned_dirs(namespaces))]
    if outside:
        sys.exit(f'arquivo fora das pastas geradas (não seria limpo na próxima vez): {outside}')
    written = sum(1 for d in owned_dirs(namespaces) if d.exists() for f in d.rglob('*.json'))
    expected = len(biome_modifiers) + len(structure_sets) + len(loot_modifiers)
    if written != expected:
        sys.exit(f'{written} arquivos escritos, {expected} esperados')

    print(f'{len(biome_modifiers)} biome modifiers -> forge:none')
    print(f'{len(structure_sets)} structure_set -> sem estruturas')
    print(f'{len(loot_modifiers)} global loot modifiers -> iceagesurvival:none')
    for file, removed in sorted(tags.items()):
        if removed:
            print(f'tag {file}: remove {", ".join(removed)} ({write_tag(file, removed, args.mesclar_tags)})')


if __name__ == '__main__':
    main()
