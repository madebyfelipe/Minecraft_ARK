package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * O pescador ({@code behavior.habits.fishing}), pela biologia do Baryonyx: estômago com escamas de peixe, focinho de
 * gavial e a garra do polegar como gancho. Vadeia a água rasa, fica parado olhando a água e dá o bote.
 *
 * <ul>
 *   <li><b>Água pescável:</b> profundidade de 1 a {@link #MAX_DEPTH} blocos e ar logo acima. Gelo por cima não
 *   deixa pescar — no mundo Era do Gelo, só os trechos de água aberta.</li>
 *   <li><b>Espera:</b> parado de {@link #MIN_WAIT_TICKS} a {@link #MAX_WAIT_TICKS} ticks (6 a 15 s), e o bote.</li>
 *   <li><b>Bote:</b> peixe vivo (tag {@code iceagesurvival:fish}) a até {@link #LIVE_FISH_REACH} blocos é captura
 *   certa; sem peixe à vista, apanha com a chance da espécie.</li>
 *   <li><b>Captura:</b> o selvagem come (vale {@link #MEAL_FRACTION} da fome, como a carniça, e sacia o bando); o
 *   domesticado guarda um peixe cru no inventário — salmão em rio ou água fria, bacalhau no resto.</li>
 * </ul>
 * Sem classes do Minecraft (D10).
 */
public final class Fishing {
    /** Água mais funda que isto não dá pé para quem pesca vadeando. */
    public static final int MAX_DEPTH = 2;
    /** A espera mais curta antes do bote: 6 s. */
    public static final int MIN_WAIT_TICKS = 120;
    /** A espera mais longa antes do bote: 15 s. */
    public static final int MAX_WAIT_TICKS = 300;
    /** Peixe vivo a até esta distância (blocos) é pego no bote. */
    public static final double LIVE_FISH_REACH = 4.0;
    /** Um peixe mata esta fração da fome (a mesma de um pedaço de carniça). */
    public static final double MEAL_FRACTION = 0.5;
    /** Chegou ao ponto de pesca: a esta distância horizontal (blocos) da água escolhida. */
    public static final double ARRIVE_DISTANCE = 2.5;
    /** Domesticado, pesca na água a até esta distância de onde mandaram parar (padrão do JSON). */
    public static final double DEFAULT_TAME_RADIUS = 6.0;
    /** Duração do bote, em ticks; o resultado sai no fim dele. */
    public static final int STRIKE_TICKS = 12;
    /** Comendo o peixe, em ticks. */
    public static final int EAT_TICKS = 60;

    /** O peixe que o domesticado guarda. */
    public enum Catch { SALMON, COD }

    private Fishing() {
    }

    /**
     * Se dá para pescar nesta água.
     *
     * @param depth     blocos de água da superfície até o fundo (0 = não é água)
     * @param openAbove ar logo acima da superfície; gelo ou qualquer bloco por cima = não
     */
    public static boolean fishable(int depth, boolean openAbove) {
        return openAbove && depth >= 1 && depth <= MAX_DEPTH;
    }

    /** Tempo parado olhando a água, com {@code roll} uniforme em [0, 1). */
    public static int waitTicks(double roll) {
        return MIN_WAIT_TICKS + (int) Math.floor((MAX_WAIT_TICKS - MIN_WAIT_TICKS + 1) * clamp01(roll));
    }

    /**
     * Se o bote apanha um peixe.
     *
     * @param liveFishInReach há peixe vivo a até {@link #LIVE_FISH_REACH}: captura certa
     * @param chance          chance da espécie por bote, sem peixe à vista
     * @param roll            uniforme em [0, 1)
     */
    public static boolean strikeCatches(boolean liveFishInReach, double chance, double roll) {
        return liveFishInReach || roll < chance;
    }

    /**
     * O peixe que o domesticado guarda: salmão em rio e em água fria (onde ele vive no vanilla: rios, rios congelados,
     * oceanos frios), bacalhau no resto (oceanos temperados).
     */
    public static Catch catchFor(boolean river, boolean cold) {
        return river || cold ? Catch.SALMON : Catch.COD;
    }

    /** O domesticado só pesca enquanto há lugar para mais um peixe no inventário. */
    public static boolean tameKeepsFishing(boolean inventoryHasRoom) {
        return inventoryHasRoom;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(0.999999, value));
    }
}
