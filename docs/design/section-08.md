## 8. Versões

| Componente | Versão | Fonte |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | 47.4.3 | Forge Maven |
| ForgeGradle | 6.x | Forge Maven |
| Gradle | 8.8 (wrapper) | wrapper do ForgeGradle |
| Java | 17 | exigido pelo Minecraft 1.20.1 |
| GeckoLib | 4.7.2 (`geckolib-forge-1.20.1`) | Cloudsmith do GeckoLib |
| Fossils and Archaeology: Revival | 9.3.4.0 | dependência externa, não empacotada |
| More Hitboxes | 1.9.2 | dependência externa, não empacotada |
| TerraBlender | 3.0.1.10 | dependência runtime exigida pelo Revival |
| Architectury | 9.2.14 | dependência runtime exigida pelo Revival |

O alvo 1.20.1 permite resolver os recursos do Revival no namespace `fossil` e usar More Hitboxes; não transportar código nem assets desses mods.

