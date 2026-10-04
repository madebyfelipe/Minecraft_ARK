## 24. Decisões técnicas

Ver a tabela em [4](section-04.md). Registro de mudanças estruturais:

- 2026-10-04 — Processo compartilhado para o teste do Minecraft, a pedido do Felipe:
  Codex coordena/integra (modelo configurado), Claude trabalho executa (`claude-opus-5-5`);
  estado local em SQLite com reservas atômicas, sem roubo por expiração; painel local
  inicia sessões reais por CLI e entrega pacote autocontido. Entradas `AGENTS.md` e
  `CLAUDE.md` curtas, `CLOUD.md` como índice de 26 seções preservadas. Leitura seletiva
  de decisões com `tools/project_context.py`. Cópia no Sites não sincroniza o estado.
  Regra técnica antiga de rede atualizada para o `SimpleChannel` do Forge instalado.
  Validação: build e 388 GameTests passaram; testes do protocolo/painel e duas
  sessões reais estritamente de leitura (Claude trabalho e Codex) verificaram entrada
  seletiva, reserva, heartbeat e handoff. Leitura inicial de 155.206 para 5.251 bytes
  (96,6% menos texto, não uma medição de tokens).

- 2026-10-01 — Worldgen restaurado ao v1 da Etapa 9: o preset Era do Gelo volta ao relevo
  `minecraft:overworld` e ao mapeamento original de biomas frios, removendo a reformulação de
  tundra aberta e relevo `amplified`. TerraBlender continua somente como dependência transitiva do
  Revival, não participa do nosso worldgen.
- 2026-10-01 — Worldgen simplificado: toda a terra do preset Era do Gelo agora é planície nevada;
  rios, praias e oceanos continuam congelados. Herbívoros reagem aos predadores pelo porte (investem
  ou fogem) e todos os carnívoros fazem a mesma escolha diante de outro predador: enfrentam os do
  próprio porte ou menores e fogem de um muito maior.
- 2026-10-01 — Garantia contra relevo deformado: o GameTest exige que o preset use
  `NoiseGeneratorSettings.OVERWORLD`; mundos criados quando o preset usava `amplified` continuam
  com aquele relevo salvo e precisam ser recriados para receber a correção.
- 2026-10-01 — Ecologia dinâmica (D24): fome, caçada pelo faro (raio de 40–72 blocos), presa e
  manada que disparam, perseguição com fôlego, estresse com humor no painel, disputa territorial
  entre machos da mesma espécie. Raios de alerta das presas dobrados.
  Carnívoros não caçavam herbívoros por três motivos: `large_prey` só tinha bichos do vanilla, o
  `NearestAttackableTargetGoal` fixava o alcance antes dos atributos da espécie, e a procura era
  rara e exigia vista.
- 2026-10-01 — Interações ecológicas com jogadores e ameaças ativas: predadores só iniciam caça
  ao jogador em sobrevivência quando estão no estado `HUNTING`; saciedade e caça oportunista não
  os fazem perseguir jogadores. Criaturas selvagens capazes de lutar passam a mirar quem as feriu
  imediatamente, exceto quando inconscientes ou atingidas por jogador criativo/espectador.
  Herbívoros cautelosos reconhecem outra criatura selvagem agressiva como ameaça mesmo sem tag de
  predador. GameTests também isolam jogadores simulados que sobraram de cenas anteriores.
- 2026-10-01 — Encontros de presa–caçador sem rivalidade entre espécies: todo herbívoro com perfil
  de cautela reconhece predadores selvagens pelo papel de caça, mesmo quando o predador está
  saciado. A resposta compara porte e número de atacantes/defensores: vantagem permite blefar ou
  investir para afastar caçadores menores; desvantagem causa fuga. Rivalidade foi restrita a machos
  adultos de bandos diferentes da mesma espécie competindo por espaço; membros da própria manada
  não disputam entre si. Removidas as tags de rivais entre espécies; a disputa não depende de uma
  fêmea próxima.
- 2026-10-01 — Zona inicial e confronto herbívoro × carnívoro: Velociraptor continua só a partir de 300
  blocos e o Smilodon (solitário ou casal, peso 4, máx. 2 por perto) passa a nascer também na planície
  nevada e nos picos de gelo; um T-Rex garantido mora a 220–297 blocos do spawn; T-Rex natural e
  urso-terrível a 300+, Utahraptor 350+, Alossauro e Espinossauro 400+; nível selvagem máximo a 300
  blocos (antes 3.000). Carnívoros não cediam a herbívoros porque só viam o herbívoro como ameaça
  enquanto ele estava agressivo (durante a investida): bufar e encarar não contava, e ao fim da
  investida o predador parava de recuar e voltava à presa; o Smilodon, ainda, espreitava com a mesma
  prioridade do `WaryGoal` e nem reagia. Agora quem encara de perto, blefa ou investe avisa o outro
  (`intimidatedBy`, 3 s), e o predador decide por `ThreatResponse.deters`: cede a quem tem o dobro
  da força dele (porte × manada ÷ bando) mesmo com fome; em forças parecidas, só o saciado e sem alvo
  cede a uma investida. A espreita passou para baixo do `WaryGoal`. Os dois lados usam a mesma conta:
  porte relativo (`ThreatResponse.sizeRatio`, antes copiado em dois goals) e grupo
  (`fightingGroup`, raio mínimo de 16 blocos — com o raio de manada de 10 o bando de raptores se
  partia e a decisão virava de lado entre ticks). O porte individual sozinho não decide mais contra
  quem não caça: cada raptor do bando fugia do Elasmotério. A ameaça avisada pela manada agora passa
  por `isThreat` antes de valer. Contrato em `ConfrontationTableTest` (portes reais por par de
  espécies). GameTest do Brontossauro em lote próprio: solto na arena comum, ele afastava os
  predadores das cenas vizinhas. Calor do Elasmotério e mordida montada limpam a arena antes de
  medir: bichos soltos de cenas vizinhas davam calor máximo e roubavam a mordida (instáveis já na
  `main`). Suíte conferida em 4 rodadas seguidas.
- 2026-10-01 — Defesa durante a caça: o WaryGoal agora pode responder a uma investida ativa mesmo
  quando o predador já tem presa-alvo. Se a reação for recuo/fuga, limpa o alvo para que a caçada
  termine e o carnívoro se afaste; GameTest reproduz Utahraptor caçando enquanto um Mamute o expulsa.
- 2026-10-01 — Spawn: o `Animal` só nascia em grama ou na luz, então à noite, na neve, a reposição
  e boa parte do spawn da geração falhavam (`checkSpawnRules` agora fica com `checkSurfaceSpawnRules`).
  Reposição escolhe a posição e depois as espécies do bioma dela, a cada 20 s, com rajada de 3
  grupos em região vazia; teto 18 → 40; pesos e manadas maiores.
- 2026-10-01 — Golpe montado proporcional ao corpo (`MountedReach`): alcance 3,5 + 1,25 × largura,
  cone de 140° que pega o bicho baixo; quebra de blocos mais funda e larga.
- 2026-10-01 — Pteranodonte (D25): rasante, curva limitada e assento no `rider_pos`; `seat_forward`
  voltou a valer (perdido no port).
- 2026-10-01 — Densidade e zona inicial: teto de fauna selvagem 72 → 50 (~30% menor), pesos dos
  biome modifiers reduzidos na mesma proporção quando os inteiros permitem; Smilodon solitário fica
  no spawn inicial, enquanto Velociraptor e Pteranodonte começam a 300 blocos. Competição
  territorial entre machos da mesma espécie não exige fêmea próxima.
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
- 2026-09-30 — Etapa 7: montaria (D18, D19), reposição de fauna (D20) e comandos de teste `/ias`. A fauna que não voltava num mundo já explorado era a categoria `CREATURE` do vanilla, não os biome modifiers; ver [11](section-11.md).
- 2026-09-30 — `CLAUDE.md` criado: Sonnet 5.5 para espécies e ajustes pela receita, Opus 5.5 para as etapas 7–10; trabalho grande dividido em subagentes, um por função.
- 2026-09-30 — Primeiro dinossauro: `tyrannosaurus`, pela receita de espécie terrestre (sem classe nova).
  Spawn em biomas frios por ora (D6 ainda aberta). **Não é montável**: quando ele foi feito o codec
  `Species` ainda não tinha bloco `mount`; agora tem, e deixá-lo de fora passou a ser escolha de
  balanceamento à espera do Felipe — ver [26](section-26.md).
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
- 2026-10-01 — Velociraptor próximo do real: porte de peru grande (0,6 × 0,9), sozinho ou em par, espreita bicho pequeno (coelho, galinha, raposa, sapo, dodô), **não caça gente** (`behavior.hunts_players: false`) mas reage a quem chega perto (alerta, ameaça, recua, morde encurralado); peso 5 e teto 4 por jogador (antes 16/14, superpopulação). Nenhum predador nasce já faminto (fome inicial até 90%, antes 110%: um em sete nascia caçando o jogador). Predadores caçam aldeões e mercadores (`large_prey`) e o golem de ferro ataca predador selvagem (`VillageInteractions`). Lobo-terrível desligado (tag `disabled`: não nasce, sem ovo na aba, o selvagem que sobrou some; domesticados ficam). Pteranodonte não "anda no ar" (o cliente desligava o voo selvagem, que só o servidor conhece). Cura com comida: ferida, a domesticada aceita a comida da espécie a cada meio segundo e cada porção cura de 3% a 25% da vida (a melhor comida, 20%); come sozinha do inventário a cada 5 s; afinidade só no ritmo normal.
- 2026-10-01 — Modo Era do Gelo como "DLC": criaturas em mundo normal (tags de bioma estendidas) e frio/termômetro só no preset Era do Gelo ou com `iceAgeMode=ON` (§15).
- 2026-10-01 — Tricerátopo (Revival em runtime, escala 3,0, hitbox própria).
- 2026-10-01 — Galimimo (Revival em runtime, hitbox própria). Estegossauro não aparecia: só a 300+ blocos e com peso 6, perdia as vagas do teto para dodôs, raptores e Pteranodontes; agora nasce desde o spawn com peso 12 (9 na geração do terreno).
- 2026-10-01 — Interface: aviso piscando sob a barra de domesticação quando a criatura desmaiada fica sem comida no inventário (`DATA_NEEDS_TAMING_FOOD`); menu "Localizar criatura" (tecla O) com as domesticadas do jogador, da mais perto à mais longe, com nível, seta, distância e coordenadas — a posição vem de `CreatureLocator` (dados do Overworld, gravada a cada 5 s e ao descarregar, então acha criatura em chunk descarregado); "Localizar" liga uma bússola acima da barra de itens e faz a criatura brilhar 15 s se estiver carregada. Plugin opcional do Jade (`compat/JadeCompat`, `compileOnly`) esconde a caixa dele sobre as nossas criaturas, que já têm painel próprio. A reposição de fauna não roda no servidor de GameTest (o Pteranodonte em qualquer bioma invadia as cenas) e três cenas de caça ganharam lote próprio.
- 2026-10-01 — Todo carnívoro (quem come carne de vaca) aceita carne de dodô: a crua junto da galinha (no Espinossauro, da carne vermelha) e a assada junto das carnes cozidas.
- 2026-10-01 — Ajustes do Felipe: dodô lento (metade do passo) e manso como o real, dropando pena comum; roupa de pena feita com pena comum e o peitoral vira "Casaco de Penas" (isolamento 0,4, o dobro das outras peças; conjunto segue 1,0); flecha tranquilizante também por narcótico + pena + graveto (a de flecha + narcótico continua); `spawn.spacing` (um grupo por região) com 300 blocos para Brontossauro e T-Rex; Pteranodonte em qualquer bioma, desde o spawn, abundante, e com voo selvagem (`WildFlightGoal`); prévia do modelo no painel de status com os pés no fundo do quadro, escala pela altura e pelo comprimento e recorte.
- 2026-10-01 — População inicial reforçada a pedido do Felipe: reposição sobe para 5 grupos a cada 8 s enquanto a região estiver vazia e o teto total por jogador vai de 40 para 72. Elasmotérios têm peso e teto maiores. Velociraptores passam a nascer em tundra aberta e picos de gelo, inclusive no spawn, em bandos de 4–6; enxergam e caçam mais cedo, perseguem por 30 s como o Alossauro, recebem o mesmo bônus de bando e incluem o Elasmotério solitário entre as presas. O limiar de tamanho por caçador foi ajustado levemente para que um bando mínimo de quatro consiga abater Elasmotério. Ice Age - Frozen World foi avaliado, mas não pode ser instalado: para 1.20.1 há apenas Fabric, incompatível com Forge 47.4.3.
- 2026-10-01 — Bandos e território (D26): bando com id e limite, fome do bando, guerra entre bandos da mesma espécie, território contra qualquer criatura do mod, jogador na tabela de dieta, ronda do carnívoro faminto, menos fauna por jogador. CLAUDE.md: todo o trabalho com Opus 5.5.
- 2026-10-01 — Espreita (D27): todo carnívoro acompanha a manada sem ser notado (percebido a 50% do alerta) e dispara por oportunidade ou depois de 20–40 s, com o bando junto; faro de caça sempre ≥ alerta das presas + 8 (`Perception`, urso-terrível 40 → 44). Espinossauro nasce sobretudo na beira d'água (peso 3), mas também em planícies e taigas (peso 1), com o novo `spawn.favored`. Na perseguição, a presa que abre 30 blocos escapa (antes 1,5× o faro: até 108 blocos no T-Rex).
- 2026-10-01 — Documentação: `docs/ecologia.html` reescrito (bandos, território, espreita, faro, 30 blocos, jogador na dieta) e `docs/comportamento.html` novo, uma ficha por espécie gerada dos JSON por `tools/gen_comportamento.py` (rodar de novo quando um JSON de espécie mudar). Versão 0.3.0 (alfa).
- 2026-10-01 — Spawn dos predadores: no preset Era do Gelo todo bioma de terra vira planície nevada, e o Utahraptor e o urso-terrível não a tinham na tag de spawn — nunca nasciam lá (incluída; GameTest `everySpeciesCanSpawnInTheIceAge` exige a planície nevada de toda espécie que nasce). E os herbívoros, com pesos e manadas maiores, enchiam o teto de 28: agora ocupam no máximo 3/4 dele (21) e o resto só carnívoro ocupa (`WildSpawnRules.HERBIVORE_SHARE`). Na simulação da reposição numa planície nevada a 600 blocos, o Smilodon passa de 40% para 85% das regiões e o Utahraptor de 0% para 81%. Modpack `.mrpack` para o Prism (`tools/build_mrpack.py`, mods pelo Modrinth).
- 2026-10-02 — Pacote de criaturas: população 28 → 36 por jogador (27 herbívoros, 9 carnívoros); força e porte pelo animal real (Smilodon, Estegossauro, Tricerátopo maiores; D28); bônus de bando em todo predador de bando; sexo ♂/♀ no painel de mira e ao lado do nome; slot de sela no inventário da criatura; bico do Pteranodonte segue o voo e fôlego de voo por nível (D31); dardo sedativo, rifle e besta de dardos (D31); corpo, implante e mesa de reviver (D29); estação de preparação (carne → podre, carne + açúcar → charque, que entra na domesticação dos carnívoros); Kelenken com a bicada, Smilodon com a emboscada e Utahraptor com o salto (D28); troféu de apex e desafio para T-Rex e Espinossauro (D30). Corrigida a corrida em que a manada se sabia caçada antes do bote de quem espreita. Versão 0.4.0.
- 2026-10-02 — Ajustes do Felipe (D32): T-Rex vencido deixa de ser atacado pelas criaturas do desafiante; tributo com 5 s de rugido antes da luta, e o apex vai nas criaturas primeiro; o corpo da domesticada deixa de ser alvo dos selvagens; fraquezas — Smilodon selvagem com pavor de água, Utahraptor sem quebrar folhas, Kelenken com o bico travado no escudo; Tricerátopo, Estegossauro e Kelenken maiores pelo status; rifle 200 de torpor a cada 2,5 s e besta a cada 2 s; modelos 3D do rifle e da besta e sprite novo do dardo. GameTests de arena sensíveis a chunk ganharam `setupTicks` (a arena podia cruzar um chunk ainda sem entidades).
- 2026-10-02 — TerraFirmaCraft opcional (D33): preset Era do Gelo com o gerador do TFC a −10 °C, frio pelo clima do TFC, biomas primitivos com peso dobrado no mundo padrão (mixin), spawn por relevo, sabertooth/direwolf do TFC removidos, presas, comida e receitas com os equivalentes do TFC; TFC e Patchouli no modpack.
- 2026-10-02 — Ornitholestes (D34): pequeno terópode noturno da zona inicial, arisco, camuflado, com o agarrão, que come carniça e, domesticado, é sentinela. Os hábitos são genéricos (`behavior.habits`): primeiro horário de atividade da fauna. Presa nova do lobo-terrível (pela `small_prey`), do Velociraptor e da Kelenken.
- 2026-10-02 — Baryonyx e novas frentes (D35–D40): Jurassic Reborn e Prehistoric Fauna como fontes de assets em runtime com o conteúdo deles desligado por dados; renderer de poses Tabula e gesto sincronizado para modelos que não são GeckoLib; Baryonyx pescador, anfíbio, com a garra-gancho e montaria que nada; estações do Primal Stage (grelha, forno de olaria, varal de charque, tora de corte, bigorna de pedra); rifle hitscan com traçante e armas sem o balanço do clique; muros, portões, armadilhas e cobertura de fosso com dono, madeira que cede aos gigantes e as bancadas de Construção e de Armeiro. O bote agora tira da espreita o bando inteiro que ronda a mesma manada, não só quem mira o mesmo animal (o `aSpottedPackPounces` falhava quando os dois Alossauros escolhiam Galimimos diferentes). Ainda intermitentes, uma ou duas falhas em seis rodadas: `tyrannosaurusHuntsThroughTheForest` (o T-Rex perde o jogador de vista na mata), `aHungryWildBaryonyxGoesFishing` e `stegosaurusDrivesOffSatedVelociraptor`.
- 2026-10-03 — Spawn em todo bioma com o ideal ×3 (D41) e Quetzalcoatlus (D42). Baryonyx deita uma vez e fica deitado (o SLEEPING do JR é o ato de deitar); Pteranodonte domado com peixe. A Megalania tem a base pronta (peçonha, `VenomBite`, `AntidoteItem`), mas o tipo, o ovo e o antídoto saíram do registro até a frente fechar (sem JSON de espécie, 4 GameTests falhavam). `tools/gen_comportamento.py` entende o spawn em todo bioma e o ideal, a dieta por tag, a garra-gancho, o engolir e a pesca. Release por tag: `.github/workflows/release.yml` compila, monta o `.mrpack` e publica o release com as notas de `docs/releases/<tag>.md`. Versão 0.5.0.
- 2026-10-03 — Quetzalcoatlus três vezes maior, a pedido do Felipe ("minúsculo" em jogo): o modelo do Revival ia na escala 1,2 — menor que a do próprio Revival (1,75) e que o nosso Pteranodonte (2,25). Agora 3,6, com o assento a 6,3 e as hitboxes do More Hitboxes ×3. A caixa de colisão fica 2,2 × 4,5: com a do Revival na nova escala (3,6 × 7,2) o porte das regras de ecologia iria de 7,8 a 20,6, quase o dobro do T-Rex (11,6), e o Quetzal passaria a intimidá-lo. Ele também estava sem nenhuma animação (o arquivo do Revival dele não usa o prefixo `animation.quetzalcoatlus.`; `CreatureAppearance.UNPREFIXED_ANIMATIONS`) e o ovo gerador sem modelo de item; `AssetReferenceTests` confere as duas coisas para toda espécie. Release 0.5.1 pelo workflow (push na `main` com `docs/releases/v0.5.1.md`). `runGameTestServer` cria o mundo de teste plano e sem estruturas num checkout novo.
- 2026-10-03 — Megalania (D43), carcaça (D45) e ajustes de IA (D46): a Megalania nasce (peçonha com choque nas criaturas do mod e sangramento sem regeneração no jogador, antídoto na mesa química, mordida que solta e rastro); presa grande morta sem jogador vira carcaça com porções, o mais forte come primeiro e o jogador carneia a sobra; Ornitholestes e Quetzal dormindo só acordam feridos; a cautela encara a ameaça maior (o Tricerátopo atrás do jogador, não o jogador); o T-Rex volta a pegar presa fácil de manada e o último recurso; o spawn sorteia com diversidade e põe os voadores num teto próprio; Pteranodonte pescador em todo bioma; modpack sem TerraFirmaCraft; quebra de blocos por espécie (D44: madeira a golpes só para alcançar o alvo, muro 6/3, madeira vanilla 3/2, tronco ao esbarrar só para gigante). Pesquisa: Pteranodonte (88–80,5 Ma, mar interior) e Quetzalcoatlus (68–66 Ma, Texas) nunca conviveram — sem rivalidade nem predação documentada; ficam em nichos separados (o Ptero pesca, o Quetzal caça no chão). Versão 0.6.0.
- 2026-10-03 — Etapa 10 (D47): caverna da arena gerada por código em anéis concêntricos (4 por mundo, ~1650–3400 blocos do spawn), rastreador de troféus jogado como o Olho do Ender, Altar da Arena e o boss Giganotosaurus — sangrador em 3 fases, 1024 de vida com resistência 0/30/50%, rugido que puxa e investida que derruba, preso à arena; vencido dá a cabeça-troféu e dentes serrilhados (espada serrilhada). O tributo no altar o chama de novo. Versão 0.7.0.

- 2026-10-03 — Anquilossauro (D48): herbívoro solitário a 300+ que vira a cauda para a ameaça e quebra a perna de quem chega pelas costas (o contra da espreita), duelos de flanco sem morte, montaria que minera pedra e minérios comuns (`mount.break_tool`) e esterco que aduba; ficha em `docs/comportamento.html`. Versão 0.8.0.
- 2026-10-03 — Localizador com cara de aparelho, a pedido do Felipe ("ficou tosco"; formato, escopo e extras escolhidos por ele): ao localizar, a caixinha de texto acima da barra de itens vira um **radar circular no canto superior direito** — proa para cima, anel de bússola (N/L/S/O e marcas de 15°) que gira com o olhar, índice no topo, varredura de 2,4 s com rastro, e o marcador da criatura, que acende quando a varredura passa; uma marca âmbar por fora do anel dá a direção (alinhada com o índice, está à frente). Até 200 blocos o marcador anda dentro do disco (raiz quadrada: 50 blocos já é meio raio, o anel do meio); além, vira seta presa na borda. Em outra dimensão, chuvisco e "sem sinal". Embaixo: nome, nível, distância e diferença de altura (▲/▼, "mesmo nível" abaixo de 2 blocos). Desce abaixo dos ícones de efeito e some com o F3. A tela da lista (tecla O) ganhou o mesmo estilo: painel com cantoneiras, bússola pequena por linha apontando para a criatura, distância, altura e coordenadas, botões chapados. Contas em `core/locator/RadarMath` (JUnit); formas (disco, anel, setor, seta) em `client/HudShapes`. Alcance do disco, período da varredura e cores escolhidos na implementação.
- 2026-10-03 — Rastreador da caverna vira aparelho de mão, a pedido do Felipe (o "localizador tosco" era este; mecânica, visual e precisão escolhidos por ele): não é mais jogado como o Olho do Ender. Segurado em qualquer das mãos, mostra o mesmo radar do localizador de criaturas (agora `client/RadarHud`, compartilhado) com "Caverna da Arena", a distância e a diferença de altura até a **boca exata** da caverna mais próxima, e o ícone vira uma bússola de verdade (32 quadros de `tools/gen_cave_tracker.py`, `CompassItemPropertyFunction` como a bússola vanilla; sem caverna na dimensão, a agulha gira e o radar diz "sem sinal"). A boca sai de `ArenaCaveStructure.entranceAt`, a mesma busca no ruído que a geração usa (sem carregar chunk; GameTest confere que bate com a peça de entrada nos mundos reais). O servidor guarda a busca até o jogador andar 256 blocos ou trocar de dimensão e manda o alvo (`CaveTargetPayload`, protocolo 8) ao pegar o rastreador e quando muda. Receita dá 1, um por espaço, não se gasta. Com o rastreador na mão, o radar da caverna tem a vez sobre o das criaturas.
- 2026-10-03 — Todas as telas e o HUD no visual de aparelho do radar, a pedido do Felipe (escopo "telas + HUD" escolhido por ele): `client/TechStyle` (paleta, moldura com linhas de varredura e cantoneiras, cabeçalho com ponto piscando e linha com brilho correndo, espaço de item escuro de borda verde, barra segmentada, painel do HUD com a barra de cor à esquerda) e `client/TechButton` (botão chapado; nos apitos, a ordem em vigor fica acesa como aba). Aplicado ao status da criatura (prévia do modelo num quadro de holograma), ao inventário da criatura (aba da sela com moldura própria), às estações (incubadora; mesa química, de preparo e de reanimação), às bancadas de construção e do arsenal, à lista de localizar, ao painel da criatura no topo (verde sua, laranja de outro, cinza selvagem), ao fôlego de voo e ao termômetro (tubo escuro com escala e coluna segmentada). Os corações congelados continuam sendo os do vanilla tingidos. Tela nova do mod usa `TechStyle`/`TechButton`, nunca `Button.builder` nem textura de GUI.
- 2026-10-03 — Analisador, DINO FILE e manual in-game com o tema Dino Crisis 2 em todas as telas (D49). `tools/gen_comportamento.py` corrigido: "55,%" virava no lugar de "55%" (zeros e ponto à direita) e o Giganotossauro saía "Herbívoro, 0 × 0 blocos" (o registro dele tem um terceiro argumento e ele é agressivo sem tabela de presas); a ficha dele agora diz que ataca quem chega perto com a IA própria. Versão 0.9.0.
- 2026-10-03 — D50: voadores 40% mais lentos e mergulho com zona morta de 15° (pose a 40°); montaria voadora sem
  dano de queda e cavaleiro sem sufocar na copa; texto do Analisador reescrito com a lore (fonte em `tools/wiki_lore/`,
  não mais `docs/*.html`); Analisador com modelo 3D animado do GeckoLib.
- 2026-10-03 — Nova lore do portal e fim do boss (D51), a pedido do Felipe: antimatéria no permafrost, base militar, apex secreto ainda sem nome (texto ambíguo de propósito), postos com computadores que o Analisador escaneia (só decidido). Giganotosaurus vira fauna comum (vida 180, ataque 22, mordida que sangra, `hunt_special: bleed`); caverna, arena, altar, rastreador, cabeça-troféu do boss, `CreatureAction.INJURED` e `leg_break_immune` removidos; `BossPhase` e seus testes saíram, o empilhamento do sangramento foi para `HuntSpecials`. Diário, ecologia, sobrevivência e fichas do manual reescritos (`tools/wiki_lore/`); Etapa 10 reaberta.

