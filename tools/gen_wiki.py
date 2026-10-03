#!/usr/bin/env python3
"""Gera assets/iceagesurvival/wiki/manual.json: o texto da wiki in-game, lido da documentação HTML.

Fonte: docs/ecologia.html e docs/comportamento.html (este sai de tools/gen_comportamento.py; rode-o antes quando
um JSON de espécie mudar). Cada <h2> vira uma página do capítulo, e o que vem antes do primeiro vira "Introdução";
cada <article class="species"> vira a ficha da espécie, pelo id da entidade, e a página "Fichas" sai dos capítulos.
Blocos: heading (h3–h6), paragraph, list, table e note (aside). Rodapé, carimbo, sobrelinha, <nav> e índice ficam
de fora. Ao fim confere que toda espécie de species/*.json tem ficha, que as tabelas são retangulares, que nenhum
texto ficou vazio e que nenhum trecho de texto do HTML se perdeu.

Uso: python3 tools/gen_wiki.py   (rodar de novo sempre que docs/*.html mudar)
"""
import json
import re
import sys
import unicodedata
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / 'docs'
SPECIES = ROOT / 'src/main/resources/data/iceagesurvival/iceagesurvival/species'
OUT = ROOT / 'src/main/resources/assets/iceagesurvival/wiki/manual.json'
NAMESPACE = 'iceagesurvival:'
IGNORED_SPECIES = {'test_creature'}

CHAPTERS = [
    ('ecologia', 'Ecologia', DOCS / 'ecologia.html'),
    ('comportamento', 'Comportamento', DOCS / 'comportamento.html'),
]

VOID = {'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track',
        'wbr'}
INLINE = {'a', 'abbr', 'b', 'br', 'cite', 'code', 'em', 'i', 'kbd', 'mark', 'q', 's', 'small', 'span', 'strong',
          'sub', 'sup', 'time', 'u', 'var'}
SKIP_TAGS = {'head', 'style', 'script', 'nav', 'footer', 'template', 'hr'}
SKIP_CLASSES = {'stamp', 'eyebrow', 'toc'}
NOTE_CLASSES = {'notice', 'callout', 'warning', 'aviso'}
# Depois destes o <br> vira só espaço; depois de um título curto ("Percepção<br>O carnívoro…") vira ": ".
SENTENCE_END = '.:;!?…—–-'


class Node:
    def __init__(self, tag, attrs):
        self.tag = tag
        self.attrs = attrs
        self.children = []

    def classes(self):
        return set((self.attrs.get('class') or '').split())


class TreeBuilder(HTMLParser):
    """Monta uma árvore simples; o HTML dos docs é bem formado, então basta fechar até a tag correspondente."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node('#root', {})
        self.stack = [self.root]

    def handle_starttag(self, tag, attrs):
        node = Node(tag, dict(attrs))
        self.stack[-1].children.append(node)
        if tag not in VOID:
            self.stack.append(node)

    def handle_startendtag(self, tag, attrs):
        self.stack[-1].children.append(Node(tag, dict(attrs)))

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag:
                del self.stack[i:]
                return

    def handle_data(self, data):
        self.stack[-1].children.append(data)


def parse(path):
    builder = TreeBuilder()
    builder.feed(path.read_text(encoding='utf-8'))
    builder.close()
    return builder.root


def skipped(node):
    return node.tag in SKIP_TAGS or bool(node.classes() & SKIP_CLASSES)


def clean(text):
    text = re.sub(r'\s+', ' ', text.replace('§', 'S'))
    return ''.join(c for c in text if unicodedata.category(c) not in ('Cc', 'Cf')).strip()


def collect(node, parts):
    for child in node.children:
        if isinstance(child, str):
            parts.append(child)
            continue
        if skipped(child):
            continue
        if child.tag == 'br':
            before = clean(''.join(parts))
            parts.append(' ' if not before or before[-1] in SENTENCE_END else ': ')
            continue
        cls = child.classes()
        if 'badge' in cls:
            parts.append(' (')
            collect(child, parts)
            parts.append(') ')
            continue
        block = child.tag not in INLINE
        if block:
            parts.append(' ')
        collect(child, parts)
        if 'pref' in cls:  # rótulo de preferência da dieta: "favorita: Estegossauro, …"
            parts.append(':')
        if block:
            parts.append(' ')


def text_of(*nodes):
    holder = Node('#run', {})
    holder.children = list(nodes)
    parts = []
    collect(holder, parts)
    return clean(''.join(parts))


def slug(text):
    text = unicodedata.normalize('NFKD', text)
    text = ''.join(c for c in text if not unicodedata.combining(c)).lower()
    return re.sub(r'[^a-z0-9]+', '-', text).strip('-')


def descendants(node, tag):
    for child in node.children:
        if isinstance(child, Node):
            if child.tag == tag:
                yield child
            elif child.tag != 'table':
                yield from descendants(child, tag)


def table_rows(node, in_head=False):
    for child in node.children:
        if isinstance(child, Node):
            if child.tag == 'tr':
                yield child, in_head
            elif child.tag != 'table':
                yield from table_rows(child, in_head or child.tag == 'thead')


def table_block(table):
    """Cabeçalho: a primeira linha, se estiver no <thead> ou for só de <th>. Linha "<th>rótulo</th><td>valor</td>"
    das fichas é dado, não cabeçalho."""
    columns, rows = [], []
    for tr, in_head in table_rows(table):
        cells = [c for c in tr.children if isinstance(c, Node) and c.tag in ('th', 'td')]
        texts = []
        for cell in cells:
            span = int(cell.attrs.get('colspan') or 1)
            texts.extend([text_of(*cell.children)] * span)
        header = in_head or all(c.tag == 'th' for c in cells)
        if header and not columns and not rows:
            columns = texts
        else:
            rows.append(texts)
    return {'type': 'table', 'columns': columns, 'rows': rows}


class Chapter:
    def __init__(self, cid, title):
        self.id, self.title, self.subtitle = cid, title, ''
        self.pages = []
        self.fichas_on_page = Counter()
        self.omitted = []

    def add(self, block):
        if not self.pages:
            self.pages.append({'id': f'{self.id}/intro', 'title': 'Introdução', 'blocks': []})
        self.pages[-1]['blocks'].append(block)

    def section(self, title):
        number = re.match(r'(\d+)[.)]\s', title)
        pid = f'{self.id}/{number.group(1) if number else slug(title)}'
        self.pages.append({'id': pid, 'title': title, 'blocks': []})

    def heading(self, level, title):
        if level == 1 and not self.subtitle:
            self.subtitle = title
        elif level == 2:
            self.section(title)
        else:
            self.add({'type': 'heading', 'text': title})

    def ficha_here(self):
        self.fichas_on_page[len(self.pages) - 1] += 1

    def to_json(self):
        # A página que só abrigava fichas ("Fichas") não entra: o conteúdo dela está em "species".
        pages = [p for i, p in enumerate(self.pages) if p['blocks'] or not self.fichas_on_page[i]]
        self.omitted = [p['title'] for p in self.pages if p not in pages]
        return {'id': self.id, 'title': self.title, 'subtitle': self.subtitle, 'pages': pages}


class Ficha:
    def __init__(self, name, badges):
        self.name, self.badges, self.blocks = name, badges, []

    def add(self, block):
        self.blocks.append(block)

    def heading(self, level, title):
        self.add({'type': 'heading', 'text': title})

    def ficha_here(self):
        pass

    def to_json(self):
        return {'name': self.name, 'badges': self.badges, 'blocks': self.blocks}


def ficha(article, fichas):
    header = next(c for c in article.children if isinstance(c, Node) and c.tag == 'header')
    name = text_of(next(descendants(header, 'h3')))
    badges = []
    for span in descendants(header, 'span'):
        badge = text_of(*span.children)
        if 'badge' in span.classes() and badge not in badges:
            badges.append(badge)
    sink = Ficha(name, badges)
    body = Node('#body', {})
    body.children = [c for c in article.children if c is not header]
    walk(body, sink, fichas)
    fichas[NAMESPACE + article.attrs['id']] = sink.to_json()


def walk(node, sink, fichas):
    run = []

    def flush():
        paragraph = text_of(*run)
        run.clear()
        if paragraph:
            sink.add({'type': 'paragraph', 'text': paragraph})

    for child in node.children:
        if isinstance(child, str):
            run.append(child)
            continue
        if skipped(child):
            continue
        cls = child.classes()
        if child.tag in INLINE and not cls & NOTE_CLASSES and 'formula' not in cls:
            run.append(child)
            continue
        flush()
        tag = child.tag
        if re.fullmatch(r'h[1-6]', tag):
            sink.heading(int(tag[1]), text_of(*child.children))
        elif tag == 'aside' or cls & NOTE_CLASSES:
            sink.add({'type': 'note', 'text': text_of(*child.children)})
        elif tag in ('p', 'pre') or 'formula' in cls:
            run.append(child)
            flush()
        elif tag in ('ul', 'ol'):
            items = [text_of(*li.children) for li in child.children if isinstance(li, Node) and li.tag == 'li']
            sink.add({'type': 'list', 'ordered': tag == 'ol', 'items': [i for i in items if i]})
        elif tag == 'table':
            sink.add(table_block(child))
        elif tag == 'article' and 'species' in cls and fichas is not None:
            sink.ficha_here()
            ficha(child, fichas)
        else:
            walk(child, sink, fichas)
    flush()


def all_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, item in value.items():
            if key not in ('type', 'ordered'):
                yield from all_strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from all_strings(item)


def source_fragments(node):
    for child in node.children:
        if isinstance(child, str):
            if clean(child):
                yield clean(child)
        elif not skipped(child):
            yield from source_fragments(child)


def check(manual, trees, omitted):
    errors = []
    expected = {NAMESPACE + f.stem for f in SPECIES.glob('*.json') if f.stem not in IGNORED_SPECIES}
    for missing in sorted(expected - manual['species'].keys()):
        errors.append(f'espécie sem ficha: {missing}')
    for extra in sorted(manual['species'].keys() - expected):
        errors.append(f'ficha sem arquivo de espécie: {extra}')

    owners = [(f"página {p['id']}", p['blocks']) for c in manual['chapters'] for p in c['pages']]
    owners += [(f'ficha {sid}', s['blocks']) for sid, s in manual['species'].items()]
    seen_pages = Counter(p['id'] for c in manual['chapters'] for p in c['pages'])
    errors += [f'id de página repetido: {pid}' for pid, n in seen_pages.items() if n > 1]
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

    strings = list(all_strings(manual)) + list(manual['species'].keys())
    for text in strings:
        if not text:
            errors.append('texto vazio no manual')
        elif '§' in text or any(unicodedata.category(c) in ('Cc', 'Cf') for c in text):
            errors.append(f'caractere proibido em: {text[:60]}')

    corpus = '\n'.join(strings)
    for name, tree in trees:
        for fragment in source_fragments(tree):
            if fragment not in corpus and fragment not in omitted:
                errors.append(f'{name}: trecho perdido: {fragment[:80]}')
    return errors


def main():
    chapters, fichas, trees, omitted = [], {}, [], set()
    for cid, title, path in CHAPTERS:
        tree = parse(path)
        trees.append((path.name, tree))
        chapter = Chapter(cid, title)
        walk(tree, chapter, fichas)
        chapters.append(chapter.to_json())
        omitted.update(chapter.omitted)
    manual = {'chapters': chapters, 'species': fichas}

    errors = check(manual, trees, omitted)
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
