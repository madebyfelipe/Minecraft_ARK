package dev.madebyfelipe.iceagesurvival.menu;

import dev.madebyfelipe.iceagesurvival.block.IncubatorBlockEntity;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

public class IncubatorMenu extends StationMenu {
    public static final int EGG_X = 80;
    public static final int EGG_Y = 26;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 56;

    /** Lado do cliente: a tela recebe o conteúdo e os números pelo menu. */
    public IncubatorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new SimpleContainerData(IncubatorBlockEntity.DATA_COUNT));
    }

    public IncubatorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.INCUBATOR.get(), containerId, playerInventory, container, data);
    }

    @Override
    protected void addStationSlots() {
        addSlot(new Slot(container, IncubatorBlockEntity.EGG_SLOT, EGG_X, EGG_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CREATURE_EGG.get());
            }
        });
        addSlot(new Slot(container, IncubatorBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            }
        });
    }

    public float incubation() {
        int total = data(IncubatorBlockEntity.DATA_TOTAL);
        return total > 0 ? data(IncubatorBlockEntity.DATA_PROGRESS) / (float) total : 0.0F;
    }

    /** Segundos até chocar; −1 sem ovo. */
    public int secondsLeft() {
        int total = data(IncubatorBlockEntity.DATA_TOTAL);
        return total > 0 ? (total - data(IncubatorBlockEntity.DATA_PROGRESS)) / 20 : -1;
    }

    public float fuel() {
        int duration = data(IncubatorBlockEntity.DATA_BURN_DURATION);
        return duration > 0 ? data(IncubatorBlockEntity.DATA_BURN) / (float) duration : 0.0F;
    }

    public boolean blockHeat() {
        return data(IncubatorBlockEntity.DATA_BLOCK_HEAT) != 0;
    }

    public boolean heated() {
        return blockHeat() || data(IncubatorBlockEntity.DATA_BURN) > 0;
    }
}
