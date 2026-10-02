package dev.madebyfelipe.iceagesurvival.primal;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bigorna de pedra: põe-se o item em cima e martela-se com uma ferramenta da tag
 * {@code #iceagesurvival:anvil_hammers} (picaretas; martelos do TFC se ele estiver instalado). O
 * Primal Stage martelava lingotes em chapas com um maço de sílex; aqui ela quebra pedra e osso.
 */
public class StoneAnvilBlock extends PrimalStationBlock {
    /** O que martela na bigorna de pedra. */
    public static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, IceAgeSurvival.id("anvil_hammers"));

    public StoneAnvilBlock(Properties properties) {
        super(properties, box(1, 0, 1, 15, 12, 15));
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type() {
        return PrimalStations.STONE_ANVIL_ENTITY;
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

    /** O que está na bigorna. */
    public static class Station extends HitStationBlockEntity {
        public Station(BlockPos pos, BlockState state) {
            super(PrimalStations.STONE_ANVIL_ENTITY.get(), pos, state);
        }

        @Override
        protected RecipeType<HitRecipe> recipeType() {
            return PrimalStations.FORGING_RECIPE.get();
        }

        @Override
        public boolean isTool(ItemStack stack) {
            return stack.is(HAMMERS);
        }

        @Override
        protected SoundEvent hitSound() {
            return SoundEvents.STONE_HIT;
        }

        @Override
        protected double topHeight() {
            return 12.0 / 16.0;
        }
    }
}
