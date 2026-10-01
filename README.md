# Ice Age Survival

Mod de sobrevivência e domesticação para Minecraft Java Edition 1.20.1 (Forge 47.4.3): um mundo congelado, criaturas pré-históricas, torpor, domesticação, genética e um superpredador no fim.

O design e as decisões técnicas estão em [CLOUD.md](CLOUD.md).

## Compatibilidade do modpack

- **Ice Age - Frozen World não é compatível:** o lançamento para Minecraft 1.20.1 é somente Fabric; este modpack usa Forge. O preset próprio `iceagesurvival:ice_age` usa relevo `amplified`, encostas nevadas, ice spikes e picos congelados, sem biomas verdes ou dependência de worldgen.
- **Fauna inicial:** velociraptores agora nascem em tundras e picos de gelo, inclusive próximos do spawn, em bandos maiores, caçam Elasmotérios solitários e a reposição mantém até 72 criaturas selvagens do mod por área de jogador.
- A instância Prism `IceAgeSurvival` tem Xaero's Minimap 26.5.0, Xaero's World Map 1.46.0, JEI 15.62.0.217 + MezzConfig 0.6.6, Jade 11.13.3 e AppleSkin 2.5.1 (Forge 1.20.1). São mods externos; nenhum é empacotado ou exigido pelo jar do Ice Age Survival. Xaero's Minimap/World Map são mods de cliente: não os instale no servidor dedicado.

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
