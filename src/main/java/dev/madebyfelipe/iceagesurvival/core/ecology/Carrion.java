package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * O necrófago ({@code behavior.habits.scavenges}): com fome, vai até a carne crua largada no chão — a sobra da
 * caçada de outro predador ou do jogador — e come, mas só se nenhum predador maior estiver perto dela.
 * Sem classes do Minecraft (D10).
 */
public final class Carrion {
    /** Um pedaço de carne mata esta fração da fome. */
    public static final double MEAL_FRACTION = 0.5;
    /** Predador maior a até esta distância da carne: não chega perto. */
    public static final double SAFE_RADIUS = 16.0;
    /** Procura a carne até esta distância. */
    public static final double SEARCH_RADIUS = 24.0;
    /** Ao alcance da boca. */
    public static final double REACH = 1.5;

    private Carrion() {
    }

    /**
     * Se dá para ir comer: sem predador maior a menos de {@link #SAFE_RADIUS} da carne.
     *
     * @param biggerPredatorDistance distância do predador maior mais perto da carne; infinito se não há
     */
    public static boolean safe(double biggerPredatorDistance) {
        return biggerPredatorDistance >= SAFE_RADIUS;
    }
}
