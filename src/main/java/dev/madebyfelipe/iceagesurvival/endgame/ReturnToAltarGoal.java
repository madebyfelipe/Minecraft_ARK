package dev.madebyfelipe.iceagesurvival.endgame;

import dev.madebyfelipe.iceagesurvival.entity.GiganotosaurusBoss;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * O boss preso à arena (D47): afastado do altar mais que o raio da arena — puxado, atraído para fora, empurrado —
 * larga o alvo e volta andando. Se não chega em {@link #TELEPORT_AFTER_TICKS} (caminho fechado, preso num buraco) ou
 * foi parar a mais de duas vezes o raio, reaparece ao lado do altar.
 */
public class ReturnToAltarGoal extends Goal {
    /** Andando de volta por mais que isto sem chegar, reaparece no altar. */
    public static final int TELEPORT_AFTER_TICKS = 200;
    private static final int REPATH_TICKS = 20;
    /** Volta até ficar a esta fração do raio da arena do altar. */
    private static final double HOME_FRACTION = 0.4;

    private final GiganotosaurusBoss boss;
    private final double speed;
    private int ticks;

    public ReturnToAltarGoal(GiganotosaurusBoss boss, double speed) {
        this.boss = boss;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        double radius = boss.arenaRadius();
        return boss.altar() != null && boss.distanceToAltarSqr() > radius * radius;
    }

    @Override
    public boolean canContinueToUse() {
        double home = homeDistance(boss.arenaRadius());
        return boss.altar() != null && boss.distanceToAltarSqr() > home * home;
    }

    /**
     * Até onde volta: bem para dentro da arena ({@link #HOME_FRACTION} do raio), mas sempre um pouco além de onde o
     * altar o põe ao reaparecer, para uma arena pequena não fazer o boss reaparecer em laço.
     */
    public static double homeDistance(int arenaRadius) {
        return Math.max(arenaRadius * HOME_FRACTION, Math.min(ArenaAltarBlock.SPAWN_DISTANCE + 1, arenaRadius - 1));
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
        double far = boss.arenaRadius() * 2.0;
        if (ticks >= TELEPORT_AFTER_TICKS || boss.distanceToAltarSqr() > far * far) {
            boss.teleportToAltar();
            ticks = 0;
            return;
        }
        if (ticks % REPATH_TICKS == 0 || boss.getNavigation().isDone()) {
            walkHome();
        }
    }

    private void walkHome() {
        BlockPos altar = boss.altar();
        if (altar != null) {
            boss.getNavigation().moveTo(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5, speed);
        }
    }
}
