# Ice Age Survival

Mod de sobrevivência e domesticação para Minecraft Java Edition 1.20.1 (Forge 47.4.3): um mundo congelado, criaturas pré-históricas, torpor, domesticação, genética e um superpredador no fim.

O design e as decisões técnicas estão em [CLOUD.md](CLOUD.md).

## Compatibilidade do modpack

- **Ice Age - Frozen World não é compatível:** o lançamento para Minecraft 1.20.1 é somente Fabric; este modpack usa Forge. O preset `iceagesurvival:ice_age` continua sendo o worldgen glacial do pack.
- **Fauna inicial:** velociraptores agora nascem em tundras e picos de gelo, inclusive próximos do spawn, em bandos maiores, caçam Elasmotérios solitários e a reposição mantém até 72 criaturas selvagens do mod por área de jogador.
- Mods de mapa e interface, como Xaero's Minimap, são opcionais de cliente e devem ser instalados na versão Forge 1.20.1 correspondente; não são dependências do jar do Ice Age Survival.

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
