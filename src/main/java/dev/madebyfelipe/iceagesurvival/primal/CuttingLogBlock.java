package dev.madebyfelipe.iceagesurvival.primal;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;

/**
 * Tora de corte: põe-se a tora (ou a tábua) em cima e bate-se com o machado (qualquer item que
 * corta madeira, inclusive os de outros mods). Rende mais tábuas e gravetos que a mesa de trabalho.
 */
public class CuttingLogBlock extends PrimalStationBlock {
    public CuttingLogBlock(Properties properties) {
        super(properties, box(0, 0, 0, 16, 3, 16));
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type() {
        return PrimalStations.CUTTING_LOG_ENTITY;
    }

    @Override
    protected boolean ticks() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** O que está na tora. */
    public static class Station extends HitStationBlockEntity {
        public Station(BlockPos pos, BlockState state) {
            super(PrimalStations.CUTTING_LOG_ENTITY.get(), pos, state);
        }

        @Override
        protected RecipeType<HitRecipe> recipeType() {
            return PrimalStations.CUTTING_RECIPE.get();
        }

        @Override
        public boolean isTool(ItemStack stack) {
            return stack.canPerformAction(ToolActions.AXE_DIG);
        }

        @Override
        protected SoundEvent hitSound() {
            return SoundEvents.AXE_STRIP;
        }

        @Override
        protected double topHeight() {
            return 3.0 / 16.0;
        }
    }
}
