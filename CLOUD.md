# CLOUD.md — Ice Age Survival

Game Design + Technical Design Document vivo do projeto.
Atualizar sempre que uma decisão estrutural mudar.

- **Última revisão:** 2026-09-30
- **Etapa atual:** ver [23. Roadmap](#23-roadmap)
- **Brief original:** o documento de 43 seções que originou o projeto. Este arquivo é a fonte da verdade a partir daqui; onde divergir do brief, a divergência está justificada em [4. Decisões](#4-decisões).

---

## 1. Visão do jogo

> ARK simplificado dentro do Minecraft, durante uma Era do Gelo dominada por criaturas pré-históricas.

O jogador começa indefeso num mundo congelado, é caçado, aprende a sobreviver, derruba criaturas com torpor, domestica, usa cada criatura para alcançar a próxima, cria linhagens por reprodução e genética e, no fim, leva uma equipe preparada a uma arena para enfrentar um superpredador físico.

Os mods externos são infraestrutura. O nosso mod é o jogo.

## 2. Objetivos

- Loop principal: sobreviver → explorar → torpor → domesticar → usar a criatura para a próxima domesticação → reproduzir → linhagem → caverna → boss.
- Funcionar em single-player, servidor dedicado e multiplayer, com lógica autoritativa no servidor.
- Adicionar uma espécie nova sem tocar no núcleo (espécies definidas por dados).
- Poucos sistemas, bem feitos. Teste de corte para qualquer feature: *"isso melhora o loop principal?"*

Fora de escopo: máquinas, árvores tecnológicas, dezenas de armaduras, arsenal de armas, magia, qualquer coisa que exista só porque "ARK tem".

## 3. Referências

- ARK: Survival Evolved — estrutura de torpor, domesticação, breeding, boss em arena.
- Minecraft vanilla — linguagem visual, simplicidade de itens e receitas.
- Eye of Ender — inspiração (não cópia) para o rastreador da caverna.

## 4. Decisões

| # | Decisão | Motivo | Estado |
|---|---|---|---|
| D1 | Minecraft **1.21.1** | É a versão em que todos os candidatos de infraestrutura têm build estável para NeoForge (GeckoLib, Lithostitched, Tectonic, Cold Sweat, SmartBrainLib) e a versão LTS de fato do ecossistema NeoForge. | Fechada |
| D2 | Loader **NeoForge 21.1.252** | Registries de datapack, data maps, payloads tipados e GameTest nativos; ver [8](#8-versões). Fabric só traria vantagem para Frostiful, que foi rejeitado como dependência. | Fechada |
| D3 | **GeckoLib** como única dependência obrigatória | MIT, release estável para 1.21.1, padrão do ecossistema para criaturas animadas. | Fechada |
| D4 | **Não** depender de nenhum mod de fauna | Todos os mods de fauna relevantes são All Rights Reserved ou licença custom; nenhum asset pode ser incorporado. Além disso o brief exige que progressão/domesticação sejam nossas. | Fechada |
| D5 | Temperatura: **sistema interno leve**, atrás de uma interface | Frostiful no NeoForge é alpha e exige Forgified Fabric API; Cold Sweat é estável mas muito mais complexo do que o brief pede ("secundária, sem burocracia"). Ver [15](#15-temperatura). | Fechada na Etapa 6 — o sistema interno cabe em ~200 linhas e não precisou de HUD próprio; a costura `ColdSource` continua de pé |
| D6 | Mundo glacial por **world preset próprio em datapack**, sem mod de worldgen obrigatório | Um preset com a fonte de biomas restrita a biomas frios resolve "mundo predominantemente congelado" sem dependência. Ver [18](#18-worldgen). | Provisória — reavaliar na Etapa 9 |
| D7 | Espécies num **registry de datapack** sincronizado | Permite adicionar espécie por JSON, com validação por Codec e sync automático para o cliente. | Fechada |
| D8 | Dados da criatura no **NBT da própria entidade**, serializados por Codec | As entidades são nossas; não precisamos de attachments para anexar dados a entidades alheias. | Fechada |
| D9 | IA com **Goals vanilla** | Suficiente para território e manada; SmartBrainLib fica como opção se os Goals virarem gargalo. | Provisória |
| D10 | Lógica pura (stats, genética, torpor) **sem dependência de classes do Minecraft** | Permite testes JUnit rápidos, sem subir o jogo. | Fechada |
| D11 | Repositório GitHub **privado** | Pode ser aberto depois; publicar é irreversível. | Fechada até o Felipe decidir o contrário |
| D12 | Indivíduo = **pontos por atributo**; nível = 1 + total de pontos | Dois animais do mesmo nível ficam diferentes ("esse tem ataque melhor"), e é exatamente o que a reprodução vai herdar atributo a atributo. Evita "mob com HP multiplicado". Ver [11](#11-criaturas). | Fechada |
| D13 | Dono e estado domesticado via **`TamableAnimal`** vanilla | Persistência, sync e regras de aliado já prontas e compatíveis com outros mods. | Fechada |
| D14 | Sem pacote `network/` até existir o primeiro payload | O registry de espécies já sincroniza sozinho. O pacote nasceu na Etapa 4, com os comandos. | Cumprida |
| D15 | Ordem de ataque vale para **todas** as criaturas do jogador ao alcance | Exigir escolher uma criatura antes de apontar o alvo pediria um estado de "selecionada" escondido. Não é o sistema de grupos do brief (que continua fora). | Fechada |
| D16 | O frio **é** o `ticksFrozen` do vanilla, não um valor paralelo sincronizado | Sincronização, persistência, vinheta de gelo e lentidão já existem e são de graça; um valor próprio pediria payload, HUD e NBT para o mesmo resultado. Custo: parar em 139/140 e assumir o dano (ver [15](#15-temperatura)). | Fechada |
| D17 | Estado de frio do jogador em **attachment do NeoForge** | D8 dispensa attachments para *nossas* entidades; o jogador é de terceiros, e é exatamente o caso que os attachments existem para resolver. | Fechada |
| D18 | Montaria pelo **modelo de veículo do vanilla** (`travelRidden`, `PlayerRideableJumping`) | É o caminho do cavalo: o cliente de quem monta simula o movimento e manda a posição do veículo, o servidor confere que o remetente é o controlador. Escrever controle próprio seria reinventar a predição e a reconciliação. Ver [11](#11-criaturas). | Fechada na Etapa 7 |
| D19 | A sela é o **`minecraft:saddle` do vanilla**, com receita nossa | Um item por espécie (como no ARK) seria uma dúzia de itens e texturas para a mesma função; o item do vanilla já é reconhecível e o que gateia a montaria é a espécie ter bloco `mount`. A receita existe porque no vanilla a sela não é craftável, e depender de baú de estrutura num mundo glacial travaria a Fase 4. | Fechada na Etapa 7 |
| D20 | **Reposição de fauna própria**, em vez de mudar a categoria das criaturas | `CREATURE` do vanilla só nasce na geração do terreno; trocar para `MONSTER` faria a fauna aparecer e *desaparecer* sozinha, contra o design. A reposição repõe no que já existe, com teto de densidade por espécie. Ver [11](#11-criaturas). | Fechada na Etapa 7 |
| D21 | Arte do F&A Revival **no repositório** (e portanto no jar), como placeholder | A pedido do Felipe: os modelos ficam abertos para edição no Blockbench sem depender de um resource pack local, e o jogo funciona logo depois de clonar. Custo aceito: a arte é *All Rights Reserved*, então **o jar deixa de ser distribuível** até ser substituída; o repositório é privado. | Provisória, por escolha — reverter ao substituir a arte |

## 5. Mods avaliados

Fonte: API do Modrinth, consultada em 2026-09-30, filtrando pela combinação exata `1.21.1` + `neoforge`. Downloads e datas são os daquele dia.

**Limites desta pesquisa:** o CurseForge não foi consultado (a API exige chave) e nenhum código-fonte dos mods foi lido — a avaliação é de metadados, licença e descrição. "Primal Era" não existe no Modrinth; pode existir só no CurseForge.

### Temperatura / frio

| Mod | Licença | 1.21.1 NeoForge | Observação |
|---|---|---|---|
| Frostiful | LGPL-3.0 | 2.3.3 — **alpha** (2026-06) | A própria página avisa: port "very experimental", feito de forma "Fabric-like" sobre Forgified Fabric API. Requer Thermoo, Cloth Config e Forgified Fabric API. |
| Thermoo | LGPL-3.0 | 4.8.1 — **alpha** (2026-03) | Biblioteca do Frostiful; mesma dependência de Forgified Fabric API. |
| Cold Sweat | GPL-3.0-or-later | 2.4.3.1 — release (2026-09) | Nativo NeoForge, muito ativo (44 builds para 1.21.1), configurável. Sistema completo de temperatura, mais complexo que o nosso alvo. |
| Tough As Nails | All Rights Reserved | 10.1.0.13 — beta (2024-10) | Parado em 1.21.1 desde 2024; inclui sede, que não queremos. Requer GlitchCore. |
| Legendary Survival Overhaul | All Rights Reserved | 2.4.7.2 — release (2026-08) | Temperatura + sede + dano localizado; escopo grande demais. |
| Homeostatic | MIT | 2.12.0.0 — release (2026-06) | Temperatura + sede. |
| Serene Seasons | All Rights Reserved | sim | Estações; irrelevante para mundo permanentemente frio. |

### Fauna pré-histórica

| Mod | Licença | 1.21.1 NeoForge | Observação |
|---|---|---|---|
| Jurassic Reborn | All Rights Reserved | 1.4.2 — release (2026-09) | Dinossauros; requer Citadel + GeckoLib. Assets não reutilizáveis. |
| Jurassic Saga | All Rights Reserved | 0.2.3 — release | Idem. |
| Jurassic Revived | CC-BY-ND-4.0 | sim | "ND" proíbe derivados. |
| Blast from the Past | All Rights Reserved | 1.0.7 — release (2026-09) | Bioma "Frostbite Forest" com fauna fantástica; requer GeckoLib. |
| Not Enough Dinosaurs (`archocraft`) | All Rights Reserved | 1.2.0 — beta (2025-02) | Sem código-fonte publicado. |
| Unusual Prehistory 2 | Licença custom | 2.0.0-beta3 (2026-09) | Beta. |
| ShineaL's Prehistoric Expansion | All Rights Reserved | 1.5.2 | Mistura mitologia. |
| Fossils and Archaeology: Legacy | **MIT** | 1.4.9 — release (2025-02) | Única fauna com licença permissiva e código aberto; parado desde 2025-02. |
| Prehistoric Kingdom | MIT | 1.3.0 (2026-09-30) | 86 downloads, sem código-fonte publicado. |
| GlacialAge | All Rights Reserved | **não** — só Forge 1.20.1 | 819 downloads, sem código-fonte. |
| Fossils and Archeology: Revival | Licença custom | **não** — só 1.20.1 | |
| Alex's Mobs | GPL-3.0 | **não** — só 1.20.1 | |
| Primal Era | — | não encontrado no Modrinth | Não verificado. |

Nenhum mod no Modrinth para 1.21.1/NeoForge implementa torpor, tranquilizante ou domesticação no estilo ARK (buscas por "torpor" e "tranquilizer" retornam vazio). O núcleo do projeto não tem concorrente para reutilizar.

### Worldgen

| Mod | Licença | 1.21.1 NeoForge | Observação |
|---|---|---|---|
| Primal Winter | **MIT** | 6.0.2 — release (2024-10) | Converte o mundo inteiro em deserto gelado com nevasca permanente. Exatamente a fantasia, mas sem update desde 2024-10. |
| Ice Age | GPL-3.0 | 1.0.0 (2024-11) | "Cobre o mundo inteiro de neve"; 1,9 mil downloads. |
| Tectonic | MIT | 3.0.28 — release (2026-09) | Terreno (montanhas, vales) — não biomas. Requer Lithostitched. |
| Lithostitched | MIT | 1.8.0 — release (2026-09) | Biblioteca de worldgen por datapack. |
| TerraBlender | LGPL-3.0 | 4.1.0.8 — beta (2025-01) | Injeção de biomas; desnecessário se o preset for nosso. |
| Terralith | Licença Stardust Labs | sim | Biomas; licença restritiva. |
| Natural Temperature | Licença custom | 1.1.10 | Reorganiza biomas por temperatura. |

### Animação / IA / infraestrutura

| Mod | Licença | 1.21.1 NeoForge | Observação |
|---|---|---|---|
| GeckoLib | **MIT** | 4.9.3 — release (2026-09) | 71 milhões de downloads, 27 builds para 1.21.1. |
| SmartBrainLib | MPL-2.0 | 1.16.11 — release (2025-10) | IA baseada em Brain. |
| Citadel | LGPL-3.0 | 2.7.1 — release | Biblioteca dos mods do Alex; não precisamos. |

Não existe biblioteca de montaria ou de breeding/genética reutilizável para 1.21.1/NeoForge; o que aparece são mods pequenos de conteúdo (Animal Husbandry, ARR). Ambos os sistemas serão nossos.

## 6. Mods escolhidos

| Mod | Papel | Tipo |
|---|---|---|
| GeckoLib 4.9.3 | Modelos e animações das criaturas | Dependência obrigatória |

Compatibilidade opcional a avaliar mais tarde, sem dependência em código: Tectonic (terreno), Cold Sweat (se D5 for revertida).

## 7. Mods rejeitados

- **Frostiful / Thermoo** — alpha no NeoForge e dependente de Forgified Fabric API. Risco alto demais para uma mecânica de sobrevivência, mesmo secundária.
- **Todos os mods de fauna como dependência** — licenças fechadas e, principalmente, fariam do projeto "Minecraft com vários mods de dinossauro" (seção 42 do brief).
- **Tough As Nails, Legendary Survival Overhaul, Homeostatic** — trazem sede e outros sistemas fora do escopo.
- **TerraBlender, Terralith, Citadel** — desnecessários para a abordagem escolhida.
- **Primal Winter como dependência** — sem manutenção desde 2024-10. Fica como *referência de design* (MIT, código aberto) para a Etapa 9.

## 8. Versões

| Componente | Versão | Fonte |
|---|---|---|
| Minecraft | 1.21.1 | — |
| NeoForge | 21.1.252 | maven.neoforged.net (última 21.1.x em 2026-09-30) |
| ModDevGradle | 2.0.148 | template oficial `MDK-1.21.1-ModDevGradle` |
| Parchment | 2024.11.17 (para 1.21.1) | maven.parchmentmc.org |
| Gradle | 9.2.1 (wrapper) | template oficial |
| Java | 21 | exigido pelo Minecraft 1.21.1 |
| GeckoLib | 4.9.3 (`geckolib-neoforge-1.21.1`) | Cloudsmith do GeckoLib |

Por que não uma versão mais nova do Minecraft: GeckoLib, Tectonic e Lithostitched existem em versões mais recentes, mas 1.21.1 é onde o ecossistema de mods de conteúdo para NeoForge está concentrado, e onde Cold Sweat (nosso plano B de temperatura) tem release estável.

## 9. Licenças

- **Nosso código:** licença ainda não definida (ver [26](#26-ainda-não-decidido)). Enquanto isso, `All Rights Reserved` no metadata e repositório privado.
- **GeckoLib (MIT):** usado como dependência, não redistribuído dentro do nosso jar.
- **Template MDK do NeoForge:** base do build; licença do template em `TEMPLATE_LICENSE.txt`.
- **Assets:** a regra de chegada é que todo modelo, textura, animação e som seja original ou de licença compatível — e é a regra para o mod ser distribuível. Hoje há uma exceção consciente e temporária, abaixo.

**Assets de criatura.** A arte de mamute, smilodon e tyrannosaurus é do *Fossils and
Archeology: Revival* e é **All Rights Reserved**. Desde 2026-09-30 ela fica no repositório
(privado) e, por consequência, dentro do jar — ver [D21](#4-decisões) e
`src/main/resources/assets/iceagesurvival/ASSET_LICENSES.md`. **O jar não pode ser
distribuído** até essas espécies terem arte nossa ou licenciada.

## 10. Arquitetura

```
dev.madebyfelipe.iceagesurvival
├── IceAgeSurvival          entrypoint, registro nos event buses
├── core/                   lógica pura, sem classes do Minecraft (testável por JUnit)
│   ├── stats/              atributos, escala por nível
│   ├── genetics/           genoma, herança 50/50, mutações
│   ├── taming/             torpor, eficiência, afinidade
│   └── temperature/        curva de frio, proteção, ritmo de congelamento
├── species/                definição de espécie (Codec) + registry de datapack
├── entity/                 classe base de criatura, goals, dados sincronizados
├── temperature/            leitura do ambiente e congelamento do jogador
├── item/                   flechas tranquilizantes, rifle, implante, rastreador
├── network/                payloads (comandos, UI)
├── registry/               DeferredRegisters, tags usadas em código, attachments
├── config/                 configuração comum e de servidor
└── client/                 renderers, HUD, telas (só client)
```

Princípios:

- **Servidor decide tudo.** Cliente envia intenção (payload), servidor valida dono, distância e estado.
- **`core/` não importa `net.minecraft`.** A entidade é uma casca fina que alimenta e consome a lógica pura.
- **Dados antes de código.** Número de balanceamento vive no JSON da espécie ou na config, nunca espalhado em classes.
- **Sem mixins** enquanto um evento ou API do NeoForge resolver.

### Espécie por dados

Registry de datapack `iceagesurvival:species`, arquivos em `data/<namespace>/iceagesurvival/species/<nome>.json`, validados por Codec no carregamento e sincronizados para o cliente. O formato é definido na Etapa 2 a partir do exemplo do brief (stats base, taming, comportamento, montaria).

Limite conhecido: o *tipo de entidade* (modelo, hitbox, registro) continua sendo código — uma espécie nova precisa de um `EntityType` registrado e de assets. O JSON define tudo o que é balanceamento e comportamento.

### Persistência

- Dados da criatura (nível, genes, torpor, dono, afinidade, comando atual): NBT da entidade via Codec.
- Campos que o cliente precisa ver (torpor, nível, inconsciente, dono): `SynchedEntityData`.
- Implante: Data Component no item, carregando o snapshot serializado da criatura.

### Networking

Payloads tipados (`CustomPacketPayload` + `StreamCodec`) registrados em `RegisterPayloadHandlersEvent`. Um payload por intenção do jogador; nada de payload genérico.

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

**Limites do vanilla:** `max_health` satura em 1024 e `armor` em 30. Valores acima são cortados pelo jogo. Espécies grandes e o boss precisam caber nisso ou usar outro mecanismo (ver [25](#25-riscos)).

### Espécies

Todas as espécies terrestres usam a mesma classe (`LandCreature`); o que as diferencia é o JSON e os assets.

| Espécie | Papel | Tamanho (colisão) | Comportamento | Nasce em |
|---|---|---|---|---|
| **Lobo-terrível** (`dire_wolf`) | Primeira domesticação; predador de matilha | 0,8 × 1,2 | Agressivo, matilha de 2–4, defesa em grupo, caça presas pequenas, recua com 20% de vida | taiga, taiga nevada, taigas antigas, grove, planície nevada |
| **Smilodon** (`smilodon`) | Predador territorial solitário, rápido | 1,3 × 2,3 | Agressivo, território de 32 blocos, caça presas grandes, recua com 25% de vida | taiga, taiga nevada, taigas antigas, grove, encostas nevadas |
| **Mamute-lanoso** (`mammoth`) | Herbívoro de manada, tanque | 2,0 × 3,1 | Pacífico até ser provocado; manada de 2–4 que se defende junta | planície nevada, ice spikes, taiga nevada |
| **Tyrannosaurus rex** (`tyrannosaurus`) | Primeiro dinossauro; predador de topo solitário, raro | 1,8 × 3,6 | Agressivo, raio de percepção 24, território de 48 blocos, caça presas grandes, recua com 10% de vida | taiga, taiga nevada, taigas antigas, planície nevada (peso 1) |
| Criatura de teste (`test_creature`) | Só para testes automáticos; usa o modelo do porco | 0,9 × 0,9 | Passiva | não nasce |

Montáveis: Smilodon, mamute e Tyrannosaurus. O lobo-terrível é pequeno demais e fica de fora — o que o exclui é não ter bloco `mount` no JSON, não uma regra em código.

**Adicionar uma espécie terrestre:**

1. `tools/gen_<especie>.py` — ossos, cores e detalhes; gera geometria, textura e as quatro animações (`idle`, `walk`, `attack`, `unconscious`).
2. Uma linha em `ModEntities` (id e caixa de colisão) e um ovo gerador em `ModItems`.
3. `data/iceagesurvival/iceagesurvival/species/<especie>.json` — atributos, corpo, domesticação, comportamento.
4. Opcional: `neoforge/biome_modifier/spawn_<especie>.json` + tag de biomas, para nascer no mundo.
5. Traduções.

Nenhuma classe Java nova, nenhuma mudança no núcleo. Espécies com mecânica própria (voar, nadar, o boss) vão precisar de classe.

Pendentes do brief — Era do Gelo: rinoceronte-lanoso, megaloceros, megatherium, urso-das-cavernas, bisão, auroque, mastodonte. Dinossauros: ~~tyrannosaurus~~ (feito), triceratops, velociraptor, ankylosaurus, spinosaurus, giganotosaurus (boss).

### Comportamento (implementado)

Bloco `behavior` do JSON de espécie (ausente = passiva, sem território):

```json
"behavior": {
  "aggressive": true,
  "aggro_radius": 14,
  "territory_radius": 32,
  "flee_health_fraction": 0.25,
  "herd_radius": 10,
  "group_defense": true,
  "prey": "#iceagesurvival:small_prey"
}
```

- O território é centrado em onde a criatura entrou no mundo pela primeira vez (salvo no NBT). Ela não vagueia nem persegue além do raio, e volta para dentro quando o alvo foge.
- `aggro_radius` é o raio em que percebe um jogador e a distância em que desiste do alvo.
- Abaixo de `flee_health_fraction` de vida, recua de quem a feriu em vez de lutar até morrer.
- Domesticada, perde o território.
- `herd_radius` > 0 faz a espécie andar em **manada**: o líder é o indivíduo selvagem de menor id por perto, e os outros voltam para junto dele quando passam do raio. Não há estado compartilhado nem registro de manadas; cada membro procura o líder a cada ~5–7 s.
- `group_defense`: quando uma é ferida, as selvagens da mesma espécie por perto que estejam sem alvo atacam o agressor. Uma varredura por agressão.
- `prey`: tag de tipos de entidade que a espécie caça. A procura acontece em média a cada 30 s por predador, para ser barata e não zerar a fauna. Tags atuais: `small_prey` (coelho, galinha, ovelha, porco, raposa) e `large_prey` (vaca, ovelha, porco, cabra, cavalo, burro, lhama).

As velocidades dos goals são calculadas por espécie para que o passeio ocioso fique em ~1,8 bloco/s qualquer que seja a velocidade base.

### Montaria (implementado)

Bloco `mount` do JSON de espécie. **Ausente = espécie não montável**; é isso que deixa o lobo-terrível de fora.

```json
"mount": { "seat_height": 2.3, "min_affinity": 25, "speed_multiplier": 0.6, "jump_strength": 0.62 }
```

- `seat_height`: altura do assento em blocos a partir dos pés (0 = 85% da altura da colisão). Fica aqui, e não nos assets, porque é a colisão que manda — trocar o modelo por resource pack não muda onde o jogador senta.
- `min_affinity` (padrão 25): uma criatura recém-domesticada tem afinidade 50 × eficiência, então uma domesticação ruim precisa ser alimentada antes de aceitar alguém em cima.
- `speed_multiplier`: a velocidade montada é o atributo `MOVEMENT_SPEED` puro, numa escala diferente da dos goals de IA (que multiplicam o atributo pelo modificador do goal). O Smilodon corre a `speed` 0,4 na IA e monta a 0,24 — sem o multiplicador, montá-lo daria ~17 blocos/s.
- `jump_strength` em blocos/tick; 0 tira o pulo e a barra de carga.

Como funciona:

- **Selar:** o dono clica com um `minecraft:saddle` na criatura domesticada e acordada. A sela fica na criatura (sincronizada para o cliente, salva no NBT) e volta ao mundo quando ela morre. Receita nossa, em [D19](#4-decisões).
- **Montar:** clique com a mão livre. Alimentar tem preferência enquanto a criatura estiver com fome, então uma mão cheia de carne não impede montar depois.
- **Controlar:** `travelRidden` do vanilla ([D18](#4-decisões)). A criatura aponta para onde o jogador olha, ré e passo lateral são reduzidos como no cavalo, e o pulo usa a barra de carga do cavalo (`PlayerRideableJumping`): o cliente de quem monta carrega e aplica o impulso, o servidor toca o som e aceita a posição do veículo.
- **Recusa:** sem sela, com afinidade abaixo do mínimo, com outra pessoa em cima, ou de quem não é o dono. A criatura diz o motivo.
- **Desce sozinho:** cair inconsciente e tirar a sela ejetam quem estiver montado. Montar uma criatura mandada ficar a solta da ordem.

- **Atacar montado:** o clique de ataque, com o condutor em cima, vira mordida da criatura (`MountAttackPayload`): o ataque do jogador é cancelado no cliente e o servidor confere condutor, criatura acordada, alvo a até 3 blocos da colisão, recarga de 1 s e que o alvo não é o dono nem criatura dele. O dano é o atributo de ataque da criatura.

Ainda não existe: tirar a sela em jogo (só `/ias saddle` ou a morte da criatura), carga, e montaria de água ou ar.

### Workflow de assets

Modelos são gerados por script em `tools/` (um `gen_<especie>.py` por espécie, sobre a biblioteca `modelgen.py`), que escreve geometria Bedrock, textura e animações direto em `src/main/resources/assets/iceagesurvival/{geo,textures,animations}/entity/`. Os arquivos abrem no Blockbench para conferência e ajuste.

O tamanho do modelo no jogo fica em `assets/iceagesurvival/creature_models/<especie>.json` (`{ "scale": 2.0 }`), junto do modelo e não nos dados da espécie: quem troca o modelo por resource pack também acerta a escala. A caixa de colisão é definida no registro do tipo de entidade e precisa acompanhar.

**Placeholders do F&A Revival (no repositório desde 2026-09-30):** por decisão do Felipe, a arte
de mamute, smilodon e tyrannosaurus vem do mod *Fossils and Archeology: Revival* enquanto não há
modelos definitivos. `tools/install-revival-placeholders.py` baixa o Revival para um cache fora do
projeto e escreve geometria, textura, as quatro animações e a escala direto em
`src/main/resources/assets/`. O script não contém arte.

Antes essa arte ficava num resource pack dentro da instância do Prism, fora do git; [D21](#4-decisões)
conta por que mudou. Duas consequências que valem ser lembradas:

- A arte é **All Rights Reserved** e agora vai dentro do jar: **o jar não pode ser distribuído** até
  ela ser substituída. Ver `ASSET_LICENSES.md` ao lado dos assets, com as três saídas possíveis.
- As espécies com arte de fora ficam listadas em `tools/hand_authored.txt` e o `modelgen` **recusa**
  regerá-las, para um `gen_<especie>.py` distraído não apagar o modelo em uso. Para voltar ao
  placeholder de blocagem, que é nosso: `IAS_FORCE_GEN=1 python3 tools/gen_<especie>.py`.

Rodar o instalador de novo sobrescreve edições manuais — ao começar a editar um modelo à mão, tirar
a espécie de `SPECIES` no script.

### Spawn natural (implementado)

Cada espécie tem um `neoforge/biome_modifier/spawn_<especie>.json` do tipo `neoforge:add_spawns`, ligado a uma tag de biomas `spawns_<especie>`, com peso e tamanho de grupo. Regra de posição: chão firme e na superfície (vale neve, gelo e sob copa de árvore; nunca em caverna).

As criaturas são da categoria `CREATURE` do vanilla: nascem quando o terreno é gerado e não desaparecem. Na prática isso significa que **num mundo já explorado a fauna que morre não volta** — foi o que apareceu em jogo na Etapa 7.

### Reposição de fauna (implementado)

Bloco `spawn` do JSON de espécie, lido pela reposição própria do mod. **Ausente = a espécie só nasce com o terreno** (é o caso da criatura de teste).

```json
"spawn": { "biomes": "#iceagesurvival:spawns_smilodon", "weight": 2, "group_min": 1, "group_max": 1, "max_nearby": 2 }
```

- Uma tentativa por jogador a cada `wildSpawnIntervalSeconds` (padrão 45 s): sorteia uma espécie entre as que podem nascer no bioma do jogador **e ainda têm vaga**, proporcionalmente ao `weight`, e procura posição num anel de `wildSpawnMinDistance` a `wildSpawnMaxDistance` (padrão 40 a 96 blocos) — longe da vista, dentro da distância de simulação.
- A posição passa pelas mesmas checagens do spawn natural do vanilla (mapa de altura, tipo de colocação, colisão, regra de superfície da espécie) mais o bioma da tag, e o nascimento passa pelos eventos do NeoForge, então outro mod pode barrar.
- `max_nearby` é o teto de indivíduos daquela espécie no `wildSpawnDensityRadius` em volta do jogador. É ele que impede a reposição de encher o mundo: a densidade converge para `max_nearby` por raio de densidade onde o jogador andou, e não cresce além disso. Criaturas domesticadas não entram na contagem.
- Não trocamos a categoria para `MONSTER` para conseguir spawn contínuo: isso faria a fauna desaparecer sozinha ([D20](#4-decisões)).
- Tudo desligável em `wildSpawnEnabled`, para quem quiser a fauna só na geração do terreno.

O `neoforge:add_spawns` continua: ele povoa chunk novo, a reposição cuida do que já existe. Os dois usam a mesma tag de biomas.

Ainda não existe: nível variando por região, horário de atividade.

## 12. Progressão

| Fase | Conteúdo | Ferramenta de torpor |
|---|---|---|
| 1 Sobrevivência | madeira, pedra, comida, abrigo, fogo, frio, primeiros predadores | — |
| 2 Primeira domesticação | criatura pequena, primeiro companheiro | Arco + flecha tranquilizante |
| 3 Exploração | criaturas maiores, regiões perigosas | Besta |
| 4 Domínio | montarias, carga | Besta |
| 5 Genética | reprodução, incubadora, herança, mutações | — |
| 6 Endgame | criaturas de alto nível, rastreador da caverna | Rifle tranquilizante |
| 7 Boss | caverna, arena, superpredador | — |

## 13. Domesticação

Implementado na criatura de teste.

1. Flechas tranquilizantes acumulam torpor; ao atingir o máximo a criatura cai inconsciente.
2. Inconsciente, clique direito abre o **inventário da criatura** (9 espaços). Ela come sozinha dali: uma unidade por vez, sempre o alimento de maior `quality` disponível. Cada unidade soma o `value` do alimento ao progresso.
3. Entre uma refeição e outra há uma espera (`feed_interval_seconds`) — é o tempo em que o jogador precisa proteger a criatura.
4. O torpor continua caindo. Se zerar antes do progresso completar, ela acorda e **o progresso é perdido**. Mais flechas mantêm o torpor, mas o dano delas reduz a eficiência.
5. Progresso completo → domesticada. **O dono é quem a derrubou** (o autor do torpor que a fez cair); só essa pessoa abre o inventário enquanto ela está caída. Se ninguém a derrubou (dispensador, por exemplo), vale quem abrir o inventário primeiro. Acordar libera a criatura de novo.

**Alimento exigido** = `required_food × (1 + required_food_per_level × (nível − 1))`.

**Eficiência** (0 a 1) = média da `quality` dos alimentos, ponderada pelo `value` × (1 − dano sofrido inconsciente ÷ vida máxima).

**Recompensa:** `nível × tamingBonusLevelFraction × eficiência` pontos extras de atributo (padrão: até +50% do nível), distribuídos ao acaso.

**Afinidade** (0 a 100): começa em `50 × eficiência`. Ver [13.1](#131-comandos-e-afinidade) para o que ela faz e como sobe.

Bloco `taming` do JSON de espécie (ausente = espécie não acumula torpor):

```json
"taming": {
  "torpor_multiplier": 1.0,
  "torpor_decay_per_second": 1.0,
  "required_food": 40,
  "required_food_per_level": 0.02,
  "feed_interval_seconds": 5,
  "foods": [
    { "items": "minecraft:carrot", "value": 20 },
    { "items": ["minecraft:potato", "minecraft:beetroot"], "value": 10, "quality": 0.5 },
    { "items": "#minecraft:leaves", "value": 4, "quality": 0.2 }
  ]
}
```

`items` aceita um item, uma lista ou uma tag. O primeiro alimento que casar vale.

Feedback ao jogador: texto sob a mira ao olhar para a criatura — nome e nível, torpor %, domesticação %. Sem menu.

O inventário continua existindo depois de domesticada (o dono abre com agachar + clique direito), cai no chão se a criatura morrer e é salvo no NBT.

Ainda não existe: tribo/time — a criatura é de um jogador só.

### 13.1 Comandos e afinidade

Implementado. Cada criatura domesticada tem uma **ordem** permanente, salva no NBT e visível no texto sob a mira:

| Ordem | Acompanha o dono | Revida | Defende o dono |
|---|---|---|---|
| Seguir | sim | sim | não |
| Ficar | não | não | não |
| Defender (padrão) | sim | sim | sim |
| Fugir | sim | não — corre de quem a ferir | não |

**Atacar** não é uma ordem, é uma ação: o jogador mira um alvo e todas as suas criaturas num raio de 32 blocos cuja ordem permita lutar (Seguir, Defender) recebem aquele alvo. Largam o alvo se ele morrer ou se afastar mais de 40 blocos. Não pode mirar as próprias criaturas nem a si mesmo; contra outro jogador, respeita a regra de PvP do servidor.

Atalhos (remapeáveis em Controles → Ice Age Survival): **R** alterna a ordem da criatura sob a mira; **G** manda atacar o que está sob a mira. A mira alcança 48 blocos e é bloqueada por paredes.

**Obediência:** a chance de a criatura acatar um comando vai de `minObedience` (config, padrão 60%) com afinidade 0 até 100% com afinidade máxima. Quando ignora, o jogador é avisado.

**Ganhar afinidade:** dar à criatura domesticada um alimento da espécie cura `value` de vida e soma `5 × quality` de afinidade, no máximo uma vez a cada 30 s. É opcional — não há fome nem manutenção.

Rede: dois payloads cliente → servidor (`set_order`, `attack_order`). O servidor revalida dono, distância, consciência da criatura e validade do alvo; o cliente só envia a intenção. Comandos por grupo não existem; `CreatureCommands` recebe criatura ou alvo individualmente, então um seletor de grupo caberia por cima sem mudar o protocolo de ordem.

## 14. Torpor

- Valor separado da vida; máximo = atributo `torpor` do indivíduo (escala com os pontos).
- Decai `torpor_decay_per_second` o tempo todo, acordada ou não. Atualizado uma vez por segundo.
- Ao atingir o máximo, a criatura fica inconsciente até o torpor zerar ou a domesticação concluir: sem IA, sem animação, e não pode ser empurrada nem sofre recuo de golpe.
- Criaturas domesticadas são imunes.
- Torpor, inconsciência e progresso são salvos no NBT; uma criatura inconsciente num chunk descarregado continua como estava ao recarregar.

**Flecha tranquilizante** (`iceagesurvival:tranq_arrow`): flecha + narcótico. Dano base 0,5 contra 2,0 da flecha comum. Torpor = `tranqArrowTorpor` (config, padrão 25) × velocidade ÷ 3 — arco totalmente puxado dá o valor cheio. A Força do arco multiplica o torpor por 1 + 0,25 × (nível + 1): Força V dá 2,5×. Funciona em arco, besta e dispensador.

**Narcótico** (`iceagesurvival:narcotic`): carne podre + fruta-negra. Além de ingrediente da flecha, pode ser dado com clique direito a uma criatura **inconsciente**: soma `narcoticTorpor` (config, padrão 40) de torpor, sem causar dano e sem contar como alimento. Não tem espera entre doses; o custo é o item. Não funciona em criatura acordada.

**Árvore de fruta-negra:** tronco de abeto com folhas próprias (`black_fruit_leaves`), copa arredondada, 3–4 blocos de tronco. Nasce em taiga, taiga nevada, taigas antigas, grove e planície nevada (tag de bioma `has_black_fruit_tree`), em média uma tentativa a cada 3 chunks. As folhas amadurecem sozinhas enquanto presas a uma árvore viva; maduras, o clique direito solta 1–2 frutas e elas voltam a amadurecer. Quebrar folhas maduras também solta frutas. Cerca de 1 em 4 folhas já nasce madura. Não há muda: a árvore não é plantável por enquanto.

Cadeia para a primeira domesticação: achar a árvore → fruta-negra + carne podre → narcótico → + flecha → flecha tranquilizante.

Escala das ferramentas: arco < besta < rifle. Hoje a besta ganha só ~5% pela velocidade maior; o multiplicador próprio dela e o rifle entram nas fases correspondentes.

## 15. Temperatura

Implementado. Mecânica secundária: a pergunta do jogador é *"tenho recursos para essa viagem?"*.

Um único valor por jogador, a **exposição** (0 a 1), no servidor. A cada tick ela anda conforme o **frio líquido** do lugar, relido do mundo uma vez por segundo:

```
temperatura sentida = temperatura do bioma − altitude × queda_por_bloco − (noite) − (tempestade a céu aberto)
frio        = max(0, (0,5 − temperatura sentida) / 1,0)
proteção    = fonte de calor (cai com a distância) + peças de armadura isolante + teto
frio líquido = clamp(frio − proteção, −1, 1)
```

Positivo esfria, negativo aquece, na mesma velocidade: `coldSecondsToFreeze` (padrão 120) é o tempo para congelar no frio extremo *e* o tempo para se recuperar no calor. Noite, altitude e tempestade derrubam a **temperatura do lugar** em vez de somar frio direto — assim uma noite de chuva num bioma temperado continua confortável, e só esfria de verdade o que já era frio.

Escala de referência (temperaturas base do vanilla): planície 0,8 → frio 0; taiga 0,25 → 0,25; planície nevada 0 → 0,5; taiga nevada −0,5 → 1,0. Armadura de couro completa dá 0,8 de proteção, um teto 0,25 e estar em cima de uma fogueira 0,8.

**Feedback: o congelamento do vanilla, sem nada novo.** A exposição é escrita em `ticksFrozen`, que já é sincronizado, desenha a vinheta de gelo, freia o jogador e é salvo com ele. Não há HUD, payload nem menu. Duas consequências registradas:

- A exposição cheia escreve **139 de 140** ticks, um abaixo do máximo, para que o vanilla nunca considere o jogador "totalmente congelado" e some o dano dele ao nosso. O dano é nosso (`coldDamage`, padrão 1 a cada 2 s, com o tipo de dano `freeze` do vanilla, para a mensagem de morte certa).
- Esse teto tirava os corações azuis do HUD, que o vanilla liga em `isFullyFrozen()`. Devolvidos no cliente por `PlayerHeartTypeEvent`, olhando a fração de congelamento sincronizada.

Dentro de powder snow o mod sai da frente: o congelamento ali é do vanilla, e disputar o mesmo contador não faria sentido. Criativo e espectador não acumulam frio, e morrer zera a exposição.

**Dados:** `#iceagesurvival:heat_sources` (fogueiras, fogo, lava, magma, tochas, lanternas) e `#iceagesurvival:insulating_armor` (as quatro peças de couro). Todo o balanceamento está na config de servidor (`coldEnabled`, `coldNightDrop`, `coldHeatRadius`, …).

**Persistência:** a exposição é um attachment do NeoForge no jogador — as criaturas do mod guardam estado no próprio NBT (D8), mas o jogador não é nossa entidade. Não acompanha a morte: renascer aquece.

**Isolamento (D5):** a medida de frio vem de uma interface, `ColdSource`. Trocar o sistema interno por Cold Sweat é escrever outra implementação e apontar `ColdExposure` para ela; `coldEnabled = false` desliga o nosso sem desinstalar nada.

Limites conhecidos: toda fonte de calor aquece igual (uma tocha vale uma fogueira — uma tag não carrega intensidade); "abrigo" é só não ver o céu, então uma caverna aberta protege tanto quanto uma casa; molhar-se não esfria. A sobrevivência da primeira hora a céu aberto, sem couro nem fogo, não foi medida em jogo ainda.

## 16. Reprodução

Macho + fêmea da mesma espécie, domesticados → gestação ou ovo → incubadora → filhote. Adultos não exigem alimentação de manutenção. Detalhes na Etapa 8.

## 17. Genética

- Cada atributo é herdado de um dos pais, 50/50, independentemente dos outros.
- Mutações raras:
  - **Ataque:** sem teto.
  - **Velocidade:** teto de +30%.
  - **Vida:** só sobe em linhagens que carregam o gene de mutação de vida.
  - Demais atributos: não escalam por mutação.
- O genoma é lógica pura em `core/genetics`, coberta por testes JUnit com RNG semeado.

## 18. Worldgen

Desenho provisório (D6): world preset `iceagesurvival:ice_age` em datapack, com a fonte de biomas do overworld restrita a biomas frios (vanilla primeiro, próprios depois). Água congelada e cobertura de neve vêm da temperatura dos biomas. Nada de dimensão separada.

A confirmar na Etapa 9: variedade suficiente só com biomas frios, regiões "relativamente seguras", e se vale suporte opcional a Tectonic para relevo.

## 19. Caverna

Estrutura subterrânea procedural (jigsaw) com entradas, túneis, salas, áreas profundas e caminho até a arena. Localizada por um rastreador temático próprio, usado repetidamente para triangular a direção. Etapa 10.

## 20. Boss

Giganotosaurus (ou superpredador equivalente): físico, biologicamente plausível, sem magia. Arena fechada, HP e dano altos, fases por mudança de comportamento. Deve exigir equipe de criaturas preparada. Etapa 10.

## 21. Multiplayer

Checklist por feature, antes de considerá-la pronta: single-player, servidor dedicado, persiste após relog, não duplica item, não perde entidade, sem erro no console, sem exploit óbvio, configurável, documentada.

Pontos de atenção: validação de dono em todo payload, montaria (autoridade de movimento), criatura inconsciente quando o chunk descarrega, implantes (duplicação).

### Testes

- **JUnit** (`./gradlew test`, roda no `build`): lógica pura de `core/`.
- **GameTest** (`./gradlew runGameTestServer`): comportamento em servidor real — carregamento da espécie, sorteio e aplicação de atributos, persistência de atributos e dono, tags e attachments.
- **Manual:** `runClient` e sessão multiplayer de verdade (dois clientes) — não automatizados.

**Comandos de teste (`/ias`, permissão 2).** Existem para não refazer a cadeia torpor → alimentar → domesticar a cada recompilação. Sem seletor agem na criatura sob a mira (ou na mais próxima); com `<criaturas>` agem no seletor inteiro.

| Comando | O que faz |
|---|---|
| `/ias tame [criaturas]` | Domestica na hora, com os níveis bônus de uma domesticação perfeita e afinidade cheia |
| `/ias knockout` / `wake` | Derruba (torpor no máximo) ou acorda |
| `/ias torpor <valor>` / `level <n>` / `affinity <v>` | Ajusta torpor, refaz os atributos num nível, ajusta afinidade |
| `/ias saddle` | Põe ou tira a sela |
| `/ias ride` | Domestica, sela, enche a afinidade e monta — o caminho curto para testar a Etapa 7 |
| `/ias info` | Nível, vida, torpor, dono, afinidade, ordem, sela e os pontos por atributo |
| `/ias kit` | Arco, flechas tranquilizantes, narcóticos, selas e comida |
| `/ias spawn <espécie> [nível] [qtd] [wild\|tamed\|knocked]` | Faz nascer à frente do jogador; `tamed` entrega o estado de meio de jogo direto |
| `/ias spawns` | Diagnóstico: bioma atual, quais espécies nascem nele, quantas já existem no raio e o teto de cada uma |
| `/ias repopulate` | Força uma tentativa de reposição agora e diz quantas nasceram |

Nenhuma mecânica vive nos comandos: cada subcomando só chama o sistema correspondente. Em jogo, é por `/ias spawns` e `/ias repopulate` que se investiga fauna que não aparece.

O mundo do GameTest é plano e de bioma temperado, então o frio não chega a subir lá: a curva de temperatura é coberta por JUnit e o que o gametest confere é a tubulação (tags carregadas, attachment anexado e salvo, criativo imune).

## 22. Performance

- Nada de varredura de entidades a cada tick; usar intervalos e resultados em cache.
- Manada com líder e raio limitado, não N×N.
- IA reduzida longe de jogadores.
- Estado de domesticação seguro a descarregamento de chunk (baseado em game time, não em contadores por tick).
- O frio roda a cada tick por jogador, mas só com aritmética: a leitura do mundo (que varre blocos procurando fonte de calor) acontece uma vez por segundo e fica em cache no attachment.
- A reposição de fauna é uma varredura de entidades por jogador a cada 45 s, e uma só para todas as espécies (as contagens saem da mesma lista). A procura por posição não carrega chunk: coluna em chunk descarregado é descartada.

## 23. Roadmap

| Etapa | Conteúdo | Estado |
|---|---|---|
| 0 | Pesquisa e este documento | ✅ 2026-09-30 |
| 1 | Workspace, Gradle, build, runServer | ✅ 2026-09-30 (`runClient` ainda não verificado) |
| 2 | Core: níveis, atributos, ownership, persistência, registry de espécies | ✅ 2026-09-30 |
| 3 | Domesticação com criatura de teste | ✅ 2026-09-30 (falta conferir no cliente) |
| 4 | Smilodon | ✅ 2026-09-30, conferido em jogo pelo Felipe |
| 5 | Mais criaturas, spawning | ✅ 2026-09-30 em testes automáticos (mamute, lobo-terrível, manada, caça, spawn); falta conferir em jogo |
| 6 | Temperatura | ✅ 2026-09-30 em testes automáticos; falta sentir o frio em jogo e balancear a primeira hora |
| 7 | Montaria | ✅ 2026-09-30 em testes automáticos (sela, controle, pulo, recusas) + reposição de fauna e comandos `/ias`; falta conferir em jogo |
| 8 | Reprodução e genética | — |
| 9 | Worldgen | — |
| 10 | Endgame: rastreador, caverna, arena, boss | — |

MVP = Etapas 1–4 + versão mínima de 6, 7 e 9 (mundo frio, temperatura básica, montar o Smilodon).

## 24. Decisões técnicas

Ver a tabela em [4](#4-decisões). Registro de mudanças estruturais:

- 2026-09-30 — Arte do Revival passou do resource pack local para o repositório (D21), com guarda em
  `modelgen` contra regeração e `ASSET_LICENSES.md` ao lado dos assets. O jar fica não distribuível
  até a arte ser substituída.
- 2026-09-30 — Etapa 7: montaria (D18, D19), reposição de fauna (D20) e comandos de teste `/ias`. A fauna que não voltava num mundo já explorado era a categoria `CREATURE` do vanilla, não os biome modifiers; ver [11](#11-criaturas).
- 2026-09-30 — `CLAUDE.md` criado: Sonnet 5.5 para espécies e ajustes pela receita, Opus 5.5 para as etapas 7–10; trabalho grande dividido em subagentes, um por função.
- 2026-09-30 — Primeiro dinossauro: `tyrannosaurus`, pela receita de espécie terrestre (sem classe nova).
  Spawn em biomas frios por ora (D6 ainda aberta). **Não é montável**: quando ele foi feito o codec
  `Species` ainda não tinha bloco `mount`; agora tem, e deixá-lo de fora passou a ser escolha de
  balanceamento à espera do Felipe — ver [26](#26-ainda-não-decidido).
- 2026-09-30 — Etapa 6: temperatura. D5 fechada (sistema interno fica), D16 e D17. O frio reaproveita o congelamento do vanilla em vez de ter HUD próprio.
- 2026-09-30 — Documento criado; D1–D11 registradas.
- 2026-09-30 — Etapa 2: D12–D14. Criatura de teste antecipada para a Etapa 2.
- 2026-09-30 — Assets: workflow de modelos gerados por script aprovado. `blockbench-mcp` (enfp-dev-studio) avaliado e descartado: é só um esqueleto que envia `hello_world`.
- 2026-09-30 — Narcótico, árvore de fruta-negra e nova receita da flecha, a pedido do Felipe. Smilodon ampliado (escala 2,0, dorso a ~2,4 blocos) e imune a recuo.
- 2026-09-30 — Escala do modelo saiu de `body.model_scale` (dados) para `creature_models/` (assets). Placeholders do F&A Revival como resource pack local, fora do repositório.
- 2026-09-30 — Etapa 5: `LandCreature` genérica substitui a classe `Smilodon`; mamute e lobo-terrível; manada, defesa em grupo, caça e spawn natural por dados.
- 2026-09-30 — Inventário de criatura (come sozinha; dono = quem derrubou), bloco `body` e correções de pathfinding do Smilodon (degrau 1,1, atravessa folhas, colisão 1,3 × 2,3).
- 2026-09-30 — A instância de teste do Prism passou a receber uma cópia do jar (`tools/deploy-prism.sh`): o atalho para `build/libs` quebrava o jogo aberto a cada recompilação.
- 2026-09-30 — Etapa 4: comandos, obediência por afinidade e D15.
- 2026-09-30 — Etapa 3: torpor, domesticação e flecha tranquilizante; regras nas seções 13 e 14.
- 2026-09-30 — Tyrannosaurus em dobro (colisão 3,6×7,2; escala do modelo 5,66; assento 6,2; degrau 2,8) e ataque de quem monta.
- 2026-09-30 — Assento do Tyrannosaurus de 6,2 para 7,4: o jogador montado ficava dentro do corpo; 7,4 é a altura do osso `rider_pos` (21 px) na escala 5,66.
- 2026-09-30 — Sons do F&A Revival (All Rights Reserved, mesmo caso dos modelos) para Smilodon, mamute e T-Rex: bloco opcional `sounds` no JSON de espécie (`ambient`, `hurt`, `death`, `alert` ao escolher alvo, `volume`), instalados por `tools/install-revival-placeholders.py`. O lobo-terrível segue mudo: o Revival não tem ele.
- 2026-09-30 — Tyrannosaurus reduzido de 2x para 1,5x: colisão 2,7×5,4, escala do modelo 4,25, assento 5,6 (osso `rider_pos` × escala), degrau 2,1.

## 25. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| **Assets de criaturas.** Não há artista no projeto e nenhum asset externo é reutilizável. | Médio — a arte em uso é do F&A Revival, que resolve o visual mas é *All Rights Reserved* e **trava a distribuição do jar** ([D21](#4-decisões)). Os geradores por script seguem como a alternativa nossa, de qualidade de blocagem. | Editar os modelos no Blockbench a partir daqui até serem nossos, ou licenciar. Enquanto isso, não entregar o jar a ninguém. |
| Escopo: dez etapas, vários sistemas grandes. | Alto | MVP estreito; não avançar com etapa instável. |
| Balanceamento de torpor/níveis/genética. | Médio | Tudo em dados e config; testes de lógica pura. |
| O frio matar o jogador na primeira hora, antes de haver couro ou fogueira. | Médio | Curva e tempos todos em config; medir em jogo e afrouxar `coldSecondsToFreeze` ou a temperatura de conforto. |
| Montaria em multiplayer (latência, dessincronização). | Baixo — resolvido reaproveitando o `travelRidden` do vanilla ([D18](#4-decisões)); a autoridade de movimento é a mesma do cavalo. | Conferir com dois clientes de verdade, que é o que ainda não foi feito. |
| Muitas entidades com IA em servidor. | Médio | Ver [22](#22-performance). Varreduras de entidade são espaçadas (manada ~5 s, caça ~30 s, reposição ~45 s, defesa só ao ser ferida). Ainda não medido com fauna densa. |
| Fauna finita: criaturas `CREATURE` só nascem com a geração do terreno. | Resolvido na Etapa 7 pela reposição própria ([D20](#4-decisões)). | Novo risco em troca: a reposição encher o mundo. Contido pelo `max_nearby` por espécie; medir a densidade em jogo ao longo de uma sessão longa. |
| Teto de 1024 de vida do vanilla limita criaturas gigantes e o boss. | Médio | Decidir na Etapa 10: redução de dano por fase, ou atributo de vida próprio. |
| 1.21.1 envelhecer. | Baixo | `core/` independente do Minecraft facilita port. |

## 26. Ainda não decidido

1. **Licença do nosso código** e se o repositório será público.
4. World preset próprio vs. conversão global no estilo Primal Winter — decidir na Etapa 9 (D6).
5. Integração com criaturas de mods externos: possível em tese (registrar uma espécie apontando para um `EntityType` alheio), mas exigiria anexar nossos dados a entidades de terceiros. Não planejado.
6. Criaturas voadoras e de carga: quais espécies.
7. Gestação vs. ovo por espécie.
8. Nome final do mod (`Ice Age Survival` / id `iceagesurvival` são provisórios).
10. Muda da árvore de fruta-negra (plantar perto da base) — hoje só se colhe de árvores naturais.
12. Como tirar a sela em jogo (hoje só `/ias saddle` ou a morte da criatura): tecla, tela de inventário da criatura, ou clique com a mão vazia agachado.
13. ~~Atacar montado~~ Feito em 2026-09-30 (clique de ataque = mordida da montaria).
14. ~~Tyrannosaurus montável?~~ Decidido: sim (2026-09-30). Bloco `mount` em `tyrannosaurus.json`: assento 5,6 (osso `rider_pos` do modelo × escala), afinidade 25, velocidade ×0,8 (≈ Smilodon), pulo 0,45.
11. Mods de fauna só no CurseForge (ex.: Primal Era) — não verificados.
