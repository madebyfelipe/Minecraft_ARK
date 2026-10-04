package dev.madebyfelipe.iceagesurvival.defense;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Portão de defesa, de madeira ou de pedra: o comum tem 1 de largura × 2 de altura; o grande, 5 × 5, para o Bronto e
 * o T-Rex passarem. Cada bloco é uma parte ({@link #COLUMN}, {@link #ROW}); a parte mestra (embaixo, no meio, onde o
 * item foi posto) guarda o dono. Só o dono e os aliados dele abrem e fecham, com clique direito em qualquer parte;
 * fechado, bloqueia todos. Quebrar uma parte desfaz o portão inteiro e devolve um item só.
 */
public class DefenseGateBlock extends BaseEntityBlock implements DefenseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Coluna da parte, da esquerda para a direita de quem colocou (0 a largura − 1). */
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 4);
    /** Fileira da parte, de baixo para cima (0 a altura − 1). */
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 4);

    /** A folha fechada, atravessada no meio do bloco; para os lados leste/oeste, girada. */
    private static final VoxelShape PANEL_X = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape PANEL_Z = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    /** A fileira de cima colide até 1,5, como o muro: ninguém pula o portão. */
    private static final VoxelShape TOP_X = Block.box(0.0, 0.0, 6.0, 16.0, 24.0, 10.0);
    private static final VoxelShape TOP_Z = Block.box(6.0, 0.0, 0.0, 10.0, 24.0, 16.0);
    /** A folha aberta, dobrada 90° e encostada numa face do bloco, como uma porta do vanilla aberta. */
    private static final VoxelShape LEAF_WEST = Block.box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0);
    private static final VoxelShape LEAF_EAST = Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape LEAF_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 4.0);
    private static final VoxelShape LEAF_SOUTH = Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0);

    private final boolean wood;
    private final int width;
    private final int height;

    public DefenseGateBlock(boolean wood, int width, int height, Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK));
        this.wood = wood;
        this.width = width;
        this.height = height;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false)
                .setValue(COLUMN, 0).setValue(ROW, 0));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public boolean yieldsToGiants() {
        return wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, COLUMN, ROW);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ---- Geometria ----

    /** A direita de quem colocou, olhando para {@code facing}: o sentido das colunas. */
    private static Direction right(Direction facing) {
        return facing.getClockWise();
    }

    /** A parte da coluna 0, fileira 0. */
    private static BlockPos origin(BlockPos pos, BlockState state) {
        return pos.relative(right(state.getValue(FACING)), -state.getValue(COLUMN)).below(state.getValue(ROW));
    }

    /** A parte mestra: embaixo, no meio. */
    public BlockPos master(BlockPos pos, BlockState state) {
        return origin(pos, state).relative(right(state.getValue(FACING)), width / 2);
    }

    private boolean isMaster(BlockState state) {
        return state.getValue(COLUMN) == width / 2 && state.getValue(ROW) == 0;
    }

    private BlockPos partPos(BlockPos origin, Direction facing, int column, int row) {
        return origin.relative(right(facing), column).above(row);
    }

    @Override
    public BlockPos damageKey(BlockGetter level, BlockPos pos, BlockState state) {
        return master(pos, state);
    }

    // ---- Colocar e desfazer ----

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos origin = context.getClickedPos().relative(right(facing), -(width / 2));
        Level level = context.getLevel();
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                BlockPos part = partPos(origin, facing, column, row);
                if (part.getY() >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(part)
                        || !part.equals(context.getClickedPos()) && !level.getBlockState(part).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(COLUMN, width / 2).setValue(ROW, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos origin = origin(pos, state);
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                BlockPos part = partPos(origin, facing, column, row);
                if (!part.equals(pos)) {
                    level.setBlock(part, state.setValue(COLUMN, column).setValue(ROW, row), Block.UPDATE_ALL);
                }
            }
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof DefenseOwnerBlockEntity owned) {
            owned.setOwner(player);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** As outras partes do mesmo portão (mesmo bloco, mesma direção, índices batendo com a posição). */
    private void forEachOtherPart(Level level, BlockPos pos, BlockState state, java.util.function.BiConsumer<BlockPos, BlockState> action) {
        Direction facing = state.getValue(FACING);
        BlockPos origin = origin(pos, state);
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                BlockPos part = partPos(origin, facing, column, row);
                if (part.equals(pos)) {
                    continue;
                }
                BlockState other = level.getBlockState(part);
                if (other.is(this) && other.getValue(FACING) == facing && other.getValue(COLUMN) == column
                        && other.getValue(ROW) == row) {
                    action.accept(part, other);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            DefenseDamage.forget(level, master(pos, state));
            if (!level.isClientSide) {
                // Sai o portão inteiro; só a parte quebrada derruba o item (as outras somem sem drop).
                forEachOtherPart(level, pos, state, (part, other) -> level.setBlock(part, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMaster(state) ? new DefenseOwnerBlockEntity(pos, state) : null;
    }

    @Nullable
    private DefenseOwnerBlockEntity owner(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(master(pos, state)) instanceof DefenseOwnerBlockEntity owned ? owned : null;
    }

    // ---- Abrir e fechar ----

    /** Se {@code player} abre este portão: o dono ou um aliado; portão sem dono (posto por comando), qualquer um. */
    public boolean mayOperate(Level level, BlockPos pos, BlockState state, Player player) {
        DefenseOwnerBlockEntity owned = owner(level, pos, state);
        return owned == null || owned.owner() == null || owned.isOwnerOrAlly(player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!mayOperate(level, pos, state, player)) {
            player.displayClientMessage(Component.translatable("message.iceagesurvival.gate.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        setOpen(level, pos, state, !state.getValue(OPEN));
        return InteractionResult.CONSUME;
    }

    /** Abre ou fecha o portão inteiro. */
    public void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        forEachOtherPart(level, pos, state, (part, other) -> level.setBlock(part, other.setValue(OPEN, open), Block.UPDATE_ALL));
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        SoundEvent sound = wood
                ? open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE
                : open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE;
        level.playSound(null, master(pos, state), sound, SoundSource.BLOCKS, 1.0F, width > 1 ? 0.6F : 1.0F);
    }

    // ---- Forma e caminhos ----

    private static boolean alongX(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z;
    }

    /**
     * Para que lado fica a folha aberta desta parte: abre como porta dupla, cada folha encostada na face externa da
     * própria coluna — a coluna 0 na face da esquerda, a última na da direita. No portão comum (uma coluna só), a
     * folha encosta na esquerda. As colunas do meio do portão grande não têm folha ({@code null}).
     */
    @Nullable
    public Direction openLeafSide(BlockState state) {
        Direction facing = state.getValue(FACING);
        int column = state.getValue(COLUMN);
        if (column == 0) {
            return facing.getCounterClockWise();
        }
        if (column == width - 1) {
            return right(facing);
        }
        return null;
    }

    private static VoxelShape leaf(Direction side) {
        return switch (side) {
            case WEST -> LEAF_WEST;
            case EAST -> LEAF_EAST;
            case NORTH -> LEAF_NORTH;
            default -> LEAF_SOUTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            Direction side = openLeafSide(state);
            return side == null ? Shapes.empty() : leaf(side);
        }
        return alongX(state) ? PANEL_X : PANEL_Z;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            // Aberto, nada colide: nem o vão do meio, nem as folhas encostadas (o vão inteiro fica livre).
            return Shapes.empty();
        }
        boolean top = state.getValue(ROW) == height - 1;
        return alongX(state) ? top ? TOP_X : PANEL_X : top ? TOP_Z : PANEL_Z;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Nullable
    @Override
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return state.getValue(OPEN) ? null : BlockPathTypes.FENCE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
}
