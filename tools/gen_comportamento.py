#!/usr/bin/env python3
"""Gera docs/comportamento.html: uma ficha de comportamento por espécie, lida dos dados do mod.

Fonte: os JSON de espécie (data/iceagesurvival/iceagesurvival/species), as tags de entidades e de
biomas, os nomes em pt_br e os tamanhos registrados em ModEntities.java. As regras derivadas
(faro efetivo, raio de território, raio da ronda) repetem as do código — Perception, defendRadius e
StalkGoal — e precisam acompanhar se elas mudarem.

Uso: python3 tools/gen_comportamento.py   (rodar de novo sempre que um JSON de espécie mudar)
"""
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data'
SPECIES = DATA / 'iceagesurvival/iceagesurvival/species'
LANG = ROOT / 'src/main/resources/assets/iceagesurvival/lang/pt_br.json'
ENTITIES = ROOT / 'src/main/java/dev/madebyfelipe/iceagesurvival/registry/ModEntities.java'
OUT = ROOT / 'docs/comportamento.html'

# Regras do código (core/ecology/Perception, StalkGoal, PrehistoricCreature.defendRadius).
PERCEPTION_MARGIN = 8.0
STALKER_FACTOR = 0.5
RING_MARGIN = 3.0
CHASE_GIVE_UP = 30
# Padrões dos codecs (EcologyProfile, WarinessProfile, BehaviorProfile).
ECOLOGY_DEFAULT = {'hunt_radius': 48, 'hunger_seconds': 360, 'chase_seconds': 25, 'rival_radius': 32,
                   'nervousness': 1.0}
WARINESS_DEFAULT = {'players': True, 'alert_radius': 12, 'charge_radius': 0, 'bluff_chance': 0,
                    'retreat_chance': 0, 'charge_chance': 0, 'sneak_factor': 0.5, 'calf_radius': 12,
                    'charge_speed': 1.6, 'flee_speed': 1.4}
SATED_DEFAULT = 180

VANILLA = {
    'minecraft:player': 'Jogador', 'minecraft:cow': 'vaca', 'minecraft:sheep': 'ovelha', 'minecraft:pig': 'porco',
    'minecraft:goat': 'cabra', 'minecraft:horse': 'cavalo', 'minecraft:donkey': 'burro', 'minecraft:llama': 'lhama',
    'minecraft:villager': 'aldeão', 'minecraft:wandering_trader': 'mercador', 'minecraft:chicken': 'galinha',
    'minecraft:rabbit': 'coelho', 'minecraft:fox': 'raposa', 'minecraft:frog': 'sapo', 'minecraft:cod': 'bacalhau',
    'minecraft:salmon': 'salmão', 'minecraft:mule': 'mula', 'minecraft:camel': 'camelo',
}
BIOMES = {
    'snowy_plains': 'planície nevada', 'ice_spikes': 'picos de gelo', 'snowy_taiga': 'taiga nevada',
    'snowy_slopes': 'encostas nevadas', 'snowy_beach': 'praia nevada', 'frozen_river': 'rio congelado',
    'frozen_ocean': 'oceano congelado', 'deep_frozen_ocean': 'oceano congelado profundo', 'grove': 'bosque nevado',
    'frozen_peaks': 'picos congelados', 'jagged_peaks': 'picos irregulares', 'meadow': 'prado',
    'windswept_hills': 'colinas ventosas', 'plains': 'planície', 'taiga': 'taiga', 'river': 'rio',
    'swamp': 'pântano', 'mangrove_swamp': 'mangue', 'beach': 'praia', 'savanna': 'savana',
    'savanna_plateau': 'planalto de savana', 'forest': 'floresta', 'birch_forest': 'floresta de bétulas',
    'dark_forest': 'floresta escura', 'old_growth_pine_taiga': 'taiga antiga de pinheiros',
    'old_growth_spruce_taiga': 'taiga antiga de abetos', 'desert': 'deserto', 'stony_peaks': 'picos rochosos',
    'flower_forest': 'floresta de flores', 'sunflower_plains': 'planície de girassóis',
    'windswept_forest': 'floresta ventosa', 'windswept_gravelly_hills': 'colinas de cascalho',
    'stony_shore': 'costa rochosa', 'cherry_grove': 'bosque de cerejeiras', 'jungle': 'selva',
    'badlands': 'terras áridas', 'ocean': 'oceano', 'cold_ocean': 'oceano frio',
    'windswept_savanna': 'savana ventosa', 'wooded_badlands': 'terras áridas arborizadas', 'sparse_jungle': 'selva esparsa',
}
TFC_BIOMES = {
    'plains': 'planícies', 'lowlands': 'terras baixas', 'hills': 'colinas', 'rolling_hills': 'colinas onduladas',
    'highlands': 'terras altas', 'plateau': 'planalto', 'mountains': 'montanhas', 'old_mountains': 'montanhas antigas',
    'volcanic_mountains': 'montanhas vulcânicas', 'oceanic_mountains': 'montanhas oceânicas',
    'volcanic_oceanic_mountains': 'montanhas oceânicas vulcânicas', 'badlands': 'terras áridas',
    'inverted_badlands': 'terras áridas invertidas', 'canyons': 'cânions', 'low_canyons': 'cânions baixos',
    'river': 'rio', 'lake': 'lago', 'mountain_lake': 'lago de montanha', 'old_mountain_lake': 'lago de montanha antiga',
    'volcanic_mountain_lake': 'lago vulcânico', 'oceanic_mountain_lake': 'lago de montanha oceânica',
    'volcanic_oceanic_mountain_lake': 'lago vulcânico oceânico', 'plateau_lake': 'lago de planalto', 'shore': 'costa',
    'tidal_flats': 'planícies de maré', 'salt_marsh': 'pântano salgado', 'ocean': 'oceano', 'ocean_reef': 'recife',
    'deep_ocean': 'oceano profundo', 'deep_ocean_trench': 'fossa oceânica',
}
TAG_NAMES = {'#iceagesurvival:fish': 'peixes'}
# Tag que põe a espécie em todo bioma (overworld + TFC); o ideal vem em spawn.favored.
ANYWHERE = '#iceagesurvival:spawn_anywhere'
PREFERENCE = {3: 'favorita', 2: 'boa', 1: 'aceitável', 0: 'último recurso'}
SPECIAL = {
    'ambush': 'emboscada: arrancada +50% por 4 s no bote, e o primeiro golpe agarra (presa lenta 2 s)',
    'pack_leap': 'salto: no bote, pula sobre a presa a 3–10 blocos',
    'beak_strike': 'bicada: 30% do dano a mais ignorando armadura, e recua 1,5 s depois de acertar',
    'grab': 'agarrão: sem arrancada; o primeiro golpe do bote prende a presa do porte dele para baixo (lenta 2 s)',
    'gaff': 'garra-gancho: o primeiro golpe depois do bote fisga a presa na água (qualquer porte) ou em terra (do '
            'porte dele para baixo), puxa-a para perto e a prende',
    'swallow': 'engole inteira: a presa da dieta que cabe no bico (porte até 0,1 do dele, nunca filhote) some de uma '
               'vez e vale meia refeição; a maior leva só a bicada',
    'venom': 'mordida que solta: morde, recua e segue o rastro provando o ar com a língua; a peçonha (30 s) é choque '
             '(torpor) nas criaturas do mod e sangramento sem regeneração no jogador e nos vanilla; cura com o antídoto',
}
ACTIVITY = {'nocturnal': 'noturno: de dia dorme escondido e não caça; só acorda se atacado',
            'diurnal': 'diurno: à noite dorme escondido e não caça; só acorda se atacado'}


def esc(text):
    return html.escape(str(text))


def num(value):
    value = float(value)
    return f'{value:.0f}' if value == int(value) else f'{value:.2f}'.rstrip('0').replace('.', ',')


def tag_values(kind, ref, seen=None):
    """Resolve uma tag (com tags aninhadas) em ids."""
    seen = seen or set()
    if ref in seen:
        return []
    seen.add(ref)
    namespace, path = ref.lstrip('#').split(':')
    file = DATA / namespace / 'tags' / kind / f'{path}.json'
    if not file.exists():
        return []
    out = []
    for value in json.loads(file.read_text())['values']:
        value = value if isinstance(value, str) else value['id']
        out += tag_values(kind, value, seen) if value.startswith('#') else [value]
    return out


def includes_tag(kind, ref, target, seen=None):
    """Se a tag (ou uma tag aninhada nela) inclui a tag alvo."""
    seen = seen or set()
    if ref == target:
        return True
    if ref in seen:
        return False
    seen.add(ref)
    namespace, path = ref.lstrip('#').split(':')
    file = DATA / namespace / 'tags' / kind / f'{path}.json'
    if not file.exists():
        return False
    values = [v if isinstance(v, str) else v['id'] for v in json.loads(file.read_text())['values']]
    return any(includes_tag(kind, v, target, seen) for v in values if v.startswith('#'))


def biome_names(ids):
    """Nomes dos biomas, os do TerraFirmaCraft à parte."""
    vanilla = [BIOMES.get(i.split(':')[1], i.split(':')[1]) for i in ids if not i.startswith('tfc:')]
    tfc = [TFC_BIOMES.get(i.split(':')[1], i.split(':')[1]) for i in ids if i.startswith('tfc:')]
    text = ', '.join(dict.fromkeys(vanilla))
    if tfc:
        text += ('; ' if text else '') + 'no TerraFirmaCraft: ' + ', '.join(dict.fromkeys(tfc))
    return text


lang = json.loads(LANG.read_text())
sizes = {name: (float(w), float(h)) for name, w, h in
         re.findall(r'landCreature\("(\w+)",\s*([\d.]+)F,\s*([\d.]+)F\)', ENTITIES.read_text())}
species = {f.stem: json.loads(f.read_text()) for f in sorted(SPECIES.glob('*.json')) if f.stem != 'test_creature'}
disabled = set(tag_values('entity_types', '#iceagesurvival:disabled'))
apex = set(tag_values('entity_types', '#iceagesurvival:apex'))


def name(entity_id):
    if entity_id.startswith('#'):
        return TAG_NAMES.get(entity_id, entity_id)
    if entity_id.startswith('iceagesurvival:'):
        return lang.get('entity.iceagesurvival.' + entity_id.split(':')[1], entity_id)
    namespace, path = entity_id.split(':')
    if namespace != 'minecraft':
        return f'{path.replace("_", " ")} ({namespace.upper()})'
    return VANILLA.get(entity_id, path)


def behavior(key):
    return species[key].get('behavior', {})


def wariness(key):
    w = behavior(key).get('wariness')
    return None if w is None else {**WARINESS_DEFAULT, **w}


def ecology(key):
    return {**ECOLOGY_DEFAULT, **behavior(key).get('ecology', {})}


def prey_types(key):
    ref = behavior(key).get('prey')
    return tag_values('entity_types', ref) if ref else []


def largest_prey_alert(key):
    largest, who = 0.0, None
    for entity in prey_types(key):
        other = entity.split(':')[1]
        if entity.startswith('iceagesurvival:') and other in species and other != key and wariness(other):
            w = wariness(other)
            alert = max(w['alert_radius'], w['calf_radius'])
            if alert > largest:
                largest, who = alert, entity
    return largest, who


def defend_radius(key):
    herd = behavior(key).get('herd_radius', 0)
    width = sizes.get(key, (1.0, 1.0))[0]
    radius = herd + 6.0 if herd > 0 else width * 4.0 + 4.0
    return min(max(radius, 8.0), 24.0)


def diet_prey(entry):
    """As presas de uma linha da dieta: lista de ids ou uma tag."""
    prey = entry['prey']
    if isinstance(prey, str):
        return tag_values('entity_types', prey) if prey.startswith('#') else [prey]
    return prey


def diet(key):
    """Preferência de cada presa: a primeira linha da dieta que casa, e o que está só na tag fica com 1."""
    table = {}
    for entry in behavior(key).get('diet', []):
        for entity in diet_prey(entry):
            table.setdefault(entity, entry['preference'])
    for entity in prey_types(key):
        table.setdefault(entity, 1)
    if 'minecraft:player' not in {e for entry in behavior(key).get('diet', []) for e in diet_prey(entry)}:
        table.pop('minecraft:player', None)
    return table


def diet_labels(key, entities):
    """Troca pelo nome da tag os membros de uma tag da dieta que estão todos na mesma preferência."""
    out = list(entities)
    for entry in behavior(key).get('diet', []):
        tag = entry['prey']
        if isinstance(tag, str) and tag in TAG_NAMES:
            members = set(tag_values('entity_types', tag))
            if members and members <= set(out):
                out = [e for e in out if e not in members] + [tag]
    return out


carnivores = [k for k in species if behavior(k).get('prey')]
hunted_by = {}
for hunter in carnivores:
    if f'iceagesurvival:{hunter}' in disabled:
        continue
    for entity, preference in diet(hunter).items():
        hunted_by.setdefault(entity, []).append((hunter, preference))


def group_text(spawn):
    low, high = spawn.get('group_min', 1), spawn.get('group_max', 1)
    return f'{low}–{high}' if high != low else str(low)


def row(label, value):
    return f'<tr><th>{esc(label)}</th><td>{value}</td></tr>'


def card(key):
    data = species[key]
    b = behavior(key)
    w = wariness(key)
    eco = ecology(key)
    entity = f'iceagesurvival:{key}'
    width, height = sizes.get(key, (0, 0))
    carnivore = bool(b.get('prey'))
    role = 'Carnívoro' if carnivore else 'Herbívoro'
    badges = [f'<span class="badge {"meat" if carnivore else "leaf"}">{role}</span>']
    if 'mount' in data:
        badges.append('<span class="badge">Montável</span>')
    if entity in disabled:
        badges.append('<span class="badge off">Desligado</span>')
    if b.get('aggressive'):
        badges.append('<span class="badge">Agressivo</span>')
    if entity in apex:
        badges.append('<span class="badge off">Apex</span>')

    social = []
    herd = b.get('herd_radius', 0)
    spawn = data.get('spawn', {})
    group = group_text(spawn)
    social.append(row('Bando', f'{group} (limite {spawn.get("group_max", 1)}); '
                       + (f'manada de raio {herd}' if herd else 'solitário')
                       + ('; defende o grupo' if b.get('group_defense') else '')
                       + ('; migra' if b.get('migrates') else '')))
    social.append(row('Território do bando', f'{num(defend_radius(key))} blocos em volta de cada membro'
                       + (f'; passeia num raio de {b["territory_radius"]} da toca' if b.get('territory_radius') else '')))
    if b.get('flee_health_fraction'):
        social.append(row('Foge ferido', f'abaixo de {num(b["flee_health_fraction"] * 100)}% da vida'))
    social.append(row('Nervosismo', num(eco['nervousness'])))
    if entity in apex:
        social.append(row('Apex', 'não cai com tranquilizante; só se doma vencendo o desafio com a cabeça de outro da espécie'))
    body = data.get('body', {})
    breaks = body.get('breaks', 'plants' if body.get('breaks_leaves') else 'none')
    giant = body.get('giant', False)
    social.append(row('Quebra blocos', {
        'wood': 'madeira (muro/portão nosso em ' + ('3' if giant else '6') + ' golpes, madeira vanilla em '
                + ('2' if giant else '3') + ') só para alcançar o alvo' + ('; derruba tronco ao esbarrar' if giant else ''),
        'plants': 'só folhas e plantas, ao passar',
        'none': 'nada',
    }.get(breaks, 'nada')))
    carcass = b.get('ecology', {}).get('carcass_portions', 0)
    if carcass:
        social.append(row('Carcaça', f'morto selvagem sem jogador, fica caído com {carcass} porções de carne por até '
                           f'10 min: o predador mais forte come primeiro; o jogador carneia a sobra com machado ou espada'))
    habits = b.get('habits', {})
    if habits.get('activity') in ACTIVITY:
        social.append(row('Horário', ACTIVITY[habits['activity']]))
    if habits.get('camouflage', 1) < 1:
        social.append(row('Camuflagem', f'escondido em folhas, mato ou neve alta, é notado a '
                           f'{num(habits["camouflage"] * 100)}% do raio'))
    if habits.get('scavenges'):
        social.append(row('Carniça', 'com fome, come carne crua do chão, longe de predador maior (16 blocos)'))
    fishing = habits.get('fishing')
    if fishing:
        social.append(row('Pesca', f'selvagem, pesca na água sem gelo a {num(fishing["radius"])} blocos '
                           f'({num(fishing["chance"] * 100)}% de chance quando procura comida); domesticado e parado, '
                           f'pesca a {num(fishing["tame_radius"])} blocos e guarda o peixe'))
    if habits.get('sentinel_radius'):
        social.append(row('Sentinela', f'domesticado, avisa o dono de predador selvagem a {num(habits["sentinel_radius"])} blocos'))
    dung = data.get('dung')
    if dung:
        social.append(row('Esterco', f'adulto acordado deixa um a cada ~{num(dung.get("interval_seconds", 600) / 60)} min, '
                           f'e mais um a cada {num(dung.get("food_per_dung", 60))} de alimento que come; aduba como farinha de osso'))
    mount = data.get('mount', {})
    if mount.get('break_tool'):
        social.append(row('Montado', f'a mordida minera a tag {esc(mount.get("break_blocks", "?"))} até dureza '
                           f'{num(mount.get("break_hardness", 0))}, com os drops de {esc(mount["break_tool"])}'))
    flight = data.get('stats', {}).get('flight_stamina')
    if flight:
        social.append(row('Fôlego de voo', f'{num(flight["base"])} s no nível 1 (+{num(flight.get("per_point", 0) * 100)}% por ponto)'))

    sense = []
    if w:
        sense.append(row('Alerta', f'{num(w["alert_radius"])} blocos'
                         + (f' ({num(w["calf_radius"])} com filhote)' if w['calf_radius'] > w['alert_radius'] else '')))
        sense.append(row('Nota quem espreita a', f'{num(max(w["alert_radius"], 0) * STALKER_FACTOR)} blocos'))
        reaction = []
        if w.get('defense') == 'tail_club':
            reaction.append(f'não foge nem investe: a {num(w["charge_radius"])} blocos vira a cauda e golpeia quem entra '
                            'no arco de trás (100°, 4,5 blocos do centro), mesmo sem tê-lo notado; o golpe quebra a perna '
                            '(−60% de velocidade, sem pulo, 5 s); pela frente não tem golpe; entre dois da espécie, duelo '
                            'no flanco que não passa de 40% da vida')
        elif w['charge_radius'] > 0:
            reaction.append(f'investe a {num(w["charge_radius"])} blocos')
            if w['bluff_chance']:
                reaction.append(f'{num(w["bluff_chance"] * 100)}% blefe')
        else:
            reaction.append('não investe: foge')
        if w['retreat_chance']:
            reaction.append(f'{num(w["retreat_chance"] * 100)}% recua')
        sense.append(row('Lutar ou fugir', ', '.join(reaction)))
        sense.append(row('Jogador', 'é ameaça (agachado, a '
                         + f'{num(w["sneak_factor"] * 100)}% do alerta)' if w['players'] else 'não o teme'))
    else:
        sense.append(row('Cautela', 'sem perfil de cautela'))

    hunt = ''
    if carnivore:
        largest, who = largest_prey_alert(key)
        effective = max(eco['hunt_radius'], largest + PERCEPTION_MARGIN if largest else 0)
        rows = [
            row('Faro de caça', f'<strong>{num(effective)}</strong> blocos'
                + (f' (maior alerta das presas: {num(largest)}, de {esc(name(who))}, + {num(PERCEPTION_MARGIN)})'
                   if who else '')),
            row('Fome', f'caça de novo depois de {eco["hunger_seconds"]} s sem comer; saciado por '
                f'{b.get("sated_seconds", SATED_DEFAULT)} s (o bando inteiro)'),
            row('Caça', ('espreita e dispara' if b.get('hunt_style', 'stalk') == 'stalk' else 'vai direto')
                + f'; fôlego de {eco["chase_seconds"]} s; desiste se a presa abrir {CHASE_GIVE_UP} blocos'),
        ]
        if b.get('hunt_special') in SPECIAL:
            rows.append(row('Marca', esc(SPECIAL[b['hunt_special']])))
        pack = data.get('pack_bonus')
        if pack:
            rows.append(row('Bônus de bando', f'+{num(pack["attack_multiplier"] * 100)}% de dano e velocidade com aliado do bando a {pack["radius"]} blocos'))
        table = diet(key)
        by_pref = {}
        for prey, preference in table.items():
            by_pref.setdefault(preference, []).append(prey)
        items = []
        for preference in sorted(by_pref, reverse=True):
            names = sorted(diet_labels(key, by_pref[preference]), key=lambda e: (e != 'minecraft:player', name(e)))
            items.append(f'<li><span class="pref p{preference}">{PREFERENCE[preference]}</span> '
                         + ', '.join(f'<strong>{esc(name(e))}</strong>' if e == 'minecraft:player' else esc(name(e))
                                     for e in names) + '</li>')
        rings = []
        for prey in sorted(table, key=lambda e: -table[e]):
            other = prey.split(':')[1]
            if prey.startswith('iceagesurvival:') and wariness(other):
                ring = wariness(other)['alert_radius'] * STALKER_FACTOR + RING_MARGIN
                rings.append(f'{esc(name(prey))} a ~{num(ring)}')
        hunt = (f'<h4>Caça</h4><table>{"".join(rows)}</table>'
                f'<h4>Dieta</h4><ul class="diet">{"".join(items)}</ul>'
                + (f'<p class="small">Ao espreitar, ronda a esta distância da presa: {"; ".join(rings[:6])} blocos.</p>' if rings else ''))

    predators = sorted(hunted_by.get(entity, []), key=lambda item: (-item[1], name(f'iceagesurvival:{item[0]}')))
    chased = ('<p class="small"><strong>Caçado por:</strong> '
              + ', '.join(f'{esc(name("iceagesurvival:" + h))} ({PREFERENCE[p]})' for h, p in predators)
              + '</p>') if predators else '<p class="small"><strong>Caçado por:</strong> ninguém.</p>'

    spawn_rows = []
    if spawn:
        favored = spawn.get('favored')
        base = spawn.get('weight', 10)
        if includes_tag('worldgen/biome', spawn['biomes'], ANYWHERE):
            spawn_rows.append(row('Biomas', 'qualquer bioma da superfície (e do TerraFirmaCraft)'))
        else:
            spawn_rows.append(row('Biomas', esc(biome_names(tag_values('worldgen/biome', spawn['biomes'])))))
        weight = f'peso {base}'
        if favored:
            spawn_rows.append(row('Ideal', esc(biome_names(tag_values('worldgen/biome', favored['biomes'])))))
            weight += f'; {favored["weight"]} no ideal (×{num(favored["weight"] / base)})'
        spawn_rows.append(row('Frequência', weight + f'; até {spawn.get("max_nearby", 4)} perto do jogador'
                              + (f'; um grupo a cada {spawn["spacing"]} blocos' if spawn.get('spacing') else '')
                              + (f'; a {spawn["min_distance"]}+ blocos do spawn' if spawn.get('min_distance') else
                                 '; desde o spawn')))
    else:
        spawn_rows.append(row('Spawn', 'não nasce sozinho'))

    speed = data.get('stats', {}).get('speed', {}).get('base')
    return f'''
    <article class="species" id="{key}">
      <header><h3>{esc(name(entity))}</h3><code>{key}</code><div class="badges">{"".join(badges)}</div></header>
      <p class="small">{num(width)} × {num(height)} blocos{f" · velocidade {num(speed)}" if speed else ""}</p>
      <h4>Bando e território</h4><table>{"".join(social)}</table>
      <h4>Percepção</h4><table>{"".join(sense)}</table>
      {hunt}
      {chased}
      <h4>Spawn</h4><table>{"".join(spawn_rows)}</table>
    </article>'''


def summary_row(key):
    b = behavior(key)
    w = wariness(key)
    entity = f'iceagesurvival:{key}'
    spawn = species[key].get('spawn', {})
    if b.get('prey'):
        largest, _ = largest_prey_alert(key)
        faro = num(max(ecology(key)['hunt_radius'], largest + PERCEPTION_MARGIN if largest else 0))
    else:
        faro = '—'
    off = ' <span class="badge off">desligado</span>' if entity in disabled else ''
    return (f'<tr><td><a href="#{key}">{esc(name(entity))}</a>{off}</td>'
            f'<td>{"Carnívoro" if b.get("prey") else "Herbívoro"}</td>'
            f'<td>{group_text(spawn)}</td>'
            f'<td>{num(w["alert_radius"]) if w else "—"}</td><td>{faro}</td>'
            f'<td>{len(hunted_by.get(entity, []))}</td></tr>')


order = sorted(species, key=lambda k: (not behavior(k).get('prey'), name(f'iceagesurvival:{k}')))
STYLE = (ROOT / 'docs/ecologia.html').read_text().split('<style>')[1].split('</style>')[0]
EXTRA = '''
    .species { margin: 1.25rem 0; padding: 1.25rem; border: 1px solid var(--line); border-radius: .8rem;
      background: linear-gradient(145deg, var(--panel), #142123); }
    .species > header { display: flex; flex-wrap: wrap; align-items: baseline; gap: .4rem .8rem; width: auto;
      margin: 0; padding: 0; }
    .species > header h3 { margin: 0; font-size: 1.4rem; }
    .species h4 { margin: 1.1rem 0 .4rem; color: var(--accent); font-size: .8rem; letter-spacing: .12em;
      text-transform: uppercase; }
    .species table { background: transparent; }
    .species th { width: 34%; background: transparent; color: var(--text); font-weight: 600; }
    .species th, .species td { padding: .4rem .6rem .4rem 0; font-size: .92rem; }
    .badges { display: flex; flex-wrap: wrap; gap: .35rem; }
    .badge { padding: .05rem .5rem; border: 1px solid var(--line); border-radius: 999px; color: var(--muted);
      font-size: .74rem; }
    .badge.meat { border-color: #8a5a4c; color: #f0b4a0; }
    .badge.leaf { border-color: #4f7a5c; color: #a8dcb4; }
    .badge.off { border-color: #7a6a3a; color: var(--warn); }
    .diet { margin: .3rem 0; padding-left: 1.1rem; }
    .pref { display: inline-block; min-width: 7.5rem; color: var(--text); font-size: .82rem; font-weight: 700; }
    .pref.p3 { color: #f1cb85; } .pref.p2 { color: var(--accent); } .pref.p0 { color: #8fa39c; }
    .toc { columns: 3 12rem; padding-left: 1.1rem; }
'''

page = f'''<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Ficha de comportamento de cada espécie do Ice Age Survival: bando, território, percepção, dieta, quem a caça e onde nasce.">
  <title>Comportamento — Ice Age Survival</title>
  <style>{STYLE}{EXTRA}  </style>
</head>
<body>
  <!-- Gerado por tools/gen_comportamento.py a partir dos JSON de espécie. Não editar à mão. -->
  <header>
    <div class="eyebrow">Ice Age Survival · documentação técnica</div>
    <h1>Comportamento por espécie</h1>
    <p class="lead">Uma ficha por criatura, lida dos dados do mod: com quem anda, o que defende, de quão longe percebe,
      o que come, quem a caça e onde nasce. As regras gerais estão em <a href="ecologia.html">Ecologia</a>.</p>
    <div class="stamp">Gerado de <code>species/*.json</code> · Minecraft 1.20.1 / Forge</div>
  </header>
  <main>
    <aside class="notice"><strong>Como ler:</strong> o <em>faro de caça</em> é o efetivo, sempre pelo menos o maior
      alerta das presas + {num(PERCEPTION_MARGIN)} blocos (o carnívoro percebe antes de ser percebido). Quem espreita é
      notado a {num(STALKER_FACTOR * 100)}% do alerta, e ronda logo além disso (+{num(RING_MARGIN)}). Preferência da dieta:
      favorita › boa › aceitável › último recurso.</aside>

    <h2>Resumo</h2>
    <div class="table-wrap"><table>
      <thead><tr><th>Espécie</th><th>Papel</th><th>Bando</th><th>Alerta</th><th>Faro de caça</th><th>Predadores</th></tr></thead>
      <tbody>{"".join(summary_row(k) for k in order)}</tbody>
    </table></div>

    <h2>Fichas</h2>
    {"".join(card(k) for k in order)}
  </main>
  <footer><p>Gerado por <code>tools/gen_comportamento.py</code>. Para decisões e roadmap, consulte
    <a href="../CLOUD.md">CLOUD.md</a>.</p></footer>
</body>
</html>
'''
OUT.write_text(page)
print(f'{OUT.relative_to(ROOT)}: {len(species)} espécies')
