# Procedência dos assets de criatura

Este mod usa modelos, texturas, animações e sons fornecidos em runtime pelo mod
**Fossils and Archeology: Revival** para algumas espécies. Esses arquivos não são copiados,
incluídos neste repositório nem empacotados no jar do Ice Age Survival. O mod Revival deve ser
instalado separadamente, a partir de um canal oficial, em uma versão compatível com Minecraft
1.20.1 Forge.

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
