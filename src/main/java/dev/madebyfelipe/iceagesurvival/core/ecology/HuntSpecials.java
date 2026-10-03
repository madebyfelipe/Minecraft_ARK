package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * O golpe próprio de cada caçador, pela biologia real:
 * <ul>
 *   <li><b>Emboscada</b> (Smilodon, 160–280 kg, patas dianteiras mais fortes que as de qualquer felino vivo): a
 *   arrancada mais rápida dos três, curta; o primeiro golpe agarra e prende a presa.</li>
 *   <li><b>Salto</b> (Utahraptor, caçador de bando): pula sobre a presa a média distância.</li>
 *   <li><b>Bicada</b> (Kelenken, a maior ave-terrível): o bico fundido ao crânio golpeia como uma marreta (parte do
 *   dano ignora armadura) e a ave recua em seguida — golpe e recua. Num escudo erguido, o bico trava: a fraqueza.</li>
 *   <li><b>Agarrão</b> (Ornitholestes, 15 kg, mãos longas de garras afiadas): sem arrancada; o primeiro golpe do bote
 *   prende a presa pequena, que não escapa correndo.</li>
 *   <li><b>Garra-gancho</b> (Baryonyx, garra do polegar de ~31 cm, provável gancho de peixe): o primeiro golpe depois
 *   do bote fisga a presa na água, ou a do porte dele para baixo em terra, puxa-a para perto e a prende.</li>
 * </ul>
 * Sem classes do Minecraft (D10).
 */
public final class HuntSpecials {
    /** Emboscada: +50% de velocidade por 4 s a partir do bote. */
    public static final double AMBUSH_SPEED_BONUS = 0.5;
    public static final int AMBUSH_TICKS = 80;
    /** O agarrão: lentidão forte (nível 5) por 2 s. */
    public static final int GRAB_TICKS = 40;
    public static final int GRAB_AMPLIFIER = 4;
    /** Bicada: fração do dano que ignora armadura, somada ao golpe. */
    public static final double BEAK_PIERCE = 0.3;
    /** A bicada que bate num escudo trava o bico: a ave fica parada e sem atacar por 2 s. */
    public static final int BEAK_STUCK_TICKS = 40;
    /** Mordida que corta ({@code bleed}): níveis de sangramento por golpe, teto e duração (ticks). */
    public static final int BLEED_LEVELS_PER_BITE = 1;
    public static final int BLEED_LEVEL_CAP = 3;
    public static final int BLEED_TICKS = 120;
    /** Recuo depois da bicada, em ticks. */
    public static final int RETREAT_TICKS = 30;
    /** Salto: só a esta distância da presa (perto demais, morde; longe demais, corre). */
    public static final double LEAP_MIN = 3.0;
    public static final double LEAP_MAX = 10.0;
    public static final double LEAP_SPEED = 0.9;
    public static final double LEAP_LIFT = 0.45;

    /** Agarrão: o bote deixa o golpe pronto por este tempo. */
    public static final int GRAB_WINDOW_TICKS = 80;
    /** Agarrão: só prende presa até este porte relativo (área de colisão da presa ÷ a do caçador). */
    public static final double GRAB_MAX_SIZE_RATIO = 1.0;

    /** Garra-gancho: o bote (da caçada ou da pesca) deixa o golpe pronto por este tempo (4 s). */
    public static final int GAFF_WINDOW_TICKS = 80;
    /** Garra-gancho: fisga presa na água de qualquer porte, e em terra até este porte relativo. */
    public static final double GAFF_MAX_SIZE_RATIO = 1.0;
    /** Garra-gancho: velocidade máxima do puxão (blocos/tick) numa presa do porte do caçador ou menor. */
    public static final double GAFF_PULL_MAX = 0.9;
    /** Garra-gancho: quanto o puxão cresce por bloco de distância. */
    public static final double GAFF_PULL_PER_BLOCK = 0.25;
    /** Garra-gancho: o puxão para a esta distância (blocos) do caçador — a presa fica ao alcance da boca. */
    public static final double GAFF_HOLD_DISTANCE = 1.5;
    /** Garra-gancho: o tranco para cima que tira a presa da água. */
    public static final double GAFF_LIFT = 0.25;

    private HuntSpecials() {
    }

    /**
     * A garra-gancho fisga a presa na água (o peixe, o que nada) de qualquer porte; em terra, só a do porte do caçador
     * para baixo.
     *
     * @param inWater   a presa está na água
     * @param sizeRatio área de colisão da presa ÷ a do caçador
     */
    public static boolean gaffs(boolean inWater, double sizeRatio) {
        return inWater || sizeRatio <= GAFF_MAX_SIZE_RATIO;
    }

    /**
     * O puxão da garra-gancho: na direção do caçador, mais forte quanto mais longe (até {@link #GAFF_PULL_MAX}) e
     * dividido pelo porte da presa maior que ele — a presa grande na água vem menos. Para a
     * {@link #GAFF_HOLD_DISTANCE} do caçador.
     *
     * @param dx        caçador menos presa, em x
     * @param dz        caçador menos presa, em z
     * @param sizeRatio área de colisão da presa ÷ a do caçador
     * @return velocidade {x, y, z} da presa
     */
    public static double[] gaffPull(double dx, double dz, double sizeRatio) {
        double length = Math.sqrt(dx * dx + dz * dz);
        double reach = Math.max(0.0, length - GAFF_HOLD_DISTANCE);
        if (length < 1.0E-6 || reach <= 0.0) {
            return new double[] {0.0, GAFF_LIFT / Math.max(1.0, sizeRatio), 0.0};
        }
        double weight = Math.max(1.0, sizeRatio);
        double strength = Math.min(GAFF_PULL_MAX, reach * GAFF_PULL_PER_BLOCK) / weight;
        return new double[] {dx / length * strength, GAFF_LIFT / weight, dz / length * strength};
    }

    public static boolean inLeapRange(double distance) {
        return distance >= LEAP_MIN && distance <= LEAP_MAX;
    }

    /** Velocidade do salto na direção (dx, dz) da presa: {x, y, z}. */
    public static double[] leapVelocity(double dx, double dz) {
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6) {
            return new double[] {0.0, LEAP_LIFT, 0.0};
        }
        return new double[] {dx / length * LEAP_SPEED, LEAP_LIFT, dz / length * LEAP_SPEED};
    }

    /** O agarrão segura presa do porte do caçador para baixo; a maior se solta. */
    public static boolean grabs(double sizeRatio) {
        return sizeRatio <= GRAB_MAX_SIZE_RATIO;
    }

    /** O dano extra da bicada, que ignora armadura. */
    public static double beakPierce(double attackDamage) {
        return attackDamage * BEAK_PIERCE;
    }

    /**
     * O nível de sangramento depois de uma mordida, como amplificador do efeito (nível 1 = 0): soma {@code levels}
     * ao que a vítima já tem ({@code currentAmplifier}, ou -1 se não sangra) e para no teto ({@code cap} níveis).
     * Uma vítima que já sangra acima do teto não baixa.
     */
    public static int stackedBleedAmplifier(int currentAmplifier, int levels, int cap) {
        int current = Math.max(-1, currentAmplifier);
        int stacked = Math.min(current + levels, cap - 1);
        return Math.max(current, stacked);
    }
}
