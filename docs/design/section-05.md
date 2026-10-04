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

### TerraFirmaCraft (2026-10-02)

| Mod | Licença | Forge 1.20.1 | Observação |
|---|---|---|---|
| TerraFirmaCraft | EUPL-1.2 | 3.2.26 (2026-09) | Reformulação total: gerador próprio (30 biomas só de relevo; clima por posição, −20 a 30 °C), sem temperatura do jogador, desliga ~230 receitas vanilla, fauna própria (inclui sabertooth e direwolf), exige Patchouli. Integrado como opcional ([D33](section-04.md)). |

### Animação / IA / infraestrutura

| Mod | Licença | 1.21.1 NeoForge | Observação |
|---|---|---|---|
| GeckoLib | **MIT** | 4.9.3 — release (2026-09) | 71 milhões de downloads, 27 builds para 1.21.1. |
| SmartBrainLib | MPL-2.0 | 1.16.11 — release (2025-10) | IA baseada em Brain. |
| Citadel | LGPL-3.0 | 2.7.1 — release | Biblioteca dos mods do Alex; não precisamos. |

Não existe biblioteca de montaria ou de breeding/genética reutilizável para 1.21.1/NeoForge; o que aparece são mods pequenos de conteúdo (Animal Husbandry, ARR). Ambos os sistemas serão nossos.

