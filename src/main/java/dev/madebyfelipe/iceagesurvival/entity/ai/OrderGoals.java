package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;

/** Goals vanilla condicionados à ordem atual de uma criatura domesticada. */
public final class OrderGoals {
    private OrderGoals() {
    }

    /** Segura a criatura no lugar enquanto a ordem for ficar. */
    public static class Stay extends Goal {
        private final PrehistoricCreature creature;

        public Stay(PrehistoricCreature creature) {
            this.creature = creature;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return creature.isTame() && !creature.order().followsOwner();
        }

        @Override
        public void start() {
            creature.getNavigation().stop();
        }
    }

    public static class Follow extends FollowOwnerGoal {
        private final PrehistoricCreature creature;

        public Follow(PrehistoricCreature creature, double speedModifier, float startDistance, float stopDistance) {
            super(creature, speedModifier, startDistance, stopDistance);
            this.creature = creature;
        }

        @Override
        public boolean canUse() {
            return creature.order().followsOwner() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return creature.order().followsOwner() && super.canContinueToUse();
        }
    }

    public static class DefendOwner extends OwnerHurtByTargetGoal {
        private final PrehistoricCreature creature;

        public DefendOwner(PrehistoricCreature creature) {
            super(creature);
            this.creature = creature;
        }

        @Override
        public boolean canUse() {
            return creature.order().defendsOwner() && super.canUse();
        }
    }

    public static class AssistOwner extends OwnerHurtTargetGoal {
        private final PrehistoricCreature creature;

        public AssistOwner(PrehistoricCreature creature) {
            super(creature);
            this.creature = creature;
        }

        @Override
        public boolean canUse() {
            return creature.order().defendsOwner() && super.canUse();
        }
    }

    /** Revidar: sempre para selvagens; para domesticadas, só se a ordem permitir. */
    public static class Retaliate extends HurtByTargetGoal {
        private final PrehistoricCreature creature;

        public Retaliate(PrehistoricCreature creature) {
            super(creature);
            this.creature = creature;
        }

        @Override
        public boolean canUse() {
            return (!creature.isTame() || creature.order().fightsBack()) && super.canUse();
        }
    }
}
