package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Camuflagem de quem vive escondido no sub-bosque (Ornitholestes): com folhas, mato ou neve alta em volta, quem o
 * procura — a presa que vigia, o predador que caça — só o nota a uma fração do raio de sempre
 * ({@code behavior.habits.camouflage}).
 * Sem classes do Minecraft (D10).
 */
public final class Camouflage {
    /** Blocos de esconderijo em volta (pés, cabeça e os quatro vizinhos de cada) para contar como escondido. */
    public static final int MIN_COVER_BLOCKS = 2;
    /** Camadas de neve a partir das quais o monte de neve esconde. */
    public static final int MIN_SNOW_LAYERS = 3;
    /** Sem camuflagem: o raio inteiro. */
    public static final double NONE = 1.0;

    private Camouflage() {
    }

    public static boolean hidden(int coverBlocks) {
        return coverBlocks >= MIN_COVER_BLOCKS;
    }

    /** Até onde quem o procura o nota: o raio todo, ou {@code camouflage} × raio se ele está escondido. */
    public static double perceivedRadius(double radius, double camouflage, boolean hidden) {
        return hidden ? radius * camouflage : radius;
    }
}
