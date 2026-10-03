# Procedência dos assets de criatura

Este mod usa modelos, texturas, animações e sons fornecidos em runtime pelo mod
**Fossils and Archeology: Revival** para algumas espécies. Esses arquivos não são copiados,
incluídos neste repositório nem empacotados no jar do Ice Age Survival. O mod Revival deve ser
instalado separadamente, a partir de um canal oficial, em uma versão compatível com Minecraft
1.20.1 Forge.

Da mesma forma, o **Baryonyx** usa o modelo Tabula, as poses, as texturas e os sons do mod
**Jurassic Reborn** (All Rights Reserved), lidos em runtime do namespace `jurassicreborn`, e o
**Prehistoric Fauna** (All Rights Reserved) fica disponível como fonte de modelos para espécies
futuras. Nenhum arquivo dos dois é copiado para este repositório ou para o jar; ambos (e o Citadel,
dependência do Jurassic Reborn) devem ser instalados separadamente. O conteúdo próprio deles fica
desligado por dados (`tools/gen_fauna_cleanup.py`).

Os demais assets deste diretório são do projeto. `textures/item/fur_*.png`, `textures/item/pelt.png`
e `textures/models/armor/fur_layer_*.png` (`tools/gen_fur_armor.py`) usam das texturas de couro do
Minecraft apenas o canal alfa; toda a cor é gerada pelo script.

Os modelos 3D e as texturas das armas tranquilizantes (`models/item/tranq_rifle.json`,
`models/item/tranq_crossbow.json`, `textures/item/tranq_rifle.png`, `textures/item/tranq_crossbow.png`
e `textures/item/tranq_dart.png`) são autorais, gerados por `tools/gen_weapons.py`; nenhum asset de
terceiros foi usado.

O preset `datapacks/tfc_compat/data/iceagesurvival/worldgen/world_preset/ice_age.json` reproduz a
estrutura e as camadas de rocha (`rock_layer_settings`) do preset `tfc:overworld` do
**TerraFirmaCraft** (licença EUPL-1.2), mudando só a temperatura para uma constante fria. Esse
pacote só é ativado quando o TerraFirmaCraft está instalado; o TFC não é distribuído no jar do Ice
Age Survival e deve ser instalado separadamente.

## Primal Stage (MIT)

Os modelos de bloco das estações primitivas — `models/block/primitive_grill.json`,
`models/block/kiln.json`, `models/block/drying_rack.json`, `models/block/cutting_log.json` e
`models/block/stone_anvil.json` — e a textura `textures/block/kiln_bricks.png` vêm do mod
**Primal Stage** 1.2.6, de nanokulon, sob a licença MIT abaixo. O código das estações foi
reimplementado para Forge neste projeto (`primal/`).

```
MIT License

Copyright (c) 2023 nanokulon

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

`textures/item/cave_tracker.png` (Rastreador da Caverna, D47) é autoral, desenhada pixel a pixel por `tools/gen_cave_tracker.py`.

O **Anquilossauro** usa o modelo, a textura, as animações e os sons do Revival (`fossil:ankylosaurus`),
carregados em runtime; nenhum desses arquivos é copiado para este repositório ou para o jar. As
caixas de acerto em `data/iceagesurvival/hitboxes/ankylosaurus.json` são do projeto, medidas a
partir das posições dos ossos do modelo. `textures/item/dung.png` (Esterco) e
`textures/mob_effect/broken_leg.png` (Perna quebrada) são autorais, desenhadas pixel a pixel por
`tools/gen_dung.py`.
