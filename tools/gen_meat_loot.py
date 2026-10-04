#!/usr/bin/env python3
"""Ajusta a carne que cada criatura larga ao porte dela (pedido do Felipe, 2026-10-04: "não faz sentido ganhar
2 beefs sendo que uma vaca dá a mesma quantia; deve ser consideravelmente mais").

Como os outros gen_*.py: rodar de novo SOBRESCREVE edições feitas à mão — aqui, a faixa de carne (e o looting dela)
em src/main/resources/data/iceagesurvival/loot_tables/entities/<especie>.json. O resto de cada tabela (couro, pelego,
ossos, dentes, cabeças, soro, projetor, condições, random_sequence) é lido e devolvido como estava.

A REGRA
  1. O porte vem da caixa de colisão registrada em registry/ModEntities.java (``landCreature("id", largura, altura)``):
     volume = largura² x altura, a mesma régua do Carcass.GALLIMIMUS_VOLUME. Mudou a caixa lá, rode de novo.
  2. r = volume / volume da vaca (0,9 x 0,9 x 1,4 = 1,134). A vaca vanilla larga 1–3 carnes: média 2.
  3. média = 2 x r^(2/3), limitada a 48. O expoente 2/3 (não 1) porque a caixa é um prisma largura² x altura e
     superestima a massa de corpo longo e baixo: sobre a caixa, o T-Rex vale 35 vacas, mas pesa ~12 (8 t contra ~0,65 t).
     Com 2/3 o T-Rex dá ~11 vezes a vaca, o Giganotossauro ~11 (8 t ≈ 12 vacas) e o Titanovenator ~27 (13–18 t ≈ 24;
     aqui o teto de 48 o segura em 24): bate com as massas que o próprio ModEntities cita. Expoente 1 daria 35 vezes
     a vaca no T-Rex e passaria de 250 de carne no Tricerátopo; 2/3 é o piso do intervalo 2/3–1 e fica proporcional
     sem inflar. O teto de 48 só pega o Tricerátopo (5,2 x 5,5, maior que o T-Rex por decisão do Felipe) e o Titano.
  4. A faixa é a média ±35%, arredondada: min = máx(1, 0,65 x média) e max = 1,35 x média, sempre com max >= min + 1
     (o 1–2 dos pequenos é o piso: dodô, velociraptor e lobo-terrível ficam onde já estavam, perto da vaca/galinha, o
     ornitholestes entra igual, e ninguém larga zero) e max <= 64 (uma pilha). A vaca (r = 1) cai exatamente em 1–3.
  5. looting_enchant por nível: 0 a máx(1, média/2). A vaca dá 0–1 por nível; a proporção da média sobe junto, então
     Looting III rende o mesmo +75% da vaca em qualquer bicho.
  6. O tipo de carne (beef, mutton, chicken, dodo_meat) e o furnace_smelt com a criatura em chamas ficam como estão.
  7. Ossos NÃO escalam: não foram pedidos, o teste do Titanovenator confere ossos >= 4 e osso vira farinha de osso na
     forja (4 por osso), então escalá-los inflaria a farinha, não a comida.

Carcaça (PrehistoricCreature.butcher): quem morre selvagem sem jogador vira carcaça (sem soltar loot na morte) e o
machado/espada solta o MESMO loot da tabela multiplicado por porções restantes / porções totais, arredondado por pilha.
Então a parte que o jogador leva é sempre proporcional ao que sobrou e nunca passa da morte comum; as tabelas maiores
só deixam esse arredondamento mais fino.

ESPÉCIES SEM TABELA (largavam NADA: LandCreature não tem loot próprio, o jogo usa entities/<id>.json): as de ``NEW_TABLES``
abaixo ganham uma tabela só de carne, no mesmo molde das outras (com furnace_smelt). Sem couro/pelego/osso, de propósito:
são outros drops, decisão do Felipe.

Uso:  python3 tools/gen_meat_loot.py          (escreve as tabelas)
      python3 tools/gen_meat_loot.py --dry    (só mostra a tabela, sem escrever)
"""
import json
import math
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ENTITIES = REPO / "src" / "main" / "java" / "dev" / "madebyfelipe" / "iceagesurvival" / "registry" / "ModEntities.java"
LOOT = REPO / "src" / "main" / "resources" / "data" / "iceagesurvival" / "loot_tables" / "entities"

# ---- Parâmetros (ajustáveis) ----
COW_VOLUME = 0.9 * 0.9 * 1.4   # caixa da vaca vanilla
COW_AVERAGE = 2.0              # vaca: 1–3 carnes
EXPONENT = 2.0 / 3.0           # entre 2/3 (área) e 1 (massa pura da caixa); ver o docstring
AVERAGE_CAP = 48.0             # teto da média (o Tricerátopo e o Titanovenator batem aqui)
SPREAD = 0.35                  # faixa = média ±35% (a vaca: 2 -> 1–3)
STACK = 64                     # teto do máximo sorteado: uma pilha

# Carnes que as tabelas já usam: o gerador acha a piscina da carne por elas e mantém o tipo.
MEAT_ITEMS = {
    "minecraft:beef", "minecraft:mutton", "minecraft:chicken", "minecraft:porkchop", "minecraft:rabbit",
    "iceagesurvival:dodo_meat",
}

# Espécies que hoje não têm tabela: id -> carne. Só carne; ver o docstring.
NEW_TABLES = {
    "stegosaurus": "minecraft:beef",        # herbívoro grande, como o Tricerátopo
    "direbear": "minecraft:beef",           # carne vermelha de mamífero grande
    "kelenken": "minecraft:chicken",        # ave-terrível
    "ornitholestes": "minecraft:chicken",   # pequeno terópode, como o Velociraptor
    "pteranodon": "minecraft:chicken",      # pterossauro: carne de ave
    "quetzalcoatlus": "minecraft:chicken",  # idem
}


def half_up(value):
    return int(math.floor(value + 0.5))


def read_sizes():
    """id -> (largura, altura) de cada ``landCreature("id", largura F, altura F ...)`` do registro."""
    source = ENTITIES.read_text(encoding="utf-8")
    pattern = r'landCreature\(\s*"(\w+)"\s*,\s*([\d.]+)F\s*,\s*([\d.]+)F'
    return {m.group(1): (float(m.group(2)), float(m.group(3))) for m in re.finditer(pattern, source)}


def meat_range(width, height):
    """(r, média, min, max, looting_max) de carne para a caixa largura x altura."""
    ratio = width * width * height / COW_VOLUME
    average = min(AVERAGE_CAP, COW_AVERAGE * ratio ** EXPONENT)
    low = max(1, half_up((1.0 - SPREAD) * average))
    high = min(STACK, max(low + 1, half_up((1.0 + SPREAD) * average)))
    looting = max(1, half_up(average / 2.0))
    return ratio, average, low, high, looting


def uniform(low, high):
    return {"type": "minecraft:uniform", "min": low, "max": high}


def smelt_when_burning():
    return {
        "function": "minecraft:furnace_smelt",
        "conditions": [{
            "condition": "minecraft:entity_properties",
            "entity": "this",
            "predicate": {"flags": {"is_on_fire": True}},
        }],
    }


def new_table(meat, low, high, looting):
    """Tabela só de carne, no molde das existentes (set_count, looting, furnace_smelt)."""
    return {
        "type": "minecraft:entity",
        "pools": [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "name": meat,
                "functions": [
                    {"function": "minecraft:set_count", "count": uniform(low, high)},
                    {"function": "minecraft:looting_enchant", "enchantment": "minecraft:looting",
                     "count": uniform(0, looting)},
                    smelt_when_burning(),
                ],
            }],
        }],
    }


def meat_entry(table):
    """A entrada de carne da tabela (a única com item de MEAT_ITEMS), ou None."""
    found = [entry for pool in table["pools"] for entry in pool.get("entries", [])
             if entry.get("name") in MEAT_ITEMS]
    if len(found) > 1:
        raise SystemExit("mais de uma carne na tabela: " + ", ".join(entry["name"] for entry in found))
    return found[0] if found else None


def apply_range(entry, low, high, looting):
    """Troca só as faixas de set_count e looting_enchant; o resto das funções fica intacto."""
    for function in entry.get("functions", []):
        if function["function"] == "minecraft:set_count":
            function["count"] = uniform(low, high)
        elif function["function"] == "minecraft:looting_enchant":
            function["count"] = uniform(0, looting)


def write(path, table):
    path.write_text(json.dumps(table, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    dry = "--dry" in sys.argv[1:]
    sizes = read_sizes()
    if not sizes:
        raise SystemExit("nenhuma landCreature lida de " + str(ENTITIES))
    print(f"{'espécie':<16}{'caixa':>11}{'x vaca':>8}{'média':>7}  carne      looting  antes")
    for species, (width, height) in sorted(sizes.items(), key=lambda item: item[1][0] ** 2 * item[1][1]):
        ratio, average, low, high, looting = meat_range(width, height)
        path = LOOT / f"{species}.json"
        before = "(sem tabela)"
        if path.exists():
            table = json.loads(path.read_text(encoding="utf-8"))
            entry = meat_entry(table)
            if entry is None:
                print(f"{species:<16} tabela sem carne conhecida: não mexi")
                continue
            old = next((f["count"] for f in entry.get("functions", []) if f["function"] == "minecraft:set_count"), {})
            before = f"{entry['name'].split(':')[1]} {old.get('min')}–{old.get('max')}"
            apply_range(entry, low, high, looting)
        elif species in NEW_TABLES:
            table = new_table(NEW_TABLES[species], low, high, looting)
            before += " -> criada"
        else:
            print(f"{species:<16} sem tabela e fora de NEW_TABLES: não mexi")
            continue
        print(f"{species:<16}{width:>5.2f}x{height:<5.2f}{ratio:>8.2f}{average:>7.1f}  {low:>3}–{high:<3}"
              f"    0–{looting:<3}    {before}")
        if not dry:
            write(path, table)
    print("(--dry: nada escrito)" if dry else "tabelas escritas em " + str(LOOT.relative_to(REPO)))


if __name__ == "__main__":
    main()
