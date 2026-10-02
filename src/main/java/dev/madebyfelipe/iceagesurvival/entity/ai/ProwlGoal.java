package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Carnívoro com fome e sem presa à vista: sai rondando o mapa até achar uma. Anda trechos longos (48 a
 * 96 blocos), para fora do próprio território se for preciso; a procura de presa ({@link HuntGoal})
 * continua no caminho. Num bando só o líder ronda — os outros o seguem ({@link FollowHerdGoal}). Com a
 * barriga cheia (ou com um alvo), volta a valer o território.
 */
public class ProwlGoal extends Goal {
    private static final int MIN_LEG = 48;
    private static final int MAX_LEG = 96;
    private static final int VERTICAL_RANGE = 10;

    private final PrehistoricCreature creature;
    private final double speedModifier;

    public ProwlGoal(PrehistoricCreature creature, double speedModifier) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return hungry() && creature.tickCount % 20 == 0 && isLeader();
    }

    private boolean hungry() {
        return !creature.isTame() && !creature.isBaby() && !creature.isUnconscious() && !creature.isVehicle()
                && creature.getTarget() == null && creature.yieldingFrom() == null
                && creature.behavior().flatMap(BehaviorProfile::prey).isPresent()
                && creature.hungerDrive() == Hunger.Drive.HUNTING;
    }

    private boolean isLeader() {
        return creature.groupMembers(PrehistoricCreature.GROUP_RANGE).stream()
                .noneMatch(member -> member.getId() < creature.getId());
    }

    @Override
    public boolean canContinueToUse() {
        return hungry();
    }

    @Override
    public void start() {
        creature.clearRestriction();
        nextLeg();
    }

    @Override
    public void tick() {
        if (creature.getNavigation().isDone()) {
            nextLeg();
        }
    }

    private void nextLeg() {
        int leg = MIN_LEG + creature.getRandom().nextInt(MAX_LEG - MIN_LEG + 1);
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 destination = DefaultRandomPos.getPos(creature, leg, VERTICAL_RANGE);
            if (destination != null && destination.distanceTo(creature.position()) >= MIN_LEG / 2.0) {
                creature.getNavigation().moveTo(destination.x, destination.y, destination.z, speedModifier);
                return;
            }
        }
    }

    @Override
    public void stop() {
        creature.getNavigation().stop();
        creature.restoreTerritory();
    }
}
