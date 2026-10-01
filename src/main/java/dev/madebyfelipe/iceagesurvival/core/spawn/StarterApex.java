package dev.madebyfelipe.iceagesurvival.core.spawn;

/**
 * O apex garantido perto do spawn: um T-Rex mora num território pequeno a menos de
 * {@link #OUTER_RADIUS} blocos do spawn do mundo. A natureza não garante nada (peso 1, e só a
 * partir da distância mínima da espécie); este fica sempre ali, e volta depois de um tempo se
 * morrer ou for domesticado.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class StarterApex {
    /** Distâncias do spawn em que o centro do território é sorteado. */
    public static final int MIN_DISTANCE = 220;
    public static final int MAX_DISTANCE = 265;
    /** Raio do território: centro + raio nunca passa de {@link #OUTER_RADIUS}. */
    public static final int TERRITORY_RADIUS = 32;
    /** O apex fica dentro deste raio do spawn. */
    public static final int OUTER_RADIUS = 300;
    /** Não nasce à vista: nenhum jogador a menos disto do ponto. */
    public static final int MIN_PLAYER_GAP = 48;
    /** Com um jogador a menos disto da última posição conhecida e o apex não carregado ali, sumiu. */
    public static final int LOST_CHECK_RADIUS = 32;
    /** Morto ou domesticado, outro chega depois deste tempo (três dias de jogo). */
    public static final long RESPAWN_TICKS = 24000L * 3;

    private StarterApex() {
    }

    /** Se um centro de território a esta distância do spawn mantém o apex dentro do raio. */
    public static boolean validCenter(double distanceFromSpawn) {
        return distanceFromSpawn >= MIN_DISTANCE && distanceFromSpawn + TERRITORY_RADIUS <= OUTER_RADIUS;
    }

    /** Se é hora de pôr um apex: nenhum vivo e a espera depois do último já passou. */
    public static boolean due(boolean hasApex, long now, long respawnAt) {
        return !hasApex && now >= respawnAt;
    }
}
