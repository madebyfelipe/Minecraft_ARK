package dev.madebyfelipe.iceagesurvival.menu;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CreatureStorageMenu extends AbstractContainerMenu {
    private final Container storage;
    private final ContainerData data;
    private final int rows;

    private record ClientData(Container storage, int rows, int pages) {
    }

    public CreatureStorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, clientData(playerInventory, extraData));
    }

    private CreatureStorageMenu(int containerId, Inventory playerInventory, ClientData clientData) {
        this(containerId, playerInventory, clientData.storage(), null,
                clientData.rows(), clientData.pages(), 0);
    }

    public CreatureStorageMenu(int containerId, Inventory playerInventory, PrehistoricCreature creature) {
        this(containerId, playerInventory, creature.inventory(), creature,
                rowsFor(creature.inventory().getContainerSize()),
                Math.max(1, (creature.inventory().getContainerSize() + 53) / 54), 0);
    }

    private CreatureStorageMenu(int containerId, Inventory playerInventory, Container backing,
                                PrehistoricCreature creature, int rows, int pages, int page) {
        super(ModMenus.CREATURE_STORAGE.get(), containerId);
        this.rows = rows;
        this.data = new SimpleContainerData(2);
        data.set(0, page);
        data.set(1, pages);
        this.storage = new PagedContainer(backing, creature, data, rows * 9);
        storage.startOpen(playerInventory.player);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(storage, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }
        int inventoryY = rows * 18 + 31;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    private static ClientData clientData(Inventory playerInventory, FriendlyByteBuf data) {
        int entityId = data.readVarInt();
        int slots = data.readVarInt();
        int rows = data.readVarInt();
        int pages = data.readVarInt();
        Container storage;
        if (playerInventory.player.level().getEntity(entityId) instanceof PrehistoricCreature creature) {
            storage = creature.inventory();
        } else {
            storage = new SimpleContainer(Math.max(9, slots));
        }
        return new ClientData(storage, rows, pages);
    }

    private static int rowsFor(int slots) {
        return Math.max(1, Math.min(6, (slots + 8) / 9));
    }

    public int rows() {
        return rows;
    }

    public int page() {
        return data.get(0);
    }

    public int pages() {
        return data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pages() <= 1 || (id != 0 && id != 1)) {
            return false;
        }
        int next = page() + (id == 0 ? -1 : 1);
        if (next < 0 || next >= pages()) {
            return false;
        }
        data.set(0, next);
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int storageSlots = rows * 9;
        if (index < storageSlots) {
            if (!moveItemStackTo(stack, storageSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, storageSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private static final class PagedContainer implements Container {
        private final Container backing;
        private final PrehistoricCreature creature;
        private final ContainerData data;
        private final int visibleSlots;

        private PagedContainer(Container backing, PrehistoricCreature creature, ContainerData data, int visibleSlots) {
            this.backing = backing;
            this.creature = creature;
            this.data = data;
            this.visibleSlots = visibleSlots;
        }

        private int backingIndex(int slot) {
            return data.get(0) * 54 + slot;
        }

        @Override
        public int getContainerSize() {
            return visibleSlots;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < visibleSlots; slot++) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            int index = backingIndex(slot);
            return index < backing.getContainerSize() ? backing.getItem(index) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            int index = backingIndex(slot);
            return index < backing.getContainerSize() ? backing.removeItem(index, amount) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            int index = backingIndex(slot);
            return index < backing.getContainerSize() ? backing.removeItemNoUpdate(index) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            int index = backingIndex(slot);
            if (index < backing.getContainerSize()) {
                backing.setItem(index, stack);
            }
        }

        @Override
        public void setChanged() {
            backing.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return creature != null
                    ? creature.canAccessInventory(player) && player.distanceToSqr(creature) <= 64.0
                    : backing.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < visibleSlots; slot++) {
                setItem(slot, ItemStack.EMPTY);
            }
        }

        @Override
        public void startOpen(Player player) {
            backing.startOpen(player);
        }

        @Override
        public void stopOpen(Player player) {
            backing.stopOpen(player);
        }
    }
}
