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

