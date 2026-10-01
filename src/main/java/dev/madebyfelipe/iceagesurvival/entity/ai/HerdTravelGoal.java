package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Lideres de manada escolhem um destino distante e conduzem os demais numa viagem. */
public class HerdTravelGoal extends Goal {
    private static final int MIN_PAUSE_TICKS = 20 * 35;
    private static final int PAUSE_VARIATION_TICKS = 20 * 55;
    private static final double TRAVEL_RANGE = 80.0;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    private final int herdRadius;
    private int nextTripTick;

    public HerdTravelGoal(PrehistoricCreature creature, double speedModifier, int herdRadius) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        this.herdRadius = herdRadius;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (creature.isTame() || creature.isUnconscious() || creature.getTarget() != null
                || creature.tickCount < nextTripTick || !isLeader()) {
            return false;
        }
        Vec3 destination = DefaultRandomPos.getPos(creature, (int) TRAVEL_RANGE, 7);
        if (destination == null) {
            nextTripTick = creature.tickCount + MIN_PAUSE_TICKS;
            return false;
        }
        creature.getNavigation().moveTo(destination.x, destination.y, destination.z, speedModifier);
        nextTripTick = creature.tickCount + MIN_PAUSE_TICKS
                + creature.getRandom().nextInt(PAUSE_VARIATION_TICKS);
        return true;
    }

    private boolean isLeader() {
        double searchRange = herdRadius * 3.0;
        return creature.level().getEntitiesOfClass(
                        PrehistoricCreature.class,
                        creature.getBoundingBox().inflate(searchRange),
                        other -> other != creature && other.getType() == creature.getType()
                                && other.isAlive() && !other.isTame())
                .stream()
                .min(Comparator.comparingInt(Entity::getId))
                .map(other -> creature.getId() < other.getId())
                .orElse(true);
    }

    @Override
    public boolean canContinueToUse() {
        return !creature.isTame() && creature.getTarget() == null && !creature.getNavigation().isDone();
    }
}
