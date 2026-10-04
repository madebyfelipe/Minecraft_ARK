package dev.madebyfelipe.iceagesurvival.core.genetics;

/**
 * Crescimento do filhote, da eclosão (0) ao adulto (1). O tamanho sobe em linha reta de 40% a 100%,
 * sem degraus; a pele de filhote fica na primeira metade, como no Revival, em que o estágio de bebê
 * vai até perto da metade da idade adulta ({@code teenAgeDays} / {@code adultAgeDays} entre 0,4 e 0,5)
 * e o adolescente já usa a pele adulta.
 */
public final class Growth {
    /** Tamanho ao nascer, em fração do adulto. */
    public static final float NEWBORN_SCALE = 0.4F;
    /** Até onde do crescimento vale a pele de filhote. */
    public static final float JUVENILE_LOOK_UNTIL = 0.5F;

    private Growth() {
    }

    /** Escala do corpo para a fração de crescimento {@code progress} (limitada a 0–1). */
    public static float scale(float progress) {
        return NEWBORN_SCALE + (1.0F - NEWBORN_SCALE) * clamp(progress);
    }

    /** Se a criatura ainda tem cara de filhote. */
    public static boolean juvenileLook(float progress) {
        return clamp(progress) < JUVENILE_LOOK_UNTIL;
    }

    /**
     * Fração cumprida a partir da idade do vanilla ({@code age}, negativa enquanto filhote, contando para
     * zero) e do total de ticks do crescimento.
     */
    public static float progress(int age, int totalTicks) {
        if (age >= 0 || totalTicks <= 0) {
            return 1.0F;
        }
        return clamp(1.0F + age / (float) totalTicks);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
