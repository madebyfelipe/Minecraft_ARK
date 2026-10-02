package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.item.ImplantItem;
import dev.madebyfelipe.iceagesurvival.menu.ReviveTableMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Mesa de reviver: implante (espaço 0) + diamante (espaço 1) → a criatura volta em cima da mesa, com um quarto da
 * vida. O espaço 2 não é usado (a tela é a das estações de duas entradas).
 */
public class ReviveTableBlockEntity extends StationBlockEntity {
    public static final int IMPLANT = 0;
    public static final int DIAMOND = 1;
    /** Dez segundos de procedimento. */
    public static final int REVIVE_TICKS = 200;

    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == ChemistryBenchBlockEntity.DATA_PROGRESS ? progress : ready() ? REVIVE_TICKS : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ReviveTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REVIVE_TABLE.get(), pos, state, 3);
    }

    private boolean ready() {
        return ImplantItem.holdsCreature(getItem(IMPLANT)) && getItem(DIAMOND).is(Items.DIAMOND);
    }

    @Override
    public void serverTick() {
        if (!ready()) {
            progress = 0;
            return;
        }
        if (++progress >= REVIVE_TICKS && level instanceof ServerLevel serverLevel) {
            progress = 0;
            Vec3 top = Vec3.atBottomCenterOf(worldPosition.above());
            if (ImplantItem.revive(serverLevel, getItem(IMPLANT), top).isPresent()) {
                getItem(IMPLANT).shrink(1);
                getItem(DIAMOND).shrink(1);
                serverLevel.playSound(null, worldPosition, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == IMPLANT ? ImplantItem.holdsCreature(stack) : slot == DIAMOND && stack.is(Items.DIAMOND);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.iceagesurvival.revive_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ReviveTableMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
    }
}
