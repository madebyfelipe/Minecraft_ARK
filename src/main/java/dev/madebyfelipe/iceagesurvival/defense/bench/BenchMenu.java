package dev.madebyfelipe.iceagesurvival.defense.bench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Menu de bancada: a lista de receitas da bancada (como o cortador de pedra) em cima e o inventário do jogador
 * embaixo. A tela manda o clique pelo pacote de botão do vanilla ({@link #clickMenuButton}); o servidor tira os
 * ingredientes do inventário principal e entrega o resultado. O botão é {@code índice * 2}, mais 1 com Shift
 * (fabrica até encher uma pilha do resultado); o pacote leva um byte, então cabem 64 receitas por bancada.
 */
public class BenchMenu extends AbstractContainerMenu {
    /** Receitas por bancada que o byte do pacote de botão comporta. */
    public static final int MAX_RECIPES = 64;

    private final BenchKind kind;
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final List<BenchRecipe> recipes;

    public BenchMenu(BenchKind kind, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(kind.menu(), containerId);
        this.kind = kind;
        this.access = access;
        this.inventory = inventory;
        this.recipes = recipesFor(inventory.player.level().getRecipeManager(), kind);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** As receitas de uma bancada, em ordem de id (a mesma no cliente e no servidor). */
    public static List<BenchRecipe> recipesFor(RecipeManager manager, BenchKind kind) {
        return manager.getAllRecipesFor(Benches.BENCH_RECIPE.get()).stream()
                .filter(recipe -> recipe.bench() == kind)
                .sorted(Comparator.comparing(BenchRecipe::getId))
                .limit(MAX_RECIPES)
                .toList();
    }

    public BenchKind kind() {
        return kind;
    }

    public List<BenchRecipe> recipes() {
        return recipes;
    }

    /** O inventário principal do jogador (36 espaços), de onde saem os ingredientes. */
    public List<ItemStack> playerItems() {
        return inventory.items;
    }

    public boolean canCraft(BenchRecipe recipe) {
        return recipe.plan(playerItems()) != null;
    }

    /** O número do botão que fabrica a receita {@code index} (com Shift, até encher uma pilha). */
    public static int buttonId(int index, boolean repeat) {
        return index * 2 + (repeat ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int index = id >> 1;
        if (id < 0 || index >= recipes.size()) {
            return false;
        }
        BenchRecipe recipe = recipes.get(index);
        int limit = (id & 1) == 0 ? 1 : Math.max(1, recipe.result().getMaxStackSize() / Math.max(1, recipe.result().getCount()));
        int crafted = 0;
        while (crafted < limit && craftOnce(player, recipe)) {
            crafted++;
        }
        if (crafted > 0) {
            access.execute((level, pos) -> level.playSound(null, pos, kind.craftSound(), SoundSource.BLOCKS, 1.0F,
                    0.9F + level.random.nextFloat() * 0.2F));
        }
        return crafted > 0;
    }

    /** Paga e entrega uma vez; falso (sem mexer em nada) se faltar ingrediente. */
    private boolean craftOnce(Player player, BenchRecipe recipe) {
        List<ItemStack> items = playerItems();
        int[] taken = recipe.plan(items);
        if (taken == null) {
            return false;
        }
        List<ItemStack> leftovers = new ArrayList<>();
        for (int i = 0; i < taken.length; i++) {
            if (taken[i] == 0) {
                continue;
            }
            ItemStack stack = items.get(i);
            if (stack.hasCraftingRemainingItem()) {
                ItemStack remainder = stack.getCraftingRemainingItem();
                for (int n = 0; n < taken[i]; n++) {
                    leftovers.add(remainder.copy());
                }
            }
            stack.shrink(taken[i]);
        }
        ItemStack result = recipe.result().copy();
        result.onCraftedBy(player.level(), player, result.getCount());
        ItemHandlerHelper.giveItemToPlayer(player, result);
        leftovers.forEach(leftover -> ItemHandlerHelper.giveItemToPlayer(player, leftover));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, kind.block());
    }

    /** Shift-clique troca entre o inventário principal (0–26) e a barra (27–35). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
