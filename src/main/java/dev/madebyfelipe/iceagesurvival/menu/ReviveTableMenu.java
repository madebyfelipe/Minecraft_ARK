package dev.madebyfelipe.iceagesurvival.menu;

import dev.madebyfelipe.iceagesurvival.item.ImplantItem;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ReviveTableMenu extends ChemistryBenchMenu {
    public ReviveTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    public ReviveTableMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.REVIVE_TABLE.get(), containerId, playerInventory, container, data);
    }

    @Override
    protected boolean accepts(ItemStack stack) {
        return ImplantItem.holdsCreature(stack) || stack.is(Items.DIAMOND);
    }

    @Override
    public String hintKey() {
        return "iceagesurvival.revive_table.hint";
    }
}
