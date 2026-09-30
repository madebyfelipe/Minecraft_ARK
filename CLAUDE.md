# Ice Age Survival — instruções para o Claude

Mod NeoForge 1.21.1 de criaturas pré-históricas (domesticação, torpor, frio, montaria). O
projeto e a documentação são em **português**; o roadmap e as decisões técnicas ficam em
[CLOUD.md](CLOUD.md). Leia o CLOUD.md antes de qualquer etapa.

## Decisão: qual modelo usar (2026-09-30)

| Tipo de trabalho | Modelo |
|---|---|
| Espécie nova pela receita do CLOUD.md §"Adicionar uma espécie terrestre", ajustes de dados/config, testes, documentação, assets por script | **Sonnet 5.5** |
| Sistemas novos ou decisões que atravessam várias camadas: Etapa 7 (montaria, autoridade de movimento em multiplayer), Etapa 8 (reprodução e genética), Etapa 9 (worldgen, D6), Etapa 10 (boss e teto de 1024 de vida) | **Opus 5.5** |

**Por quê:** o trabalho de receita é delimitado e os testes automáticos pegam erro; o custo de
errar o desenho de um sistema novo (sobretudo o que só aparece com dois clientes) é maior que
o custo do modelo. A recomendação vem do formato das tarefas, não de benchmark neste
repositório — reavaliar comparando o diff de uma espécie feita por cada modelo.

## Trabalhar com subagentes, cada um com uma função

Etapas grandes devem ser divididas: o agente principal **coordena e integra**, e subagentes
fazem partes independentes em paralelo, cada um com **uma função só**. Isso mantém o contexto
do principal limpo e acelera etapas com muitos arquivos.

Funções típicas (ajuste ao que a etapa pede):

- **Assets** — `tools/gen_<especie>.py`, geometria, textura, animações, placeholder do Revival.
- **Dados** — JSON de espécie, biome modifier, tag de biomas, traduções.
- **Núcleo/Java** — registros, código novo em `core/` e `entity/`, só quando a receita não basta.
- **Testes** — JUnit de lógica pura e GameTest; escreve os testes a partir da especificação,
  sem copiar a implementação.
- **Revisão** — lê o diff pronto e procura bugs, sem editar (`/code-review`).
- **Pesquisa** — varre código ou mods de referência e devolve só as conclusões.

Regras:

1. Dê a cada subagente um prompt **autocontido**: objetivo, arquivos que ele pode tocar, o que
   não pode, e como provar que terminou. Ele não herda a conversa.
2. **Sem sobreposição de arquivos** entre subagentes em paralelo. Se duas funções precisam do
   mesmo arquivo (ex.: `ModEntities.java`, `lang/*.json`), só uma delas edita.
3. Só delegue o que é independente. Tarefa pequena (uma espécie simples, um ajuste de config)
   é mais rápida direto, sem subagente.
4. O principal **confere o resultado**: roda `./gradlew build` e `./gradlew runGameTestServer`
   depois de integrar; o relatório do subagente não basta.
5. Decisões de arquitetura (formato de dados, autoridade de rede) ficam com o principal.

## Fluxo de trabalho

- Mudança de código em **worktree** isolado; commit na branch e push. **Nunca push na `main`**
  nem force-push; a integração é por PR.
- Verificar antes de dizer que terminou: `./gradlew build` (JUnit) e
  `./gradlew runGameTestServer` (servidor real). `runClient` e multiplayer são manuais.
- Testar no jogo: `tools/deploy-prism.sh` copia o jar para a instância `IceAgeSurvival` do
  Prism (usa cópia, não atalho). Placeholders de modelo: `tools/install-revival-placeholders.py`.
- **Licença:** a arte do F&A Revival é *All Rights Reserved* e **está no repositório** por
  decisão do Felipe (2026-09-30), logo também vai no jar. Consequência: o jar não pode ser
  distribuído a ninguém enquanto isso. Espécies afetadas em `tools/hand_authored.txt`;
  detalhes e as três saídas em `src/main/resources/assets/iceagesurvival/ASSET_LICENSES.md`.
  Não regerar essas espécies com `tools/gen_*.py` — o `modelgen` recusa de propósito.
- Ao fechar uma etapa ou decisão estrutural: atualizar o roadmap (§23) e o registro de
  mudanças (§24) do CLOUD.md.
