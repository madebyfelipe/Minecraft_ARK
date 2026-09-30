package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/**
 * Recua de quem a feriu: selvagem, quando está com pouca vida; domesticada, quando a ordem é fugir.
 */
public class FleeWhenWeakGoal extends PanicGoal {
    private final PrehistoricCreature creature;

    public FleeWhenWeakGoal(PrehistoricCreature creature, double speedModifier) {
        super(creature, speedModifier);
        this.creature = creature;
    }

    @Override
    protected boolean shouldPanic() {
        if (creature.getLastHurtByMob() == null) {
            return false;
        }
        if (creature.isTame()) {
            return creature.order() == CreatureOrder.FLEE;
        }
        double fraction = creature.behavior().map(BehaviorProfile::fleeHealthFraction).orElse(0.0);
        return creature.getHealth() < creature.getMaxHealth() * fraction;
    }
}
