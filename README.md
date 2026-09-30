# Ice Age Survival

Mod de sobrevivência e domesticação para Minecraft Java Edition 1.21.1 (NeoForge): um mundo congelado, criaturas pré-históricas, torpor, domesticação, genética e um superpredador no fim.

O design e as decisões técnicas estão em [CLOUD.md](CLOUD.md).

## Desenvolvimento

Requer JDK 21.

```sh
./gradlew build            # compila e roda os testes
./gradlew runClient        # cliente de desenvolvimento
./gradlew runServer        # servidor dedicado de desenvolvimento
./gradlew runGameTestServer
tools/deploy-prism.sh       # copia o jar para a instância de teste do Prism Launcher
```

A instância do Prism usa uma cópia do jar. Depois de `./gradlew build`, rode `tools/deploy-prism.sh` e reinicie o jogo para testar a versão nova.
