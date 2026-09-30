package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Receitas da mesa química: duas entradas, uma saída, sem combustível. Rendem mais que a mesa
 * de trabalho — é o que justifica construir a estação. Ficam em código por ora: são poucas, e
 * um tipo de receita de datapack é trabalho de sobra para três linhas.
 */
public record ChemistryRecipe(Supplier<Item> first, int firstCount, Supplier<Item> second, int secondCount,
                              Supplier<Item> result, int resultCount, int ticks) {
    public static final List<ChemistryRecipe> ALL = List.of(
            // Mesa de trabalho: 1 + 1 → 1 narcótico.
            new ChemistryRecipe(ModItems.BLACK_FRUIT::get, 1, () -> Items.ROTTEN_FLESH, 1, ModItems.NARCOTIC::get, 2, 100),
            new ChemistryRecipe(() -> Items.SUGAR, 2, () -> Items.SWEET_BERRIES, 1, ModItems.STIMULANT::get, 2, 100),
            // Mesa de trabalho: 1 flecha + 1 narcótico → 1 flecha tranquilizante.
            new ChemistryRecipe(() -> Items.ARROW, 4, ModItems.NARCOTIC::get, 1, ModItems.TRANQ_ARROW::get, 4, 120));

    /** Receita que as entradas atendem, em qualquer ordem. */
    public static Optional<ChemistryRecipe> find(ItemStack a, ItemStack b) {
        return ALL.stream().filter(recipe -> recipe.matches(a, b) || recipe.matches(b, a)).findFirst();
    }

    public boolean matches(ItemStack a, ItemStack b) {
        return a.is(first.get()) && a.getCount() >= firstCount && b.is(second.get()) && b.getCount() >= secondCount;
    }

    public ItemStack output() {
        return new ItemStack(result.get(), resultCount);
    }

    /** Se o item é ingrediente de alguma receita (para o que a tela aceita nas entradas). */
    public static boolean isIngredient(ItemStack stack) {
        return ALL.stream().anyMatch(recipe -> stack.is(recipe.first.get()) || stack.is(recipe.second.get()));
    }
}
