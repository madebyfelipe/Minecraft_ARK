package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;

/** Goals vanilla condicionados ao movimento e à postura de uma criatura domesticada. */
public final class OrderGoals {
    private OrderGoals() {
    }

    /**
     * Segura a criatura no lugar enquanto ela estiver mandada ficar. Com um alvo (revidar ou
     * ordem de ataque) ela sai para lutar e fica onde a briga acabar.
     */
    public static class Stay extends Goal {
        private final PrehistoricCreature creature;

        public Stay(PrehistoricCreature creature) {
            this.creature = creature;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return creature.isTame() && creature.movement() == Movement.STAY && creature.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            creature.getNavigation().stop();
        }
    }

    public static class Follow extends FollowOwnerGoal {
        private final PrehistoricCreature creature;

        public Follow(PrehistoricCreature creature, double speedModifier, float startDistance, float stopDistance) {
            super(creature, speedModifier, startDistance, stopDistance, true);
            this.creature = creature;
        }

        @Override
        public boolean canUse() {
            return creature.movement() == Movement.FOLLOW && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return creature.movement() == Movement.FOLLOW && super.canContinueToUse();
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
            return creature.stance().defendsOwner() && super.canUse();
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
            return creature.stance().defendsOwner() && super.canUse();
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
            return (!creature.isTame() || creature.stance().fightsBack()) && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            creature.alertHerd(creature.getLastHurtByMob());
        }
    }
}
