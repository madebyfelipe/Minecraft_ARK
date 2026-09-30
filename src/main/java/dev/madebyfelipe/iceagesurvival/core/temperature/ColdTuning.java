package dev.madebyfelipe.iceagesurvival.core.temperature;

/**
 * Balanceamento do frio, vindo da configuração de servidor.
 *
 * <p>{@code altitudeDropPerBlock}, {@code nightDrop}, {@code stormDrop} e {@code wetDrop} são quedas na
 * temperatura do lugar, na escala do Minecraft; {@code shelterWarmth} e {@code heatWarmth} são proteção,
 * na mesma escala do frio devolvido por {@link Coldness#chill}.
 */
public record ColdTuning(
        double altitudeDropPerBlock,
        double nightDrop,
        double stormDrop,
        double shelterWarmth,
        double heatWarmth,
        double wetDrop) {
}
