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

