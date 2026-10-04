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

