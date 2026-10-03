package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Carcaça: a presa grande (porte do Galimimo para cima, {@code behavior.ecology.carcass_portions} > 0) que morre
 * selvagem sem jogador fica caída com porções de carne, e quem tem fome vem comer — decisão do Felipe (2026-10-02).
 *
 * <ul>
 *   <li>Some quando acaba a carne ou em {@value #LIFETIME_TICKS} ticks (10 min).</li>
 *   <li>Carniceiros e predadores famintos a farejam a {@value #SMELL_RADIUS} blocos.</li>
 *   <li>O mais forte toma (porte × grupo, a mesma conta do confronto); o fraco espera a sobra por perto.</li>
 *   <li>Quem come tira uma porção por bocada; enche a barriga com {@link #mealPortions} porções.</li>
 *   <li>O jogador carneia a sobra com machado ou espada (o que resta, em proporção).</li>
 * </ul>
 * Isca (jogador pôr carcaça para atrair) ficou de fora, por decisão dele.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class Carcass {
    /** Dez minutos. */
    public static final int LIFETIME_TICKS = 20 * 60 * 10;
    /** Faro da carcaça. */
    public static final double SMELL_RADIUS = 64.0;
    /** Uma bocada a cada tanto. */
    public static final int BITE_INTERVAL_TICKS = 40;
    /** Quem espera a sobra fica a esta distância da carcaça. */
    public static final double WAIT_DISTANCE = 14.0;
    /** Volume da caixa de colisão do Galimimo (1,2 × 1,2 × 2,3): a régua de "uma refeição de 3 porções". */
    public static final double GALLIMIMUS_VOLUME = 1.2 * 1.2 * 2.3;
    /** Porções que enchem um comedor do porte do Galimimo. */
    public static final int REFERENCE_MEAL = 3;

    private Carcass() {
    }

    /**
     * Quantas porções enchem a barriga de quem come, pelo volume da caixa dele: o Galimimo 3, o T-Rex 8, o raptor 2.
     * Cresce como a área (expoente 0,42, o mesmo das âncoras do Felipe entre Galimimo e Tricerátopo).
     */
    public static int mealPortions(double eaterVolume) {
        double ratio = Math.max(0.01, eaterVolume / GALLIMIMUS_VOLUME);
        return Math.max(2, (int) Math.round(REFERENCE_MEAL * Math.pow(ratio, 0.42)));
    }

    /** Quanto da fome cada porção mata, para quem come. */
    public static double portionFraction(double eaterVolume) {
        return 1.0 / mealPortions(eaterVolume);
    }

    /**
     * Se quem chega toma a carcaça de quem está comendo: força relativa
     * ({@link ThreatResponse#confrontationPower}) de quem chega sobre quem come maior que 1.
     *
     * @param arrivingPower força de quem chega vista por quem come
     */
    public static boolean takesOver(double arrivingPower) {
        return arrivingPower > 1.0;
    }

    /** Carneada pelo jogador: que fração do loot sai, pelo que resta da carcaça. */
    public static double butcherShare(int portionsLeft, int portionsTotal) {
        if (portionsTotal <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) portionsLeft / portionsTotal));
    }
}
