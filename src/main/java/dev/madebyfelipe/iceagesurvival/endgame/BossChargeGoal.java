package dev.madebyfelipe.iceagesurvival.endgame;

import dev.madebyfelipe.iceagesurvival.entity.CreatureAction;
import dev.madebyfelipe.iceagesurvival.entity.GiganotosaurusBoss;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * A investida do Giganotosaurus a partir da fase 2 (D47): para, abaixa a cabeça um instante ({@link #WINDUP_TICKS})
 * e corre em linha reta na direção do alvo. Quem estiver no caminho — numa faixa larga, não só o alvo — leva o golpe
 * e é derrubado ({@link GiganotosaurusBoss#chargeHit}); uma vez por investida cada um. Acaba no tempo, ou quando bate
 * numa parede.
 */
public class BossChargeGoal extends Goal {
    public static final int WINDUP_TICKS = 15;
    public static final int RUN_TICKS = 30;
    /** Investe em quem está entre estas distâncias: perto, morde; longe, persegue. */
    public static final double MIN_DISTANCE = 5.0;
    public static final double MAX_DISTANCE = 20.0;
    /** Velocidade da corrida em blocos/tick: 12 blocos/s na fase 2, 15 na 3. */
    private static final double RUN_SPEED = 0.6;
    private static final double FRENZY_RUN_SPEED = 0.75;
    /** Quanto a faixa atingida passa do corpo para os lados. */
    private static final double SWEEP = 1.5;

    private final GiganotosaurusBoss boss;
    private final Set<LivingEntity> struck = new HashSet<>();
    private LivingEntity target;
    private Vec3 direction = Vec3.ZERO;
    private int ticks;

    public BossChargeGoal(GiganotosaurusBoss boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity candidate = boss.getTarget();
        if (candidate == null || !candidate.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(candidate)
                || !boss.chargeReady() || !boss.onGround() || !boss.inArena(candidate)) {
            return false;
        }
        double distance = boss.distanceTo(candidate);
        return distance >= MIN_DISTANCE && distance <= MAX_DISTANCE && boss.hasLineOfSight(candidate);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < WINDUP_TICKS + RUN_TICKS && boss.isAlive();
    }

    @Override
    public void start() {
        target = boss.getTarget();
        ticks = 0;
        struck.clear();
        boss.getNavigation().stop();
        boss.startAction(CreatureAction.ROAR, WINDUP_TICKS);
        boss.playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.7F);
    }

    @Override
    public void stop() {
        boss.startChargeCooldown();
        boss.getNavigation().stop();
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(0.2, 1.0, 0.2));
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks <= WINDUP_TICKS) {
            // Mira: a direção trava no fim da preparação.
            if (target != null && target.isAlive()) {
                boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
                Vec3 toward = new Vec3(target.getX() - boss.getX(), 0.0, target.getZ() - boss.getZ());
                if (toward.lengthSqr() > 1.0E-4) {
                    direction = toward.normalize();
                }
            }
            boss.setDeltaMovement(0.0, boss.getDeltaMovement().y, 0.0);
            return;
        }
        if (ticks == WINDUP_TICKS + 1) {
            boss.startAction(CreatureAction.ATTACK, RUN_TICKS);
        }
        if (direction.lengthSqr() < 1.0E-4) {
            ticks = WINDUP_TICKS + RUN_TICKS; // sem direção (o alvo sumiu na preparação): desiste
            return;
        }
        double speed = boss.phase() == BossPhase.THREE ? FRENZY_RUN_SPEED : RUN_SPEED;
        boss.setDeltaMovement(direction.x * speed, boss.getDeltaMovement().y, direction.z * speed);
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        boss.setYRot(yaw);
        boss.yBodyRot = yaw;
        boss.yHeadRot = yaw;
        for (LivingEntity victim : boss.level().getEntitiesOfClass(LivingEntity.class,
                boss.getBoundingBox().inflate(SWEEP, 0.5, SWEEP), this::canStrike)) {
            struck.add(victim);
            boss.chargeHit(victim, direction);
        }
        if (boss.horizontalCollision && ticks > WINDUP_TICKS + 2) {
            ticks = WINDUP_TICKS + RUN_TICKS; // bateu na parede
        }
    }

    private boolean canStrike(LivingEntity victim) {
        return victim != boss && victim.isAlive() && !struck.contains(victim)
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(victim)
                && !(victim instanceof GiganotosaurusBoss);
    }
}
