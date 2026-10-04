#!/usr/bin/env python3
"""Consulta pequena do design, sem carregar o histórico na sessão do agente."""
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('kind', choices=['section', 'decision'])
    parser.add_argument('value')
    args = parser.parse_args()
    if args.kind == 'section':
        if not args.value.isdigit() or not 1 <= int(args.value) <= 26:
            parser.error('Seção deve estar entre 1 e 26.')
        print((ROOT / f'docs/design/section-{int(args.value):02}.md').read_text())
    else:
        if not re.fullmatch(r'D[1-9][0-9]*', args.value):
            parser.error('Decisão deve ter formato D51.')
        lines = (ROOT / 'docs/design/section-04.md').read_text().splitlines()
        matches = [line for line in lines if re.match(r'\|\s*' + args.value + r'\s*\|', line)]
        if not matches:
            parser.error('Decisão não encontrada.')
        print('\n'.join(matches))


if __name__ == '__main__':
    main()
