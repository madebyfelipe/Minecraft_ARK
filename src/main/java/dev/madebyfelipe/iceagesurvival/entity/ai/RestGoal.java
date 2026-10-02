package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Camouflage;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fora do horário de atividade ({@code behavior.habits.activity}), a criatura selvagem procura um esconderijo por
 * perto (folhas, mato, neve alta — {@link Camouflage}) e dorme ali até a hora dela. Acorda ferida, com alvo ou quando
 * uma ameaça chega perto: a cautela ({@link WaryGoal}, acima deste) toma o controle e o sono acaba.
 */
public class RestGoal extends Goal {
    /** Raio em que procura o esconderijo. */
    private static final int HIDE_RADIUS = 12;
    private static final int HIDE_TRIES = 16;
    /** Desiste de chegar ao esconderijo depois deste tempo e dorme onde está. */
    private static final int HIDE_TIMEOUT_TICKS = 200;
    /** Ferida há menos que isto, não dorme. */
    private static final int HURT_GRACE_TICKS = 200;

    private final PrehistoricCreature creature;
    private final double speed;
    @Nullable
    private BlockPos hideout;
    private int walkTicks;
    /**
     * Espera até a próxima conferência. Contador, não {@code tickCount % n}: o vanilla só avalia objetivos novos em
     * ticks alternados (pela paridade de tick + id), e um módulo par nunca bateria para metade dos indivíduos.
     */
    private int checkCooldown;

    public RestGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean able() {
        return creature.restsNow() && !creature.isUnconscious() && !creature.isVehicle() && !creature.isInWater()
                && creature.getTarget() == null && creature.yieldingFrom() == null
                && (creature.getLastHurtByMobTimestamp() == 0
                || creature.tickCount - creature.getLastHurtByMobTimestamp() > HURT_GRACE_TICKS);
    }

    @Override
    public boolean canUse() {
        if (--checkCooldown > 0) {
            return false;
        }
        checkCooldown = 10;
        return able();
    }

    @Override
    public boolean canContinueToUse() {
        return able();
    }

    @Override
    public void start() {
        walkTicks = 0;
        hideout = creature.isConcealed() ? null : findHideout();
        if (hideout != null) {
            creature.getNavigation().moveTo(hideout.getX() + 0.5, hideout.getY(), hideout.getZ() + 0.5, speed);
        } else {
            lieDown();
        }
    }

    @Override
    public void tick() {
        if (creature.isResting()) {
            return;
        }
        walkTicks++;
        if (hideout == null || creature.getNavigation().isDone() || walkTicks > HIDE_TIMEOUT_TICKS
                || creature.blockPosition().closerThan(hideout, 1.5)) {
            lieDown();
        }
    }

    private void lieDown() {
        hideout = null;
        creature.getNavigation().stop();
        creature.setResting(true);
    }

    @Override
    public void stop() {
        hideout = null;
        creature.setResting(false);
    }

    /** O ponto do chão mais perto com esconderijo em volta; nulo se não há nenhum no raio. */
    @Nullable
    private BlockPos findHideout() {
        BlockPos origin = creature.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < HIDE_TRIES; i++) {
            int x = origin.getX() + creature.getRandom().nextInt(HIDE_RADIUS * 2 + 1) - HIDE_RADIUS;
            int z = origin.getZ() + creature.getRandom().nextInt(HIDE_RADIUS * 2 + 1) - HIDE_RADIUS;
            // O chão sob as copas: ignora as folhas para achar onde se deita, não o topo da árvore.
            BlockPos ground = creature.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, origin.getY(), z));
            if (Math.abs(ground.getY() - origin.getY()) > 4) {
                continue;
            }
            if (Camouflage.hidden(PrehistoricCreature.coverBlocksAt(creature.level(), ground))) {
                double distance = ground.distSqr(origin);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = ground;
                }
            }
        }
        return best;
    }
}
