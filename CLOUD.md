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
- Eye of Ender — inspiração (não cópia) para o rastreador da caverna (removido em D51).

## 4. Decisões

| # | Decisão | Motivo | Estado |
|---|---|---|---|
| D1 | Minecraft **1.20.1** | Escolhida para compatibilidade com o Fossils and Archaeology: Revival e More Hitboxes como dependências externas, sem copiar os assets deles. | Fechada; substitui o alvo inicial 1.21.1 |
| D2 | Loader **Forge 47.4.3** | Compatível com as dependências externas e com a API multipartes do More Hitboxes; o projeto usa capacidades, `SimpleChannel` e GameTest do Forge. | Fechada |
| D3 | **GeckoLib 4.7.2** | Build Forge para Minecraft 1.20.1, padrão de animação das criaturas e integração com More Hitboxes. | Fechada |
| D4 | Gameplay, dados e código de criaturas são nossos; F&A Revival só fornece assets carregados em runtime | Não copiar código, modelos, texturas, animações ou sons. A dependência foi aceita após a migração para 1.20.1 Forge, exclusivamente para resolver assets do mod instalado pelo jogador; domesticação, progressão e IA continuam próprias. | Fechada; substitui a decisão inicial de não depender de fauna |
| D5 | Temperatura: **sistema interno leve**, atrás de uma interface | Cold Sweat é muito mais complexo do que o brief pede ("secundária, sem burocracia"). Ver [15](#15-temperatura). | Fechada na Etapa 6 — `ColdSource` continua de pé |
| D6 | Mundo glacial por **world preset próprio em datapack**, sem mod de worldgen obrigatório | Um preset com a fonte de biomas restrita a biomas frios resolve "mundo predominantemente congelado" sem dependência. Ver [18](#18-worldgen). | Fechada na Etapa 9 — a fonte troca toda a terra por planície nevada e mantém águas congeladas, sobre o relevo vanilla do overworld. Ice Age - Frozen World não entra: para 1.20.1 ele só existe para Fabric, enquanto este pack usa Forge. |
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
| D24 | **Ecologia por fome, estresse e disputa territorial**, decidida no servidor e com o núcleo em `core/ecology` (JUnit) | Caçada só por intervalo aleatório e linha de visão deixava o mundo parado. Agora o predador caça quando tem fome (`Hunger`), escolhe a presa mais fácil pelo faro (`HuntChoice`: filhote, ferida, desgarrada; bando encara presa maior), persegue com fôlego limitado e desiste. Herbívoros reconhecem caçadores pelo perfil de caça e comparam porte e tamanho dos grupos para investir, intimidar ou fugir; se o caçador está em desvantagem, interrompe a caça e recua. A presa caçada avisa a manada, que reage. Cada selvagem tem um termômetro de estresse (`Stress`) movido por eventos (caçador à vista, ferida, manada atacada, disputa territorial, jogador perto) e pelo temperamento da espécie. Machos adultos selvagens da mesma espécie disputam espaço dentro do raio territorial; espécies diferentes não são rivais por padrão. | Fechada 2026-10-01; falta sentir em jogo e balancear |
| D25 | Voo montado com **curva e inclinação limitadas** e energia (mergulho ganha velocidade, subida perde); assento no osso `rider_pos` animado | A câmera gira na hora; a montaria segue com velocidade angular de `flight_turn_rate`. O rasante do grifo do ARK sai da troca altura × velocidade. O assento acompanha a animação de voo (que inclina o corpo), como no Revival. | Fechada 2026-10-01; falta sentir em jogo |
| D26 | **Bando com identidade**: id salvo em cada criatura selvagem, limite `group_max`, fome e saciedade do bando, território contra qualquer criatura do mod | "Por espécie, quem estiver perto" fundia manadas e não deixava dois bandos se enfrentarem. Agora quem nasce junto é do mesmo bando, bandos da mesma espécie brigam até um fugir ou morrer, o bando defende um raio (8–24 blocos) contra outra espécie mais fraca e o mais fraco evita entrar no de um bando mais forte. Comer sacia o bando inteiro. O jogador entra na tabela de dieta (`diet`) como mais uma presa, defendido pelos aliados por perto; o predador faminto sem presa ronda o mapa (`ProwlGoal`). Jogador e bichos do vanilla ficam fora do território. | Fechada 2026-10-01; falta sentir em jogo |
| D27 | **Espreita (stalk) é o jeito de caçar de todo carnívoro**; o faro de caça é sempre maior que o alerta das presas (`core/ecology/Perception`) | Pedido do Felipe: o carnívoro acompanha a manada, ronda sem ser notado e só então dispara. Escolhida a presa, a manada ainda não se sabe caçada; o predador ronda logo além do raio em que seria notado (quem espreita é percebido a 50% do alerta) e dispara no que vier primeiro: presa desgarrada, presa que chega perto, manada que o nota, ferimento ou 20–40 s. O bando dispara junto, e o fôlego da perseguição conta da disparada. Sozinho, o carnívoro também acompanha a manada que não encara (`HuntChoice.chooseToStalk`), mas só dá o bote se a presa estiver atacável na hora (a desgarrada); senão desiste. Na perseguição, a presa que abre 30 blocos do predador escapou (`HuntGoal.CHASE_GIVE_UP_DISTANCE`). O `hunt_radius` efetivo nunca fica abaixo do maior alerta das presas da dieta + 8 blocos; um GameTest falha se um JSON violar a regra. | Fechada 2026-10-01; falta sentir em jogo |
| D28 | **Força e porte pela biologia real; cada caçador com a sua marca** (`behavior.hunt_special`, `core/ecology/HuntSpecials`) | O Smilodon (160–280 kg) tinha um terço da vida do Utahraptor, e Estegossauro e Tricerátopo (2,7–9 t) eram pouco maiores que ele. Agora: Smilodon 95/14, Estegossauro 300/22 (3,2 × 4,0), Tricerátopo 420/26 (3,4 × 3,6). Marcas: Smilodon **emboscada** (arrancada +50% por 4 s no bote e o primeiro golpe agarra, lentidão forte 2 s); Utahraptor **salto** no bote a 3–10 blocos e o maior bônus de bando (+40%); Kelenken **bicada** (30% do dano a mais ignorando armadura) e golpe e recua (1,5 s). Bônus de bando para todo predador de bando (Alossauro +25%, Utahraptor +40%, Velociraptor +25%). | Fechada 2026-10-02; falta sentir em jogo |
| D29 | **A domesticada não some ao morrer: vira corpo com o implante** | O corpo fica 20 min (ou até ser esvaziado), invulnerável e caído, com o inventário e o **implante** — a criatura inteira (espécie, nível, pontos, genes, nome, dono), sem o inventário e sem o UUID para não duplicar. Na **mesa de reviver** (ferro + diamante + mesa química), implante + 1 diamante a trazem de volta com 25% da vida. Dano que ignora invulnerabilidade (void, `/kill`) ainda mata de vez. | Fechada 2026-10-02 |
| D30 | **Apex só se doma pelo desafio** (tag `iceagesurvival:apex`: T-Rex e Espinossauro; `core/ecology/ApexDuel`) | Pedido do Felipe: tributo antes de domar. Morto por um jogador, o apex deixa a cabeça (bloco-troféu de chão ou parede). Com a cabeça na mão, outro da espécie não o caça nem ataca: encara e ruge. Clicar nele com a cabeça a consome e abre o duelo: valem o desafiante e até 3 criaturas dele; uma quarta, ou outro jogador/criatura que o fira, cancela, assim como morrer ou se afastar 48 blocos. Vencido, não morre: cai desmaiado no nome do desafiante, que o doma com carne. Flecha, dardo e rifle não aplicam torpor no apex (o narcótico, na domesticação, sim). | Fechada 2026-10-02 |
| D31 | **Armas tranquilizantes de disparo imediato e fôlego de voo por nível** | Dardo sedativo (pepita de ferro + narcótico → 4); rifle e besta de dardos (escala revista em 2026-10-02, ver D32; antes rifle 3× a cada 2 s e besta 2×, 1 s, aceita flecha tranquilizante) usam a mesma entidade da flecha com torpor fixo. Fôlego de voo é um atributo (`flight_stamina`, segundos) só de quem voa: gasta voando (2× subindo, 0,2× planando em descida), recarrega pousado; esgotado só plana e decola de novo com 30%; o selvagem pousa a 25%. O bico do Pteranodonte segue a trajetória (inclinação calculada no cliente). | Fechada 2026-10-02; falta sentir em jogo |
| D32 | **Fraquezas dos três caçadores, janela do duelo e escala das armas por torpor/s** | Pedido do Felipe. **Smilodon** (`behavior.fears_water`, só selvagem): o caminho nunca passa pela água, presa na água não é caçada nem atacada, e se cair nada direto para a margem (`EscapeWaterGoal`, sem pathfinder). **Utahraptor**: `breaks_leaves` false — não quebra copas, despista-se nas árvores. **Kelenken**: a bicada num escudo erguido trava o bico 2 s (parada, sem atacar). **Duelo do apex**: aceito o tributo, o apex ruge 5 s sem atacar; depois vai nas criaturas do desafiante (no jogador só sem criaturas por perto ou se ele bater). Vencido, quem o atacava larga o alvo, e a domesticada não ataca a criatura desmaiada que o dono está domando. O **corpo** da domesticada não é alvo (`canBeSeenAsEnemy`). **Armas** por torpor/s: arco 25 = besta 25 (50 a cada 2 s) < arco Força V 62,5 < rifle 80 (200 a cada 2,5 s); a besta nunca passa do arco Força V. Modelos 3D autorais do rifle e da besta (`tools/gen_weapons.py`). **Porte pelo status**: Tricerátopo ×1,52 (escala 6,15, caixa 5,2×5,5, assento 4,8), Estegossauro ×1,36 (escala 2,30, caixa 4,35×5,45), Kelenken +14% sobre o Utahraptor (escala 1,41, caixa 1,8×4,0). | Fechada 2026-10-02; falta sentir em jogo |
| D33 | **TerraFirmaCraft como integração opcional** (`compat/tfc`, `datapacks/tfc_compat`, `mixin/tfc/ChooseBiomesMixin`) | Pedido do Felipe. Sem o TFC nada muda. Com o TFC 3.2+ instalado: o datapack embutido (obrigatório, só registrado com o TFC) troca o preset **Era do Gelo** pelo gerador `tfc:overworld` com `temperature_scale` 0 e `temperature_constant` −0,6 (≈ −10 °C constantes, sem estações); o modo Era do Gelo reconhece esse gerador; o frio do jogador lê o clima do TFC (`Climate.toVanillaTemperature`), porque todo bioma dele diz 0,5. No **mundo padrão** (que com o TFC já é o dele) um mixin dobra o peso de old_mountains, volcanic_*, badlands, canyons e lowlands no sorteio de `ChooseBiomes` (não no Era do Gelo). Spawn por relevo (pastadores em plains/lowlands/hills/rolling_hills; caçadores de mata em hills/highlands/plateau/old_mountains; raptores, Kelenken e Alossauro também em badlands/canyons; Espinossauro na água; Pteranodonte nas alturas e na costa). Sabertooth e direwolf do TFC removidos (repetem os nossos); o resto da fauna dele vira presa. Comida de domesticação por tags `taming/*` com os equivalentes do TFC; receitas irmãs `*_tfc` com ferro forjado. Entradas `tfc:` sempre opcionais (`required: false`). TFC e Patchouli no modpack. | Fechada 2026-10-02; falta sentir em jogo |
| D34 | **Hábitos por espécie: horário, camuflagem, carniça e sentinela** (`behavior.habits`, `core/ecology/{Activity,Camouflage,Carrion,Sentinel}`) | Pedido do Felipe ao trazer o Ornitholestes, pela biologia real (sigiloso, oportunista, sempre alerta; órbitas enormes, provavelmente noturno). Sub-bloco porque o codec do `behavior` já tinha 14 dos 16 campos. **Horário** (`activity`: `always`, `nocturnal`, `diurnal`): fora dele, a selvagem procura esconderijo a até 12 blocos e dorme (animação de sono), não caça nem ronda, nota ameaças a metade do raio e acorda ferida ou com a ameaça (desde D46, só acorda ferida); domesticada segue o dono a qualquer hora. **Camuflagem** (`camouflage`, fração do raio): com 2+ blocos de esconderijo nos pés/cabeça e vizinhos (tag `undergrowth`: folhas, mato, arbustos, neve fofa; mais camadas de neve ≥ 3, para valer na tundra do preset), presas e predadores a notam a essa fração do raio — a espreita chega mais perto. **Carniça** (`scavenges`): com fome, come um pedaço de carne crua do chão (tag de itens `carrion`, metade da fome), nunca com predador maior a 16 blocos dela. **Sentinela** (`sentinel_radius`): domesticada, avisa o dono (barra de ação, chamada e animação `call`) do predador selvagem a até o raio, com distância e rumo, uma vez por predador a cada 60 s. O agarrão (`hunt_special: grab`) é o golpe do bote sem arrancada, só em presa do porte do caçador para baixo. Ficaram de fora, por decisão dele: ladrão de ovos, caça a filhotes, roubar comida do jogador e exibição de corte. | Fechada 2026-10-02; falta conferir em jogo |
| D35 | **Jurassic Reborn e Prehistoric Fauna como fontes de assets em runtime, com o conteúdo deles desligado** (`compat/ExternalFaunaCleanup`, `tools/gen_fauna_cleanup.py`) | Pedido do Felipe, na linha de D21/D22: os dois entram no modpack só para fornecer modelos, texturas, poses/animações e sons, lidos dos namespaces deles; nada é copiado para o jar. Tudo o que eles adicionam ao jogo fica desligado por dados: 113 biome modifiers de features e 3 de spawn do JR, 8 structure sets, global loot modifiers (insetos, action figures), profissões de aldeão, pinturas; do Prehistoric Fauna, fósseis, árvores petrificadas, 13 structure sets, biome modifiers, o tipo de mundo e os itens no criativo. As dimensões do PF existem mas ficam sem caminho (o portal exige henostone, que só aparece nelas e nas estruturas desligadas). Os modelos do PF são classes Java: uma espécie futura dele usa a geometria registrada em runtime com animação nossa. | Fechada 2026-10-02 |
| D36 | **Renderer de poses Tabula** para os modelos do Jurassic Reborn (`client/tabula/**`) e **gesto sincronizado** (`entity/CreatureAction`, `PrehistoricCreature#currentAction/startAction`) | Os dinossauros do JR não são GeckoLib: são modelos Tabula (`.tbl`, zip com `model.json`) animados por **poses** (o JSON da fase lista as poses de cada animação — IDLE, WALKING, RUNNING, ATTACKING, SLEEPING, SWIMMING, FISH_LOOKING, ROARING, CALLING, EATING…; todas com a mesma árvore de cubos). O renderer lê o `.tbl` do mod instalado, interpola as poses e escolhe a animação pelo estado (andar com a passada pela velocidade, nadar, dormir, gesto). Como esses renderers não recebem o `triggerAnim` do GeckoLib, o gesto (ataque, rugido, comer, pescar, bote) é um byte sincronizado que o servidor liga por um tempo. Uma espécie nova do JR é uma linha em `JurassicRebornAppearance`. | Fechada 2026-10-02; falta conferir em jogo |
| D37 | **Baryonyx**, o pescador da beira d'água, pela biologia real (`species/baryonyx.json`, `entity/ai/{FishingGoal,WaterEdgeStrollGoal,FleeWhenWeakGoal}`, `core/ecology/Fishing`, `core/mount/SwimModel`) | Pedido do Felipe, com todas as mecânicas propostas. Espinossaurídeo de 7,5–10 m: predador médio (vida 150, ataque 18, porte ≈ Alossauro, caixa 1,8 × 2,7), solitário ou em par (30%), a 300+ blocos, sobretudo em rio/praia/pântano e também na planície nevada; montável; presa do Espinossauro. **Pescador** (`behavior.habits.fishing`): com fome, vai à água rasa (até 2 de fundura) sem gelo, espera 6–15 s olhando e dá o bote; peixe vivo (tag `fish`) a 4 blocos é captura certa, sem ele 35%; o peixe mata metade da fome. Domesticado com a ordem **Parar** perto d'água (até 6 blocos), pesca e guarda o peixe cru no inventário (salmão em rio ou água fria, bacalhau no resto). **Garra-gancho** (`hunt_special: gaff`): o bote (da caça ou da pesca) arma por 4 s o golpe que fisga — puxa a presa para perto (na água qualquer porte, em terra até o dele) e a prende com lentidão forte. **Anfíbio** (`body.amphibious`): não se afoga, nada baixo (60% do corpo submerso) e rápido, passeia pela beira, e ferido foge para a água. **Montaria que nada** (`mount.swims`): quem monta não cai ao mergulhar, a montaria fica na linha d'água e o Espaço sobe e pula para a margem. Come carniça. Modelo, poses, texturas e sons do Jurassic Reborn (D36). | Fechada 2026-10-02; falta sentir em jogo |
| D38 | **Estações primitivas do Primal Stage portadas** (`primal/**`) | Pedido do Felipe ("as estruturas desse"): o Primal Stage é só Fabric e não tem estruturas de mundo — as "estruturas" dele são blocos. Reimplementados para Forge, com os modelos e a textura MIT dele (crédito em `ASSET_LICENSES.md`): **grelha** primitiva (cozinha sobre fogo), **forno de olaria** (kiln, com tijolos próprios), **varal de secagem** por madeira (o jeito primitivo de fazer **charque**; seca só de dia), **tora de corte** (golpes viram tábuas e gravetos) e **bigorna de pedra** (golpes quebram pedra e osso). Sem tela: o item fica à vista em cima; receitas por dados, um tipo por estação (`iceagesurvival:{grill,kiln,drying,cutting,forging}`). | Fechada 2026-10-02; falta conferir em jogo |
| D39 | **Rifle hitscan e armas sem o "bater" do clique** (`item/TranqRifleItem`, `client/{GunClientExtensions,RifleTracers}`, `network/RifleTracerPayload`) | Pedido do Felipe. O clique direito do rifle e da besta não balança mais o braço (`consume`, não `success`): pose de mira, coice no disparo e arma abaixada durante a recarga. O **rifle** não lança mais dardo: um raio de 96 blocos do olho para a mira para no primeiro bloco sólido (grama e flores não seguram) e acerta a primeira criatura viva, inclusive pelas partes do More Hitboxes, nunca a própria montaria; dano e torpor iguais aos do dardo de antes (D32), creditados ao atirador; o que se vê é só o **traçante** da boca do cano ao impacto. A besta continua lançando o dardo, que se recolhe. | Fechada 2026-10-02; falta sentir em jogo |
| D40 | **Estruturas de defesa e as bancadas de Construção e de Armeiro** (`defense/**`, `defense/bench/**`, `tools/gen_{defenses,benches}.py`) | Pedido do Felipe. Toda defesa guarda o **dono** (quem a colocou); "estranho" é quem não é o dono, um aliado dele ou uma domesticada deles. **Muro alto** de madeira e de pedra: bloco cheio com colisão de 1,5 (ninguém pula o topo), cerca para os caminhos; junto de defesa o degrau de qualquer criatura cai para 1, senão T-Rex, Espinossauro e Bronto subiriam o muro de um bloco. **Madeira cede aos gigantes** (tag `wall_breakers`: T-Rex, Espinossauro, Bronto): o selvagem com o caminho fechado golpeia o bloco entre ele e o alvo, um golpe por segundo (`BreakDefenseGoal`), e a mordida montada também conta golpe; 6 golpes derrubam o bloco (ou o portão inteiro), com rachaduras visíveis; o dano fica em memória e respeita o `mobGriefing`. **Pedra**, nenhuma criatura quebra; nenhuma defesa quebra por esbarrar. **Portões** de madeira e de pedra, comum 1 × 2 e **grande 5 × 5** (para Bronto e T-Rex), que só o dono e os aliados abrem. **Armadilha de espetos** (3 de dano por segundo e lentidão), **paliçada de espinhos** (2 de dano e empurrão ao encostar), **armadilha de urso** (6 de dano, prende 5 s — 2 s um gigante —, o dono rearma) e **cobertura de folhagem** para fosso (firme para jogadores e domesticadas do dono; o resto afunda e a desfaz): todas poupam o dono, os aliados e as domesticadas deles. **Bancadas**: receita sem grade (`iceagesurvival:bench`, ingredientes com quantidade tirados do inventário; a tela lista e o clique fabrica). A de **Construção** faz as defesas; a de **Armeiro**, rifle, besta e dardo, cujas receitas saíram da mesa de trabalho (as irmãs `*_tfc` também foram para a bancada). | Fechada 2026-10-02; falta conferir em jogo |
| D41 | **Spawn em todo bioma, com o ideal ×3** (`tags/worldgen/biome/{spawn_anywhere,ideal_<id>}`, `forge/biome_modifier/spawn_<id>{,_ideal}`) | Pedido do Felipe. Toda espécie que nasce (menos o lobo-terrível, desligado, e a criatura de teste) nasce em **qualquer bioma** da superfície — `#iceagesurvival:spawn_anywhere` = `#minecraft:is_overworld` + os biomas do TFC (`required: false`) — com o peso de antes, e no **ideal** com o triplo: o modifier `spawn_<id>` dá o peso `w` em todo bioma e o `spawn_<id>_ideal` soma `2w` no `ideal_<id>` (habitat real da espécie + planície nevada, para valer no preset Era do Gelo, + os biomas do TFC que ela já tinha). A reposição lê o mesmo par em `spawn.biomes`/`spawn.favored`. GameTests em `SpawnCoverageTests`. No preset, Espinossauro e Baryonyx agora preferem a planície nevada ao rio congelado (fica para o Felipe pôr `frozen_river`/`snowy_beach` no ideal deles). | Fechada 2026-10-02; falta sentir em jogo |
| D42 | **Quetzalcoatlus**, o maior pterossauro, pela biologia real (`species/quetzalcoatlus.json`, `core/ecology/Swallow`, `entity/SwallowStrike`, `core/mount/{LeapTakeoff,Thermals,FlightStamina}`, `MountProfile.FlightStyle`) | Pedido do Felipe. 10–11 m de envergadura, ~220 kg: caçador a pé como a cegonha — **engole inteira** (`hunt_special: swallow`) a presa da dieta que cabe no bico (porte ≤ 0,1 do dele, nunca filhote), que some e vale meia refeição; a maior leva só a bicada. **Decola num salto** (quad launch), parado, e só com 4 blocos de céu aberto acima; **térmica** de dia, sem chuva e sobre terra, que o sobe sem gastar fôlego até 48 blocos do chão; pousa com fome e ao anoitecer (diurno); **pesca** pela margem (30%, 32 blocos). Custos de voo por espécie em `mount.flight` (aceleração, subida, cruzeiro, planeio, salto, térmica; o padrão é o voo do Pteranodonte). Bando de 2–4 com defesa em grupo, a 400+ blocos; vida 110, ataque 12, fôlego de voo 90 s; montaria de viagem longa (cruzeiro 1,1, curva 50°/s, 27 espaços). Domado com peixe cru (favorito), charque e carne pequena. Modelo e animações do Revival na escala **3,6** (o triplo da primeira versão, que saiu menor que o Pteranodonte), assento a 6,3 e hitboxes do More Hitboxes ×3; a caixa de colisão fica 2,2 × 4,5, menor que o desenho, porque é ela que dá o porte das regras de ecologia (3,6 × 7,2 daria porte maior que o do T-Rex). Sons do Jurassic Reborn, em runtime. | Fechada 2026-10-02; falta sentir em jogo |
| D43 | **Megalania**, o varanídeo gigante do Pleistoceno australiano, pela biologia real (`species/megalania.json`, `entity/VenomBite`, `entity/ai/VenomTrackGoal`, `effect/VenomEffect`, `item/AntidoteItem`) | Pedido do Felipe (criatura surpresa, design livre). **Mordida que solta** (`hunt_special: venom`), como a do dragão-de-komodo (Fry et al. 2009; Bull et al. 2010): morde, recua 1,5 s e segue o rastro da vítima a 8 blocos, provando o ar com a língua (`tongueflick`), sem morder enquanto a peçonha age; perde o rastro a 64 blocos. **Peçonha** (30 s, renovada a cada mordida): nas criaturas do mod é choque (+5% do torpor máximo por segundo, até derrubar; a selvagem vai lá e termina a presa); no jogador e nos mobs vanilla é sangramento (1/3 de vida por segundo, atravessa armadura, −15% de velocidade) **sem regeneração**, e quem morre sangrando conta como abate da Megalania. **Antídoto** na mesa química (carvão ou carvão vegetal + frasco de vidro), bebido ou dado a uma criatura. Domesticada, o choque da mordida **derruba a presa no nome do dono**; não envenena nem ataca o apex (D30). **Diurna**, solitária, a 300+ blocos; ideal savana e badlands (e a planície nevada, D41). Vida 120, ataque 14, armadura 4, speed 0,3, torpor 200; colisão 1,5 × 1,1, modelo, animações e sons do Revival em runtime na escala 1,3. Valores da peçonha, do recuo, da dieta e da domesticação escolhidos na implementação (o Felipe não deu os números). Quetzalcoatlus e Megalania entram em `#predators`; o Quetzalcoatlus vira presa do T-Rex. | Fechada 2026-10-03; falta sentir em jogo |
| D44 | **Quebra de blocos pela fauna, por espécie** (`entity/BlockBreaking`, `defense/{DefenseDamage,BreakDefenseGoal}`, `body.breaks`/`body.giant`) | Decisão do Felipe (2026-10-02), implementada em 2026-10-03. Cada espécie quebra `wood`, `plants` ou `none` (padrão; sem o campo vale o `breaks_leaves` antigo). Madeira: T-Rex, Espinossauro, Bronto, Alossauro, Tricerátopo, Mamute, Estegossauro, Elasmotério, Baryonyx. Só folhas e plantas: Smilodon, urso-terrível, Galimimo, Kelenken. Nada: raptores, Ornitholestes, dodô, Pteranodonte, Quetzalcoatlus, Megalania (e o lobo-terrível, fora da tabela dele). Pedra, nenhuma selvagem. Golpes só para alcançar o alvo: a selvagem adulta com o caminho fechado golpeia uma vez por segundo; muro/portão nossos em 6 golpes (3 nos **gigantes**, `body.giant`: T-Rex, Espinossauro, Bronto, Tricerátopo, Mamute, Estegossauro), madeira vanilla (`#iceagesurvival:creature_breakable_wood`) em 3 (2 nos gigantes); os golpes somam por bloco. Tronco cai ao esbarrar só no gigante adulto (o Estegossauro ganhou `plow_hardness` 2); filhote não quebra madeira (abre folhas, para não ficar preso na copa). Tudo respeita `mobGriefing`. A mordida montada conta os mesmos golpes na madeira (o tronco do mamute coletor leva 2 mordidas) e passa pelo `BreakEvent` como quem monta; o resto do terreno segue `mount.break_hardness` (o T-Rex montado ainda quebra pedra). Sai a tag `wall_breakers`; a armadilha de urso usa `body.giant`. Muros e portões em `#fossil:unbreakable`. GameTests em `BlockBreakingTests`. | Fechada 2026-10-03; falta sentir em jogo |
| D45 | **Carcaça** (`core/ecology/Carcass`, `entity/ai/CarcassGoal`, `behavior.ecology.carcass_portions`) | Decisão do Felipe (2026-10-02), implementada em 2026-10-03. A presa grande — a espécie com `carcass_portions` > 0: Galimimo 6, Utahraptor 6, urso-terrível 8, Baryonyx 10, Alossauro 12, Elasmotério 18, Estegossauro 22, Espinossauro 26, Mamute 28, T-Rex 28, Tricerátopo 30, Brontossauro 60 (as âncoras dele e o resto pelo peso real) — que morre selvagem **sem jogador** (nem a criatura dele, nem ferida por ele há pouco) fica caída como o corpo da domesticada, intocável; some quando acaba a carne ou em 10 min. Quem caça ou é carniceiro, com fome, fareja a carcaça a 64 blocos e come uma porção a cada 2 s; a barriga enche com porções pelo porte de quem come (Galimimo 3, T-Rex 8, raptor 2) e as bocadas somam. **O mais forte toma** (porte × grupo, a conta do confronto); o fraco espera a sobra a 14 blocos. Comendo, não sai caçando. O jogador **carneia a sobra** com machado ou espada: o loot da espécie na proporção da carne que resta. A carcaça não conta no teto do spawn. Isca ficou de fora, por decisão dele. | Fechada 2026-10-03; falta conferir em jogo |
| D46 | **Ajustes de IA de 2026-10-03: sono pesado, ameaça maior, caça do solitário e spawn justo** (`WaryGoal`, `RestGoal`, `HuntChoice`, `HuntGoal`, `core/spawn/WildSpawnRules`, `world/WildSpawner`) | Pedidos do Felipe. **Sono:** deitada fora do horário, a criatura não nota ameaça, aviso do bando, intruso nem jogador — só acorda ferida (antes notava a ameaça a metade do raio). Vale para todo `habits.activity` (Ornitholestes e Quetzal). **Ameaça maior:** a cautela escolhe a ameaça de maior porte relativo (quem investe ou persegue alguém pesa 1,5×; a distância só desempata, margem de 20%) e troca para outra bem maior que chegue; quem persegue um jogador conta como ameaça para quem está em volta — o jogador que foge do Tricerátopo para o mamute não vira o alvo do mamute. **Caça do T-Rex:** três causas de ele ignorar presa fácil com fome — (1) o caçador solitário descartava **toda** presa de manada, até o dodô no bando; agora só evita o grupo que se defende junto e pesa (porte × defensores ≥ 0,5: o jogador com aliados para o Alossauro, sim; os dodôs para o T-Rex, não); (2) a presa de preferência 0 tinha nota negativa e nunca era escolhida — agora é o último recurso (nota mínima 0,01); (3) espreitando a manada da favorita que não podia atacar, seguia nela com presa fácil ao lado — agora troca pela atacável (a cada 2 s). **Spawn:** o sorteio era só pelo peso e a fauna não some sozinha, então as espécies de peso alto e bando grande enchiam o teto e a composição congelava; cada espécie nova diluía as outras. Agora a espécie ausente entra com peso ×3 e, com a vaga da categoria pela metade, quem já tem um bando inteiro por perto espera; voadores (Pteranodonte, Quetzal) têm teto próprio de 8, fora do teto do chão (o Ptero tomava até 8 das 27 vagas de herbívoro). Simulação dos herbívoros do spawn: todas as sete presentes em 95% (antes 43%; o Bronto faltava em 20%). **Pteranodonte** com ideal em todo bioma (peso cheio em todo lugar) e pescador com fome (`habits.fishing`, sem `prey`: as manadas não o temem). Modpack sem TerraFirmaCraft e Patchouli (a integração opcional segue no código). | Fechada 2026-10-03; falta conferir em jogo |
| D48 | **Anquilossauro**, a clava da cauda pela biologia real (`species/ankylosaurus.json`, `core/ecology/TailClub`, `entity/TailClubStrike`, `effect/BrokenLegEffect`, `species/DungProfile`, `wariness.defense`, `mount.break_tool`) | Escolha da criatura pelo Claude, mecânicas decididas pelo Felipe em 2026-10-03 (as quatro recomendadas). Pesquisa: 6–8 m, ~5–8 t (Arbour &amp; Mallon 2017); cauda gira ~100° para o lado; flancos quebrados e cicatrizados do *Zuul crurivastator* indicam duelos de clavadas (Arbour, Zanno &amp; Evans 2022); osteodermos resistentes a dente, pálpebras ósseas; pastejo rente ao chão (até ~1 m) e fermentação no intestino (*magniventris*); adulto provavelmente solitário. Sem fonte: velocidade, força da clava, "agachar". **Clava** (`wariness.defense: tail_club`): no lugar de investir, blefar ou fugir — inclusive do T-Rex — a selvagem para e gira de costas para a ameaça (4,5°/tick, 90°/s); a cada 5 ticks golpeia o inimigo mais perto no arco de trás (±50°) a até 4,5 blocos do centro, sem precisar tê-lo notado (inimigo: o alvo, o jogador — agachado, metade do alcance —, predador selvagem e monstros; da domesticada, só o alvo); recarga 1,5 s; com alvo (`ChaseGoal`) chega perto e vira a cauda, nunca morde de frente. A brecha: quem circula de perto pela frente (a 3 blocos, ~107°/s correndo) fica fora da cauda. **Perna quebrada** (`broken_leg`): −60% de velocidade por atributo e o pulo desfeito (`LivingJumpEvent`, dos dois lados) por 5 s; não pega na mesma espécie nem na tag `leg_break_immune` (o boss). **Duelo**: o `RivalryGoal` de sempre; a clavada num selvagem da mesma espécie não leva abaixo de 40% da vida (`LivingHurtEvent`). **Montaria que minera**: `mount.break_tool` faz a mordida dropar como se minerado com aquela ferramenta (picareta de pedra: pedra → pedregulho, ferro → ferro bruto), na tag `ankylosaurus_harvestable` (pedra, pedregulho, cascalho, carvão, ferro, cobre, lápis) até dureza 4,5 (os minérios de deepslate); a mordida montada continua na frente. **Esterco** (`species.dung`, item `dung` = farinha de osso): adulto acordado a cada ~10 min (5–15) e um a cada 60 de alimento comido (domesticação, dono, inventário). Ataque 24 (entre Estegossauro 22 e Tricerátopo 26) e caixa 2,6 × 2,2 escolhidos pelo Claude; escala 1,7 (~7,6 blocos). | Fechada 2026-10-03; falta sentir em jogo |
| D47 | ~~**Endgame: caverna da arena, rastreador e o boss Giganotosaurus**~~ **(substituído pela D51 em 2026-10-03: caverna, arena, altar, rastreador e boss saíram; o texto abaixo é o histórico)** (`world/cave/**`, `registry/ModStructures`, `item/CaveTrackerItem`, `entity/GiganotosaurusBoss`, `endgame/**`, `effect/BleedingEffect`) | Decisões do Felipe em 2026-10-03 (as quatro recomendadas): boss sangrador em 3 fases, vida 1024 + resistência por fase, rastreador de troféus, volta com tributo. **Caverna:** gerada por código (sem NBT nem jigsaw), em anéis concêntricos como as fortalezas (`structure_set/arena_caves`: distance 40, spread 4, count 4, `stronghold_biased_to`) — 4 por mundo, entre ~1650 e ~3400 blocos do spawn; o ângulo do primeiro anel coincide com o da fortaleza nº 0 (o vanilla usa a mesma semente). Boca só em terra seca acima do mar, marcada por costelas de osso e pedregulho musgoso; túneis sinuosos com salas descem até um salão profundo e a porta 3×4 da arena: 40×40, 18 de altura, casca de 3 de deepslate, chão em y = −30, luz fraca de líquen, o **Altar da Arena** no centro. Túneis a 6+ blocos sob o chão/fundo do mar e casca que veda água e lava. Overworld, preset Era do Gelo e TFC (`#iceagesurvival:has_arena_cave`). **Rastreador** (cabeça de T-Rex + cabeça de Espinossauro + bússola → 1): aparelho de mão — segurado, mostra o radar com a direção e a distância até a **boca exata** da caverna mais próxima, e o ícone vira bússola apontando para ela; não se gasta (até 2026-10-03 era jogado como o Olho do Ender, dava 4 e apontava só o chunk do anel). **Boss:** `GiganotosaurusBoss` (subclasse de `LandCreature`, IA própria), modelo, poses e sons do Jurassic Reborn em runtime (pele `sand`), caixa 3,0 × 4,5 (não passa pela porta). Vida 1024, ataque 20, armadura 10, XP 250. Fases pela vida (`endgame/BossPhase`): 1) caça e corta — a mordida abre sangramento que empilha (+1 nível até 3, 6 s); 2) abaixo de 50%: ruge 2,5 s puxando quem está a 20 blocos e passa a investir derrubando (`BossChargeGoal`: 1,5 s a 12 blocos/s, 75% do ataque, empurrão forte, a cada 8 s), 30% do dano não passa, +10% de velocidade; 3) abaixo de 25%: ferido e frenético (pose INJURED), +35% de velocidade, investidas a cada 5 s a 15 blocos/s, mordida +2 níveis até 5 (8 s), 50% do dano não passa. **Sangramento:** 0,5 de vida/s por nível no jogador e nos vanilla, 0,4% da vida máxima/s por nível nas criaturas do mod; atravessa armadura, sem empurrão, não impede regeneração (diferente da peçonha da Megalania). Imune a torpor, persistente, não nasce sozinho; ataca jogadores e criaturas na arena (raio 40 do altar) e volta ao altar andando, ou reaparece nele se for arrastado a mais de 80 blocos ou não chegar em 10 s. Barra de boss para quem está na arena. **Altar:** indestrutível; a cabeça-troféu de um apex consome-se, 3 s de rugido e o boss aparece a 6 blocos, ligado àquele altar (um por vez). Vencido: a **cabeça do Giganotosaurus**, 3–5 **dentes serrilhados** e ossos; o altar aceita outro tributo. **Espada serrilhada** (espada de diamante + dente): o golpe faz sangrar 3 s. Valores de combate escolhidos na implementação; "chama a equipe" da fase 2 virou o rugido que puxa todos para perto (não invoca ajudantes) — o Felipe confirma. | Fechada 2026-10-03; falta conferir em jogo |
| D49 | **Analisador, DINO FILE, manual in-game e tema Dino Crisis 2** (`item/AnalyzerItem`, `world/DinoFileData`, `network/{DinoFilePayload,ScanResultPayload}`, `core/wiki/{Manual,ManualParser}`, `client/dex/**`, `client/TechStyle`, `tools/gen_wiki.py`) | Pedido do Felipe ("wiki in-game com tudo dos HTML em docs, analyser tipo pokédex, aesthetic Dino Crisis 2"); escolhas dele: **tema DC2 em tudo**, **um aparelho só**, **DINO FILE trancada até escanear**, **todo jogador começa com um**. O **Analisador** (1 por espaço, sem receita) é dado no primeiro login (marca em `PERSISTED_NBT_TAG`, não volta ao morrer). Mirando numa criatura do mod a até 24 blocos e segurando o clique por 1,5 s (bipes subindo de tom; perder a mira cancela), escaneia: a espécie entra na DINO FILE do jogador (dados do Overworld, por UUID) e a leitura do indivíduo vai ao cliente (nível, sexo, vida, torpor, ataque, armadura, estado, humor e perigo: 0 domesticada, 1 selvagem que se defende, 2 selvagem agressiva ou que caça). Com ele na mão a criatura deixa o clique passar. Clicando no ar abre o **terminal**: aba DINO FILE (lista numerada na ordem do manual, "???" e silhueta até registrar; modelo girando numa plataforma; ficha da espécie; ANÁLISE DO INDIVÍDUO; "NOVO REGISTRO" no primeiro scan) e aba MANUAL (capítulos e páginas). O conteúdo vem de `docs/*.html` por `tools/gen_wiki.py` → `assets/iceagesurvival/wiki/manual.json` (só em português, como os docs), recarregável com F3+T; espécie com selo "Desligado" (lobo-terrível) fica fora da DINO FILE. **Tema DC2** (inspiração, nenhum asset da Capcom) no `TechStyle`/`TechButton`: azul-petróleo com grade, moldura de metal chanfrada com bisel, títulos brancos em itálico e caixa alta atrás de um losango âmbar, dados em ciano, cursor âmbar piscando, vermelho de perigo — vale para todas as telas, o HUD e os radares. Alcance, tempo de scan e a regra de perigo escolhidos na implementação. | Fechada 2026-10-03; falta sentir em jogo |
| D50 | **Voo mais lento, mergulho só íngreme, montaria voadora sem dano de queda/sufocação; Analisador com a lore e modelo 3D** (`core/mount/FlightModel`, `entity/ai/WildFlightGoal`, `entity/FlightMountRider`, `tools/wiki_lore/**`, `tools/gen_wiki.py`, `client/item/AnalyzerRenderer`, `tools/gen_analyzer.py`) | Pedido do Felipe após testar a 0.9.0; escolhas dele: **voadores −40%** (Quetzal `flight_speed` 1,1 → 0,66: 13,2 blocos/s de cruzeiro, 29 no mergulho; Pteranodonte 0,85 → 0,51: 10,2 e 22,4; voo selvagem 0,6 → 0,36), **zona morta + escala** no mergulho (até 15° de descida é planeio sem ganho; acima, `gravity·sen((pitch−15)·90/75)`; pose de mergulho a partir de 40°, antes 25°), **narrador cientista da base**, **manual todo narrativo**, **modelo 3D animado**. Dano ao bater em árvores montado tinha duas causas: a queda acumulada na descida montada (o servidor não zerava a distância de queda e o dano repetia no cavaleiro) — montaria voadora não leva nem passa dano de queda; e a cabeça do cavaleiro (assento 6,3) entrando no tronco — `FlightMountRider` cancela `in_wall` de quem monta voador. A domesticação do Quetzal funciona no servidor (GameTest); suspeita: as partes do More Hitboxes ficam na pose em pé no servidor e o clique na cabeça/pescoço caídos é recusado pelo alcance — falta o Felipe confirmar. **Lore:** um experimento falho numa base militar (o "Projeto Limiar", nome provisório) abriu um portal que mesclou épocas; o texto in-game passa a ter fonte própria em `tools/wiki_lore/` (manual em 4 capítulos e uma ficha por espécie), sem números nem termos técnicos — `docs/*.html` seguem como documentação de design; o gerador recusa termos técnicos. **Modelo 3D** GeckoLib autoral (mão, chão, moldura; ícone 2D na GUI), animações `idle` e `scan` (enquanto o clique está seguro) e glowmask na tela. | Fechada 2026-10-03; falta sentir em jogo |
| D51 | **Nova lore do portal; Giganotosaurus vira animal comum; fim da caverna e do boss** (`species/giganotosaurus.json`, `species/BehaviorProfile.HuntSpecial.BLEED`, `core/ecology/HuntSpecials`, `effect/BleedingEffect`, `tools/wiki_lore/**`) | Decidido pelo Felipe em 2026-10-03. **Lore:** o biólogo foi chamado pelas espécies conservadas no permafrost, mas a base militar procurava, na verdade, **antimatéria congelada** no gelo; o clarão foi a instabilidade dela colapsando o espaço-tempo e trazendo espécies de eras diferentes para o mesmo vale. O portal **fechou**; as criaturas vivem em equilíbrio. A base militar ainda existe e guarda um **apex tiranossaurídeo secreto**, hipotético (menos de 2% das espécies que já existiram foram descobertas), que ameaça o equilíbrio por tamanho e hiperpredação — **ainda sem nome nem design: o texto o deixa ambíguo de propósito**. A lore chega por **postos militares** com blocos-computador decorativos que o Analisador escaneia para destravar notas — **só decidido, ainda não implementado** (etapa própria). **Giganotosaurus:** boss → fauna comum: sem caverna/arena/altar/rastreador/cabeça-troféu; caixa 3,0 × 4,5 (a do modelo; o T-Rex tem 2,7 × 5,4), vida 180, ataque 22, torpor 420, armadura 5 (T-Rex: 220/28/500/6), domesticável, sem montaria; nasce raro (peso 1 e ×3 no bioma ideal, espaçamento 320, só 1 por vez); caça presas grandes; mordida que sangra (`hunt_special: bleed`: 1 nível por golpe, teto 3, 6 s); dropa carne, ossos e 1–3 dentes serrilhados. Fora também `CreatureAction.INJURED` e a tag `leg_break_immune`. |

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
| TerraFirmaCraft | EUPL-1.2 | 3.2.26 (2026-09) | Reformulação total: gerador próprio (30 biomas só de relevo; clima por posição, −20 a 30 °C), sem temperatura do jogador, desliga ~230 receitas vanilla, fauna própria (inclui sabertooth e direwolf), exige Patchouli. Integrado como opcional ([D33](#4-decisões)). |

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
├── item/                   flechas tranquilizantes, rifle, implante
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
- Não trocamos a categoria para `MONSTER` para conseguir spawn contínuo: isso faria a fauna desaparecer sozinha ([D20](#4-decisões)).
- Tudo desligável em `wildSpawnEnabled`, para quem quiser a fauna só na geração do terreno.

**Um grupo por região:** `spawn.spacing` (blocos, padrão 0) é a distância mínima entre dois grupos selvagens da espécie. Brontossauro e T-Rex usam 300: uma manada de bronto e um T-Rex a cada 300 blocos. Como a geração do terreno e a reposição não enxergam chunk descarregado, cada grupo deixa uma **marca** (`GroupSpacing`, dados da dimensão; regra pura em `core/spawn/SpeciesSpacing`):

- A regra de colocação aceita um grupo novo só a mais de `spacing` de toda marca e marca o lugar; quem cai a até 64 blocos de uma marca é do mesmo grupo e passa (os membros da manada).
- A cada 10 s, marca em chunk carregado vai para onde o grupo está; sem ninguém do grupo ali por 1 min, é apagada. Cada indivíduo selvagem renova a sua a cada 30 s (e cria uma, se faltar), então uma manada migrando leva a marca junto — e pode passar perto de outra sem sumir.
- Mundos de antes da regra: a criatura da geração do terreno conferida pela primeira vez perto demais de outro grupo some (como a fauna fora da zona). Domesticada, com dono ou presa ao mundo não conta nem some — exceto o apex garantido, que conta e afasta os T-Rex naturais.

O `forge:add_spawns` continua: ele povoa chunk novo, a reposição cuida do que já existe. Os dois usam a mesma tag de biomas.

Horário de atividade: por espécie, em `behavior.habits.activity` (D34); por enquanto só o Ornitholestes é noturno.

## 12. Progressão

| Fase | Conteúdo | Ferramenta de torpor |
|---|---|---|
| 1 Sobrevivência | madeira, pedra, comida, abrigo, fogo, frio, primeiros predadores | — |
| 2 Primeira domesticação | criatura pequena, primeiro companheiro | Arco + flecha tranquilizante |
| 3 Exploração | criaturas maiores, regiões perigosas | Besta |
| 4 Domínio | montarias, carga | Besta |
| 5 Genética | reprodução, incubadora, herança, mutações | — |
| 6 Endgame | criaturas de alto nível | Rifle tranquilizante |
| 7 Boss | superpredador secreto na base militar (ver §19–20, D51) | — |

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

Escala das ferramentas, em torpor por segundo (D32): arco puxado 25 = besta de dardos 25 (50 a cada 2 s, sem puxar) < arco Força V 62,5 < rifle 80 (200 a cada 2,5 s). A besta nunca passa do arco Força V, nem por tiro.

## 15. Temperatura

**Modo Era do Gelo (2026-10-01):** o frio é uma "DLC" do mod. Só vale num mundo criado com o preset Era do Gelo, reconhecido pela fonte de biomas do Overworld (`RemappedBiomeSource`, em `world/IceAgeMode`); em mundo normal não há frio nem termômetro. `iceAgeMode` na config do mundo força (`ON` em qualquer mundo, `OFF` nunca; padrão `AUTO`). As criaturas nascem nos dois: cada `spawns_<especie>` lista os biomas frios e os equivalentes de um mundo normal (planícies, savana, florestas, deserto para o Galimimo, rios e pântanos para o Espinossauro); no preset os biomas normais são trocados por frios e não aparecem.

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

O preset conserva cavernas, estruturas, dimensões e relevo vanilla. A fonte de biomas
`iceagesurvival:remapped` troca toda a terra do overworld por `minecraft:snowy_plains` e as águas
por rios e oceanos congelados. Não há taiga, florestas, picos, encostas ou uma tabela climática
complexa: o mundo é deliberadamente uma tundra simples, com planícies nevadas predominantes.

O GameTest aceita apenas planície/praia nevada, rios e oceanos congelados, além das cavernas
vanilla. Assim nenhum bioma temperado, nem um bioma frio verde, volta a aparecer no preset.

**Toda a superfície neva.** As cavernas guardam a temperatura delas (lush caves 0,5): descer é
se abrigar.

**Conteúdo do Revival desligado ([D22](#4-decisões)):** o biome modifier `remove_revival_features` tira os minérios (fóssil, âmbar, permafrost, rocha vulcânica) e a estátua moai; `data/fossil/worldgen/structure_set/` vazios desligam sítios de fóssil, poços de piche, templos astecas, academia egípcia e o barco do Nether; os itens dele saem das abas do criativo (`RevivalCleanup`). O TerraBlender só injeta o bioma vulcão em fontes `multi_noise` puras, então ele não aparece no preset (embrulhado pela nossa).

Nether e End são os do vanilla. Mundos já criados não mudam: o preset vale na criação. Relevo mais dramático (Tectonic) fica opcional e fora do escopo; o preset funciona com ele instalado, porque troca só os biomas.

## 19. Caverna

**Removida em D51** (2026-10-03): não haverá mais caverna, arena nem rastreador. O lugar do endgame é a **base militar** e os **postos militares** em volta dela (§20).

## 20. Boss

**Redefinido em D51.** O Giganotosaurus deixou de ser boss e é fauna comum (ver D51). O endgame passa a ser um **apex tiranossaurídeo secreto** guardado na base militar, ainda sem nome, modelo ou números, pela lore: menos de 2% das espécies que já existiram foram descobertas, e este é uma delas; sua ameaça é o tamanho e a hiperpredação sobre o equilíbrio do vale. **Postos militares** com blocos-computador decorativos, escaneáveis pelo Analisador para destravar notas, contam a história (antimatéria no permafrost, o clarão, o portal que fechou). Nada disso está implementado: a decisão fica aqui até virar etapa.

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
| 5 | Mais criaturas, spawning | 🟡 Estegossauro, Pteranodonte, dodô e Elasmotério (início de jogo), migração de herbívoros, bônus de bando do Alossauro e ecologia dinâmica (fome, caçadas, estresse, disputas territoriais entre machos da mesma espécie — D24) implementados; caçador abandona presa quando uma investida mostra desvantagem; interações cobertas por GameTests; bandos com identidade e território (D26) e espreita de todo carnívoro (D27); falta conferir comportamento em jogo; Kelenken e a marca de cada caçador (D28), força pelo porte real, troféu de apex e desafio (D30); Ornitholestes e os hábitos por espécie — horário, camuflagem, carniça, sentinela (D34); Baryonyx pescador, anfíbio e montaria que nada, com modelo do Jurassic Reborn (D35–D37); spawn em todo bioma com o ideal ×3 (D41); Quetzalcoatlus, que engole a presa, decola num salto e sobe nas térmicas (D42); Megalania com peçonha, rastro e antídoto (D43); quebra de blocos por espécie (D44); carcaça (D45); sono pesado, ameaça maior, caça do solitário e spawn justo (D46); Anquilossauro com a clava da cauda, perna quebrada, duelo de flanco, mineração montada e esterco (D48) |
| 6 | Temperatura | ✅ 2026-09-30 em testes automáticos; falta sentir o frio em jogo e balancear a primeira hora |
| 7 | Montaria | 🟡 armazenamento por espécie, voo no estilo do Cobblemon, pulo sem barra de carga, montaria sem sela e More Hitboxes implementados; build e GameTests passam, falta conferir controles e hitboxes em jogo; slot de sela no inventário, fôlego de voo por nível e bico que segue o voo (D31); custos de voo por espécie, decolagem por salto e térmicas (D42); voo −40%, mergulho só íngreme e montaria voadora sem dano de queda nem sufocação (D50) |
| 8 | Reprodução e genética | ✅ 2026-09-30 em testes automáticos (genética em JUnit; acasalamento, gestação, ovo, incubadora, mesa química e estimulante em gametests); falta conferir em jogo |
| 9 | Worldgen | ✅ 2026-09-30 em teste automático (os biomas possíveis do preset são todos frios); falta criar um mundo e andar por ele |
| 10 | Endgame: base militar, postos com notas, apex secreto | 🔴 reaberta em 2026-10-03 (D51): a caverna e o boss da D47 saíram; falta desenhar o apex, os postos e o bloco-computador |

MVP = Etapas 1–4 + versão mínima de 6, 7 e 9 (mundo frio, temperatura básica, montar o Smilodon).

**Estudo visual em revisão:** Titanovenator, sobre o Rex do Revival instalado, conforme pedido do
Felipe. Passo 3 da textura em `art/titanovenator/README.md`, mantendo o corpo do passo 1 aprovado;
derivados locais ignorados
pelo Git, sem integração ao jogo. Referência nova: carvão e ocre dourado com manchas;
textura em revisão, cerdas discretas e animações pendentes.

## 24. Decisões técnicas

Ver a tabela em [4](#4-decisões). Registro de mudanças estruturais:

- 2026-10-03 — Titanovenator: a pedido do Felipe, restaurados os olhos verdes originais
  (pixels da íris/pupila e posição do passo 2), mantendo a pintura carvão/ocre do corpo.

- 2026-10-03 — Titanovenator, passo 3: pintura carvão/ocre dourado conforme fotografia
  enviada pelo Felipe. Atlas via ImageGen integrado, UVs remapeados sem mudanças geométricas;
  preservada comparação com a cabeça do passo 2. Derivados somente locais, sem inclusão no jar.
  Suíte de 388 GameTests: uma falha em `stegosaurusDrivesOffSatedVelociraptor`; sem merge.

- 2026-10-03 — Titanovenator, passo 2 autorizado pelo Felipe: focinho/mandíbula mais largos,
  bochechas discretas, cristas reduzidas e peças labiais articuladas nos ossos originais.
  Corpo do passo 1 preservado, 32 ossos/30 cubos, textura original intacta. Comparador com
  original/passo 1/passo 2 e renderizações locais da cabeça; revisão visual antes de cores e animações.
  Validação: build e os 388 GameTests passaram; sem integração ao jogo nesta etapa.

- 2026-10-03 — Estudo do Titanovenator: Felipe descartou o primeiro modelo autoral e pediu
  alterações graduais sobre o Rex do Fossils and Archeology: Revival. `tools/study_titanovenator.py`
  lê o jar instalado e gera comparação original/primeiro ajuste de volumes em
  `art/titanovenator/local/` (ignorado; não distribuído). Hierarquia, pivôs, pose-base e textura
  originais preservados, animações guardadas separadamente para a etapa posterior. Nenhum recurso
  derivado entra no jar/repositório. O papel da criatura no jogo continua fora desta etapa.

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
| Teto de 1024 de vida do vanilla limita criaturas gigantes e o apex secreto. | Médio | A decidir quando o apex for desenhado (D51); a solução da D47 (1024 com resistência por fase) saiu junto com o boss. |
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
