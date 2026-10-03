package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Térmica: a coluna de ar quente que sobe do chão aquecido pelo sol, onde o planador ganha altura sem bater as asas.
 * O Quetzalcoatlus, com 10–11 m de envergadura e asas de planador de terra, voava como o condor: subia em círculos
 * nas térmicas e planava de uma à outra (Habib 2008; Witton &amp; Habib 2010).
 *
 * <ul>
 *   <li>Só há térmica de dia, sem chuva e sobre terra: sobre a água o ar não esquenta o bastante.</li>
 *   <li>A subida é plena perto do chão e se esgota no teto da térmica ({@code thermal_ceiling}, blocos acima do
 *   chão); acima dele não há mais o que subir.</li>
 * </ul>
 * Sem classes do Minecraft (D10).
 */
public final class Thermals {
    private Thermals() {
    }

    /** Se há térmica aqui e agora: de dia, sem chuva e sobre terra. */
    public static boolean active(boolean daytime, boolean raining, boolean overWater) {
        return daytime && !raining && !overWater;
    }

    /**
     * A subida da térmica, em blocos/tick, a esta altura acima do chão.
     *
     * @param strength          subida plena, perto do chão ({@code thermal_lift}); 0 = a espécie não usa térmicas
     * @param heightAboveGround blocos entre o planador e o chão embaixo dele
     * @param ceiling           altura acima do chão em que a térmica se esgota ({@code thermal_ceiling})
     */
    public static double lift(double strength, double heightAboveGround, double ceiling) {
        if (strength <= 0.0 || ceiling <= 0.0) {
            return 0.0;
        }
        double fade = 1.0 - Math.max(0.0, heightAboveGround) / ceiling;
        return strength * Math.max(0.0, Math.min(1.0, fade));
    }
}
