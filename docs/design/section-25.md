## 25. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| **Assets de criaturas.** Parte dos modelos usa recursos externos do F&A Revival em runtime. | Médio — jogador precisa instalar a versão Forge compatível do Revival; mudanças internas no mod upstream podem renomear recursos e quebrar a aparência. Os assets do Revival não são empacotados nem distribuídos pelo addon. | Fixar e documentar a versão compatível, testar com Revival instalado e substituir gradualmente por arte original. |
| Escopo: dez etapas, vários sistemas grandes. | Alto | MVP estreito; não avançar com etapa instável. |
| Balanceamento de torpor/níveis/genética. | Médio | Tudo em dados e config; testes de lógica pura. |
| O frio matar o jogador na primeira hora, antes de haver couro ou fogueira. | Médio | Curva e tempos todos em config; medir em jogo e afrouxar `coldSecondsToFreeze` ou a temperatura de conforto. |
| Montaria em multiplayer (latência, dessincronização). | Baixo — resolvido reaproveitando o `travelRidden` do vanilla ([D18](section-04.md)); a autoridade de movimento é a mesma do cavalo. | Conferir com dois clientes de verdade, que é o que ainda não foi feito. |
| Muitas entidades com IA em servidor. | Médio | Ver [22](section-22.md). Varreduras de entidade são espaçadas (manada ~5 s, caça ~30 s, reposição ~45 s, defesa só ao ser ferida). Ainda não medido com fauna densa. |
| Fauna finita: criaturas `CREATURE` só nascem com a geração do terreno. | Resolvido na Etapa 7 pela reposição própria ([D20](section-04.md)). | Novo risco em troca: a reposição encher o mundo. Contido pelo `max_nearby` por espécie; medir a densidade em jogo ao longo de uma sessão longa. |
| Teto de 1024 de vida do vanilla limita criaturas gigantes e o apex secreto. | Médio | A decidir quando o apex for desenhado (D51); a solução da D47 (1024 com resistência por fase) saiu junto com o boss. |
| 1.21.1 envelhecer. | Baixo | `core/` independente do Minecraft facilita port. |

