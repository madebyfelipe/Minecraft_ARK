package dev.madebyfelipe.iceagesurvival.endgame;

/**
 * As três fases do Giganotosaurus da arena (D47), pela fração de vida que resta. Lógica pura, sem Minecraft: a
 * entidade ({@code GiganotosaurusBoss}) pergunta aqui quanto do golpe passa, quanto mais rápido ela anda e quão
 * fundo a mordida corta.
 *
 * <ul>
 *   <li><b>Fase 1</b> (100–50%): caça e corta. A mordida abre sangramento, que empilha até 3 níveis.</li>
 *   <li><b>Fase 2</b> (abaixo de 50%): ruge, agrupa e investe derrubando. 30% do dano não passa.</li>
 *   <li><b>Fase 3</b> (abaixo de 25%): ferido e frenético. Mais rápido, a mordida corta dois níveis de uma vez,
 *   até 5, e 50% do dano não passa.</li>
 * </ul>
 */
public enum BossPhase {
    ONE(1, 0.0, 0.0, 1, 3, 120, 0),
    TWO(2, 0.30, 0.10, 1, 3, 120, 160),
    THREE(3, 0.50, 0.35, 2, 5, 160, 100);

    /** Abaixo desta fração da vida máxima começa a fase 2. */
    public static final double PHASE_TWO_BELOW = 0.50;
    /** Abaixo desta fração da vida máxima começa a fase 3. */
    public static final double PHASE_THREE_BELOW = 0.25;

    private final int number;
    private final double damageReduction;
    private final double speedBonus;
    private final int bleedLevelsPerBite;
    private final int bleedLevelCap;
    private final int bleedTicks;
    private final int chargeCooldownTicks;

    BossPhase(int number, double damageReduction, double speedBonus, int bleedLevelsPerBite, int bleedLevelCap,
              int bleedTicks, int chargeCooldownTicks) {
        this.number = number;
        this.damageReduction = damageReduction;
        this.speedBonus = speedBonus;
        this.bleedLevelsPerBite = bleedLevelsPerBite;
        this.bleedLevelCap = bleedLevelCap;
        this.bleedTicks = bleedTicks;
        this.chargeCooldownTicks = chargeCooldownTicks;
    }

    /** A fase pela fração da vida (0 a 1). */
    public static BossPhase of(double healthFraction) {
        if (healthFraction < PHASE_THREE_BELOW) {
            return THREE;
        }
        return healthFraction < PHASE_TWO_BELOW ? TWO : ONE;
    }

    /** 1, 2 ou 3. */
    public int number() {
        return number;
    }

    /** Fração do dano que não passa (0 / 0,3 / 0,5). */
    public double damageReduction() {
        return damageReduction;
    }

    /** O dano que passa desta fase. */
    public float reduce(float amount) {
        return (float) (amount * (1.0 - damageReduction));
    }

    /** Bônus de velocidade sobre a da espécie (multiplicador somado: +10% na fase 2, +35% na 3). */
    public double speedBonus() {
        return speedBonus;
    }

    /** Quantos níveis de sangramento cada mordida soma. */
    public int bleedLevelsPerBite() {
        return bleedLevelsPerBite;
    }

    /** Até quantos níveis a mordida desta fase empilha o sangramento. */
    public int bleedLevelCap() {
        return bleedLevelCap;
    }

    /** Quanto dura o sangramento depois da última mordida, em ticks (renova a cada mordida). */
    public int bleedTicks() {
        return bleedTicks;
    }

    /** Se a fase investe derrubando (fase 2 em diante). */
    public boolean charges() {
        return chargeCooldownTicks > 0;
    }

    /** Quanto espera entre uma investida e a próxima, em ticks: 8 s na fase 2, 5 s na 3. */
    public int chargeCooldownTicks() {
        return chargeCooldownTicks;
    }

    /**
     * O nível de sangramento depois de uma mordida, como amplificador do efeito (nível 1 = 0): soma {@code levels}
     * ao que a vítima já tem ({@code currentAmplifier}, ou -1 se não sangra) e para no teto ({@code cap} níveis).
     * Uma vítima que já sangra acima do teto (fase 3, depois uma mordida da fase 1) não baixa.
     */
    public static int stackedAmplifier(int currentAmplifier, int levels, int cap) {
        int current = Math.max(-1, currentAmplifier);
        int stacked = Math.min(current + levels, cap - 1);
        return Math.max(current, stacked);
    }
}
