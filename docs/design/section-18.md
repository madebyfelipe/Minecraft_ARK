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

**Conteúdo do Revival desligado ([D22](section-04.md)):** o biome modifier `remove_revival_features` tira os minérios (fóssil, âmbar, permafrost, rocha vulcânica) e a estátua moai; `data/fossil/worldgen/structure_set/` vazios desligam sítios de fóssil, poços de piche, templos astecas, academia egípcia e o barco do Nether; os itens dele saem das abas do criativo (`RevivalCleanup`). O TerraBlender só injeta o bioma vulcão em fontes `multi_noise` puras, então ele não aparece no preset (embrulhado pela nossa).

Nether e End são os do vanilla. Mundos já criados não mudam: o preset vale na criação. Relevo mais dramático (Tectonic) fica opcional e fora do escopo; o preset funciona com ele instalado, porque troca só os biomas.

