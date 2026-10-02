package dev.madebyfelipe.iceagesurvival.defense.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bancada sem inventário próprio: o clique direito abre a lista de receitas dela. */
public class BenchBlock extends Block {
    private final BenchKind kind;

    public BenchBlock(BenchKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public BenchKind kind() {
        return kind;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) ->
                new BenchMenu(kind, id, inventory, ContainerLevelAccess.create(level, pos)), getName());
    }
}
