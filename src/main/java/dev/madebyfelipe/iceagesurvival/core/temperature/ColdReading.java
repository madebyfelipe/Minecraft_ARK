package dev.madebyfelipe.iceagesurvival.core.temperature;

/**
 * O que o mundo ao redor de um jogador diz sobre o frio, num instante.
 *
 * @param biomeTemperature    temperatura base do bioma na escala do Minecraft (0,8 planície, 0
 *                            planície nevada, −0,5 taiga nevada, −0,7 pico congelado)
 * @param blocksAboveSeaLevel altura acima do nível do mar; negativo conta como zero
 * @param night               se é noite
 * @param exposedToStorm      se está precipitando e o céu alcança o jogador
 * @param sheltered           se há teto sobre o jogador
 * @param heatProximity       0 (nenhuma fonte de calor por perto) a 1 (dentro de uma)
 * @param insulation          soma do isolamento do couro vanilla e da roupa de pele do mod
 * @param wet                 se está na água ou tomando chuva: roupa molhada não segura o calor
 */
public record ColdReading(
        double biomeTemperature,
        int blocksAboveSeaLevel,
        boolean night,
        boolean exposedToStorm,
        boolean sheltered,
        double heatProximity,
        double insulation,
        boolean wet) {
}
