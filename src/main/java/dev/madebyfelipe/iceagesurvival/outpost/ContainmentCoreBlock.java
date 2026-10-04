package dev.madebyfelipe.iceagesurvival.outpost;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O Núcleo da Contenção, no centro do salão da base: indestrutível, mantém o campo de êxtase enquanto algum
 * {@link StasisGeneratorBlock gerador} perto dele estiver de pé. A lógica fica em {@link ContainmentCoreBlockEntity}.
 */
public class ContainmentCoreBlock extends BaseEntityBlock {
    public ContainmentCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainmentCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, Outposts.CONTAINMENT_CORE_ENTITY.get(), ContainmentCoreBlockEntity::serverTick);
    }
}
