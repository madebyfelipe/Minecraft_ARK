package dev.madebyfelipe.iceagesurvival.primal;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Estação que trabalha com o tempo (grelha, forno de olaria, varal): cada espaço conta os seus ticks
 * enquanto a estação pode trabalhar ({@link #canWork()}: fogo embaixo, sol) e, no tempo da receita,
 * o item vira o resultado e fica ali até ser tirado.
 */
public abstract class TimedStationBlockEntity extends PrimalStationBlockEntity {
    private final int[] progress;
    private final int[] total;

    protected TimedStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
        progress = new int[size];
        total = new int[size];
    }

    protected abstract RecipeType<TimedRecipe> recipeType();

    /** Se a estação trabalha agora (fogo embaixo, dia claro). */
    public abstract boolean canWork();

    public Optional<TimedRecipe> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(recipeType(), new SimpleContainer(stack), level);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return recipeFor(stack).isPresent();
    }

    /** O espaço tem um item que ainda não ficou pronto (o cliente usa para a fumaça). */
    public boolean isWorking(int slot) {
        return !items.get(slot).isEmpty() && progress[slot] < total[slot];
    }

    @Override
    protected void onInserted(int slot) {
        progress[slot] = 0;
        total[slot] = recipeFor(items.get(slot)).map(TimedRecipe::time).orElse(0);
    }

    @Override
    protected void onRemoved(int slot) {
        progress[slot] = 0;
        total[slot] = 0;
    }

    @Override
    public void serverTick() {
        if (level == null || !canWork()) {
            return;
        }
        boolean progressed = false;
        boolean finished = false;
        for (int slot = 0; slot < items.size(); slot++) {
            if (!isWorking(slot)) {
                continue;
            }
            progressed = true;
            if (++progress[slot] < total[slot]) {
                continue;
            }
            // A receita pode ter saído num /reload: o item fica como está, parado.
            ItemStack input = items.get(slot);
            Optional<TimedRecipe> recipe = recipeFor(input);
            if (recipe.isPresent()) {
                items.set(slot, recipe.get().assemble(new SimpleContainer(input), level.registryAccess()));
                finished = true;
            }
        }
        if (finished) {
            changed();
        } else if (progressed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Progress", progress);
        tag.putIntArray("Total", total);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        copy(tag.getIntArray("Progress"), progress);
        copy(tag.getIntArray("Total"), total);
    }

    private static void copy(int[] from, int[] to) {
        for (int slot = 0; slot < to.length; slot++) {
            to[slot] = slot < from.length ? from[slot] : 0;
        }
    }
}
