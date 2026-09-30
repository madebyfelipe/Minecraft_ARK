package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Espreita: com um alvo, a criatura selvagem se aproxima devagar, pelas costas dele, sem
 * rugir. Dá o bote — larga a espreita e deixa o {@link ChaseGoal} correr atrás — quando
 * chega perto, quando é ferida ou, se o alvo for um jogador, quando ele a vê (ela entra no
 * campo de visão dele sem nada no meio). Um alvo que já a viu não é espreitado de novo.
 */
public class StalkGoal extends Goal {
    /** A esta distância do alvo a espreita acaba e vem o bote. */
    public static final double POUNCE_DISTANCE = 4.5;
    /** Cosseno do meio ângulo de visão considerado "na tela" (~55°, a tela em FOV normal). */
    private static final double SIGHT_COS = Math.cos(Math.toRadians(55.0));
    /** Não tenta chegar exatamente atrás; fica a esta distância da nuca enquanto se aproxima. */
    private static final double BEHIND_OFFSET = 3.0;
    private static final int REPATH_TICKS = 10;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    @Nullable
    private LivingEntity revealedTo;
    private int ticksUntilRepath;

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
        return !creature.isTame() && !creature.isVehicle() && target.isAlive() && target != revealedTo
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
        if (creature.distanceTo(target) <= POUNCE_DISTANCE || creature.hurtTime > 0
                || target instanceof Player player && sees(player, creature)) {
            pounce(target);
            return;
        }
        creature.getLookControl().setLookAt(target, 10.0F, 10.0F);
        if (--ticksUntilRepath <= 0) {
            ticksUntilRepath = REPATH_TICKS;
            Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0.0F, target.getYRot()).scale(BEHIND_OFFSET));
            // Longe ainda, vai pela nuca; perto, direto — a nuca está ali.
            Vec3 goal = creature.distanceToSqr(behind) > creature.distanceToSqr(target) ? target.position() : behind;
            creature.getNavigation().moveTo(goal.x, goal.y, goal.z, speedModifier);
        }
    }

    private void pounce(LivingEntity target) {
        revealedTo = target;
        creature.setStalking(false);
        creature.playAlert();
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
