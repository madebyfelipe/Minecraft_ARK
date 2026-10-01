package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Persegue e morde o alvo. Difere do {@code MeleeAttackGoal} do vanilla em um ponto: sem
 * caminho completo até o alvo — mato fechado, um bicho largo demais para o vão entre as
 * árvores — a criatura não desiste nem fica parada; vai em linha reta e, se atravessa a
 * vegetação ({@link PrehistoricCreature#plows()}), abre caminho quebrando.
 */
public class ChaseGoal extends Goal {
    private static final int REPATH_TICKS = 10;
    private static final int ATTACK_INTERVAL = 20;
    /** Fim do caminho mais longe que isto do alvo = caminho incompleto. */
    private static final double PATH_END_TOLERANCE_SQR = 4.0;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    private int ticksUntilRepath;
    private int ticksUntilAttack;
    private boolean direct;

    public ChaseGoal(PrehistoricCreature creature, double speedModifier) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return validTarget() != null && !creature.isStalking() && !creature.isVehicle();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = validTarget();
        if (target == null || creature.isStalking() || creature.isVehicle()) {
            return false;
        }
        if (creature.isHunting()) {
            // Caçada: segue a presa para fora do território, mas o fôlego e o alcance acabam.
            return creature.huntTicks() < creature.ecology().chaseSeconds() * 20L
                    && creature.distanceTo(target) < creature.ecology().huntRadius() * 1.5;
        }
        if (target instanceof PrehistoricCreature rival && rival.yieldingFrom() == creature) {
            // O rival desistiu e foi embora: a disputa acabou.
            return false;
        }
        return creature.isTame() || creature.isWithinRestriction(target.blockPosition())
                || creature.isRival(target) && creature.distanceTo(target) < creature.ecology().rivalRadius()
                || creature.distanceToSqr(target) < 16 * 16;
    }

    private LivingEntity validTarget() {
        LivingEntity target = creature.getTarget();
        return target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target) ? target : null;
    }

    @Override
    public void start() {
        creature.setAggressive(true);
        ticksUntilRepath = 0;
        ticksUntilAttack = 0;
        direct = false;
    }

    @Override
    public void stop() {
        LivingEntity target = creature.getTarget();
        if (creature.isHunting()) {
            // A presa abriu distância ou o fôlego acabou: escapou.
            if (target != null && target.isAlive()) {
                creature.setTarget(null);
                creature.huntFailed();
            } else {
                creature.endHunt();
            }
        } else if (target != null && (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)
                || target instanceof PrehistoricCreature rival && rival.yieldingFrom() == creature)) {
            creature.setTarget(null);
        }
        creature.setAggressive(false);
        creature.getNavigation().stop();
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
        creature.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--ticksUntilRepath <= 0) {
            ticksUntilRepath = REPATH_TICKS + creature.getRandom().nextInt(5);
            Path path = creature.getNavigation().createPath(target, 0);
            direct = !reaches(path, target);
            if (direct) {
                creature.getNavigation().stop();
            } else {
                creature.getNavigation().moveTo(path, speedModifier);
            }
        }
        if (direct) {
            creature.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speedModifier);
        }

        ticksUntilAttack = Math.max(ticksUntilAttack - 1, 0);
        if (ticksUntilAttack <= 0 && creature.isWithinMeleeAttackRange(target)) {
            ticksUntilAttack = adjustedTickDelay(ATTACK_INTERVAL);
            creature.swing(InteractionHand.MAIN_HAND);
            creature.doHurtTarget(target);
        }
    }

    /** Se o caminho chega ao alvo; um caminho parcial deixaria a criatura parada na frente do mato. */
    private boolean reaches(Path path, LivingEntity target) {
        if (path == null) {
            return false;
        }
        Node end = path.getEndNode();
        return end != null && target.distanceToSqr(end.x + 0.5, end.y, end.z + 0.5) <= PATH_END_TOLERANCE_SQR;
    }
}
