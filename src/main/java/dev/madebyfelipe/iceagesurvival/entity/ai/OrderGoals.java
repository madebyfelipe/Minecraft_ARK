package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.phys.Vec3;

/** Goals vanilla condicionados ao movimento e à postura de uma criatura domesticada. */
public final class OrderGoals {
    private OrderGoals() {
    }

    /**
     * Segura a criatura no lugar enquanto ela estiver mandada ficar. Com um alvo (revidar ou
     * ordem de ataque) ela sai para lutar e fica onde a briga acabar. Com uma ordem de locomover
     * ({@link PrehistoricCreature#moveOrder()}), anda até o destino antes de parar.
     */
    public static class Stay extends Goal {
        /** Velocidade da caminhada até o destino da ordem de locomover. */
        private static final double MOVE_SPEED = 1.3;
        /** Tentativas de caminho antes de desistir de um destino inalcançável. */
        private static final int MAX_PATH_ATTEMPTS = 5;
        /** Desiste do destino depois deste tempo, em ticks. */
        private static final int MOVE_TIMEOUT_TICKS = 20 * 60;

        private final PrehistoricCreature creature;
        private BlockPos heading;
        private int pathAttempts;
        private int moveTicks;

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
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            heading = null;
            creature.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos target = creature.moveOrder();
            if (target == null) {
                return;
            }
            if (!target.equals(heading)) {
                heading = target;
                pathAttempts = 0;
                moveTicks = 0;
            }
            double reach = 1.5 + creature.getBbWidth() / 2.0;
            if (creature.position().distanceToSqr(Vec3.atBottomCenterOf(target)) <= reach * reach
                    || ++moveTicks > MOVE_TIMEOUT_TICKS) {
                arrive();
                return;
            }
            if (creature.getNavigation().isDone()) {
                // Sem caminho, ou chegou o mais perto que dava: depois de algumas tentativas, fica onde está.
                if (pathAttempts++ >= MAX_PATH_ATTEMPTS) {
                    arrive();
                } else {
                    creature.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                            MOVE_SPEED);
                }
            }
        }

        private void arrive() {
            creature.clearMoveOrder();
            heading = null;
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
