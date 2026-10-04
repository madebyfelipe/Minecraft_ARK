package dev.madebyfelipe.iceagesurvival.core.titan;

/**
 * As três fases do Titanovenator, pela fração de vida que resta. Lógica pura, sem Minecraft: a entidade pergunta aqui
 * quanto do golpe passa, quanto mais rápido ela anda, quão fundo a mordida corta e qual variante de animação tocar.
 *
 * <p>Números propostos (o Felipe só definiu as três fases e que o boss é bem maior que o Rex; ajustar aqui):
 * <ul>
 *   <li><b>Fase 1</b> (100–66%): territorial. Caça e morde; a mordida abre sangramento, até 3 níveis.</li>
 *   <li><b>Fase 2</b> (abaixo de 66%): ruge e passa a investir derrubando. 30% do dano não passa.</li>
 *   <li><b>Fase 3</b> (abaixo de 33%): frenesi. Mais rápido, investidas mais seguidas, a mordida corta dois níveis de
 *   uma vez (até 5) e 50% do dano não passa.</li>
 * </ul>
 */
public enum TitanPhase {
    ONE(1, 0.0, 0.0, 1, 3, 160, 0, ""),
    TWO(2, 0.30, 0.10, 1, 3, 160, 160, "_f2"),
    THREE(3, 0.50, 0.25, 2, 5, 200, 100, "_f3");

    /** Abaixo desta fração da vida máxima começa a fase 2. */
    public static final double PHASE_TWO_BELOW = 0.66;
    /** Abaixo desta fração da vida máxima começa a fase 3. */
    public static final double PHASE_THREE_BELOW = 0.33;

    private final int number;
    private final double damageReduction;
    private final double speedBonus;
    private final int bleedLevelsPerBite;
    private final int bleedLevelCap;
    private final int bleedTicks;
    private final int chargeCooldownTicks;
    private final String animationSuffix;

    TitanPhase(int number, double damageReduction, double speedBonus, int bleedLevelsPerBite, int bleedLevelCap,
               int bleedTicks, int chargeCooldownTicks, String animationSuffix) {
        this.number = number;
        this.damageReduction = damageReduction;
        this.speedBonus = speedBonus;
        this.bleedLevelsPerBite = bleedLevelsPerBite;
        this.bleedLevelCap = bleedLevelCap;
        this.bleedTicks = bleedTicks;
        this.chargeCooldownTicks = chargeCooldownTicks;
        this.animationSuffix = animationSuffix;
    }

    /** A fase pela fração da vida (0 a 1). */
    public static TitanPhase of(double healthFraction) {
        if (healthFraction < PHASE_THREE_BELOW) {
            return THREE;
        }
        return healthFraction < PHASE_TWO_BELOW ? TWO : ONE;
    }

    /** 1, 2 ou 3. */
    public int number() {
        return number;
    }

    /** Fração do dano que não passa (0 / 0,30 / 0,50). */
    public double damageReduction() {
        return damageReduction;
    }

    /** O dano que passa desta fase. */
    public float reduce(float amount) {
        return (float) (amount * (1.0 - damageReduction));
    }

    /** Bônus de velocidade sobre a da espécie (+10% na fase 2, +25% na 3). */
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

    /** Quanto dura o sangramento depois da última mordida, em ticks. */
    public int bleedTicks() {
        return bleedTicks;
    }

    /** Se a fase investe derrubando (fase 2 em diante). */
    public boolean charges() {
        return chargeCooldownTicks > 0;
    }

    /** Espera entre uma investida e a próxima, em ticks: 8 s na fase 2, 5 s na 3. */
    public int chargeCooldownTicks() {
        return chargeCooldownTicks;
    }

    /** Sufixo das animações desta fase no arquivo de animações: vazio, {@code _f2} ou {@code _f3}. */
    public String animationSuffix() {
        return animationSuffix;
    }
}
