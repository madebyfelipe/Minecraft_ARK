package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * O boss preso ao covil: afastado do ponto do covil mais que o raio — puxado, atraído para fora, empurrado — larga o
 * alvo e volta andando. Se não chega em {@link #TELEPORT_AFTER_TICKS} (caminho fechado, preso num buraco) ou foi parar
 * a mais de duas vezes o raio, reaparece no covil.
 */
public class ReturnToLairGoal extends Goal {
    /** Andando de volta por mais que isto sem chegar, reaparece no covil. */
    public static final int TELEPORT_AFTER_TICKS = 300;
    private static final int REPATH_TICKS = 20;
    /** Volta até ficar a esta fração do raio do covil. */
    private static final double HOME_FRACTION = 0.4;

    private final TitanovenatorBoss boss;
    private final double speed;
    private int ticks;

    public ReturnToLairGoal(TitanovenatorBoss boss, double speed) {
        this.boss = boss;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        double radius = boss.lairRadius();
        return boss.lair() != null && boss.distanceToLairSqr() > radius * radius;
    }

    @Override
    public boolean canContinueToUse() {
        double home = homeDistance(boss.lairRadius());
        return boss.lair() != null && boss.distanceToLairSqr() > home * home;
    }

    /** Até onde volta: bem para dentro do covil. */
    public static double homeDistance(int lairRadius) {
        return lairRadius * HOME_FRACTION;
    }

    @Override
    public void start() {
        ticks = 0;
        boss.setTarget(null);
        walkHome();
    }

    @Override
    public void stop() {
        boss.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        boss.setTarget(null);
        double far = boss.lairRadius() * 2.0;
        if (ticks >= TELEPORT_AFTER_TICKS || boss.distanceToLairSqr() > far * far) {
            boss.teleportToLair();
            ticks = 0;
            return;
        }
        if (ticks % REPATH_TICKS == 0 || boss.getNavigation().isDone()) {
            walkHome();
        }
    }

    private void walkHome() {
        BlockPos lair = boss.lair();
        if (lair != null) {
            boss.getNavigation().moveTo(lair.getX() + 0.5, lair.getY(), lair.getZ() + 0.5, speed);
        }
    }
}
