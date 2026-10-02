package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Pavor de água ({@code behavior.fears_water}): a selvagem que cai na água larga tudo e nada direto para a margem
 * seca mais próxima, se debatendo — empurrão a mais e pulo ao bater na beira. Vai em linha reta, sem o pathfinder:
 * para quem tem pavor, a água não tem caminho.
 */
public class EscapeWaterGoal extends Goal {
    /** Até onde procura a margem, em blocos. */
    private static final int SEARCH_RADIUS = 16;
    /** O empurrão por tick na direção da margem: a pressa de quem se debate. */
    private static final double THRASH = 0.04;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    @Nullable
    private BlockPos shore;

    public EscapeWaterGoal(PrehistoricCreature creature, double speedModifier) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return creature.fearsWater() && creature.isInWater() && !creature.isUnconscious() && !creature.isVehicle();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        creature.setTarget(null);
        creature.getNavigation().stop();
        shore = nearestShore();
    }

    @Override
    public void stop() {
        shore = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (shore == null || creature.tickCount % 20 == 0) {
            shore = nearestShore();
        }
        if (shore == null) {
            return; // mar aberto: o FloatGoal o mantém na superfície
        }
        Vec3 goal = Vec3.atBottomCenterOf(shore);
        creature.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, speedModifier);
        creature.getLookControl().setLookAt(goal.x, goal.y + 1.0, goal.z);
        Vec3 toward = goal.subtract(creature.position()).multiply(1, 0, 1);
        if (toward.lengthSqr() > 1.0E-4) {
            toward = toward.normalize().scale(THRASH);
            creature.setDeltaMovement(creature.getDeltaMovement().add(toward.x, 0.0, toward.z));
        }
        if (creature.horizontalCollision) {
            creature.getJumpControl().jump();
        }
    }

    /** O bloco seco e firme mais próximo onde a criatura cabe de pé. */
    @Nullable
    private BlockPos nearestShore() {
        BlockPos origin = creature.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        var level = creature.level();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -3, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, 4, SEARCH_RADIUS))) {
            BlockPos below = pos.below();
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(below).isEmpty()
                    || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }
}
