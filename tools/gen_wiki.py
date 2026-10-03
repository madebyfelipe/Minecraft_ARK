#!/usr/bin/env python3
"""Gera assets/iceagesurvival/wiki/manual.json: o texto do Analisador (DINO FILE e Manual), no tom da lore.

O texto in-game NÃO sai mais de docs/*.html (que continuam sendo a documentação técnica de design): é escrito à mão
em tools/wiki_lore/, como as notas de campo de um cientista da base que sobreviveu ao incidente do portal.

    tools/wiki_lore/manual/NN-<capitulo>.txt   um capítulo da aba MANUAL (a ordem é a do prefixo NN)
    tools/wiki_lore/especies/NN-<id>.txt       a ficha de uma espécie na DINO FILE (a ordem da lista é a do NN)

Cada arquivo começa com um cabeçalho de linhas "chave: valor" e uma linha em branco:

    capítulo:  id, titulo, subtitulo          (o id é o do capítulo no manual.json)
    espécie:   id, nome, selos                (id da entidade sem o namespace; selos separados por vírgula;
                                              o selo "Desligado" tira a espécie da DINO FILE)

No capítulo, cada "== Título" abre uma página (id = <capitulo>/<título sem acento>). O corpo é feito de blocos
separados por linha em branco:

    ### Título         heading
    - item             list (não ordenada); "1. item" faz a lista ordenada
    > texto            note (destaque); linhas seguidas com ">" viram uma nota só
    | a | b |          table; uma linha "|---|---|" logo depois da primeira marca o cabeçalho
    qualquer outro     paragraph; linhas seguidas se juntam com espaço

Ao fim confere: toda espécie de species/*.json (menos test_creature) tem ficha e vice-versa; nenhum texto vazio;
tabelas retangulares; ids de página únicos; e o tom — nada de "jogador", "bloco", "servidor", "tick", "IA",
"spawn", "JSON" ou porcentagem no texto (são termos de design, não de campo). Com erro, nada é gravado.

Uso: python3 tools/gen_wiki.py   (rodar de novo sempre que um arquivo de tools/wiki_lore/ mudar)
"""
import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LORE = ROOT / 'tools/wiki_lore'
SPECIES = ROOT / 'src/main/resources/data/iceagesurvival/iceagesurvival/species'
OUT = ROOT / 'src/main/resources/assets/iceagesurvival/wiki/manual.json'
NAMESPACE = 'iceagesurvival:'
IGNORED_SPECIES = {'test_creature'}

CHAPTER_KEYS = {'id', 'titulo', 'subtitulo'}
SPECIES_KEYS = {'id', 'nome', 'selos'}

# Termos de design que não cabem na voz do cientista (o texto é observação de campo, não regra de jogo).
FORBIDDEN = [
    (re.compile(r'\bjogador(es|a|as)?\b', re.I), '"jogador" (use pessoa, sobrevivente, nós)'),
    (re.compile(r'\bblocos?\b', re.I), '"bloco" como unidade'),
    (re.compile(r'\bservidor\b', re.I), '"servidor"'),
    (re.compile(r'\bticks?\b', re.I), '"tick"'),
    (re.compile(r'\bIA\b'), '"IA"'),
    (re.compile(r'\bspawn\b', re.I), '"spawn"'),
    (re.compile(r'\bjson\b', re.I), '"JSON"'),
    (re.compile(r'\bmobs?\b', re.I), '"mob"'),
    (re.compile(r'\d\s*%'), 'porcentagem (número de jogo)'),
]


class LoreError(Exception):
    pass


def slug(text):
    text = unicodedata.normalize('NFKD', text)
    text = ''.join(c for c in text if not unicodedata.combining(c)).lower()
    return re.sub(r'[^a-z0-9]+', '-', text).strip('-')


def clean(text):
    return re.sub(r'\s+', ' ', text).strip()


def read_header(path, allowed):
    """Lê o cabeçalho "chave: valor" até a primeira linha em branco; devolve (cabeçalho, linhas do corpo)."""
    lines = path.read_text(encoding='utf-8').splitlines()
    header = {}
    i = 0
    while i < len(lines) and lines[i].strip():
        key, sep, value = lines[i].partition(':')
        key = key.strip().lower()
        if not sep or key not in allowed:
            raise LoreError(f'{path.name}:{i + 1}: cabeçalho inválido (chaves aceitas: {", ".join(sorted(allowed))})')
        header[key] = value.strip()
        i += 1
    missing = allowed - header.keys()
    if missing:
        raise LoreError(f'{path.name}: falta no cabeçalho: {", ".join(sorted(missing))}')
    return header, lines[i:]


def split_cells(line):
    return [clean(c) for c in line.strip().strip('|').split('|')]


def parse_blocks(lines, where):
    """Converte o corpo (lista de linhas) em blocos do manual.json."""
    groups, current = [], []
    for line in lines + ['']:
        if line.strip():
            current.append(line.rstrip())
        elif current:
            groups.append(current)
            current = []

    blocks = []
    for group in groups:
        first = group[0].lstrip()
        if first.startswith('###'):
            if len(group) > 1:
                raise LoreError(f'{where}: título "{first}" precisa de linha em branco depois')
            blocks.append({'type': 'heading', 'text': clean(first.lstrip('#'))})
        elif first.startswith('>'):
            if not all(l.lstrip().startswith('>') for l in group):
                raise LoreError(f'{where}: nota misturada com outro bloco: {first[:40]}')
            blocks.append({'type': 'note', 'text': clean(' '.join(l.lstrip()[1:] for l in group))})
        elif first.startswith('|'):
            rows = [split_cells(l) for l in group]
            columns = []
            if len(rows) > 1 and all(re.fullmatch(r':?-+:?', c) for c in rows[1]):
                columns, rows = rows[0], rows[2:]
            blocks.append({'type': 'table', 'columns': columns, 'rows': rows})
        elif re.match(r'(- |\d+\. )', first):
            ordered = bool(re.match(r'\d+\. ', first))
            marker = re.compile(r'\d+\. ' if ordered else r'- ')
            items = []
            for line in group:
                stripped = line.lstrip()
                if marker.match(stripped):
                    items.append(marker.sub('', stripped, count=1))
                elif items:  # continuação do item anterior
                    items[-1] += ' ' + stripped
                else:
                    raise LoreError(f'{where}: lista mal formada: {stripped[:40]}')
            blocks.append({'type': 'list', 'ordered': ordered, 'items': [clean(i) for i in items]})
        else:
            blocks.append({'type': 'paragraph', 'text': clean(' '.join(group))})
    return blocks


def load_chapter(path):
    header, body = read_header(path, CHAPTER_KEYS)
    cid = header['id']
    pages, title, chunk = [], None, []

    def close():
        if title is not None:
            pages.append({'id': f'{cid}/{slug(title)}', 'title': title,
                          'blocks': parse_blocks(chunk, f'{path.name} › {title}')})

    for line in body:
        if line.startswith('== '):
            close()
            title, chunk = clean(line[3:]), []
        elif title is None:
            if line.strip():
                raise LoreError(f'{path.name}: texto antes da primeira página ("== Título")')
        else:
            chunk.append(line)
    close()
    return {'id': cid, 'title': header['titulo'], 'subtitle': header['subtitulo'], 'pages': pages}


def load_sheet(path):
    header, body = read_header(path, SPECIES_KEYS)
    badges = [clean(b) for b in header['selos'].split(',') if clean(b)]
    sheet = {'name': header['nome'], 'badges': badges, 'blocks': parse_blocks(body, path.name)}
    return NAMESPACE + header['id'], sheet


def ordered_files(folder):
    files = sorted(folder.glob('*.txt'))
    for f in files:
        if not re.match(r'\d+-', f.name):
            raise LoreError(f'{f.relative_to(ROOT)}: o nome precisa começar com o número de ordem (NN-)')
    return files


def all_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, item in value.items():
            if key not in ('type', 'ordered', 'id'):
                yield from all_strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from all_strings(item)


def check(manual):
    errors = []
    expected = {NAMESPACE + f.stem for f in SPECIES.glob('*.json') if f.stem not in IGNORED_SPECIES}
    for missing in sorted(expected - manual['species'].keys()):
        errors.append(f'espécie sem ficha: {missing}')
    for extra in sorted(manual['species'].keys() - expected):
        errors.append(f'ficha sem arquivo de espécie: {extra}')

    chapter_ids = Counter(c['id'] for c in manual['chapters'])
    errors += [f'id de capítulo repetido: {cid}' for cid, n in chapter_ids.items() if n > 1]
    seen_pages = Counter(p['id'] for c in manual['chapters'] for p in c['pages'])
    errors += [f'id de página repetido: {pid}' for pid, n in seen_pages.items() if n > 1]
    for chapter in manual['chapters']:
        if not chapter['pages']:
            errors.append(f"capítulo {chapter['id']}: sem páginas")

    owners = [(f"página {p['id']}", p['blocks']) for c in manual['chapters'] for p in c['pages']]
    owners += [(f'ficha {sid}', s['blocks']) for sid, s in manual['species'].items()]
    for owner, blocks in owners:
        if not blocks:
            errors.append(f'{owner}: sem blocos')
        for block in blocks:
            if block['type'] == 'list' and not block['items']:
                errors.append(f'{owner}: lista vazia')
            if block['type'] == 'table':
                width = len(block['columns']) or (len(block['rows'][0]) if block['rows'] else 0)
                if not block['rows']:
                    errors.append(f'{owner}: tabela sem linhas')
                for row in block['rows']:
                    if len(row) != width:
                        errors.append(f'{owner}: linha com {len(row)} células, esperado {width}: {row}')

    for text in all_strings(manual):
        if not text:
            errors.append('texto vazio no manual')
            continue
        if '§' in text or any(unicodedata.category(c) in ('Cc', 'Cf') for c in text):
            errors.append(f'caractere proibido em: {text[:60]}')
        for pattern, label in FORBIDDEN:
            hit = pattern.search(text)
            if hit:
                errors.append(f'fora do tom ({label}): …{text[max(0, hit.start() - 30):hit.end() + 30]}…')
    return errors


def main():
    try:
        chapters = [load_chapter(f) for f in ordered_files(LORE / 'manual')]
        fichas = {}
        for f in ordered_files(LORE / 'especies'):
            sid, sheet = load_sheet(f)
            if sid in fichas:
                raise LoreError(f'{f.name}: ficha repetida para {sid}')
            fichas[sid] = sheet
    except LoreError as e:
        sys.exit(f'gen_wiki: {e}')
    manual = {'chapters': chapters, 'species': fichas}

    errors = check(manual)
    if errors:
        sys.exit('gen_wiki: manual inválido, nada foi gravado\n  ' + '\n  '.join(errors))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(manual, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')

    blocks = Counter()
    for owner in [p for c in chapters for p in c['pages']] + list(fichas.values()):
        blocks.update(b['type'] for b in owner['blocks'])
    print(f'{OUT.relative_to(ROOT)}: {len(chapters)} capítulos, {len(fichas)} espécies')
    for chapter in chapters:
        print(f"  {chapter['id']}: {len(chapter['pages'])} páginas")
    print('  blocos: ' + ', '.join(f'{t} {blocks[t]}' for t in ('heading', 'paragraph', 'list', 'table', 'note'))
          + f' (total {sum(blocks.values())})')


if __name__ == '__main__':
    main()
