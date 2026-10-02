package dev.madebyfelipe.iceagesurvival.primal;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloco de uma estação primitiva (porte do Primal Stage): forma própria, sem tela, item à vista em
 * cima. O clique na face de trabalho vai para {@link PrimalStationBlockEntity#interact}; quebrado,
 * derruba o que tiver em cima.
 */
public abstract class PrimalStationBlock extends BaseEntityBlock {
    /** Calor que cozinha na grelha e no forno quando está logo embaixo: fogo, fogueira acesa, magma, lava. */
    public static final TagKey<Block> COOKING_HEAT = TagKey.create(Registries.BLOCK, IceAgeSurvival.id("cooking_heat"));

    private final VoxelShape shape;

    protected PrimalStationBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
    }

    /** O tipo de block entity; um supplier porque os registros saem depois dos blocos. */
    protected abstract Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type();

    /** Se a estação muda com o tempo (as de golpe não precisam de tick). */
    protected boolean ticks() {
        return true;
    }

    /** A face que recebe o clique. Por padrão, a de cima. */
    protected boolean isWorkFace(BlockState state, Direction face) {
        return face == Direction.UP;
    }

    /** O espaço mirado, pela posição do clique dentro do bloco (0–1). Por padrão, um espaço só. */
    protected int slotAt(BlockState state, Vec3 local) {
        return 0;
    }

    /** Grade 2 × 2 vista de cima: 0 noroeste, 1 nordeste, 2 sudoeste, 3 sudeste. */
    protected static int quadrant(Vec3 local) {
        return (local.x >= 0.5 ? 1 : 0) + (local.z >= 0.5 ? 2 : 0);
    }

    /** Há calor de cozinhar logo embaixo de {@code pos} (fogueira apagada não conta). */
    public static boolean heatedFromBelow(BlockGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(COOKING_HEAT)) {
            return false;
        }
        return !below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type().get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !ticks() || type != type().get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((PrimalStationBlockEntity) blockEntity).serverTick();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!isWorkFace(state, hit.getDirection())
                || !(level.getBlockEntity(pos) instanceof PrimalStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return station.interact(player, hand, slotAt(state, local));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PrimalStationBlockEntity station) {
            Containers.dropContents(level, pos, station.items());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
