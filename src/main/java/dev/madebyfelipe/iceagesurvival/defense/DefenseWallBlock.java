package dev.madebyfelipe.iceagesurvival.defense;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Muro alto: bloco cheio que se empilha, com colisão de 1,5 de altura como a cerca — no topo da pilha ninguém pula
 * por cima. Para os caminhos das criaturas é cerca: não passam nem sobem.
 */
public class DefenseWallBlock extends Block implements DefenseBlock {
    /** A colisão: o bloco inteiro e mais meio bloco para cima. */
    public static final VoxelShape COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 24.0, 16.0);

    private final boolean wood;

    public DefenseWallBlock(boolean wood, Properties properties) {
        super(properties);
        this.wood = wood;
    }

    @Override
    public boolean yieldsToGiants() {
        return wood;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return BlockPathTypes.FENCE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            DefenseDamage.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
