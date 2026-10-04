## 22. Performance

- Nada de varredura de entidades a cada tick; usar intervalos e resultados em cache.
- Manada com líder e raio limitado, não N×N.
- IA reduzida longe de jogadores.
- Estado de domesticação seguro a descarregamento de chunk (baseado em game time, não em contadores por tick).
- O frio roda a cada tick por jogador, mas só com aritmética: a leitura do mundo (que varre blocos procurando fonte de calor) acontece uma vez por segundo e fica em cache na capability.
- A reposição de fauna é uma varredura de entidades por jogador a cada 45 s, e uma só para todas as espécies (as contagens saem da mesma lista). A procura por posição não carrega chunk: coluna em chunk descarregado é descartada.

