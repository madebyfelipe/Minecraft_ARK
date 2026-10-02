package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.HuntChoice;
import dev.madebyfelipe.iceagesurvival.core.ecology.Perception;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Espreita (stalk), o jeito de caçar de todo carnívoro ({@code hunt_style} {@code stalk}). Com uma presa
 * escolhida ({@link HuntGoal}), o predador não corre: acompanha a manada devagar, rondando logo além do
 * raio em que seria notado ({@link Perception#stalkerNoticeRadius} — quem espreita é percebido a metade
 * do alerta), sem rugir. A manada ainda não se sabe caçada.
 *
 * <p>Sai em disparada — larga a espreita, ruge, avisa a manada ({@link PrehistoricCreature#onHunted}) e
 * deixa o {@link ChaseGoal} correr — no que vier primeiro:
 * <ul>
 *   <li>a presa se desgarra do bando;</li>
 *   <li>a presa chega perto (entra no raio em que o notaria): o bote vem antes de ela ver;</li>
 *   <li>alguém da manada o nota ({@link WaryGoal} chama {@link PrehistoricCreature#blowStalk});</li>
 *   <li>é ferido;</li>
 *   <li>passou 20 a 40 s rondando.</li>
 * </ul>
 * O bando do predador dispara junto ({@link PrehistoricCreature#signalPounce}). Sozinho, o predador também
 * acompanha a manada que não encara ({@link HuntChoice#chooseToStalk}), mas só dá o bote se a presa, na hora,
 * for atacável (a desgarrada); senão desiste, como numa caçada frustrada.
 *
 * <p>Contra o jogador e contra bichos sem cautela (o gado do vanilla) não há raio a respeitar: chega
 * pelas costas e dá o bote perto, ou quando o jogador o vê. Um alvo que já o viu não é espreitado de novo.
 */
public class StalkGoal extends Goal {
    /** A esta distância do alvo a espreita acaba e vem o bote. */
    public static final double POUNCE_DISTANCE = 4.5;
    /** Ronda a manada entre estes tempos (20 a 40 s) antes de disparar de qualquer jeito. */
    public static final int MIN_STALK_TICKS = 400;
    public static final int MAX_STALK_TICKS = 800;
    /** Folga além do raio em que a presa o notaria. */
    public static final double RING_MARGIN = 3.0;
    /** Cosseno do meio ângulo de visão considerado "na tela" (~55°, a tela em FOV normal). */
    private static final double SIGHT_COS = Math.cos(Math.toRadians(55.0));
    /** Não tenta chegar exatamente atrás; fica a esta distância da nuca enquanto se aproxima. */
    private static final double BEHIND_OFFSET = 3.0;
    /** Quanto a ronda avança em volta da manada a cada replanejamento, em radianos. */
    private static final double ORBIT_STEP = 0.3;
    /** Além do raio da manada, até onde procura os outros membros para não passar perto deles. */
    private static final double HERD_SCAN = 24.0;
    private static final int REPATH_TICKS = 10;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    @Nullable
    private LivingEntity revealedTo;
    private int ticksUntilRepath;
    private int stalkTicks;
    private int stalkLimit;
    private double orbitSign;

    public StalkGoal(PrehistoricCreature creature, double speedModifier) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = creature.getTarget();
        if (target == null) {
            revealedTo = null;
            return false;
        }
        // Só caçada (ou jogador): o intruso do território e o rival não são espreitados.
        return !creature.isTame() && !creature.isVehicle() && target.isAlive() && target != revealedTo
                && (creature.isHunting() || target instanceof Player)
                && creature.distanceTo(target) > POUNCE_DISTANCE && creature.getLastHurtByMob() != target;
    }

    @Override
    public boolean canContinueToUse() {
        return creature.isStalking() && canUse();
    }

    @Override
    public void start() {
        creature.setStalking(true);
        ticksUntilRepath = 0;
        stalkTicks = 0;
        stalkLimit = MIN_STALK_TICKS + creature.getRandom().nextInt(MAX_STALK_TICKS - MIN_STALK_TICKS + 1);
        orbitSign = creature.getRandom().nextBoolean() ? 1.0 : -1.0;
    }

    @Override
    public void stop() {
        creature.setStalking(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = creature.getTarget();
        if (target == null) {
            return;
        }
        stalkTicks++;
        double notice = noticeRadius(target);
        if (timeToPounce(target, notice)) {
            pounce(target);
            return;
        }
        creature.getLookControl().setLookAt(target, 10.0F, 10.0F);
        if (--ticksUntilRepath <= 0) {
            ticksUntilRepath = REPATH_TICKS;
            Vec3 goal = notice > 0.0 ? circle((PrehistoricCreature) target, notice) : approachFromBehind(target);
            creature.getNavigation().moveTo(goal.x, goal.y, goal.z, speedModifier);
        }
    }

    private boolean timeToPounce(LivingEntity target, double notice) {
        if (creature.stalkBlown() || creature.hurtTime > 0 || stalkTicks >= stalkLimit
                || creature.distanceTo(target) <= POUNCE_DISTANCE) {
            return true;
        }
        if (target instanceof Player player) {
            return sees(player, creature);
        }
        if (target instanceof PrehistoricCreature prey) {
            boolean herdAnimal = prey.behavior().map(BehaviorProfile::herdRadius).orElse(0) > 0;
            return herdAnimal && prey.isIsolated() || notice > 0.0 && gap(prey) <= notice;
        }
        return false;
    }

    private void pounce(LivingEntity target) {
        revealedTo = target;
        creature.setStalking(false);
        if (target instanceof PrehistoricCreature prey && creature.hurtTime <= 0 && !huntable(prey)) {
            // Acompanhava uma manada que não encara (sozinho, só a desgarrada): nada de bote, desiste.
            creature.setTarget(null);
            creature.huntFailed();
            return;
        }
        creature.playAlert();
        // O fôlego da perseguição conta daqui.
        creature.beginHunt();
        creature.signalPounce(target);
        creature.onPounce(target);
        if (target instanceof PrehistoricCreature hunted && !hunted.isHunted()) {
            hunted.onHunted(creature, creature.fightingGroup());
        }
    }

    /** Se a presa, agora, seria atacada pela conta normal ({@link HuntGoal#wouldHunt}). */
    private boolean huntable(PrehistoricCreature prey) {
        return creature.behavior().flatMap(BehaviorProfile::prey)
                .map(tag -> HuntGoal.wouldHunt(creature, prey, tag)).orElse(false);
    }

    /**
     * Um ponto da ronda: na mesma direção em que o predador já está, um passo de lado, logo além do raio
     * em que a presa o notaria — e afastado o bastante dos outros da manada também.
     */
    private Vec3 circle(PrehistoricCreature prey, double notice) {
        List<PrehistoricCreature> herd = prey.level().getEntitiesOfClass(PrehistoricCreature.class,
                prey.getBoundingBox().inflate(HERD_SCAN), member -> member == prey || prey.sameGroup(member));
        double angle = Math.atan2(creature.getZ() - prey.getZ(), creature.getX() - prey.getX()) + orbitSign * ORBIT_STEP;
        Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        double ring = notice + RING_MARGIN + (creature.getBbWidth() + prey.getBbWidth()) / 2.0;
        Vec3 point = prey.position().add(direction.scale(ring));
        for (int step = 0; step < 6 && tooClose(point, herd); step++) {
            ring += 2.0;
            point = prey.position().add(direction.scale(ring));
        }
        return point;
    }

    private boolean tooClose(Vec3 point, List<PrehistoricCreature> herd) {
        for (PrehistoricCreature member : herd) {
            double reach = noticeRadius(member) + RING_MARGIN + (creature.getBbWidth() + member.getBbWidth()) / 2.0;
            if (member.position().distanceToSqr(point) < reach * reach) {
                return true;
            }
        }
        return false;
    }

    private Vec3 approachFromBehind(LivingEntity target) {
        Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0.0F, target.getYRot()).scale(BEHIND_OFFSET));
        // Longe ainda, vai pela nuca; perto, direto — a nuca está ali.
        return creature.distanceToSqr(behind) > creature.distanceToSqr(target) ? target.position() : behind;
    }

    /** Distância entre as bordas, a mesma medida da cautela ({@link WaryGoal}). */
    private double gap(LivingEntity other) {
        return Math.max(0.0, creature.distanceTo(other) - (creature.getBbWidth() + other.getBbWidth()) / 2.0);
    }

    /**
     * Até onde esta criatura nota quem a espreita agora (metade do alerta, maior com filhote ou estresse).
     * 0 para quem não tem cautela — o jogador, o gado do vanilla.
     */
    public static double noticeRadius(LivingEntity prey) {
        if (!(prey instanceof PrehistoricCreature creature)) {
            return 0.0;
        }
        WarinessProfile wariness = creature.behavior().flatMap(BehaviorProfile::wariness).orElse(null);
        if (wariness == null) {
            return 0.0;
        }
        return Perception.stalkerNoticeRadius(ThreatResponse.detectionRadius(wariness.tuning(), false,
                creature.hasCalfNearby(wariness.calfRadius()), creature.stress()));
    }

    /** Se a criatura está no campo de visão do jogador, sem bloco no meio. */
    public static boolean sees(Player player, LivingEntity creature) {
        Vec3 toCreature = creature.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double distance = toCreature.length();
        if (distance < 1.0E-3) {
            return true;
        }
        return player.getViewVector(1.0F).dot(toCreature.scale(1.0 / distance)) > SIGHT_COS
                && player.hasLineOfSight(creature);
    }
}
