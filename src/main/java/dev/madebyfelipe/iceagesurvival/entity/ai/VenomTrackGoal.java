package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Depois da mordida peçonhenta, segue o rastro da presa envenenada pelo faro, sem atacar, até ela cair. */
public class VenomTrackGoal extends Goal {
    private final PrehistoricCreature creature;
    private final double speed;

    public VenomTrackGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return false;
    }
}
