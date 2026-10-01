package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Perdeu a disputa para um rival: vai embora correndo, para longe dele, até a raiva dele passar. */
public class YieldGoal extends Goal {
    private final PrehistoricCreature creature;
    private final double speed;

    public YieldGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !creature.isTame() && !creature.isVehicle() && creature.yieldingFrom() != null;
    }

    @Override
    public void tick() {
        LivingEntity rival = creature.yieldingFrom();
        if (rival != null && (creature.getNavigation().isDone() || creature.tickCount % 20 == 0)) {
            Vec3 away = DefaultRandomPos.getPosAway(creature, 24, 8, rival.position());
            if (away != null) {
                creature.getNavigation().moveTo(away.x, away.y, away.z, speed);
            }
        }
    }

    @Override
    public void stop() {
        creature.getNavigation().stop();
    }
}
