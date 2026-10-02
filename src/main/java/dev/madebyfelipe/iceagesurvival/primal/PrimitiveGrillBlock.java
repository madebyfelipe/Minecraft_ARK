package dev.madebyfelipe.iceagesurvival.primal;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Grelha primitiva: quatro espaços em cima, cozinha com calor logo embaixo
 * ({@link PrimalStationBlock#COOKING_HEAT}: fogo, fogueira acesa, magma, lava). Acesa, queima quem
 * pisa nela, como o bloco de magma.
 */
public class PrimitiveGrillBlock extends PrimalStationBlock {
    public PrimitiveGrillBlock(Properties properties) {
        super(properties, box(0, 0, 0, 16, 4, 16));
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type() {
        return PrimalStations.GRILL_ENTITY;
    }

    @Override
    protected int slotAt(BlockState state, Vec3 local) {
        return quadrant(local);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity living
                && !EnchantmentHelper.hasFrostWalker(living) && heatedFromBelow(level, pos)) {
            entity.hurt(level.damageSources().hotFloor(), 1.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!heatedFromBelow(level, pos)) {
            return;
        }
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (level.getBlockEntity(pos) instanceof Station grill) {
            for (int slot = 0; slot < grill.size(); slot++) {
                if (grill.isWorking(slot) && random.nextInt(3) == 0) {
                    double x = pos.getX() + (slot % 2 == 0 ? 0.25 : 0.75);
                    double z = pos.getZ() + (slot < 2 ? 0.25 : 0.75);
                    level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.3, z, 0.0, 0.03, 0.0);
                }
            }
        }
    }

    /** O que está na grelha. */
    public static class Station extends TimedStationBlockEntity {
        public Station(BlockPos pos, BlockState state) {
            super(PrimalStations.GRILL_ENTITY.get(), pos, state, 4);
        }

        @Override
        protected RecipeType<TimedRecipe> recipeType() {
            return PrimalStations.GRILL_RECIPE.get();
        }

        @Override
        public boolean canWork() {
            return level != null && heatedFromBelow(level, worldPosition);
        }
    }
}
