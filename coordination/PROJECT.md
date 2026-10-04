# Minecraft — contexto mínimo

Forge 47.4.3 / Minecraft 1.20.1 / Java 17. Mod de sobrevivência, criaturas
pré-históricas, torpor, domesticação, frio, montaria, reprodução e genética.
Dados de espécies em JSON; `core/` concentra lógica sem Minecraft, testada em JUnit.
O servidor decide. Rede atual: Forge `SimpleChannel` (`network/ModPayloads.java`).

## Estado válido

Etapas 2–9 têm implementações e testes documentados; várias ainda aguardam
validação manual. Não chamar isso de validação manual concluída.
D51 removeu caverna/arena/boss antigo; Giganotosaurus é fauna comum.
Endgame: postos com registros (D52) e base única com a contenção do apex (D53)
implementados; falta o Titanovenator como criatura e a luta em jogo.
O estudo do Titanovenator está em `codex/titanovenator-modelo`, sem integração ao jogo.
Não integrar essa branch como parte de uma tarefa de processo.

## Referências sob demanda

- Escopo atual: `docs/design/section-23.md`; decisões: seção 04, só a linha necessária.
- Criaturas: seção 11 + JSON da espécie; domesticação: 13–14; frio: 15.
- Reprodução: 16–17; mundo: 18; endgame: 19–20 e D51.
- Arquitetura: 10; multiplayer/testes: 21; performance: 22; riscos: 25.
- Histórico: 24. Não é material de entrada nem autoridade para desfazer decisões novas.

`python3 tools/project_context.py section 23` / `decision D51` imprimem só o trecho.
Código/configuração atuais prevalecem sobre exemplos técnicos históricos divergentes.

## Entrega

Build: `./gradlew build`; servidor real: `./gradlew runGameTestServer`.
Para iterar: `./gradlew runGameTestServer -Ptests=Base,Outpost` (só essas classes);
a suíte inteira continua obrigatória antes de integrar.
Cliente e multiplayer são manuais. Assets externos não entram no jar nem no Git.
Não criar worktree, não force-push e não integrar sem os dois checks verdes.
