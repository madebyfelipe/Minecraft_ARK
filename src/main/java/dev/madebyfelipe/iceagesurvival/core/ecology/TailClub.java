package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * A clava da cauda do Anquilossauro ({@code wariness.defense: tail_club}), pela biologia real:
 * <ul>
 *   <li>A cauda girava uns 100° para o lado e golpeava atrás e no flanco (Arbour &amp; Mallon 2017); pela frente o
 *   animal não tem arma. Em vez de investir, ele <b>vira a cauda</b> para a ameaça e golpeia quem está no arco de trás —
 *   o contrário de todo carnívoro do mod, que espreita e chega pelas costas.</li>
 *   <li>O golpe quebra ossos: quem anda fica com a <b>perna quebrada</b> (lenta e sem pular) por um tempo.</li>
 *   <li>Entre dois adultos da mesma espécie, o golpe é no flanco e não mata: os osteodermos quebrados e cicatrizados
 *   dos flancos do <i>Zuul crurivastator</i> (Arbour, Zanno &amp; Evans 2022) indicam duelos de clavadas.</li>
 * </ul>
 * O ângulo é o {@code yRot} do Minecraft, em graus: 0 olha para +z e 90 para −x. Sem classes do Minecraft (D10).
 */
public final class TailClub {
    /** Meio arco de trás: a cauda acerta quem está a até 50° da linha de trás (100° no total). */
    public static final double REAR_HALF_ARC = 50.0;
    /** Alcance da clava, do centro do corpo até a borda do alvo, em blocos (a ponta da cauda). */
    public static final double REACH = 4.5;
    /** Entre um golpe e outro, em ticks (a animação da cauda dura 1 s). */
    public static final int SWING_COOLDOWN_TICKS = 30;
    /**
     * Quanto o corpo gira por tick para pôr a cauda na ameaça (90°/s). Quem circula de perto pela frente
     * — a 3 blocos, um jogador correndo dá ~107°/s — fica fora da cauda: é a brecha para caçá-lo.
     */
    public static final float TURN_PER_TICK = 4.5F;
    /** Perna quebrada: duração, em ticks (5 s). */
    public static final int LEG_BREAK_TICKS = 100;
    /** Perna quebrada: fração da velocidade que se perde (a lentidão IV do vanilla). */
    public static final double LEG_BREAK_SLOWDOWN = 0.6;
    /** Duelo da mesma espécie: a clavada não leva o rival abaixo desta fração da vida. */
    public static final double DUEL_FLOOR = 0.4;

    private TailClub() {
    }

    /**
     * Ângulo, de 0 a 180, entre a linha de trás do animal e a direção do alvo.
     *
     * @param yaw o {@code yRot} do animal
     * @param dx  alvo menos animal, em x
     * @param dz  alvo menos animal, em z
     */
    public static double angleFromRear(float yaw, double dx, double dz) {
        if (dx * dx + dz * dz < 1.0E-8) {
            return 0.0;
        }
        double rearYaw = yaw + 180.0;
        return Math.abs(wrap(yawTowards(dx, dz) - rearYaw));
    }

    /** O alvo está no arco de trás. */
    public static boolean inRearArc(float yaw, double dx, double dz) {
        return angleFromRear(yaw, dx, dz) <= REAR_HALF_ARC;
    }

    /**
     * O golpe alcança: alvo no arco de trás e a borda dele até {@link #REACH} do centro do animal.
     *
     * @param edgeDistance distância do centro do animal até a borda do alvo
     * @param reachFactor  fração do alcance (o jogador agachado é percebido mais perto: o {@code sneak_factor})
     */
    public static boolean strikes(float yaw, double dx, double dz, double edgeDistance, double reachFactor) {
        return edgeDistance <= REACH * reachFactor && inRearArc(yaw, dx, dz);
    }

    /** O {@code yRot} que olha na direção (dx, dz). */
    public static float yawTowards(double dx, double dz) {
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    /** O {@code yRot} que põe a cauda na direção (dx, dz) do alvo: de costas para ele. */
    public static float braceYaw(double dx, double dz) {
        return yawTowards(-dx, -dz);
    }

    /** Gira de {@code current} para {@code wanted} no máximo {@code maxStep} graus, pelo lado mais curto. */
    public static float turn(float current, float wanted, float maxStep) {
        double delta = wrap(wanted - current);
        double step = Math.max(-maxStep, Math.min(maxStep, delta));
        return (float) wrap(current + step);
    }

    /**
     * O dano da clavada num rival da mesma espécie: não o leva abaixo de {@link #DUEL_FLOOR} da vida — os
     * flancos quebram e cicatrizam, ninguém morre no duelo.
     */
    public static float duelDamage(float damage, float health, float maxHealth) {
        return (float) Math.max(0.0, Math.min(damage, health - maxHealth * DUEL_FLOOR));
    }

    /** Ângulo em (−180, 180]. */
    static double wrap(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped > 180.0) {
            wrapped -= 360.0;
        } else if (wrapped <= -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }
}
