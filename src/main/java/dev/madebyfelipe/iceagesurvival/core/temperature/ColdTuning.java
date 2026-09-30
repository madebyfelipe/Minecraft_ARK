package dev.madebyfelipe.iceagesurvival.core.temperature;

/**
 * Balanceamento do frio, vindo da configuração de servidor.
 *
 * <p>Os três primeiros valores são quedas na temperatura do lugar, na escala do Minecraft; os três
 * últimos são proteção, na mesma escala do frio devolvido por {@link Coldness#chill}.
 */
public record ColdTuning(
        double altitudeDropPerBlock,
        double nightDrop,
        double stormDrop,
        double shelterWarmth,
        double heatWarmth,
        double insulationPerPiece) {
}
