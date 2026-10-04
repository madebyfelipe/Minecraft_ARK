# Titanovenator — estudo incremental sobre o Rex

Estado: **boss no jogo (D52); passo 4 e animações aguardando revisão do Felipe em jogo**. O primeiro modelo gerado
do zero foi rejeitado e descartado. A direção atual parte do Tyrannosaurus do
Fossils and Archeology: Revival instalado localmente, por pedido explícito do Felipe.

## Gerar e abrir

```bash
python3 tools/study_titanovenator.py
python3 tools/apply_titanovenator_texture.py art/titanovenator/local/passo-3-atlas.png
python3 tools/restore_titanovenator_eyes.py  # requer Pillow
python3 tools/titanovenator_step4.py         # cerdas e cicatrizes (Pillow)
python3 tools/titanovenator_anim.py          # animações autorais (stdlib)
python3 tools/render_titanovenator_anim.py walk run attack  # folhas de quadros (NumPy + Pillow)
python3 tools/titanovenator_preview.py
```

O atlas do passo 3 é gerado pelo ImageGen e fica apenas na máquina local; o comando
de aplicação exige esse arquivo. O gerador de estudo sozinho restaura a textura original.

O gerador procura o Revival 1.20.1-9.3.4.0 na instância `IceAgeSurvival` do Prism.
Outra instalação pode ser informada com `--jar /caminho/para/fossil.jar`.
Não baixa mods, não altera o jar de origem e não escreve em `src/main/resources`.

Abra `local/preview.html` em um navegador com WebGL. É autocontido e funciona
sem rede. Alterne **Rex original / Passo 1 · corpo / Passo 2 · cabeça / Passo 3 · textura**:
câmera e enquadramento permanecem iguais. Só o passo 3 usa a pintura nova.
Há vistas lateral, frontal e de cabeça,
modo argila e controle manual de abertura da mandíbula.

`local/titanovenator.bbmodel` é o projeto editável do passo 3, com a textura
embutida. `local/rex-original.bbmodel` é a referência sem ajustes de volume.
O arquivo de animações original fica separado em `local/rex-original.animation.json`;
**a prévia não reproduz animações e o bbmodel ainda não as importa**.

`local/passo-1.bbmodel` preserva o estágio aprovado. As imagens comparativas são
geradas dos mesmos arquivos pelo comando `python3 tools/render_titanovenator.py`
(requer NumPy e Pillow): `local/passo-2-cabeca.png` e `local/passo-2-mandibula.png`.

## No jogo (D52)

O Titanovenator é o boss do endgame (`entity/TitanovenatorBoss`, 1,6× o Rex). O jar **não leva nada do Revival**:
o cliente lê o `tyrannosaurus.geo.json` e a textura do Revival instalado e os deriva em memória
(`core/titan/TitanGeometry`, a mesma transformação dos passos 1, 2 e 4 abaixo, portada para Java e conferida contra
a saída do Python; `core/titan/TitanTexture`, que recolore a pele em carvão e ocre e pinta as cicatrizes). Se a
derivação falhar, desenha o Rex puro e registra o erro.

**Diferença para o estudo:** a pele do jogo é uma **aproximação programática** da pintura ImageGen aprovada no passo 3
(a pintura é um arquivo local, não pode ir no jar): mesma paleta e o mesmo ventre ocre, mas sem as manchas irregulares.
Os scripts em `tools/` continuam servindo para prévia e comparação.

## Passo 4 — cerdas e cicatrizes

Feito sobre o passo 3, sem tocar nos volumes existentes nem nos dentes (decisão do Felipe).
`tools/titanovenator_step4.py` guarda `passo-3.geo.json`/`passo-3.png` na primeira execução e
sempre reconstrói a partir deles.

- **Pernas afastadas:** coxa, canela e pé de cada lado saem 1,0 para fora do tronco (pivôs junto), para o corpo
  parecer mais largo. Só o x muda; as passadas e a hierarquia ficam como estão.
- **Cerdas (dossiê §17):** 23 cubos finos e escuros em fileira no pescoço, no dorso e na base
  da cauda, presos aos ossos que já existem (acompanham respiração e animação). Curtas, com
  altura e inclinação variando: estrutura filamentosa localizada, não uma crista de espetos.
- **Cicatrizes (§17, §38):** 5 decalques de 0,08 de espessura, pintados numa área livre do atlas
  local: garras no flanco e na cauda, perfurações de mordida no pescoço e no peito, corte no
  focinho. Só a face de fora leva a pintura. Os dentes não foram alterados.

## Animações (autorais)

`tools/titanovenator_anim.py` escreve `src/main/resources/assets/iceagesurvival/animations/entity/titanovenator.animation.json`, **versionado e dentro do jar**:
nenhum quadro vem do Revival; o arquivo só nomeia os ossos do rig e soma ângulos ao repouso.
Prefixo `animation.titanovenator.`; o gerador confere os nomes dos ossos contra o geo local.

| Animação | O que faz (dossiê) |
|---|---|
| `idle` | respiração por sacos aéreos, olhar em volta, alívio de peso entre os pés |
| `walk` | marcha bípede lenta, cauda de contrapeso, cabeça e pescoço desfazem o balanço do tronco |
| `run` | a investida (§11): tronco inclinado, pescoço estendido, cauda erguida, braços recolhidos |
| `attack` | mordida (§7): recuo → bote → mordida → ancoragem → tração cervical → recuperação |
| `attack_2` | pancada de corpo: ombro e quadril giram, cauda varre |
| `speak` | rugido de ameaça (§16): inspira, cabeça sobe, expiração curta com a garganta tremendo |
| `call` | chamado territorial grave, boca meio aberta |
| `eat` | arranca em rasgadas e engole, braços seguram a carcaça |
| `unconscious` | nocaute/doma/descanso: deitado de barriga, respiração lenta |

**Fases do boss.** Felipe definiu 3 fases, mas não o que cada uma faz. Premissa de trabalho:
a fase escala cadência, postura e amplitude. `idle`, `walk`, `run`, `attack`, `attack_2` e
`speak` têm variantes `_f2` e `_f3` (cabeça mais baixa, passada mais rápida, cauda mais ampla;
na fase 3 a boca fica entreaberta, ofegante). `roar_phase` é o rugido longo da troca de
fase. A tabela `PHASES` no topo do script é o único lugar a mudar se as fases forem outras.

Fora do escopo por decisão do Felipe: pisão/tremor e ferido/mancando.

**Conferência:** as folhas de `render_titanovenator_anim.py` rasterizam o modelo posado em
`local/anim/`. Foram vistas a caminhada, a investida, a mordida, o rugido, o nocaute, o comer e
a pancada de corpo. **Não foi testado no jogo:** falta ligar o modelo à `CreatureAppearance`
(nomes `idle/walk/attack/unconscious/run` já batem com o que o `LandCreature` usa; `speak`,
`attack_2`, `eat` e `call` são os gestos que ele já dispara) e as fases/`roar_phase` ainda
precisam de código no boss. O sentido dos ângulos segue a convenção do geo e foi validado só
no renderizador do estudo, não no GeckoLib.

## Passo 3 — textura conforme referência

A fotografia enviada pelo Felipe substitui a paleta anterior: dorso carvão, ventre
ocre dourado e manchas escuras na transição. Olhos verdes originais restaurados a pedido do Felipe, boca vinho e garras claras.
Os pixels da íris e da pupila, com posição e tamanho do passo 2, são copiados
da textura anterior por `tools/restore_titanovenator_eyes.py`.
A geometria do passo 2 permanece idêntica; apenas os UVs foram remapeados para regiões
opacas do atlas produzido pelo ImageGen integrado. O atlas gerado não preservou as
ilhas originais com precisão, por isso foi necessário esse remapeamento.

Atlas local: `local/passo-3-atlas.png` (1774×887); projeto com pintura embutida:
`local/titanovenator.bbmodel`. `local/passo-2.bbmodel` conserva a versão anterior.
O script de aplicação usa regiões específicas deste atlas; não serve para qualquer imagem.
As manchas reutilizam trechos da pintura entre partes do corpo. É uma primeira passagem
para revisão; animações, cerdas e integração ao jogo continuam pendentes.

Renderizar corpo, cabeça e mandíbula: `python3 tools/render_titanovenator.py --texture`
(com NumPy e Pillow). Saídas `local/passo-3-corpo.png`, `local/passo-3-cabeca.png`
e `local/passo-3-mandibula.png`. Prompt de geração em `local/passo-3-prompt.txt`.

## Passo 2 — cabeça e lábios

O corpo do passo 1 permanece idêntico. A cabeça ganhou focinho mais largo, mandíbula
mais larga/profunda, dois pequenos volumes nas bochechas e seis peças labiais.
As cristas foram reduzidas a relevos baixos sobre o crânio. São 32 ossos e 30 cubos;
todos os pivôs, nomes, rotações de repouso e parentescos originais foram mantidos.

As peças novas usam trechos opacos da pele na textura existente. O PNG original não
foi pintado ou substituído. Olhos verdes e região azul do pescoço ainda pertencem à
textura de referência e serão tratados na passagem de cor já planejada.

## Passo 1 — corpo preservado

Só dimensões dos volumes existentes. Nenhum cubo, osso, pivô, rotação de repouso
ou parentesco foi acrescentado/removido. Foram preservados 32 ossos e 22 cubos.

| Região | Ajuste de estudo |
|---|---|
| Abdômen | largura +12%, profundidade vertical +10%, mantendo o dorso |
| Peito | largura +10%, profundidade vertical +6%, mantendo o dorso |
| Pescoço | largura +10%, profundidade vertical +4% |
| Cabeça | largura +6%, profundidade vertical +3% |
| Focinho | largura +6% |
| Mandíbula | largura +6%, profundidade vertical +10% |
| Base da cauda | largura +12%, profundidade vertical +8% |
| Segundo segmento da cauda | largura +6%, profundidade vertical +4% |
| Coxas | largura +8%, altura +4%, comprimento +5% |

São ajustes conservadores para comparação, não números de gameplay. Nenhuma escala,
colisão, montaria, spawn ou atributo foi definido. Os UVs são convertidos de box UV
para UV por face **antes** de mudar tamanhos, preservando a posição da arte original.
O mapeamento e as inversões seguem os codecs oficiais do Blockbench v4.12.6:
[Bedrock](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/io/formats/bedrock.js)
e [Cube/updateUV](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/outliner/cube.js).

## Próximas passagens, depois da revisão da base

- Revisar a pintura carvão/ocre dourado do passo 3 conforme a referência enviada.
- Revisar cerdas, cicatrizes e as animações no jogo.
- Definir o que cada uma das 3 fases faz e ligar as variantes `_f2`/`_f3` ao boss.
- Só então decidir escala e integração. O papel do Titanovenator no gameplay não foi definido aqui.

## Procedência e distribuição

Geometria, textura e animações de base são do **Fossils and Archeology: Revival**.
O estudo é derivado; não é arte inteiramente original do Ice Age Survival.
Todos os arquivos derivados ficam em `local/`, ignorado pelo Git. Não os adicionar
ao repositório, jar ou pacote distribuído. O que é versionável nesta etapa são
somente o script de transformação, o visualizador sem assets e esta documentação.
Não usar `git add -f` nessa pasta. A política de recursos externos em runtime continua vigente.

## Verificação do passo 2 (histórico)

- Passo 2 conferido em renderizações locais da geometria com boca fechada e aberta;
  comparação na mesma câmera. JavaScript da prévia validado sintaticamente; a inspeção
  automatizada do WebGL desta passagem ficou indisponível por bloqueio de URLs locais.
- Export do Blockbench preserva hierarquia, pivôs/rotações e textura embutida.
- `./gradlew build`: passou.
- `./gradlew runGameTestServer`: os 388 testes passaram nesta passagem.
- Não houve integração à `main` nem implantação no Prism. Consulta/push remoto
  indisponíveis nesta sessão por autenticação do GitHub não configurada no ambiente.

## Verificação do passo 3

- Geometria comparada com o passo 2 após remover os UVs: idêntica.
- Pintura conferida no render do corpo e cabeça, com mandíbula fechada e aberta.
- Python compilado e JavaScript da prévia validado sintaticamente. WebGL não foi
  inspecionado por automação nesta passagem; a validação visual usou renderização local.
- Build executado; a suíte completou 388 GameTests, com uma falha em
  `stegosaurusDrivesOffSatedVelociraptor` (afastamento insuficiente). Nenhum arquivo
  de gameplay foi alterado. Sem merge na main ou implantação no Prism.
- Fetch remoto indisponível pela configuração ausente do helper de autenticação.

## Ajuste dos olhos

Olhos originais restaurados após a revisão do Felipe. Conferência visual e comparação
confirmam geometria idêntica e pixels do atlas preservados fora dos recortes reservados
à cabeça. Na verificação deste ajuste, os 388 GameTests passaram.
