package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Recua de quem a feriu: selvagem, quando está com pouca vida; domesticada, quando a ordem é fugir.
 *
 * <p>O anfíbio ({@code body.amphibious}) foge <b>para a água</b>: a água mais perto a até {@value #WATER_SEARCH} blocos
 * que não fique do lado de quem o feriu; já na água, nada para longe dele por dentro dela. Sem água por perto, foge
 * como os outros.
 */
public class FleeWhenWeakGoal extends PanicGoal {
    /** Até onde o anfíbio procura água para fugir. */
    public static final int WATER_SEARCH = 16;
    private static final int WATER_TRIES = 8;

    private final PrehistoricCreature creature;

    public FleeWhenWeakGoal(PrehistoricCreature creature, double speedModifier) {
        super(creature, speedModifier);
        this.creature = creature;
    }

    @Override
    protected boolean shouldPanic() {
        if (creature.getLastHurtByMob() == null) {
            return false;
        }
        if (creature.isTame()) {
            return creature.stance().fleesWhenHurt();
        }
        double fraction = creature.behavior().map(BehaviorProfile::fleeHealthFraction).orElse(0.0);
        return creature.getHealth() < creature.getMaxHealth() * fraction;
    }

    @Override
    protected boolean findRandomPosition() {
        if (creature.isAmphibious()) {
            Vec3 refuge = waterRefuge(creature, creature.getLastHurtByMob());
            if (refuge != null) {
                posX = refuge.x;
                posY = refuge.y;
                posZ = refuge.z;
                return true;
            }
        }
        return super.findRandomPosition();
    }

    /**
     * Para onde o anfíbio ferido foge: fora d'água, a água mais perto que fique mais perto dele que de quem o feriu;
     * dentro d'água, um ponto na água longe do agressor. Nulo se não há água.
     */
    @Nullable
    public static Vec3 waterRefuge(PrehistoricCreature creature, @Nullable LivingEntity attacker) {
        Level level = creature.level();
        if (creature.isInWater()) {
            for (int i = 0; i < WATER_TRIES; i++) {
                Vec3 pos = attacker != null
                        ? DefaultRandomPos.getPosAway(creature, 12, 6, attacker.position())
                        : DefaultRandomPos.getPos(creature, 12, 6);
                if (pos != null && isWater(level, BlockPos.containing(pos))) {
                    return pos;
                }
            }
            return null;
        }
        BlockPos origin = creature.blockPosition();
        return BlockPos.findClosestMatch(origin, WATER_SEARCH, 4,
                        pos -> isWater(level, pos) && (attacker == null
                                || attacker.distanceToSqr(Vec3.atCenterOf(pos)) > creature.distanceToSqr(Vec3.atCenterOf(pos))))
                .map(Vec3::atBottomCenterOf).orElse(null);
    }

    private static boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.below()).is(FluidTags.WATER);
    }
}
