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

**Afinidade** (0 a 100): começa em `50 × eficiência`. Ver [13.1](section-131.md) para o que ela faz e como sobe.

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

