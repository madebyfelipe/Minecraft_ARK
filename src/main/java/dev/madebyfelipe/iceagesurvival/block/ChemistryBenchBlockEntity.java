package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.menu.ChemistryBenchMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Processa uma {@link ChemistryRecipe} por vez: entradas nos espaços 0 e 1, resultado no 2. */
public class ChemistryBenchBlockEntity extends StationBlockEntity {
    public static final int INPUT_A = 0;
    public static final int INPUT_B = 1;
    public static final int OUTPUT = 2;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;

    private int progress;
    private int total;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_PROGRESS ? progress : total;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ChemistryBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMISTRY_BENCH.get(), pos, state, 3);
    }

    @Override
    public void serverTick() {
        Optional<ChemistryRecipe> recipe = ChemistryRecipe.find(getItem(INPUT_A), getItem(INPUT_B))
                .filter(this::fitsOutput);
        if (recipe.isEmpty()) {
            progress = 0;
            total = 0;
            return;
        }
        total = recipe.get().ticks();
        if (++progress >= total) {
            craft(recipe.get());
            progress = 0;
        }
        setChanged();
    }

    private boolean fitsOutput(ChemistryRecipe recipe) {
        ItemStack output = getItem(OUTPUT);
        ItemStack result = recipe.output();
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(ChemistryRecipe recipe) {
        ItemStack a = getItem(INPUT_A);
        boolean inOrder = recipe.matches(a, getItem(INPUT_B));
        getItem(inOrder ? INPUT_A : INPUT_B).shrink(recipe.firstCount());
        getItem(inOrder ? INPUT_B : INPUT_A).shrink(recipe.secondCount());
        ItemStack output = getItem(OUTPUT);
        if (output.isEmpty()) {
            setItem(OUTPUT, recipe.output());
        } else {
            output.grow(recipe.resultCount());
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != OUTPUT && ChemistryRecipe.isIngredient(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.iceagesurvival.chemistry_bench");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ChemistryBenchMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
    }
}
