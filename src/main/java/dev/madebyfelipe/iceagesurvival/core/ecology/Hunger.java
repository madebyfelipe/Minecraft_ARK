package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Fome do predador selvagem, medida pelo tempo desde a última refeição.
 *
 * <ul>
 *   <li><b>Saciado</b> (até {@code satedSeconds}): descansa, não caça nem a presa que passa ao lado.</li>
 *   <li><b>Oportunista</b>: não sai procurando, mas pega a presa fácil e próxima (filhote, ferida, desgarrada).</li>
 *   <li><b>Caçando</b> (a partir de {@code hungerSeconds}): procura presa no raio de caça inteiro e persegue.</li>
 * </ul>
 *
 * Sem classes do Minecraft (D10).
 */
public final class Hunger {
    public enum Drive { SATED, OPPORTUNISTIC, HUNTING }

    /** Ao nascer, a fome começa sorteada entre estas frações do tempo até caçar: há caçadas logo cedo. */
    public static final double SPAWN_MIN_FRACTION = 0.4;
    public static final double SPAWN_MAX_FRACTION = 1.1;

    private Hunger() {
    }

    public static Drive drive(long ticksSinceMeal, int satedSeconds, int hungerSeconds) {
        long sated = satedSeconds * 20L;
        long hungry = Math.max(sated, hungerSeconds * 20L);
        if (ticksSinceMeal < sated) {
            return Drive.SATED;
        }
        return ticksSinceMeal >= hungry ? Drive.HUNTING : Drive.OPPORTUNISTIC;
    }

    /** Ticks desde a última refeição para um animal recém-nascido, com {@code roll} uniforme em [0, 1). */
    public static long spawnTicksSinceMeal(int hungerSeconds, double roll) {
        double fraction = SPAWN_MIN_FRACTION + (SPAWN_MAX_FRACTION - SPAWN_MIN_FRACTION) * roll;
        return Math.round(hungerSeconds * 20L * fraction);
    }
}
