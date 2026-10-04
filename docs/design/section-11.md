## 11. Criaturas

### Níveis e atributos (implementado)

Atributos: `health`, `attack`, `speed`, `torpor` (torpor máximo), `armor` (resistência).

- Uma criatura selvagem nasce com nível sorteado em múltiplos de `wildLevelStep` até `maxWildLevel` (config de servidor; padrão 10 e 100 → 10, 20, …, 100). Baixar o step para 1 habilita níveis intermediários.
- O nível vira `nível − 1` pontos distribuídos ao acaso entre os atributos escaláveis (todos menos `speed`).
- Valor final = `base × (1 + per_point × pontos)`, com `base` e `per_point` vindos do JSON da espécie.
- Os pontos são salvos no NBT da entidade (`StatPoints`); o nível é sincronizado para o cliente.

Formato atual do JSON de espécie (cresce a cada etapa):

```json
{
  "stats": {
    "health": { "base": 20, "per_point": 0.2 },
    "attack": { "base": 3, "per_point": 0.05 },
    "speed":  { "base": 0.25 },
    "torpor": { "base": 50, "per_point": 0.06 },
    "armor":  { "base": 2, "per_point": 0.04 }
  }
}
```

**Velocidade de mobs:** a velocidade real cresce com o *quadrado* de `speed × modificador do goal`; o jogador anda a ~4,3 blocos/s e corre a ~5,6. Um produto de 0,5 dá ~11 blocos/s (o dobro da corrida do jogador); 0,2 dá ~1,8. O Smilodon tem `speed` 0,4 e persegue com modificador 1,25.

**Corpo:** bloco `body` na espécie, para o que é físico:

```json
"body": { "knockback_resistance": 1.0, "step_height": 1.1, "breaks_leaves": true }
```

- `knockback_resistance` (0 a 1, padrão 0). Regra de design: toda espécie maior que o jogador usa 1 — um golpe não arremessa um animal desses.
- `step_height` (padrão 0,6): altura que sobe sem pular. Animais grandes e rápidos usam 1,1; com o padrão eles travam em qualquer degrau de um bloco.
- `breaks_leaves` (padrão falso): ao esbarrar numa copa, destrói as folhas e passa, como o ravager. Respeita a regra `mobGriefing`. Sem isso, um animal de mais de 2 blocos de altura fica preso em floresta.

**Limites do vanilla:** `max_health` satura em 1024 e `armor` em 30. Valores acima são cortados pelo jogo. Espécies grandes e o boss precisam caber nisso ou usar outro mecanismo (ver [25](section-25.md)).

### Espécies

Todas as espécies terrestres usam a mesma classe (`LandCreature`); o que as diferencia é o JSON e os assets.

| Espécie | Papel | Tamanho (colisão) | Comportamento | Nasce em |
|---|---|---|---|---|
| **Lobo-terrível** (`dire_wolf`) | Primeira domesticação; predador de matilha | 0,8 × 1,2 | Agressivo, matilha de 2–4, defesa em grupo, caça presas pequenas, recua com 20% de vida | taiga, taiga nevada, taigas antigas, grove, planície nevada |
| **Smilodon** (`smilodon`) | Predador territorial solitário, rápido | 1,3 × 2,3 | Agressivo e **espreita** (`hunt_style: stalk`): aproxima-se devagar pelas costas e só dá o bote, rugindo, a 4,5 blocos, ao ser ferido ou quando o jogador o vê. Território de 32 blocos, caça presas grandes, recua com 25% de vida | taiga, taiga nevada, taigas antigas, grove, encostas nevadas, planície nevada e picos de gelo, desde o spawn; sozinho ou em casal |
| **Urso-terrível** (`direbear`) | Predador territorial solitário, resistente | 2,0 × 3,0 | Agressivo, território de 64 blocos, caça presas grandes e recua com 15% de vida | taiga, taiga nevada, taigas antigas, grove, encostas nevadas |
| **Mamute-lanoso** (`mammoth`) | Herbívoro de manada, tanque; montado, coletor de madeira | 3,0 × 4,65 | Pacífico até ser provocado; manada migratória de 2–4 que se defende junta | planície nevada, ice spikes, taiga nevada |
| **Tyrannosaurus rex** (`tyrannosaurus`) | Primeiro dinossauro; predador de topo solitário, raro | 2,7 × 5,4 | Agressivo, raio de percepção 24, território de 48 blocos, caça presas grandes, recua com 10% de vida | taiga, taiga nevada, taigas antigas, planície nevada (peso 1, a partir de 300 blocos) e um garantido a 220–297 blocos do spawn |
| **Velociraptor** (`velociraptor`) | Predador pequeno de bando | 0,7 × 1,1 | Agressivo, bando de 3–5 com defesa em grupo, caça presas pequenas | taiga, taiga nevada, grove |
| **Utahraptor** (`utahraptor`) | Raptor grande, montável (o "raptor" do ARK) | 1,2 × 2,3 | Agressivo, bando de 2–3, caça presas grandes | taigas nevadas e de abetos |
| **Kelenken** (`kelenken`) | A maior ave-terrível: a mais forte sozinha, corredora, montável | 1,1 × 2,6 | Solitária ou em par; caça mamíferos pequenos e médios (ovelha, porco, cabra, coelho, dodô) com a **bicada** (30% do dano ignora armadura) e golpe e recua; perseguição de 40 s; vida 100, ataque 20, `speed` 0,42 (montada 0,9×, atrás do Galimimo); modelo, animações e sons do Revival | planícies, savanas e terras áridas, nevadas ou não, a 300+ do spawn |
| **Ornitholestes** (`ornitholestes`) | Pequeno terópode do sub-bosque (2 m, 15 kg); primeiro carnívoro a domesticar, **sentinela** | 0,7 × 1,1 | Arisco: foge do jogador e dos predadores (cautela com `players`), só morde encurralado ou ferido; sozinho ou em par; espreita coelho, sapo, galinha e dodô e **agarra** com as mãos (`grab`); **noturno** (dorme escondido de dia), **camuflado** no sub-bosque e na neve alta (percebido a 50%), come **carniça**; vida 12, ataque 3, `speed` 0,42; presa do lobo-terrível, Velociraptor e Kelenken; domesticado, avisa o dono de predadores a 32 blocos (D34); modelo, animações e sons do Revival | planície nevada, taigas, grove e floresta, desde o spawn (peso 3) |
| **Baryonyx** (`baryonyx`) | Espinossaurídeo **pescador** da beira d'água (7,5–10 m, 1,2–2 t); predador médio montável que **nada** | 1,8 × 2,7 | Agressivo, solitário ou em par (`pair_chance` 0,3); pesca na água rasa sem gelo (peixe vivo é captura certa) e, domesticado com "Parar" perto d'água, guarda peixe no inventário; **garra-gancho** (`gaff`) fisga e puxa a presa; **anfíbio** (não se afoga, foge para a água); come carniça; caça peixes, dodô, Galimimo, Ornitholestes, ovelha, porco, cabra, coelho, galinha e Velociraptor; vida 150, ataque 18, `speed` 0,38; presa do Espinossauro (D37); modelo, poses, texturas e sons do **Jurassic Reborn** (D36) | rio, praia, pântano e mar gelado (peso 2), planície e taiga (nevadas ou não, peso 1), a 300+ blocos |
| **Espinossauro** (`spinosaurus`) | Predador de topo das águas geladas, montável | 2,7 × 5,6 | Agressivo, atravessa o mato, domesticado com peixe; assento à frente da vela (`seat_forward`) | sobretudo na beira d'água (rio, pântano, praia, oceano congelado; peso 3), mas também em planícies e taigas, nevadas ou não (peso 1; `spawn.favored`); a 400+ do spawn |
| **Alossauro** (`allosaurus`, antes `carnotaurus`) | Predador médio rápido que caça em bando, montável, muito raro | 1,8 × 3,4 | Velocidade-base igual ao Smilodon e superior à do T-Rex; mais forte que o Smilodon; bando concede +25% velocidade e dano, caça brontos e mamutes, atravessa o mato | planície nevada, encostas nevadas, grove (a partir de 1.500 blocos do spawn) |
| **Brontossauro** (`brontosaurus`) | Saurópode gigante migratório; montável, coletor de madeira e transporte de carga | 4,0 × 8,0 | Pacífico, manada de 1–3 que viaja e se defende junta; 216 espaços de inventário (quatro baús grandes); atravessa o mato | planície nevada, grove, taiga nevada |
| **Estegossauro** (`stegosaurus`) | Herbívoro defensivo de manada; armazenamento móvel pequeno | conforme o registro | Manada migratória e defesa em grupo; 27 espaços (um baú pequeno) | savanas frias e florestas abertas |
| **Pteranodonte** (`pteranodon`) | Montaria aérea para exploração | 1,6 × 2,0 | Voo montado no estilo do Cobblemon (ver Montaria); **selvagem, voa sozinho** (`WildFlightGoal`): decola em média a cada ~20 s no chão e na hora quando ferido ou na água, plana a 10–26 blocos do chão por 30–90 s e pousa em terra; fora do voo, plana em vez de despencar; animação de voo separada da de andar; hitboxes multipartes | qualquer bioma da superfície (`#minecraft:is_overworld`), desde o spawn, abundante (peso 8/10, grupos de 2–4, até 8 por jogador) |
| **Quetzalcoatlus** (`quetzalcoatlus`) | O maior pterossauro (10–11 m de envergadura); montaria aérea de viagem longa | 2,2 × 4,5 (desenho na escala 3,6, bem maior) | Caça a pé e **engole inteira** a presa pequena (peixe, sapo, coelho, galinha, dodô; Ornitholestes e Velociraptor menos); decola num salto só com céu aberto, sobe nas **térmicas** de dia, pousa com fome e ao anoitecer, **pesca** pela margem; bando de 2–4 com defesa em grupo, não teme o jogador; vida 110, ataque 12, fôlego de voo 90 s, 27 espaços (D42); modelo e animações do Revival, sons do Jurassic Reborn | ideal: planícies, savanas, rios e planície nevada (D41), a 400+ blocos |
| **Dodô** (`dodo`) | Comida e pena do início de jogo | 0,7 × 0,9 | Como o real: lento (`speed` 0,1, metade do passo de antes), manso e **sem medo de gente** (`wariness.players: false`, nervosismo 0,3) — por isso foi extinto; foge só de predadores a até 8 blocos ou com 35% de vida; bando de 2–5 que acode quem é atacado (`group_defense`, bicadas de 1); carne (crua/assada) e **pena comum** (2–4); domesticado com frutas e sementes | biomas nevados abertos, desde o spawn (peso 14) |
| **Elasmotério** (`elasmotherium`) | Primeira montaria: pele, carga pequena e calor | 1,8 × 2,4 | Pacífico até ser provocado, manada de 1–3 com defesa em grupo; **monta sem sela**, 9 espaços de carga, **aquece** quem monta (0,6) e quem está a até 4 blocos; dá pele | planície e taiga nevadas, grove, ice spikes, desde o spawn (peso 9) |
| **Galimimo** (`gallimimus`) | Corredor; a montaria terrestre mais rápida | 1,2 × 2,3 | Pacífico e arisco: bando de 3–5 que foge de gente e de predadores (nervosismo 1,6); `speed` 0,42 e montado a 0,42 (o Alossauro, segundo, a 0,38); pula; come frutas, grãos, sementes e um pouco de carne; dá carne e pena; presa de raptores e grandes predadores | planície nevada, ice spikes, taigas e grove, desde o spawn (peso 7) |
| **Tricerátopo** (`triceratops`) | Herbívoro de manada defensivo; montaria-tanque | 2,4 × 2,8 | Pacífico até ameaçado: manada migratória de 2–4 com defesa em grupo; investe com os chifres (raio 6, arremesso 1,4/0,5) contra predador ou jogador que chega perto, às vezes só ameaça; 260 de vida, armadura 12; montado, quebra troncos e folhas; dá carne e couro; presa de T-Rex e Alossauro | planície e taiga nevadas, taiga, grove e encostas nevadas, desde o spawn (peso 8) |
| **Anquilossauro** (`ankylosaurus`) | Herbívoro blindado e solitário; montaria que **minera pedra** e dá **esterco** | 2,6 × 2,2 | Pacífico, **solitário** (casal raro, `pair_chance` 0,15); não foge nem investe: **vira a cauda** para a ameaça e a **clava** golpeia quem está no arco de trás (100°, 4,5 blocos do centro) — mesmo sem tê-lo notado, então o carnívoro que espreita pelas costas leva o golpe antes do bote; pela frente não tem golpe (quem circula de perto pela frente fica fora da cauda); o golpe dá **perna quebrada** (−60% de velocidade e sem pulo por 5 s); dois machos se cruzam e **duelam no flanco** sem passar de 40% da vida; montado, a mordida minera pedra, cascalho e minérios comuns com drops de picareta de pedra; **esterco** (farinha de osso) a cada ~10 min e a cada 60 de alimento; vida 380, ataque 24, armadura 18, `speed` 0,18; presa perigosa de T-Rex e dos grandes carnívoros; modelo, animações e sons do Revival (D48) | qualquer bioma, ideal planícies, taigas e grove (peso 3, ×3 no ideal), a 300+ blocos |
| Criatura de teste (`test_creature`) | Só para testes automáticos; usa o modelo do porco | 0,9 × 0,9 | Passiva | não nasce |

Montáveis: Elasmotério (sem sela), Smilodon, urso-terrível, mamute, Tyrannosaurus, Utahraptor, Espinossauro, Baryonyx (nada montado), Alossauro, Brontossauro, Pteranodonte, Quetzalcoatlus e Anquilossauro (coletor de pedra).

A coluna "Nasce em" é o bioma **ideal** de cada espécie: desde a D41 toda espécie nasce em qualquer bioma da superfície, com o triplo do peso no ideal. O addon carrega os modelos e sons do Revival em runtime (Deinonychus para Utahraptor e Diplodocus para Brontossauro); nenhum desses arquivos é copiado para o projeto ou para o jar.

**Animação de ataque:** toca uma vez (`LoopType.PLAY_ONCE` explícito). O `thenPlay` do GeckoLib 4 usa o `loop` do arquivo, e o `attack_normal_1` do T-Rex e o `attack` do Elasmotério vêm do Revival com `loop: true` — o T-Rex ficava com o golpe preso em loop.

**Ecologia (presas):** cada predador caça uma tag de tipos (`behavior.prey`): o T-Rex `tyrannosaurus_prey` (brontos, mamutes e os grandes do vanilla), o Alossauro `allosaurus_prey` (idem), o lobo-terrível `dire_wolf_prey` (mamutes e os pequenos do vanilla). Regras do `HuntGoal`: nunca caça criatura domesticada; caçador solitário (sem `herd_radius`) só ataca presa de manada **desgarrada** — o T-Rex pega o bronto isolado, não o do meio do grupo; caçador de bando chama o bando (`rallyPack`) e pode atacar a manada, que se defende junta. O T-Rex vagueia por um território de 160 blocos.

**Grandes animais não ficam presos no mato:** `body.plow_hardness` quebra, ao esbarrar, os blocos da tag `iceagesurvival:plowable` (troncos, folhas, plantas, neve) até essa dureza, e a perseguição (`ChaseGoal`) vai em linha reta quando não há caminho completo até o alvo. O chão e as encostas não são quebrados: sobem por eles. Os agressivos lembram do alvo por 10 s sem vê-lo. O T-Rex persegue a mais de 5,6 blocos/s, o sprint do jogador (gametest). O lobo-terrível é pequeno demais e fica de fora — o que o exclui é não ter bloco `mount` no JSON, não uma regra em código.

**Adicionar uma espécie terrestre:**

1. `tools/gen_<especie>.py` — ossos, cores e detalhes; gera geometria, textura e as quatro animações (`idle`, `walk`, `attack`, `unconscious`).
2. Uma linha em `ModEntities` (id e caixa de colisão) e um ovo gerador em `ModItems`.
3. `data/iceagesurvival/iceagesurvival/species/<especie>.json` — atributos, corpo, domesticação, comportamento.
4. Opcional: biome modifier + tag de biomas, para nascer no mundo.
5. Traduções.

Nenhuma classe Java nova para espécies cobertas pelos perfis existentes. Um comportamento realmente novo (por exemplo, voo selvagem ou boss) pode exigir extensão do núcleo; o voo montado atual usa o perfil genérico de montaria.

Pendentes do brief — Era do Gelo: rinoceronte-lanoso, megaloceros, megatherium, urso-das-cavernas, bisão, auroque, mastodonte. Dinossauros: ~~gallimimus~~, ~~triceratops~~, ~~tyrannosaurus~~, ~~velociraptor~~, ~~spinosaurus~~, ~~utahraptor~~, ~~allosaurus~~, ~~brontosaurus~~ ~~ankylosaurus~~, ~~giganotosaurus (boss)~~ (feitos; o carnotaurus virou allosaurus).

### Comportamento (implementado)

Bloco `behavior` do JSON de espécie (ausente = passiva, sem território):

```json
"behavior": {
  "aggressive": true,
  "aggro_radius": 14,
  "territory_radius": 32,
  "flee_health_fraction": 0.25,
  "herd_radius": 10,
  "migrates": true,
  "group_defense": true,
  "prey": "#iceagesurvival:small_prey"
}
```

- O território é centrado em onde a criatura entrou no mundo pela primeira vez (salvo no NBT). Ela não vagueia nem persegue além do raio, e volta para dentro quando o alvo foge.
- `aggro_radius` é o raio em que percebe um jogador e a distância em que desiste do alvo.
- Abaixo de `flee_health_fraction` de vida, recua de quem a feriu em vez de lutar até morrer.
- Domesticada, perde o território.
- `herd_radius` > 0 faz a espécie andar em **manada**: o líder é o indivíduo selvagem de menor id por perto, e os outros voltam para junto dele quando passam do raio. Não há estado compartilhado nem registro de manadas; cada membro procura o líder a cada ~5–7 s.
- `migrates: true` habilita viagens longas periódicas no líder; os demais acompanham pelo comportamento de manada. Aplicado aos herbívoros migratórios; evitar combiná-lo com um território pequeno.
- `group_defense`: quando uma é ferida, as selvagens da mesma espécie por perto que estejam sem alvo atacam o agressor. Uma varredura por agressão.
- `prey`: tag de tipos de entidade que a espécie caça. A procura acontece em média a cada 30 s por predador, para ser barata e não zerar a fauna. Tags atuais: `small_prey` (coelho, galinha, ovelha, porco, raposa) e `large_prey` (vaca, ovelha, porco, cabra, cavalo, burro, lhama).

As velocidades dos goals são calculadas por espécie para que o passeio ocioso fique em ~1,8 bloco/s qualquer que seja a velocidade base.

**Bandos, território e espreita (D26, D27):**
- Cada criatura selvagem guarda o id do seu bando. Quem nasce junto (terreno, reposição, família) é do mesmo bando; o limite é o `group_max` da espécie; criatura antiga sem bando é adotada pelo bando com vaga mais próximo. Domesticada, sai do bando.
- Fome do bando: quando um come (ou mata o jogador), o bando inteiro fica saciado.
- Território: em volta de cada membro há um raio de defesa (`defendRadius`, 8–24 blocos). Outro bando da mesma espécie: guerra até um fugir ou morrer. Outra espécie mais fraca (porte × quantos lutam juntos): o bando ameaça e investe até ela sair. Mais forte: quem sai é esta. O predador faminto diante da própria presa fica fora: aí decide a caça.
- Espreita: todo carnívoro (`hunt_style` padrão `stalk`; `chase` desliga) espreita a presa escolhida. Contra criatura com cautela, ronda logo além de `alert_radius × 0,5 + 3` dela e de cada membro da manada; contra o jogador e o gado do vanilla, chega pelas costas. Dispara na presa desgarrada, na presa que chega perto, quando a manada o nota, quando é ferido ou depois de 20–40 s. A manada só se sabe caçada (`onHunted`) na disparada. Sozinho, o carnívoro acompanha até a manada que não encararia, esperando um desgarrado; se o tempo acaba ou é notado antes, desiste em vez de investir contra o grupo.
- Perseguição: depois da disparada, a presa que se afasta mais de **30 blocos** do predador escapou — ele desiste e fica frustrado (sem caçar por 30 s). Vale para o `HuntGoal` e para o `ChaseGoal` numa caçada; a espreita continua com o alcance do faro.
- Percepção: o faro de caça efetivo é `max(hunt_radius, maior alert_radius/calf_radius das presas + 8)` (`Perception`). O GameTest `everyCarnivoreSensesItsPreyFirst` exige que os JSON já cumpram a regra (o urso-terrível subiu de 40 para 44 por causa do Brontossauro).

**Hábitos (D34):** sub-bloco `behavior.habits`, tudo opcional:

```json
"habits": { "activity": "nocturnal", "camouflage": 0.5, "scavenges": true, "sentinel_radius": 32 }
```

- `activity` (`always` padrão, `nocturnal`, `diurnal`): fora do horário, a selvagem vai a um esconderijo a até 12 blocos e dorme (`RestGoal`, animação de sono); não caça, não ronda nem disputa rivais; deitada, não nota ameaça, aviso do bando, intruso nem jogador — só acorda ferida (D46; antes notava a ameaça a metade do raio). Hora do dia: 0–12 000 é dia.
- `camouflage` (0,1 a 1; 1 = sem): escondida — 2+ blocos de esconderijo nos pés, na cabeça e nos quatro vizinhos de cada; tag de blocos `iceagesurvival:undergrowth` mais camadas de neve ≥ 3 —, presas (`WaryGoal`), predadores (`HuntGoal`) e a manada que ela espreita (`StalkGoal`) a notam a essa fração do raio.
- `scavenges`: com fome e na hora dela, vai até carne crua no chão (tag de itens `iceagesurvival:carrion`, até 24 blocos) e come um pedaço (metade da fome, a do bando junto); não vai com um predador selvagem maior a menos de 16 blocos da carne (`ScavengeGoal`).
- `sentinel_radius`: domesticada, com o dono a até 64 blocos, avisa na barra de ação do predador selvagem (tag `predators`, sem dono, outra espécie) mais perto dentro do raio, com distância e rumo a partir do dono; toca a chamada e a animação `call`. Um aviso por predador a cada 60 s.
- `hunt_special: grab` (Ornitholestes): o bote não arranca; o primeiro golpe em 4 s prende a presa do porte dele para baixo (lentidão forte por 2 s).

### Montaria (implementado)

Bloco `mount` do JSON de espécie. **Ausente = espécie não montável**; é isso que deixa o lobo-terrível de fora.

```json
"mount": { "seat_height": 2.3, "min_affinity": 25, "speed_multiplier": 0.6, "jump_strength": 0.42, "jump_forward": 0.95 }
```

- `seat_height`: altura do assento em blocos a partir dos pés (0 = 85% da altura da colisão). Fica aqui, e não nos assets, porque é a colisão que manda — trocar o modelo por resource pack não muda onde o jogador senta.
- `min_affinity` (padrão 25): uma criatura recém-domesticada tem afinidade 50 × eficiência, então uma domesticação ruim precisa ser alimentada antes de aceitar alguém em cima.
- `speed_multiplier`: a velocidade montada é o atributo `MOVEMENT_SPEED` puro, numa escala diferente da dos goals de IA (que multiplicam o atributo pelo modificador do goal). O Smilodon corre a `speed` 0,4 na IA e monta a 0,24 — sem o multiplicador, montá-lo daria ~17 blocos/s.
- `jump_strength`: impulso vertical do pulo em blocos/tick. **Padrão 0: as espécies grandes não pulam** (T-Rex, mamute, Brontossauro, Espinossauro, Alossauro, urso-terrível, Elasmotério); sobem degraus pelo `step_height`. Pulam o Smilodon e o Utahraptor.
- `jump_forward`: impulso horizontal, para a frente, no mesmo pulo. O Smilodon dá um bote longo e baixo (0,42 para cima, 0,95 para a frente, ~6 blocos); o Utahraptor pula alto (0,6 / 0,3).
- `requires_saddle` (padrão verdadeiro): falso monta sem sela — o Elasmotério, a montaria de início de jogo. Essas espécies não aceitam sela.
- `flying` e `flight_speed` (blocos/tick de cruzeiro, padrão 0,8): voo montado, abaixo.

Como funciona:

- **Selar:** o dono clica com um `minecraft:saddle` na criatura domesticada e acordada. A sela fica na criatura (sincronizada para o cliente, salva no NBT) e volta ao mundo quando ela morre. Receita nossa, em [D19](section-04.md).
- **Montar:** clique com a mão livre. Alimentar tem preferência enquanto a criatura estiver com fome, então uma mão cheia de carne não impede montar depois.
- **Controlar:** `travelRidden` do vanilla ([D18](section-04.md)). A criatura aponta para onde o jogador olha, ré e passo lateral são reduzidos como no cavalo. **Pulo na hora, sem barra de carga** ([D23](section-04.md)): o cliente de quem monta lê o Espaço (`CommandInput` → `setRiderInput`) e aplica o impulso da espécie; o servidor aceita a posição do veículo.
- **Voar (Pteranodonte), no estilo do Cobblemon:** Espaço no chão decola. No ar, a montaria vai para onde a câmera aponta — olhar para cima sobe, para baixo mergulha; **frente** acelera até `flight_speed`, **trás** freia, sem tecla ela **plana** e perde embalo devagar; abaixo de 30% da velocidade perde sustentação e afunda; **Espaço segurado** bate as asas e sobe; **correr** (Ctrl) dá impulso de 1,5×; A/D desliza de lado. Pousa ao encostar no chão sem bater as asas e sem olhar para cima. Não leva dano de queda enquanto voa. A física é `core/mount/FlightModel` (lógica pura, JUnit); o estado de voo vai ao servidor por `flight_input` só para tirar a gravidade — sem isso o servidor desconectaria quem monta por "veículo flutuando".
- **Recusa:** sem sela (nas espécies que pedem sela), com afinidade abaixo do mínimo, com outra pessoa em cima, ou de quem não é o dono. A criatura diz o motivo.
- **Desce sozinho:** cair inconsciente e tirar a sela ejetam quem estiver montado. Montar uma criatura mandada ficar a solta da ordem.

- **Atacar montado:** o clique de ataque, com o condutor em cima, vira mordida da criatura (`MountAttackPayload`). O ataque do jogador é cancelado no cliente e o clique sempre vale: o servidor confere condutor, criatura acordada e recarga de 1 s, e aí a mordida sai com animação e som (`sounds.attack`) mesmo sem ninguém na mira. Acerta o alvo mirado se estiver a até 3 blocos da colisão, senão a criatura mais próxima na frente (o corpo esticado 3 blocos para a frente); nunca o dono nem criatura dele. O dano é o atributo de ataque da criatura. Espécies com `mount.break_hardness` > 0 quebram, a cada mordida, os blocos na frente do corpo (2 de profundidade, da altura dos pés ao topo) com dureza até esse valor — T-Rex 1,5 (terra, areia, pedra, folhas). Com `mount.break_blocks` (tag de bloco) só quebra o que está na tag: o mamute é o coletor de madeira (dureza 2, tag `iceagesurvival:mammoth_harvestable` = troncos e folhas) e os troncos dropam como se cortados à mão. Respeita `mobGriefing`, proteção do spawn, o evento de quebra de bloco como se fosse quem monta (mods de proteção) e nunca quebra bloco com inventário.

Ainda não existe: tirar a sela em jogo (só `/ias saddle` ou a morte da criatura), montaria de água e voo selvagem. O voo do Pteranodonte e o pulo novo passam nos testes automáticos, mas a sensação (velocidades, sustentação) precisa ser conferida em jogo.

### Workflow de assets

Modelos autorais são gerados por script em `tools/` (um `gen_<especie>.py` por espécie, sobre a biblioteca `modelgen.py`), que escreve geometria Bedrock, textura e animações em `src/main/resources/assets/iceagesurvival/{geo,textures,animations}/entity/`. Os arquivos abrem no Blockbench para conferência e ajuste.

O tamanho do modelo no jogo fica em `assets/iceagesurvival/creature_models/<especie>.json` (`{ "scale": 2.0 }`), junto do modelo e não nos dados da espécie: quem troca o modelo por resource pack também acerta a escala. A caixa de colisão é definida no registro do tipo de entidade e precisa acompanhar.

**Assets do Revival em runtime:** o renderer busca os recursos registrados pelo mod externo
*Fossils and Archaeology: Revival*, sem baixar, extrair ou copiar arquivos. O mod original precisa
estar instalado; o addon não o embute nem redistribui. Não voltar a usar scripts que extraiam arte
do Revival. `ASSET_LICENSES.md` registra a procedência e a separação dos assets próprios.

### Spawn natural (implementado)

Cada espécie tem dois `forge:add_spawns` (D41): `spawn_<especie>.json`, ligado à tag `spawns_<especie>` (= `#iceagesurvival:spawn_anywhere`: todo o overworld e os biomas do TFC), com o peso da espécie, e `spawn_<especie>_ideal.json`, ligado à tag `ideal_<especie>` (habitat real + planície nevada + os do TFC), com o dobro dele — no ideal o peso soma o triplo. O JSON de espécie repete o par em `spawn.biomes` e `spawn.favored`, que a reposição usa. Regra de posição: chão firme e na superfície (vale neve, gelo e sob copa de árvore; nunca em caverna).

As criaturas são da categoria `CREATURE` do vanilla: nascem quando o terreno é gerado e não desaparecem. Na prática isso significa que **num mundo já explorado a fauna que morre não volta** — foi o que apareceu em jogo na Etapa 7.

### Reposição de fauna (implementado)

Bloco `spawn` do JSON de espécie, lido pela reposição própria do mod. **Ausente = a espécie só nasce com o terreno** (é o caso da criatura de teste).

```json
"spawn": { "biomes": "#iceagesurvival:spawns_smilodon", "weight": 2, "group_min": 1, "group_max": 1, "max_nearby": 1, "min_distance": 600 }
```

- Uma reposição por jogador a cada `wildSpawnIntervalSeconds` (padrão 8 s): sorteia uma espécie entre as que podem nascer no bioma do local **e ainda têm vaga**, proporcionalmente ao `weight`, e procura posição num anel de `wildSpawnMinDistance` a `wildSpawnMaxDistance` (padrão 32 a 112 blocos). Em regiões abaixo de metade do teto efetivo, tenta até 5 grupos; caso contrário, um.
- A posição passa pelas checagens de spawn do vanilla (mapa de altura, regra de superfície, colisão e bioma da tag), e o nascimento passa pelos eventos do Forge, então outro mod pode barrar.
- `max_nearby` é o teto de indivíduos daquela espécie no `wildSpawnDensityRadius` em volta do jogador, e `wildSpawnMaxTotal` (padrão 72; teto efetivo 40%, 28 por jogador) o teto da **soma de todas as espécies**, do qual os herbívoros ocupam no máximo 3/4 (21): o resto fica para os carnívoros. Os voadores (Pteranodonte, Quetzal) ficam fora desse teto, num teto próprio de 8 por jogador (D46). **Sorteio justo (D46):** a espécie sem ninguém por perto entra com peso ×3 e, com a vaga da categoria pela metade, quem já tem um bando inteiro por perto espera — sem isso as espécies de peso alto enchiam o teto e a composição congelava. Carcaças não contam. Criaturas domesticadas não entram na contagem. O raio da contagem é sempre maior que o de spawn (pelo menos `wildSpawnMaxDistance` + 32): antes os dois eram 96, quem nascia na borda saía andando, deixava de contar e abria vaga — com o teto só por espécie, uma base parada juntava mais de 40 criaturas em minutos. Os pesos de `forge:add_spawns` também foram reduzidos em aproximadamente 30% (arredondados para inteiros; espécies de peso 1 ficam no mínimo permitido), para diminuir o fluxo de fauna em chunks novos sem desfazer bandos.

**Zonas de perigo:** `spawn.min_distance` é a distância horizontal mínima do spawn do mundo para a espécie nascer, conferida na regra de colocação — vale para a reposição e para a geração do terreno. E o nível selvagem máximo cresce com a distância: 30% do `maxWildLevel` no spawn, 100% a `fullDangerDistance` (padrão 300 blocos; era 3.000). O valor fica no `serverconfig` do mundo: mundos já criados mantêm o antigo até ele ser editado.

| Zona | Distância do spawn | Espécies | Raridade |
|---|---|---|---|
| Spawn | 0–300 | dodô, Elasmotério, lobo-terrível, mamute, Brontossauro, Estegossauro, Galimimo, Pteranodonte, Ornitholestes e Smilodon | o Smilodon é o predador da zona inicial: solitário, às vezes em casal (`family.pair_chance` 0,35), também na planície nevada e nos picos de gelo onde antes nasciam os Velociraptores |
| Apex garantido | 220–297 | um T-Rex | sempre há um, com território próprio de 32 blocos a 220–265 do spawn; volta três dias depois de morrer ou ser domesticado (`starterApexEnabled`) |
| Além do spawn | 300+ | Velociraptor, urso-terrível, T-Rex natural e Anquilossauro | Velociraptor nunca dentro dos 300 blocos; o Pteranodonte nasce em qualquer distância |
| Médio | 350+ | Utahraptor | |
| Longe | 400+ | Alossauro e Espinossauro | raros (peso 1) |
- **Fauna fora da zona:** cada criatura selvagem é conferida uma vez contra a distância mínima da espécie. A geração do terreno roda antes de o spawn do mundo estar decidido, e mundos de versões anteriores têm Velociraptores no spawn; a criatura selvagem, sem dono e não presa ao mundo (nome, comando, teste) que estiver dentro do raio proibido some no primeiro tick. Postas por comando, ovo ou reprodução já nascem conferidas.
- **Apex garantido** (`StarterApexKeeper`, dados do Overworld): nasce quando um jogador já tem aquele trecho do anel carregado, nunca a menos de 48 blocos de alguém; se o jogador chega perto da última posição conhecida e ele não está lá, outro é posto.
- Não trocamos a categoria para `MONSTER` para conseguir spawn contínuo: isso faria a fauna desaparecer sozinha ([D20](section-04.md)).
- Tudo desligável em `wildSpawnEnabled`, para quem quiser a fauna só na geração do terreno.

**Um grupo por região:** `spawn.spacing` (blocos, padrão 0) é a distância mínima entre dois grupos selvagens da espécie. Brontossauro e T-Rex usam 300: uma manada de bronto e um T-Rex a cada 300 blocos. Como a geração do terreno e a reposição não enxergam chunk descarregado, cada grupo deixa uma **marca** (`GroupSpacing`, dados da dimensão; regra pura em `core/spawn/SpeciesSpacing`):

- A regra de colocação aceita um grupo novo só a mais de `spacing` de toda marca e marca o lugar; quem cai a até 64 blocos de uma marca é do mesmo grupo e passa (os membros da manada).
- A cada 10 s, marca em chunk carregado vai para onde o grupo está; sem ninguém do grupo ali por 1 min, é apagada. Cada indivíduo selvagem renova a sua a cada 30 s (e cria uma, se faltar), então uma manada migrando leva a marca junto — e pode passar perto de outra sem sumir.
- Mundos de antes da regra: a criatura da geração do terreno conferida pela primeira vez perto demais de outro grupo some (como a fauna fora da zona). Domesticada, com dono ou presa ao mundo não conta nem some — exceto o apex garantido, que conta e afasta os T-Rex naturais.

O `forge:add_spawns` continua: ele povoa chunk novo, a reposição cuida do que já existe. Os dois usam a mesma tag de biomas.

Horário de atividade: por espécie, em `behavior.habits.activity` (D34); por enquanto só o Ornitholestes é noturno.

