package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Golpe e recua (ave-terrível): depois de acertar a bicada, afasta-se da presa por um instante antes de voltar —
 * como os estudos biomecânicos sugerem para as aves-terríveis, em vez de agarrar e lutar.
 */
public class StrikeRetreatGoal extends Goal {
    /** Quanto se afasta, em blocos. */
    private static final double RETREAT_DISTANCE = 6.0;

    private final PrehistoricCreature creature;
    private final double speedModifier;

    public StrikeRetreatGoal(PrehistoricCreature creature, double speedModifier) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return creature.isRetreatingAfterStrike() && !creature.isVehicle();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        LivingEntity from = creature.retreatTarget();
        if (from == null) {
            return;
        }
        Vec3 away = creature.position().subtract(from.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, creature.getYRot() + 180.0F) : away.normalize();
        Vec3 goal = creature.position().add(away.scale(RETREAT_DISTANCE));
        creature.getNavigation().moveTo(goal.x, goal.y, goal.z, speedModifier);
    }

    @Override
    public void stop() {
        creature.getNavigation().stop();
    }
}
