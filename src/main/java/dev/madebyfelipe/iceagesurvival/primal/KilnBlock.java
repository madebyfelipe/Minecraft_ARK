package dev.madebyfelipe.iceagesurvival.primal;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Forno de olaria: um espaço, carregado pela boca (a face da frente), queima com calor logo embaixo
 * ({@link PrimalStationBlock#COOKING_HEAT}). Funde bloco de minério bruto inteiro e faz vidro e
 * terracota, sem combustível e sem fornalha.
 */
public class KilnBlock extends PrimalStationBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public KilnBlock(Properties properties) {
        super(properties, box(1, 0, 1, 15, 22, 15));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type() {
        return PrimalStations.KILN_ENTITY;
    }

    @Override
    protected boolean isWorkFace(BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!heatedFromBelow(level, pos)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(10) == 0) {
            level.playLocalSound(x, pos.getY() + 0.5, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(3) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, pos.getY() + 1.4, z,
                    0.0, 0.07, 0.0);
        }
        if (level.getBlockEntity(pos) instanceof Station kiln && kiln.isWorking(0)) {
            Direction facing = state.getValue(FACING);
            level.addParticle(ParticleTypes.FLAME, x + facing.getStepX() * 0.45, pos.getY() + 0.2,
                    z + facing.getStepZ() * 0.45, 0.0, 0.0, 0.0);
        }
    }

    /** O que está no forno. */
    public static class Station extends TimedStationBlockEntity {
        public Station(BlockPos pos, BlockState state) {
            super(PrimalStations.KILN_ENTITY.get(), pos, state, 1);
        }

        @Override
        protected RecipeType<TimedRecipe> recipeType() {
            return PrimalStations.KILN_RECIPE.get();
        }

        @Override
        public boolean canWork() {
            return level != null && heatedFromBelow(level, worldPosition);
        }
    }
}
