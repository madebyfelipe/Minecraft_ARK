package dev.madebyfelipe.iceagesurvival.menu;

import dev.madebyfelipe.iceagesurvival.block.ChemistryRecipe;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class PrepStationMenu extends ChemistryBenchMenu {
    public PrepStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    public PrepStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.PREP_STATION.get(), containerId, playerInventory, container, data);
    }

    @Override
    protected boolean accepts(ItemStack stack) {
        return ChemistryRecipe.isIngredient(ChemistryRecipe.PREPARATION, stack);
    }

    @Override
    public String hintKey() {
        return "iceagesurvival.prep_station.hint";
    }
}
