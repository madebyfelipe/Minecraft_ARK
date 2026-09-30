package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/** Criatura selvagem com pouca vida recua de quem a feriu, em vez de lutar até morrer. */
public class FleeWhenWeakGoal extends PanicGoal {
    private final PrehistoricCreature creature;

    public FleeWhenWeakGoal(PrehistoricCreature creature, double speedModifier) {
        super(creature, speedModifier);
        this.creature = creature;
    }

    @Override
    protected boolean shouldPanic() {
        if (creature.isTame() || creature.getLastHurtByMob() == null) {
            return false;
        }
        double fraction = creature.behavior().map(BehaviorProfile::fleeHealthFraction).orElse(0.0);
        return creature.getHealth() < creature.getMaxHealth() * fraction;
    }
}
