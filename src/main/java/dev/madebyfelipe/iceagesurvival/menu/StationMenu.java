package dev.madebyfelipe.iceagesurvival.menu;

import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu de estação: os espaços da estação primeiro, depois o inventário do jogador no layout
 * padrão de 176 px. O shift-clique leva da estação para o jogador e do jogador para o
 * primeiro espaço da estação que aceitar o item.
 */
public abstract class StationMenu extends AbstractContainerMenu {
    protected final Container container;
    protected final ContainerData data;
    private final int stationSlots;

    protected StationMenu(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, Container container,
                          ContainerData data) {
        super(type, containerId);
        this.container = container;
        this.data = data;
        this.stationSlots = container.getContainerSize();
        container.startOpen(playerInventory.player);
        addStationSlots();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    /** Acrescenta os espaços da estação, na ordem do inventário dela. */
    protected abstract void addStationSlots();

    public int data(int index) {
        return data.get(index);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < stationSlots) {
            if (!moveItemStackTo(stack, stationSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < stationSlots && !moved; i++) {
                if (slots.get(i).mayPlace(stack)) {
                    moved = moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
