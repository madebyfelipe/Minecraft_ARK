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

