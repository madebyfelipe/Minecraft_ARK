package dev.madebyfelipe.iceagesurvival.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Troféu de cabeça de um apex (T-Rex, Espinossauro): colocado no chão ou na parede, como decoração. Na mão, é o
 * tributo que deixa o jogador desafiar outro da espécie (ver {@code ApexChallenge}).
 */
public class TrophyHeadBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape FLOOR = Block.box(2, 0, 2, 14, 9, 14);
    private static final VoxelShape WALL_NORTH = Block.box(2, 4, 7, 14, 13, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(2, 4, 0, 14, 13, 9);
    private static final VoxelShape WALL_WEST = Block.box(7, 4, 2, 16, 13, 14);
    private static final VoxelShape WALL_EAST = Block.box(0, 4, 2, 9, 13, 14);

    public TrophyHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            // Na parede: o focinho para fora da parede.
            return defaultBlockState().setValue(FACING, clicked).setValue(WALL, true);
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WALL, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(WALL)) {
            return FLOOR;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;
            default -> WALL_NORTH;
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
