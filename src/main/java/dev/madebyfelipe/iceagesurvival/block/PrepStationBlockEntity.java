package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.menu.PrepStationMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Estação de preparação: apodrece carne e peixe (insumo do narcótico) e faz charque com carne e açúcar.
 * Ver {@link ChemistryRecipe#PREPARATION}.
 */
public class PrepStationBlockEntity extends ChemistryBenchBlockEntity {
    public PrepStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PREP_STATION.get(), pos, state);
    }

    @Override
    protected List<ChemistryRecipe> recipes() {
        return ChemistryRecipe.PREPARATION;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.iceagesurvival.prep_station");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrepStationMenu(containerId, inventory, this, data);
    }
}
