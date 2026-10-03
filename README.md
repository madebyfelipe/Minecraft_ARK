# Ice Age Survival

Mod de sobrevivência e domesticação para Minecraft Java Edition 1.20.1 (Forge 47.4.3): um mundo congelado, criaturas pré-históricas, torpor, domesticação, genética e um superpredador no fim.

O design e as decisões técnicas estão em [CLOUD.md](CLOUD.md).
As mecânicas de fome, caça, manadas, ameaças e rivalidade estão descritas em
[docs/ecologia.html](docs/ecologia.html).

## Compatibilidade do modpack

- **Ice Age - Frozen World não é compatível:** o lançamento para Minecraft 1.20.1 é somente Fabric; este modpack usa Forge. O preset próprio `iceagesurvival:ice_age` preserva o relevo normal do overworld, usa planícies nevadas em toda a terra e mantém as águas congeladas, sem dependência de worldgen.
- **Fauna inicial:** velociraptores agora nascem em tundras e picos de gelo, inclusive próximos do spawn, em bandos maiores, caçam Elasmotérios solitários e a reposição mantém até 72 criaturas selvagens do mod por área de jogador.
- **Jurassic Reborn 1.4.2 (+ Citadel 2.6.3) e Prehistoric Fauna 2.3.3** entram no modpack só como fornecedores de modelos, texturas, animações e sons, carregados em runtime (como o Revival; nada deles é copiado para o jar). O conteúdo próprio dos dois fica desligado por dados: fósseis, minérios, plantas e spawns (biome modifiers), estruturas, insetos e action figures no loot (global loot modifiers), profissões de aldeão, o tipo de mundo do Prehistoric Fauna e os itens no criativo. Os arquivos saem de `tools/gen_fauna_cleanup.py`; ao atualizar um dos mods, troque a versão no `build.gradle`, rode `./gradlew test` (baixa o jar novo) e o script de novo. As receitas deles continuam válidas, e as dimensões do Prehistoric Fauna existem mas ficam sem caminho em sobrevivência (o portal exige henostone, que só aparece nas dimensões e nas estruturas desligadas).
- A instância Prism `IceAgeSurvival` tem Xaero's Minimap 26.5.0, Xaero's World Map 1.46.0, JEI 15.62.0.217 + MezzConfig 0.6.6, Jade 11.13.3 e AppleSkin 2.5.1 (Forge 1.20.1), e os mods de desempenho Embeddium 0.3.31, ModernFix 5.27.83 e FerriteCore 6.0.1. São mods externos; nenhum é empacotado ou exigido pelo jar do Ice Age Survival. Xaero's Minimap/World Map são mods de cliente: não os instale no servidor dedicado.

O preset vale apenas para **mundos novos**. Ao criar um, escolha o tipo `Ice Age` em *Criar mundo → Tipo de mundo*; mundos existentes conservam a geração antiga.

## Desenvolvimento

Requer JDK 17.

```sh
./gradlew build            # compila e roda os testes
./gradlew runClient        # cliente de desenvolvimento
./gradlew runServer        # servidor dedicado de desenvolvimento
./gradlew runGameTestServer
tools/deploy-prism.sh       # copia o jar para a instância de teste do Prism Launcher
```

A instância do Prism usa uma cópia do jar. Depois de `./gradlew build`, rode `tools/deploy-prism.sh` e reinicie o jogo para testar a versão nova.

Para publicar um release: suba `mod_version` em `gradle.properties`, escreva as notas em `docs/releases/v<versão>.md` e envie a tag `v<versão>` (ou rode o workflow `Release` à mão, que cria a tag). Ele compila, monta o `.mrpack` (`tools/build_mrpack.py`) e publica o release com o `.mrpack` e o jar.
