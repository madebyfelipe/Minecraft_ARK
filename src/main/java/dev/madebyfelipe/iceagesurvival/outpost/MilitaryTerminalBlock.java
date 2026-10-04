package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * O terminal dos postos militares: decorativo, virado para quem o colocou. Lido com o Analisador (mira firme até a
 * leitura terminar, como numa criatura), destrava o próximo registro militar de quem leu. Clicado sem o Analisador,
 * só lembra como ler.
 */
public class MilitaryTerminalBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public MilitaryTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MilitaryTerminalBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.ANALYZER.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.translatable("block.iceagesurvival.military_terminal.hint"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
