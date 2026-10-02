package dev.madebyfelipe.iceagesurvival.entity.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O passeio do anfíbio ({@code body.amphibious}): não evita a água como o {@code WaterAvoidingRandomStrollGoal}, e entre
 * os destinos sorteados prefere a beira e o raso — o Baryonyx vadeava rios e deltas.
 */
public class WaterEdgeStrollGoal extends RandomStrollGoal {
    /** Destinos sorteados por passeio; o primeiro perto da água vence. */
    private static final int TRIES = 6;
    /** "Perto da água": água a até esta distância horizontal (blocos) do destino. */
    private static final int SHORE_RADIUS = 2;

    public WaterEdgeStrollGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        Vec3 fallback = null;
        for (int i = 0; i < TRIES; i++) {
            Vec3 pos = DefaultRandomPos.getPos(mob, 12, 7);
            if (pos == null) {
                continue;
            }
            if (nearWater(mob.level(), BlockPos.containing(pos))) {
                return pos;
            }
            if (fallback == null) {
                fallback = pos;
            }
        }
        return fallback;
    }

    /** Água a até {@value #SHORE_RADIUS} blocos, na altura dos pés ou logo abaixo. */
    public static boolean nearWater(Level level, BlockPos feet) {
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-SHORE_RADIUS, -1, -SHORE_RADIUS),
                feet.offset(SHORE_RADIUS, 0, SHORE_RADIUS))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
