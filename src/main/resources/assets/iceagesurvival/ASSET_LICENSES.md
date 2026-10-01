# Procedência dos assets de criatura

> **Antes de distribuir o mod para qualquer pessoa, leia isto.**

Nem toda arte deste diretório é nossa. As espécies listadas em
[`tools/hand_authored.txt`](../../../../../tools/hand_authored.txt) usam modelos, texturas,
animações e sons (`sounds/entity/<especie>/`, com `sounds.json`) do mod **Fossils and Archeology: Revival**
(<https://github.com/TeamFossilsArcheology/FossilsArcheologyRevival>), instalados por
`tools/install-revival-placeholders.py`.

O **código** do Revival é MIT. A **arte** é **All Rights Reserved**: redistribuí-la exige
permissão dos autores, que não pedimos.

## O que isso significa na prática

Por decisão do Felipe (2026-09-30) essa arte fica no repositório, que é privado, para poder
ser aberta e editada no Blockbench sem depender de um resource pack local. A consequência é
direta e não tem como contornar: estes arquivos estão em `src/main/resources/assets/`, então
**vão dentro de todo jar compilado**.

Ou seja, hoje o jar do mod **não pode ser distribuído**: nem para um tester, nem para um
servidor de outra pessoa, nem como release. Rodar na máquina do Felipe é uso pessoal;
entregar o jar a alguém é redistribuição.

Para destravar a distribuição, uma das três:

1. substituir os modelos dessas espécies por arte nossa (os geradores em `tools/gen_*.py`
   produzem um placeholder de blocagem que é nosso: `IAS_FORCE_GEN=1 python3 tools/gen_<especie>.py`);
2. licenciar a arte com os autores do Revival;
3. voltar a arte para fora do repositório (era um resource pack local, ver o histórico do
   git em `tools/install-revival-placeholders.py`).

## Derivado do Minecraft, só a silhueta

`textures/item/fur_*.png`, `textures/item/pelt.png` e `textures/models/armor/fur_layer_*.png`
(`tools/gen_fur_armor.py`) usam das texturas de couro do Minecraft apenas o canal alfa — onde há
pixel no layout UV da armadura e o contorno do ícone. Toda a cor é gerada pelo script.

## Nosso

Tudo o mais: geometria, texturas e animações geradas por `tools/gen_<especie>.py`, os JSONs
de espécie em `data/`, os modelos de item e de bloco, e as texturas de item e de bloco.
