package dev.madebyfelipe.iceagesurvival.menu;

import dev.madebyfelipe.iceagesurvival.block.ChemistryBenchBlockEntity;
import dev.madebyfelipe.iceagesurvival.block.ChemistryRecipe;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ChemistryBenchMenu extends StationMenu {
    public static final int INPUT_A_X = 44;
    public static final int INPUT_B_X = 62;
    public static final int INPUT_Y = 35;
    public static final int OUTPUT_X = 116;
    public static final int OUTPUT_Y = 35;

    public ChemistryBenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    public ChemistryBenchMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.CHEMISTRY_BENCH.get(), containerId, playerInventory, container, data);
    }

    @Override
    protected void addStationSlots() {
        addSlot(ingredientSlot(ChemistryBenchBlockEntity.INPUT_A, INPUT_A_X));
        addSlot(ingredientSlot(ChemistryBenchBlockEntity.INPUT_B, INPUT_B_X));
        addSlot(new Slot(container, ChemistryBenchBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    private Slot ingredientSlot(int index, int x) {
        return new Slot(container, index, x, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ChemistryRecipe.isIngredient(stack);
            }
        };
    }

    public float progress() {
        int total = data(ChemistryBenchBlockEntity.DATA_TOTAL);
        return total > 0 ? data(ChemistryBenchBlockEntity.DATA_PROGRESS) / (float) total : 0.0F;
    }
}
