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
| D5 | Temperatura: **sistema interno leve**, atrás de uma interface | Frostiful no NeoForge é alpha e exige Forgified Fabric API; Cold Sweat é estável mas muito mais complexo do que o brief pede ("secundária, sem burocracia"). Ver [15](#15-temperatura). | Provisória — reavaliar na Etapa 6 |
| D6 | Mundo glacial por **world preset próprio em datapack**, sem mod de worldgen obrigatório | Um preset com a fonte de biomas restrita a biomas frios resolve "mundo predominantemente congelado" sem dependência. Ver [18](#18-worldgen). | Provisória — reavaliar na Etapa 9 |
| D7 | Espécies num **registry de datapack** sincronizado | Permite adicionar espécie por JSON, com validação por Codec e sync automático para o cliente. | Fechada |
| D8 | Dados da criatura no **NBT da própria entidade**, serializados por Codec | As entidades são nossas; não precisamos de attachments para anexar dados a entidades alheias. | Fechada |
| D9 | IA com **Goals vanilla** | Suficiente para território e manada; SmartBrainLib fica como opção se os Goals virarem gargalo. | Provisória |
| D10 | Lógica pura (stats, genética, torpor) **sem dependência de classes do Minecraft** | Permite testes JUnit rápidos, sem subir o jogo. | Fechada |
| D11 | Repositório GitHub **privado** | Pode ser aberto depois; publicar é irreversível. | Fechada até o Felipe decidir o contrário |

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
- **Assets:** todo modelo, textura, animação e som precisa ser original ou de licença compatível e registrada aqui. Nenhum asset de mod All Rights Reserved entra no repositório.

## 10. Arquitetura

```
dev.madebyfelipe.iceagesurvival
├── IceAgeSurvival          entrypoint, registro nos event buses
├── core/                   lógica pura, sem classes do Minecraft (testável por JUnit)
│   ├── stats/              atributos, escala por nível
│   ├── genetics/           genoma, herança 50/50, mutações
│   └── taming/             torpor, eficiência, afinidade
├── species/                definição de espécie (Codec) + registry de datapack
├── entity/                 classe base de criatura, goals, dados sincronizados
├── item/                   flechas tranquilizantes, rifle, implante, rastreador
├── network/                payloads (comandos, UI)
├── registry/               DeferredRegisters
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

Nenhuma implementada. Ordem planejada:

1. **Criatura de teste** (Etapa 3) — valida torpor e domesticação.
2. **Smilodon** (Etapa 4) — predador territorial, montável.
3. Lote seguinte (Etapa 5), a definir: um herbívoro de manada (Mamute), um pequeno de início de jogo, um dinossauro.

Lista-alvo do brief — Era do Gelo: mamute-lanoso, smilodon, lobo-terrível, rinoceronte-lanoso, megaloceros, megatherium, urso-das-cavernas, bisão, auroque, mastodonte. Dinossauros: tyrannosaurus, triceratops, velociraptor, ankylosaurus, spinosaurus, giganotosaurus (boss).

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

Fluxo: enfraquecer → aplicar torpor → inconsciente → alimentar → esperar e proteger → domesticada.

- Alimentos por espécie, configurados no JSON (preferido / aceito / recusado), com multiplicador de eficiência.
- Eficiência de domesticação cai se a criatura tomar dano enquanto inconsciente.
- Detalhes numéricos: a definir na Etapa 3.

## 14. Torpor

- Valor separado da vida; máximo escala com o nível.
- Sobe com projéteis tranquilizantes; decai com o tempo.
- Ao atingir o máximo, a criatura fica inconsciente até o torpor zerar ou a domesticação concluir.
- Escala das ferramentas: arco < besta < rifle.

## 15. Temperatura

Mecânica secundária. A pergunta do jogador é *"tenho recursos para essa viagem?"*.

Desenho provisório (D5): um único valor de exposição ao frio por jogador, no servidor. Sobe conforme bioma, altitude, tempestade e noite; desce perto de fonte de calor, em abrigo e com isolamento da roupa. Reaproveitar o congelamento vanilla (o efeito de powder snow: HUD, tremor, dano) como feedback, sem HUD novo nem menu.

Isolada atrás de uma interface para que Cold Sweat possa substituí-la se o sistema interno se mostrar insuficiente.

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

## 22. Performance

- Nada de varredura de entidades a cada tick; usar intervalos e resultados em cache.
- Manada com líder e raio limitado, não N×N.
- IA reduzida longe de jogadores.
- Estado de domesticação seguro a descarregamento de chunk (baseado em game time, não em contadores por tick).

## 23. Roadmap

| Etapa | Conteúdo | Estado |
|---|---|---|
| 0 | Pesquisa e este documento | ✅ 2026-09-30 |
| 1 | Workspace, Gradle, build, runClient/runServer | em andamento |
| 2 | Core: níveis, atributos, ownership, persistência, registry de espécies, networking | — |
| 3 | Domesticação com criatura de teste | — |
| 4 | Smilodon | — |
| 5 | Mais criaturas, spawning | — |
| 6 | Temperatura | — |
| 7 | Montaria | — |
| 8 | Reprodução e genética | — |
| 9 | Worldgen | — |
| 10 | Endgame: rastreador, caverna, arena, boss | — |

MVP = Etapas 1–4 + versão mínima de 6, 7 e 9 (mundo frio, temperatura básica, montar o Smilodon).

## 24. Decisões técnicas

Ver a tabela em [4](#4-decisões). Registro de mudanças estruturais:

- 2026-09-30 — Documento criado; D1–D11 registradas.

## 25. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| **Assets de criaturas.** Não há artista no projeto e nenhum asset externo é reutilizável. | Alto — criaturas são o coração do mod. | Modelos simples no estilo vanilla; definir cedo quem produz os assets. |
| Escopo: dez etapas, vários sistemas grandes. | Alto | MVP estreito; não avançar com etapa instável. |
| Balanceamento de torpor/níveis/genética. | Médio | Tudo em dados e config; testes de lógica pura. |
| Montaria em multiplayer (latência, dessincronização). | Médio | Reaproveitar o modelo de controle de veículo vanilla. |
| Muitas entidades com IA em servidor. | Médio | Ver [22](#22-performance); medir na Etapa 5. |
| 1.21.1 envelhecer. | Baixo | `core/` independente do Minecraft facilita port. |

## 26. Ainda não decidido

1. **Licença do nosso código** e se o repositório será público.
2. **Quem produz modelos, texturas e animações** (e em que ferramenta — Blockbench é o padrão para GeckoLib).
3. Temperatura interna vs. Cold Sweat — decidir na Etapa 6 (D5).
4. World preset próprio vs. conversão global no estilo Primal Winter — decidir na Etapa 9 (D6).
5. Integração com criaturas de mods externos: possível em tese (registrar uma espécie apontando para um `EntityType` alheio), mas exigiria anexar nossos dados a entidades de terceiros. Não planejado.
6. Criaturas voadoras e de carga: quais espécies.
7. Gestação vs. ovo por espécie.
8. Nome final do mod (`Ice Age Survival` / id `iceagesurvival` são provisórios).
9. Mods de fauna só no CurseForge (ex.: Primal Era) — não verificados.
