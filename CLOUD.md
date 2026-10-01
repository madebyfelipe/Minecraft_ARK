# CLOUD.md — Ice Age Survival

Game Design + Technical Design Document vivo do projeto.
Atualizar sempre que uma decisão estrutural mudar.

- **Última revisão:** 2026-10-01
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
| D1 | Minecraft **1.20.1** | Escolhida para compatibilidade com o Fossils and Archaeology: Revival e More Hitboxes como dependências externas, sem copiar os assets deles. | Fechada; substitui o alvo inicial 1.21.1 |
| D2 | Loader **Forge 47.4.3** | Compatível com as dependências externas e com a API multipartes do More Hitboxes; o projeto usa capacidades, `SimpleChannel` e GameTest do Forge. | Fechada |
| D3 | **GeckoLib 4.7.2** | Build Forge para Minecraft 1.20.1, padrão de animação das criaturas e integração com More Hitboxes. | Fechada |
| D4 | Gameplay, dados e código de criaturas são nossos; F&A Revival só fornece assets carregados em runtime | Não copiar código, modelos, texturas, animações ou sons. A dependência foi aceita após a migração para 1.20.1 Forge, exclusivamente para resolver assets do mod instalado pelo jogador; domesticação, progressão e IA continuam próprias. | Fechada; substitui a decisão inicial de não depender de fauna |
| D5 | Temperatura: **sistema interno leve**, atrás de uma interface | Cold Sweat é muito mais complexo do que o brief pede ("secundária, sem burocracia"). Ver [15](#15-temperatura). | Fechada na Etapa 6 — `ColdSource` continua de pé |
| D6 | Mundo glacial por **world preset próprio em datapack**, sem mod de worldgen obrigatório | Um preset com a fonte de biomas restrita a biomas frios resolve "mundo predominantemente congelado" sem dependência. Ver [18](#18-worldgen). | Fechada na Etapa 9 — a fonte troca biomas quentes por frios (`iceagesurvival:remapped`) e usa o ruído `amplified` do vanilla para montanhas e vales dramáticos. Ice Age - Frozen World não entra: para 1.20.1 ele só existe para Fabric, enquanto este pack usa Forge. |
| D7 | Espécies num **registry de datapack** sincronizado | Permite adicionar espécie por JSON, com validação por Codec e sync automático para o cliente. | Fechada |
| D8 | Dados da criatura no **NBT da própria entidade**, serializados por Codec | As entidades são nossas; estado de jogador de terceiros fica em capability Forge (D17). | Fechada |
| D9 | IA com **Goals vanilla** | Suficiente para território e manada; SmartBrainLib fica como opção se os Goals virarem gargalo. | Provisória |
| D10 | Lógica pura (stats, genética, torpor) **sem dependência de classes do Minecraft** | Permite testes JUnit rápidos, sem subir o jogo. | Fechada |
| D11 | Repositório GitHub **privado** | Pode ser aberto depois; publicar é irreversível. | Fechada até o Felipe decidir o contrário |
| D12 | Indivíduo = **pontos por atributo**; nível = 1 + total de pontos | Dois animais do mesmo nível ficam diferentes ("esse tem ataque melhor"), e é exatamente o que a reprodução vai herdar atributo a atributo. Evita "mob com HP multiplicado". Ver [11](#11-criaturas). | Fechada |
| D13 | Dono e estado domesticado via **`TamableAnimal`** vanilla | Persistência, sync e regras de aliado já prontas e compatíveis com outros mods. | Fechada |
| D14 | Sem pacote `network/` até existir o primeiro payload | O registry de espécies já sincroniza sozinho. O pacote nasceu na Etapa 4, com os comandos. | Cumprida |
| D15 | Ordem de ataque vale para **todas** as criaturas do jogador ao alcance | Exigir escolher uma criatura antes de apontar o alvo pediria um estado de "selecionada" escondido. Não é o sistema de grupos do brief (que continua fora). | Fechada |
| D16 | O frio **é** o `ticksFrozen` do vanilla, não um valor paralelo sincronizado | Sincronização, persistência, vinheta de gelo e lentidão já existem e são de graça; um valor próprio pediria payload, HUD e NBT para o mesmo resultado. Custo: parar em 139/140 e assumir o dano (ver [15](#15-temperatura)). | Fechada |
| D17 | Estado de frio do jogador em **capability do Forge** | D8 dispensa estado externo para *nossas* entidades; o jogador é de terceiros. A capability é anexada e persistida pelo Forge. | Fechada |
| D18 | Montaria pelo **modelo de veículo do vanilla** (`travelRidden`) | É o caminho do cavalo: o cliente de quem monta simula o movimento e manda a posição do veículo, o servidor confere que o remetente é o controlador. Escrever controle próprio seria reinventar a predição e a reconciliação. Ver [11](#11-criaturas). | Fechada na Etapa 7; a barra de carga do pulo (`PlayerRideableJumping`) saiu em 2026-10-01 (D23) |
| D19 | A sela é o **`minecraft:saddle` do vanilla**, com receita nossa | Um item por espécie (como no ARK) seria uma dúzia de itens e texturas para a mesma função; o item do vanilla já é reconhecível e o que gateia a montaria é a espécie ter bloco `mount`. A receita existe porque no vanilla a sela não é craftável, e depender de baú de estrutura num mundo glacial travaria a Fase 4. | Fechada na Etapa 7 |
| D20 | **Reposição de fauna própria**, em vez de mudar a categoria das criaturas | `CREATURE` do vanilla só nasce na geração do terreno; trocar para `MONSTER` faria a fauna aparecer e *desaparecer* sozinha, contra o design. A reposição repõe no que já existe, com teto de densidade por espécie. Ver [11](#11-criaturas). | Fechada na Etapa 7 |
| D22 | O conteúdo próprio do Revival **fica desligado**: itens fora das abas do criativo, minérios e estátua moai removidos por biome modifier, estruturas por `structure_set` vazios | Os itens dele duplicam os nossos (ovos de dinossauro, carnes, máquinas) e os minérios/estruturas espalham fósseis, âmbar, piche e templos que não existem na nossa progressão. Desligar por dados e por um evento evita mixin e não exige mexer na config do jogador. Limites: receitas dele continuam válidas; o bioma vulcão (TerraBlender) não aparece no preset Era do Gelo, mas aparece num mundo comum. | Fechada 2026-10-01 |
| D23 | Pulo e voo montados **sem a barra de carga do cavalo**; o voo é decidido no cliente de quem monta | A barra de carga é do cavalo e não combina com predadores; o pulo sai na hora, com altura e avanço da espécie. O voo segue o Cobblemon (olhar dirige, frente acelera, embalo, Espaço bate as asas, sprint dá impulso) e, como o resto do movimento do veículo (D18), é simulado no cliente; o servidor só recebe o estado para tirar a gravidade. A física fica em `core/mount/FlightModel`, testada por JUnit. | Fechada 2026-10-01; falta sentir em jogo |
| D21 | Usar assets do F&A Revival **em runtime**, sem copiá-los ao projeto ou ao jar | O Ice Age Survival referencia os recursos registrados pelo mod original, instalado separadamente. Isso preserva a autoria/licença dos assets e mantém o jar do addon sem conteúdo do Revival. Exige a dependência compatível instalada para renderizar as espécies afetadas. | Fechada; decisão atual substitui a escolha de 2026-09-30 |
| D24 | **Ecologia por fome, estresse e rivalidade**, decidida no servidor e com o núcleo em `core/ecology` (JUnit) | Caçada só por intervalo aleatório e linha de visão deixava o mundo parado. Agora o predador caça quando tem fome (`Hunger`), escolhe a presa mais fácil pelo faro (`HuntChoice`: filhote, ferida, desgarrada; bando encara presa maior), persegue com fôlego limitado e desiste. A presa caçada avisa a manada, que dispara. Cada selvagem tem um termômetro de estresse (`Stress`) movido por eventos (predador à vista, ferida, manada atacada, rival, jogador perto) e pelo temperamento da espécie; estressado percebe de mais longe, foge antes e é imprevisível; o predador estressado ataca o jogador de mais longe. Rivais (tag `rivals`) disputam território: o mais fraco cede; briga para à metade da vida. | Fechada 2026-10-01; falta sentir em jogo e balancear |
| D25 | Voo montado com **curva e inclinação limitadas** e energia (mergulho ganha velocidade, subida perde); assento no osso `rider_pos` animado | A câmera gira na hora; a montaria segue com velocidade angular de `flight_turn_rate`. O rasante do grifo do ARK sai da troca altura × velocidade. O assento acompanha a animação de voo (que inclina o corpo), como no Revival. | Fechada 2026-10-01; falta sentir em jogo |

## 5. Mods avaliados

**Registro histórico do alvo inicial:** API do Modrinth consultada em 2026-09-30 para `1.21.1` + `neoforge`. Em 2026-10-01, o alvo mudou para Forge 1.20.1 para integrar o Revival como dependência de assets; ver §8 e D21. Downloads e datas abaixo são os daquela pesquisa.

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
- **Outros mods de fauna como dependência de gameplay** — continuam fora de escopo. A exceção deliberada é o F&A Revival como provedor externo de assets em runtime; código e mecânicas permanecem próprios.
- **Tough As Nails, Legendary Survival Overhaul, Homeostatic** — trazem sede e outros sistemas fora do escopo.
- **TerraBlender, Terralith, Citadel** — desnecessários para a abordagem escolhida.
- **Primal Winter como dependência** — sem manutenção desde 2024-10. Fica como *referência de design* (MIT, código aberto) para a Etapa 9.
- **Ice Age - Frozen World como dependência** — o arquivo para Minecraft 1.20.1 é Fabric e requer Natural Temperature; este projeto é Forge 47.4.3. Não há release Forge compatível para instalar.

## 8. Versões

| Componente | Versão | Fonte |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | 47.4.3 | Forge Maven |
| ForgeGradle | 6.x | Forge Maven |
| Gradle | 8.8 (wrapper) | wrapper do ForgeGradle |
| Java | 17 | exigido pelo Minecraft 1.20.1 |
| GeckoLib | 4.7.2 (`geckolib-forge-1.20.1`) | Cloudsmith do GeckoLib |
| Fossils and Archaeology: Revival | 9.3.4.0 | dependência externa, não empacotada |
| More Hitboxes | 1.9.2 | dependência externa, não empacotada |
| TerraBlender | 3.0.1.10 | dependência runtime exigida pelo Revival |
| Architectury | 9.2.14 | dependência runtime exigida pelo Revival |

O alvo 1.20.1 permite resolver os recursos do Revival no namespace `fossil` e usar More Hitboxes; não transportar código nem assets desses mods.

## 9. Licenças

- **Nosso código:** licença ainda não definida (ver [26](#26-ainda-não-decidido)). Enquanto isso, `All Rights Reserved` no metadata e repositório privado.
- **GeckoLib (MIT):** usado como dependência, não redistribuído dentro do nosso jar.
- **More Hitboxes (MIT):** biblioteca Forge 1.20.1, usada como dependência para hitboxes multipartes; não redistribuída dentro do nosso jar.
- **Fossils and Archaeology: Revival:** dependência externa obrigatória para os assets/runtime usados por várias espécies. Seu código e seus assets não são copiados para o projeto ou empacotados no nosso jar; instale o mod original de um canal oficial.
- **ForgeGradle:** base do build Forge.
- **Assets próprios:** todo modelo, textura, animação e som incluído no jar deve ser original ou ter licença compatível.

**Assets externos de criatura.** Algumas espécies carregam modelos, texturas, animações e sons
registrados pelo *Fossils and Archaeology: Revival* em runtime. O repositório e o jar do Ice Age
Survival não contêm esses arquivos. A instância de jogo precisa instalar o Revival compatível;
detalhes em `src/main/resources/assets/iceagesurvival/ASSET_LICENSES.md`.

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
├── registry/               DeferredRegisters, tags usadas em código, capabilities
├── config/                 configuração comum e de servidor
└── client/                 renderers, HUD, telas (só client)
```

Princípios:

- **Servidor decide tudo.** Cliente envia intenção (payload), servidor valida dono, distância e estado.
- **`core/` não importa `net.minecraft`.** A entidade é uma casca fina que alimenta e consome a lógica pura.
- **Dados antes de código.** Número de balanceamento vive no JSON da espécie ou na config, nunca espalhado em classes.
- **Sem mixins** enquanto um evento ou API do Forge resolver.

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
| **Smilodon** (`smilodon`) | Predador territorial solitário, rápido | 1,3 × 2,3 | Agressivo e **espreita** (`hunt_style: stalk`): aproxima-se devagar pelas costas e só dá o bote, rugindo, a 4,5 blocos, ao ser ferido ou quando o jogador o vê. Território de 32 blocos, caça presas grandes, recua com 25% de vida | taiga, taiga nevada, taigas antigas, grove, encostas nevadas |
| **Urso-terrível** (`direbear`) | Predador territorial solitário, resistente | 2,0 × 3,0 | Agressivo, território de 64 blocos, caça presas grandes e recua com 15% de vida | taiga, taiga nevada, taigas antigas, grove, encostas nevadas |
| **Mamute-lanoso** (`mammoth`) | Herbívoro de manada, tanque; montado, coletor de madeira | 3,0 × 4,65 | Pacífico até ser provocado; manada migratória de 2–4 que se defende junta | planície nevada, ice spikes, taiga nevada |
| **Tyrannosaurus rex** (`tyrannosaurus`) | Primeiro dinossauro; predador de topo solitário, raro | 2,7 × 5,4 | Agressivo, raio de percepção 24, território de 48 blocos, caça presas grandes, recua com 10% de vida | taiga, taiga nevada, taigas antigas, planície nevada (peso 1) |
| **Velociraptor** (`velociraptor`) | Predador pequeno de bando | 0,7 × 1,1 | Agressivo, bando de 3–5 com defesa em grupo, caça presas pequenas | taiga, taiga nevada, grove |
| **Utahraptor** (`utahraptor`) | Raptor grande, montável (o "raptor" do ARK) | 1,2 × 2,3 | Agressivo, bando de 2–3, caça presas grandes | taigas nevadas e de abetos |
| **Espinossauro** (`spinosaurus`) | Predador de topo das águas geladas, montável | 2,7 × 5,6 | Agressivo, atravessa o mato, domesticado com peixe; assento à frente da vela (`seat_forward`) | rio congelado, praia nevada, oceano congelado |
| **Alossauro** (`allosaurus`, antes `carnotaurus`) | Predador médio rápido que caça em bando, montável, muito raro | 1,8 × 3,4 | Velocidade-base igual ao Smilodon e superior à do T-Rex; mais forte que o Smilodon; bando concede +25% velocidade e dano, caça brontos e mamutes, atravessa o mato | planície nevada, encostas nevadas, grove (a partir de 1.500 blocos do spawn) |
| **Brontossauro** (`brontosaurus`) | Saurópode gigante migratório; montável, coletor de madeira e transporte de carga | 4,0 × 8,0 | Pacífico, manada de 1–3 que viaja e se defende junta; 216 espaços de inventário (quatro baús grandes); atravessa o mato | planície nevada, grove, taiga nevada |
| **Estegossauro** (`stegosaurus`) | Herbívoro defensivo de manada; armazenamento móvel pequeno | conforme o registro | Manada migratória e defesa em grupo; 27 espaços (um baú pequeno) | savanas frias e florestas abertas |
| **Pteranodonte** (`pteranodon`) | Montaria aérea para exploração | 1,6 × 2,0 | Voo montado no estilo do Cobblemon (ver Montaria); animação de voo separada da de andar; hitboxes multipartes | biomas abertos definidos pela tag da espécie |
| **Dodô** (`dodo`) | Comida e pena do início de jogo | 0,7 × 0,9 | Passivo, bando de 3–6 que foge ao ser ferido; carne (crua/assada) e pena de dodô; domesticado com frutas e sementes | biomas nevados abertos, desde o spawn (peso 14) |
| **Elasmotério** (`elasmotherium`) | Primeira montaria: pele, carga pequena e calor | 1,8 × 2,4 | Pacífico até ser provocado, manada de 1–3 com defesa em grupo; **monta sem sela**, 9 espaços de carga, **aquece** quem monta (0,6) e quem está a até 4 blocos; dá pele | planície e taiga nevadas, grove, ice spikes, desde o spawn (peso 9) |
| Criatura de teste (`test_creature`) | Só para testes automáticos; usa o modelo do porco | 0,9 × 0,9 | Passiva | não nasce |

Montáveis: Elasmotério (sem sela), Smilodon, urso-terrível, mamute, Tyrannosaurus, Utahraptor, Espinossauro, Alossauro, Brontossauro e Pteranodonte. O addon carrega os modelos e sons do Revival em runtime (Deinonychus para Utahraptor e Diplodocus para Brontossauro); nenhum desses arquivos é copiado para o projeto ou para o jar.

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

Pendentes do brief — Era do Gelo: rinoceronte-lanoso, megaloceros, megatherium, urso-das-cavernas, bisão, auroque, mastodonte. Dinossauros: ~~tyrannosaurus~~, ~~velociraptor~~, ~~spinosaurus~~, ~~utahraptor~~, ~~allosaurus~~, ~~brontosaurus~~ (feitos; o carnotaurus virou allosaurus), triceratops, ankylosaurus, giganotosaurus (boss).

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

- **Selar:** o dono clica com um `minecraft:saddle` na criatura domesticada e acordada. A sela fica na criatura (sincronizada para o cliente, salva no NBT) e volta ao mundo quando ela morre. Receita nossa, em [D19](#4-decisões).
- **Montar:** clique com a mão livre. Alimentar tem preferência enquanto a criatura estiver com fome, então uma mão cheia de carne não impede montar depois.
- **Controlar:** `travelRidden` do vanilla ([D18](#4-decisões)). A criatura aponta para onde o jogador olha, ré e passo lateral são reduzidos como no cavalo. **Pulo na hora, sem barra de carga** ([D23](#4-decisões)): o cliente de quem monta lê o Espaço (`CommandInput` → `setRiderInput`) e aplica o impulso da espécie; o servidor aceita a posição do veículo.
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

Cada espécie tem um `forge/biome_modifier/spawn_<especie>.json` do tipo `forge:add_spawns`, ligado a uma tag de biomas `spawns_<especie>`, com peso e tamanho de grupo. Regra de posição: chão firme e na superfície (vale neve, gelo e sob copa de árvore; nunca em caverna).

As criaturas são da categoria `CREATURE` do vanilla: nascem quando o terreno é gerado e não desaparecem. Na prática isso significa que **num mundo já explorado a fauna que morre não volta** — foi o que apareceu em jogo na Etapa 7.

### Reposição de fauna (implementado)

Bloco `spawn` do JSON de espécie, lido pela reposição própria do mod. **Ausente = a espécie só nasce com o terreno** (é o caso da criatura de teste).

```json
"spawn": { "biomes": "#iceagesurvival:spawns_smilodon", "weight": 2, "group_min": 1, "group_max": 1, "max_nearby": 1, "min_distance": 600 }
```

- Uma tentativa por jogador a cada `wildSpawnIntervalSeconds` (padrão 60 s): sorteia uma espécie entre as que podem nascer no bioma do jogador **e ainda têm vaga**, proporcionalmente ao `weight`, e procura posição num anel de `wildSpawnMinDistance` a `wildSpawnMaxDistance` (padrão 40 a 96 blocos) — longe da vista, dentro da distância de simulação.
- A posição passa pelas checagens de spawn do vanilla (mapa de altura, regra de superfície, colisão e bioma da tag), e o nascimento passa pelos eventos do Forge, então outro mod pode barrar.
- `max_nearby` é o teto de indivíduos daquela espécie no `wildSpawnDensityRadius` em volta do jogador, e `wildSpawnMaxTotal` (padrão 18; era 10, que os dodôs sozinhos lotariam) o teto da **soma de todas as espécies**. Criaturas domesticadas não entram na contagem. O raio da contagem é sempre maior que o de spawn (pelo menos `wildSpawnMaxDistance` + 32): antes os dois eram 96, quem nascia na borda saía andando, deixava de contar e abria vaga — com o teto só por espécie, uma base parada juntava mais de 40 criaturas em minutos.

**Zonas de perigo:** `spawn.min_distance` é a distância horizontal mínima do spawn do mundo para a espécie nascer, conferida na regra de colocação — vale para a reposição e para a geração do terreno. E o nível selvagem máximo cresce com a distância: 30% do `maxWildLevel` no spawn, 100% a `fullDangerDistance` (padrão 3.000 blocos).

| Zona | Distância do spawn | Espécies | Raridade |
|---|---|---|---|
| Spawn | 0+ | dodô, Elasmotério, lobo-terrível, mamute, Brontossauro | início de jogo abundante (dodô 14, Elasmotério 9); comuns (8, 8, 6) |
| Meio | 400+ / 600+ / 1.000+ | Velociraptor / Smilodon / Utahraptor | Smilodon raro (peso 2, máx. 1 por perto) |
| Longe | 1.500+ / 2.000+ | Alossauro, Espinossauro / T-Rex | muito raros (peso 1) |
- Não trocamos a categoria para `MONSTER` para conseguir spawn contínuo: isso faria a fauna desaparecer sozinha ([D20](#4-decisões)).
- Tudo desligável em `wildSpawnEnabled`, para quem quiser a fauna só na geração do terreno.

O `forge:add_spawns` continua: ele povoa chunk novo, a reposição cuida do que já existe. Os dois usam a mesma tag de biomas.

Ainda não existe: horário de atividade.

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
2. Inconsciente, clique direito abre o **inventário da criatura** (9 espaços por padrão; 27 no Estegossauro e 216 no Brontossauro, em páginas). Ela come sozinha dali: uma unidade por vez, sempre o alimento de maior `quality` disponível. Cada unidade soma o `value` do alimento ao progresso.
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

Implementado, no esquema do ARK. Cada criatura domesticada tem dois estados independentes, salvos no NBT e visíveis no painel sob a mira:

| Movimento | |
|---|---|
| Seguir (padrão) | acompanha o dono |
| Parar | fica onde está; com um alvo sai para lutar e fica onde a briga acabar |

| Postura | Revida | Defende o dono | |
|---|---|---|---|
| Passivo | não | não | |
| Passivo (fugir) | não | não | corre de quem a ferir |
| Neutro | sim | não | |
| Defenda-me (padrão) | sim | sim | ataca quem ferir o dono e quem o dono atacar |

**Assobios:** cada tecla muda só o movimento ou só a postura. Vale para a criatura sob a mira, se for do jogador; sem mira, para todas as dele num raio de 32 blocos. Cada criatura sorteia a obediência; o assobio toca uma flauta, com tom por comando, que todos por perto ouvem. Mundos anteriores têm a `Order` antiga convertida ao carregar (ficar → parar + passivo, seguir → neutro, fugir → passivo-fugir, defender → defenda-me).

**Atacar** não é uma ordem, é uma ação: o jogador mira um alvo e todas as suas criaturas num raio de 32 blocos cuja postura permita lutar (Neutro, Defenda-me) recebem aquele alvo. Largam o alvo se ele morrer ou se afastar mais de 40 blocos. Não pode mirar as próprias criaturas nem a si mesmo; contra outro jogador, respeita a regra de PvP do servidor.

Atalhos (remapeáveis em Controles → Ice Age Survival): **Y** seguir, **U** parar, **J** passivo, **K** neutro, **H** defenda-me, passivo-fugir sem tecla padrão, **G** atacar o que está sob a mira, **V** status. O T do ARK é o chat do Minecraft, daí as teclas diferentes. A mira alcança 48 blocos e é bloqueada por paredes.

**Painel sob a mira:** nome, nível, de quem é (borda verde = sua, laranja = de outro jogador, cinza = selvagem), sela, barra de vida, torpor e domesticação quando houver, e nas suas as ordens e a tecla de status.

**Tela de status (V):** para as criaturas do próprio jogador a até 32 blocos (ou a montaria, montado e sem mira). Modelo da criatura, cada atributo com o valor e os pontos ganhos, afinidade, ordens, sela e dono, e botões de assobio só para ela. Os pontos de atributo só existem no servidor: a tela pede (`status_request`) e recebe (`creature_status`) a cada segundo enquanto aberta.

**Obediência:** a chance de a criatura acatar um comando vai de `minObedience` (config, padrão 60%) com afinidade 0 até 100% com afinidade máxima. Quando ignora, o jogador é avisado.

**Ganhar afinidade:** dar à criatura domesticada um alimento da espécie cura `value` de vida e soma `5 × quality` de afinidade, no máximo uma vez a cada 30 s. É opcional — não há fome nem manutenção.

Rede: cliente → servidor `whistle`, `attack_order`, `mount_attack` e `status_request`; servidor → cliente `creature_status`. O servidor revalida dono, distância, consciência da criatura e validade do alvo; o cliente só envia a intenção. Grupos nomeados (como os do ARK) não existem: o assobio é para uma criatura ou para todas ao alcance.

## 14. Torpor

- Valor separado da vida; máximo = atributo `torpor` do indivíduo (escala com os pontos).
- Decai `torpor_decay_per_second` o tempo todo, acordada ou não. Atualizado uma vez por segundo.
- Ao atingir o máximo, a criatura fica inconsciente até o torpor zerar ou a domesticação concluir: sem IA, sem animação, e não pode ser empurrada nem sofre recuo de golpe.
- Criaturas domesticadas são imunes.
- Torpor, inconsciência e progresso são salvos no NBT; uma criatura inconsciente num chunk descarregado continua como estava ao recarregar.

**Flecha tranquilizante** (`iceagesurvival:tranq_arrow`): flecha + narcótico. Dano base 0,5 contra 2,0 da flecha comum. Torpor = `tranqArrowTorpor` (config, padrão 25) × velocidade ÷ 3 — arco totalmente puxado dá o valor cheio. A Força do arco multiplica o torpor por 1 + 0,25 × (nível + 1): Força V dá 2,5×. Funciona em arco, besta e dispensador.

**Narcótico** (`iceagesurvival:narcotic`): carne podre + fruta-negra. Além de ingrediente da flecha, pode ser dado com clique direito a uma criatura **inconsciente**: soma `narcoticTorpor` (config, padrão 40) de torpor, sem causar dano e sem contar como alimento. Não tem espera entre doses; o custo é o item. Não funciona em criatura acordada.

**Árvore de fruta-negra:** tronco de abeto com folhas próprias (`black_fruit_leaves`), copa arredondada, 3–4 blocos de tronco. Nasce em taiga, taiga nevada, taigas antigas, grove e planície nevada (tag de bioma `has_black_fruit_tree`), em média uma tentativa a cada 3 chunks. As folhas amadurecem sozinhas enquanto presas a uma árvore viva; maduras, o clique direito solta 1–2 frutas e elas voltam a amadurecer. Quebrar folhas maduras também solta frutas. Cerca de 1 em 4 folhas já nasce madura. A muda (`black_fruit_sapling`) cai das folhas e cresce na mesma árvore.

Cadeia para a primeira domesticação: achar a árvore → fruta-negra + carne podre → narcótico → + flecha → flecha tranquilizante.

Escala das ferramentas: arco < besta < rifle. Hoje a besta ganha só ~5% pela velocidade maior; o multiplicador próprio dela e o rifle entram nas fases correspondentes.

## 15. Temperatura

Implementado. Mecânica secundária: a pergunta do jogador é *"tenho recursos para essa viagem?"*.

Um único valor por jogador, a **exposição** (0 a 1), no servidor. A cada tick ela anda conforme o **frio líquido** do lugar, relido do mundo uma vez por segundo:

```
temperatura sentida = temperatura do bioma − altitude × queda_por_bloco − (noite) − (tempestade a céu aberto) − (molhado)
frio        = max(0, (0,5 − temperatura sentida) / 1,0)
proteção    = fonte de calor (cai com a distância) + isolamento da roupa + teto
frio líquido = clamp(frio − proteção, −1, 1)
```

Positivo esfria, negativo aquece, na mesma velocidade: `coldSecondsToFreeze` (padrão 120) é o tempo para congelar no frio extremo *e* o tempo para se recuperar no calor. Noite, altitude e tempestade derrubam a **temperatura do lugar** em vez de somar frio direto — assim uma noite de chuva num bioma temperado continua confortável, e só esfria de verdade o que já era frio.

Escala de referência (temperaturas base do vanilla): planície 0,8 → frio 0; taiga 0,25 → 0,25; planície nevada 0 → 0,5; taiga nevada −0,5 → 1,0 de dia e 1,2 à noite. Couro completo dá 0,8 de proteção, **pena** completa 1,0, **pele** completa 1,4, um teto 0,25, estar em cima de uma fogueira 0,8 e o **calor do Elasmotério** 0,6. Molhado (na água ou na chuva) derruba 0,3 da temperatura sentida.

**Degraus de roupa:** a **pena de dodô** é a primeira — completa, segura o dia em qualquer bioma nevado (taiga nevada 1,0) mas não a noite (1,2): à noite é preciso abrigo, fogo ou ir montado no Elasmotério (1,0 + 0,6). A **pele** segura também a noite. Pena: defesa 1/2/1/1, durabilidade metade da do couro, receitas nos formatos da armadura com pena de dodô; uma pena de dodô também vira uma pena comum.

**Calor do corpo:** bloco `body` da espécie, `body_heat` (proteção) e `body_heat_radius` (blocos). Montado, vale o calor inteiro da montaria; a pé, o da criatura mais quente por perto, caindo em linha reta até 0 na borda do raio (`Coldness.bodyHeat`). Não soma várias criaturas. Hoje só o Elasmotério (0,6 / 4 blocos).

**Roupa de pele:** a **pele** (`pelt`) cai do mamute (2–4), do Smilodon (1–2) e do lobo-terrível (1); com ela se fazem capuz, casaco, calças e botas nos formatos da armadura do vanilla. Defesa de couro, durabilidade 1,6× a do couro. O isolamento é calculado pelo código: 0,2 por peça de couro vanilla e 0,35 por peça de pele do mod.

**Tremer gasta comida:** no frio, cada tick soma `frio líquido × coldShiverExhaustion` de cansaço (padrão: um ponto de fome a cada 40 s no frio máximo).

**Termômetro:** à esquerda da hotbar, a temperatura do corpo (37 °C aquecido a 30 °C congelado, suavizada) e uma seta: ▼▼/▼ caindo, ▲/▲▲ subindo, = estável; "Molhado" quando estiver. O servidor manda exposição, frio líquido e molhado uma vez por segundo (`cold_status`).

**Feedback: o congelamento do vanilla, mais o termômetro.** A exposição é escrita em `ticksFrozen`, que já é sincronizado, desenha a vinheta de gelo, freia o jogador e é salvo com ele. Duas consequências registradas:

- A exposição cheia escreve **139 de 140** ticks, um abaixo do máximo, para que o vanilla nunca considere o jogador "totalmente congelado" e some o dano dele ao nosso. O dano é nosso (`coldDamage`, padrão 1 a cada 2 s, com o tipo de dano `freeze` do vanilla, para a mensagem de morte certa).
- Esse teto tirava os corações azuis do HUD, que o vanilla liga em `isFullyFrozen()`. Devolvidos no cliente por `PlayerHeartTypeEvent`, olhando a fração de congelamento sincronizada.

Dentro de powder snow o mod sai da frente: o congelamento ali é do vanilla, e disputar o mesmo contador não faria sentido. Criativo e espectador não acumulam frio, e morrer zera a exposição.

**Dados:** `#iceagesurvival:heat_sources` (fogueiras, fogo, lava, magma, tochas, lanternas); isolamento do couro e das peças de pena e de pele fica em `EnvironmentColdSource`. Todo o balanceamento está na config de servidor (`coldEnabled`, `coldNightDrop`, `coldHeatRadius`, …).

**Persistência:** a exposição é uma capability do Forge anexada ao jogador — as criaturas do mod guardam estado no próprio NBT (D8), mas o jogador não é nossa entidade. Não acompanha a morte: renascer aquece.

**Isolamento (D5):** a medida de frio vem de uma interface, `ColdSource`. Trocar o sistema interno por Cold Sweat é escrever outra implementação e apontar `ColdExposure` para ela; `coldEnabled = false` desliga o nosso sem desinstalar nada.

Limites conhecidos: toda fonte de calor aquece igual (uma tocha vale uma fogueira — uma tag não carrega intensidade); "abrigo" é só não ver o céu, então uma caverna aberta protege tanto quanto uma casa; não há calor demais (só frio). A sobrevivência da primeira hora a céu aberto, sem couro nem fogo, não foi medida em jogo ainda.

## 16. Reprodução

Implementado na Etapa 8. Toda criatura tem **sexo** (sorteado ao nascer; criaturas de mundos antigos sorteiam ao carregar). O dono **liga o acasalamento** na tela de status (V); uma fêmea adulta com ele ligado, a até 8 blocos de um macho adulto da mesma espécie e do mesmo dono também com ele ligado, concebe depois de 10 s juntos (corações sobem nos dois). Ela espera `cooldown_seconds` para cruzar de novo.

Bloco `breeding` no JSON da espécie; sem ele, a espécie não se reproduz:

| Campo | |
|---|---|
| `offspring` | `live` (mamíferos: gestação na fêmea, o filhote nasce dela) ou `egg` (dinossauros: ela põe um ovo) |
| `incubation_seconds` | tempo de gestação ou de incubação |
| `maturation_seconds` | de filhote a adulto |
| `cooldown_seconds` | espera da fêmea entre crias |

**Filhote:** nasce domesticado pelo dono, seguindo e passivo, com 40% do tamanho, sem sela nem acasalamento até crescer. Adultos não exigem alimentação de manutenção.

**Ovo** (`creature_egg`): um item só para todas as espécies, com espécie, genoma e dono no `CustomData` e as cores do ovo gerador da espécie. Só choca na **incubadora**.

**Incubadora:** um espaço de ovo e um de combustível. O ovo só avança aquecido — por uma fonte de calor (`#iceagesurvival:heat_sources`) a até 2 blocos, ou por combustível de fornalha queimando, gasto só com ovo dentro e sem outra fonte. Fria, pausa sem perder o progresso. O filhote nasce em cima dela. Receita: vidro, lã, fornalha e ferro.

**Mesa química:** duas entradas, uma saída, sem combustível; rende mais que a mesa de trabalho. Narcótico ×2 (fruta-negra + carne podre), estimulante ×2 (2 açúcares + frutas doces) e flecha tranquilizante ×4 (4 flechas + narcótico). O **estimulante** dado a uma criatura tira 150 de torpor (`stimulantTorpor`) — acorda uma criatura sua derrubada. Receita: frascos, caldeirão e tábuas. As receitas estão em código (`ChemistryRecipe`); virar tipo de receita de datapack fica para quando houver muitas.

## 17. Genética

- Cada atributo é herdado de um dos pais, 50/50, independentemente dos outros.
- Mutações raras:
  - **Ataque:** sem teto.
  - **Velocidade:** teto de +30%.
  - **Vida:** só sobe em linhagens que carregam o gene de mutação de vida.
  - Demais atributos: não escalam por mutação.
- O genoma é lógica pura em `core/genetics`, coberta por testes JUnit com RNG semeado.

Implementado na Etapa 8 (`Genome`, `Genetics`). Pontos **e** contagem de mutações de cada atributo vêm juntos de um dos pais. A cada cria, 3 tentativas de mutação de 2,5% (`mutationAttempts`, `mutationChance`); cada uma sorteia entre ataque, velocidade e — se a linhagem tiver o gene — vida. Ataque e vida ganham 2 pontos (e 2 níveis) por mutação; velocidade ganha 3% por mutação até 10 (+30%), e passado o teto a mutação se perde. O gene de vida passa se um dos pais o tiver, surge sozinho em 1% das crias (`healthGeneChance`) e 5% das selvagens já o carregam (`wildHealthGeneChance`). A tela de status mostra sexo, mutações, o gene, gestação e crescimento.

## 18. Worldgen

Implementado na Etapa 9 (D6). **Não precisa de mod.** O world preset **Era do Gelo** (`iceagesurvival:ice_age`) aparece em *Criar mundo → Tipo de mundo*; num servidor, `level-type=iceagesurvival:ice_age` no `server.properties`.

O preset conserva as cavernas, estruturas e dimensões vanilla, mas usa `minecraft:amplified` no overworld para gerar picos, encostas e vales bem mais dramáticos. A fonte de biomas embrulhada por `iceagesurvival:remapped` troca biomas quentes por `snowy_slopes`, `ice_spikes`, `frozen_peaks`, `snowy_plains` ou biomas de água congelada. Florestas densas, taigas e biomas tropicais não sobrevivem ao remapeamento; só grove mantém manchas de pinheiros. A combinação reduz os campos intermináveis de tundra, aumenta gelo e relevo e mantém toda a superfície abaixo do limite de neve. As tags de spawn também incluem as novas encostas para que a fauna continue presente.

A alternativa — listar os ~7 mil pontos climáticos do overworld num `multi_noise` só com biomas frios — daria um JSON de megabytes que precisaria ser regerado a cada versão; a troca é uma tabela curta.

**Toda a superfície neva** (temperatura ≤ 0,15, o limite de neve do vanilla; o gametest confere). Até 2026-10-01 taigas (0,25), taigas antigas, morros ventosos e costa de pedra (0,2) ficavam como estavam, "frias mas sem nevasca" — num mundo de teste eram mais da metade da superfície, que saía verde. As cavernas guardam a temperatura delas (lush caves 0,5): descer é se abrigar.

**Conteúdo do Revival desligado ([D22](#4-decisões)):** o biome modifier `remove_revival_features` tira os minérios (fóssil, âmbar, permafrost, rocha vulcânica) e a estátua moai; `data/fossil/worldgen/structure_set/` vazios desligam sítios de fóssil, poços de piche, templos astecas, academia egípcia e o barco do Nether; os itens dele saem das abas do criativo (`RevivalCleanup`). O TerraBlender só injeta o bioma vulcão em fontes `multi_noise` puras, então ele não aparece no preset (embrulhado pela nossa).

Nether e End são os do vanilla. Mundos já criados não mudam: o preset vale na criação. Relevo mais dramático (Tectonic) fica opcional e fora do escopo; o preset funciona com ele instalado, porque troca só os biomas.

## 19. Caverna

Estrutura subterrânea procedural (jigsaw) com entradas, túneis, salas, áreas profundas e caminho até a arena. Localizada por um rastreador temático próprio, usado repetidamente para triangular a direção. Etapa 10.

## 20. Boss

Giganotosaurus (ou superpredador equivalente): físico, biologicamente plausível, sem magia. Arena fechada, HP e dano altos, fases por mudança de comportamento. Deve exigir equipe de criaturas preparada. Etapa 10.

## 21. Multiplayer

Checklist por feature, antes de considerá-la pronta: single-player, servidor dedicado, persiste após relog, não duplica item, não perde entidade, sem erro no console, sem exploit óbvio, configurável, documentada.

Pontos de atenção: validação de dono em todo payload, montaria (autoridade de movimento), criatura inconsciente quando o chunk descarrega, implantes (duplicação).

### Testes

- **JUnit** (`./gradlew test`, roda no `build`): lógica pura de `core/`.
- **GameTest** (`./gradlew runGameTestServer`): comportamento em servidor real — carregamento da espécie, sorteio e aplicação de atributos, persistência de atributos e dono, tags e capabilities.
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

O mundo do GameTest é plano e de bioma temperado, então o frio não chega a subir lá: a curva de temperatura é coberta por JUnit e o que o gametest confere é a tubulação (tags carregadas, capability anexada e salva, criativo imune).

## 22. Performance

- Nada de varredura de entidades a cada tick; usar intervalos e resultados em cache.
- Manada com líder e raio limitado, não N×N.
- IA reduzida longe de jogadores.
- Estado de domesticação seguro a descarregamento de chunk (baseado em game time, não em contadores por tick).
- O frio roda a cada tick por jogador, mas só com aritmética: a leitura do mundo (que varre blocos procurando fonte de calor) acontece uma vez por segundo e fica em cache na capability.
- A reposição de fauna é uma varredura de entidades por jogador a cada 45 s, e uma só para todas as espécies (as contagens saem da mesma lista). A procura por posição não carrega chunk: coluna em chunk descarregado é descartada.

## 23. Roadmap

| Etapa | Conteúdo | Estado |
|---|---|---|
| 0 | Pesquisa e este documento | ✅ 2026-09-30 |
| 1 | Workspace, Gradle, build, runServer | ✅ 2026-09-30 (`runClient` ainda não verificado) |
| 2 | Core: níveis, atributos, ownership, persistência, registry de espécies | ✅ 2026-09-30 |
| 3 | Domesticação com criatura de teste | ✅ 2026-09-30 (falta conferir no cliente) |
| 4 | Smilodon | ✅ 2026-09-30, conferido em jogo pelo Felipe |
| 5 | Mais criaturas, spawning | 🟡 Estegossauro, Pteranodonte, dodô e Elasmotério (início de jogo), migração de herbívoros, bônus de bando do Alossauro e ecologia dinâmica (fome, caçadas, estresse, rivais — D24) implementados; build e GameTests passam, falta conferir comportamento em jogo |
| 6 | Temperatura | ✅ 2026-09-30 em testes automáticos; falta sentir o frio em jogo e balancear a primeira hora |
| 7 | Montaria | 🟡 armazenamento por espécie, voo no estilo do Cobblemon, pulo sem barra de carga, montaria sem sela e More Hitboxes implementados; build e GameTests passam, falta conferir controles e hitboxes em jogo |
| 8 | Reprodução e genética | ✅ 2026-09-30 em testes automáticos (genética em JUnit; acasalamento, gestação, ovo, incubadora, mesa química e estimulante em gametests); falta conferir em jogo |
| 9 | Worldgen | ✅ 2026-09-30 em teste automático (os biomas possíveis do preset são todos frios); falta criar um mundo e andar por ele |
| 10 | Endgame: rastreador, caverna, arena, boss | — |

MVP = Etapas 1–4 + versão mínima de 6, 7 e 9 (mundo frio, temperatura básica, montar o Smilodon).

## 24. Decisões técnicas

Ver a tabela em [4](#4-decisões). Registro de mudanças estruturais:

- 2026-10-01 — Ecologia dinâmica (D24): fome, caçada pelo faro (raio de 40–72 blocos), presa e
  manada que disparam, perseguição com fôlego, estresse com humor no painel, rivais (T-Rex ×
  Alossauro × Espinossauro, Smilodon × lobo-terrível × urso). Raios de alerta das presas dobrados.
  Carnívoros não caçavam herbívoros por três motivos: `large_prey` só tinha bichos do vanilla, o
  `NearestAttackableTargetGoal` fixava o alcance antes dos atributos da espécie, e a procura era
  rara e exigia vista.
- 2026-10-01 — Spawn: o `Animal` só nascia em grama ou na luz, então à noite, na neve, a reposição
  e boa parte do spawn da geração falhavam (`checkSpawnRules` agora fica com `checkSurfaceSpawnRules`).
  Reposição escolhe a posição e depois as espécies do bioma dela, a cada 20 s, com rajada de 3
  grupos em região vazia; teto 18 → 40; pesos e manadas maiores.
- 2026-10-01 — Golpe montado proporcional ao corpo (`MountedReach`): alcance 3,5 + 1,25 × largura,
  cone de 140° que pega o bicho baixo; quebra de blocos mais funda e larga.
- 2026-10-01 — Pteranodonte (D25): rasante, curva limitada e assento no `rider_pos`; `seat_forward`
  voltou a valer (perdido no port).
- 2026-10-01 — GameTests estáveis: bichos que fugiam para fora da arena invadiam cenas seguintes;
  cenas de movimento ganharam lote próprio e limpam os soltos ao começar.

- 2026-10-01 — Crash ao renascer: o corpo antigo do jogador recebe um último tick depois que o Forge
  invalida suas capabilities, e o `ColdExposure` exigia a de frio. O tick agora ignora jogador sem
  capability, e o `LazyOptional` do frio deixou de ser invalidado por listener (impedia o
  `reviveCaps()`); voltar do End mantém o frio, morrer zera.

- 2026-10-01 — Fauna com "lutar ou fugir" (`core/ecology/ThreatResponse`, bloco `behavior.wariness`):
  diante de jogador ou de `#iceagesurvival:predators`, o herbívoro encara, se afasta, blefa, investe
  ou foge; pego de surpresa ou guardando filhote, investe sem blefar; agachado, o jogador é notado de
  mais perto. Elasmotério passou a solitário como o rinoceronte (às vezes com filhote ou casal +
  filhote, bloco `spawn.family`); dodô só foge; mamute, Estegossauro e Brontossauro blefam e investem.
  Filhotes seguem a mãe e predador fica saciado depois de abater (`sated_seconds`). Tags vanilla
  voltaram às pastas `tags/blocks` e `tags/items` do 1.20.1 — a flecha tranquilizante não saía do arco.

- 2026-10-01 — Correções do primeiro teste em jogo do port Forge: ataque do T-Rex preso em loop
  (animação do Revival com `loop: true`; agora `PLAY_ONCE`), tela de status (V) e inventários
  transparentes (no 1.20.1 o `renderBackground` é chamado pela própria tela), barra de carga de pulo
  do cavalo removida (D23), grandes sem pulo e bote horizontal do Smilodon, voo do Pteranodonte no
  estilo do Cobblemon (`FlightModel`), preset Era do Gelo sem taiga/costa de pedra verdes e conteúdo do
  Revival desligado (D22). Dodô e Elasmotério como criaturas de início de jogo; roupa de pena entre o
  couro e a pele; Elasmotério monta sem sela, carrega 9 itens e aquece. Teto total da fauna 10 → 18.

- 2026-10-01 — Nova política de assets: Revival e More Hitboxes são dependências externas; seus
  arquivos não são copiados ao repositório nem ao jar. O projeto migrou para Forge 1.20.1:
  Estegossauro com inventário de 27 slots, Brontossauro com 216, Pteranodonte com voo montado,
  hitboxes multipartes e viagens periódicas de manadas herbívoras. Alossauro iguala a velocidade
  do Smilodon, supera o T-Rex e recebe bônus de dano/velocidade em bando. Build e GameTests passam;
  falta conferir voo, montarias, hitboxes e balanceamento em jogo.
- 2026-10-01 — Corrigidos recursos e testes Forge 1.20.1: fixtures SNBT do GameTest, caminhos e
  formatos de datapack, persistência do frio, dimensões dos filhotes, combustível via ForgeHooks
  e isolamento dos mocks de jogador. `./gradlew build` e `./gradlew runGameTestServer` passam.
- 2026-10-01 — D21 substitui a decisão anterior de incluir assets do Revival no repositório: as
  espécies resolvem os recursos do mod original em runtime; os 84 arquivos extraídos foram removidos.
- 2026-10-01 — Direbear: marcha plantígrada original em quatro tempos, com transferência de peso
  do tronco e apoios alternados; rosto ganhou focinho em três volumes e sobrancelhas próprias.
- 2026-10-01 — Rig original do urso-terrível passou de 10 para 21 ossos: patas com segmentos,
  orelhas e cauda articuladas; animações próprias de respiração, marcha e golpe de duas patas.
- 2026-10-01 — Urso-terrível integrado pela receita de espécie terrestre: modelo original, predador
  solitário com território de 64 blocos, spawn raro nos biomas frios do Smilodon e montaria.
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
- 2026-09-30 — Zonas de perigo por distância do spawn, teto total de população, presas por espécie (T-Rex caça bronto desgarrado, matilha caça mamute, bando chama o bando); Carnotauro vira Alossauro.
- 2026-09-30 — Etapa 9: world preset Era do Gelo com `RemappedBiomeSource` (D6 fechada, sem mod de worldgen).
- 2026-09-30 — Etapa 8: genética, sexo, acasalamento, gestação e ovo, incubadora, mesa química e estimulante.
- 2026-09-30 — Comandos no esquema do ARK: movimento e postura independentes, assobios por tecla (mirada ou todas ao alcance), painel sob a mira refeito e tela de status (V).
- 2026-09-30 — Mamute em 1,5x (colisão 3,0×4,65, escala 1,92, assento 3,85, degrau 1,65) e coletor de madeira pela mordida montada.
- 2026-09-30 — Tyrannosaurus reduzido de 2x para 1,5x: colisão 2,7×5,4, escala do modelo 4,25, assento 5,6 (osso `rider_pos` × escala), degrau 2,1.
- 2026-10-01 — O preset Era do Gelo troca `minecraft:overworld` por `minecraft:amplified` no overworld e remapeia biomas planos/florestais para encostas nevadas e ice spikes. GameTest amostra três sementes e exige planícies nevadas ≤35%, taiga ≤15% e encostas/picos/gelo ≥35% da terra.
- 2026-10-01 — População inicial reforçada a pedido do Felipe: reposição sobe para 5 grupos a cada 8 s enquanto a região estiver vazia e o teto total por jogador vai de 40 para 72. Elasmotérios têm peso e teto maiores. Velociraptores passam a nascer em tundra aberta e picos de gelo, inclusive no spawn, em bandos de 4–6; enxergam e caçam mais cedo, perseguem por 30 s como o Alossauro, recebem o mesmo bônus de bando e incluem o Elasmotério solitário entre as presas. O limiar de tamanho por caçador foi ajustado levemente para que um bando mínimo de quatro consiga abater Elasmotério. Ice Age - Frozen World foi avaliado, mas não pode ser instalado: para 1.20.1 há apenas Fabric, incompatível com Forge 47.4.3.

## 25. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| **Assets de criaturas.** Parte dos modelos usa recursos externos do F&A Revival em runtime. | Médio — jogador precisa instalar a versão Forge compatível do Revival; mudanças internas no mod upstream podem renomear recursos e quebrar a aparência. Os assets do Revival não são empacotados nem distribuídos pelo addon. | Fixar e documentar a versão compatível, testar com Revival instalado e substituir gradualmente por arte original. |
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
4. ~~World preset próprio vs. conversão global no estilo Primal Winter~~ Decidido na Etapa 9: preset próprio (D6).
5. Integração com criaturas de mods externos: possível em tese (registrar uma espécie apontando para um `EntityType` alheio), mas exigiria anexar nossos dados a entidades de terceiros. Não planejado.
6. Criaturas voadoras e de carga: quais espécies.
7. ~~Gestação vs. ovo por espécie~~ Decidido na Etapa 8: `breeding.offspring` no JSON — mamíferos `live`, dinossauros `egg`.
8. Nome final do mod (`Ice Age Survival` / id `iceagesurvival` são provisórios).
10. ~~Muda da árvore de fruta-negra~~ Feito em 2026-09-30: `black_fruit_sapling`, que as folhas dropam como as do vanilla (5%, mais com Fortuna; não com tesoura) e cresce na mesma árvore do mundo.
12. Como tirar a sela em jogo (hoje só `/ias saddle` ou a morte da criatura): tecla, tela de inventário da criatura, ou clique com a mão vazia agachado.
13. ~~Atacar montado~~ Feito em 2026-09-30 (clique de ataque = mordida da montaria).
14. ~~Tyrannosaurus montável?~~ Decidido: sim (2026-09-30). Bloco `mount` em `tyrannosaurus.json`: assento 5,6 (osso `rider_pos` do modelo × escala), afinidade 25, velocidade ×0,8 (≈ Smilodon), pulo 0,45.
11. Mods de fauna só no CurseForge (ex.: Primal Era) — não verificados.
