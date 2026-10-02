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

    /** Um minuto para a carne apodrecer na estação de preparação. */
    private static final int ROT_TICKS = 1200;
    private static final int JERKY_TICKS = 600;

    /**
     * Estação de preparação: carne e peixe crus apodrecem (a carne podre é insumo do narcótico); carne com
     * açúcar vira charque, que não estraga e alimenta mais. Entrada única quando {@code second} é nulo.
     */
    public static final List<ChemistryRecipe> PREPARATION = List.of(
            rot(() -> Items.BEEF), rot(() -> Items.PORKCHOP), rot(() -> Items.MUTTON), rot(() -> Items.CHICKEN),
            rot(() -> Items.RABBIT), rot(ModItems.DODO_MEAT::get), rot(() -> Items.COD), rot(() -> Items.SALMON),
            jerky(() -> Items.BEEF), jerky(() -> Items.PORKCHOP), jerky(() -> Items.MUTTON),
            jerky(() -> Items.CHICKEN), jerky(ModItems.DODO_MEAT::get));

    private static ChemistryRecipe rot(Supplier<Item> raw) {
        return new ChemistryRecipe(raw, 1, null, 0, () -> Items.ROTTEN_FLESH, 1, ROT_TICKS);
    }

    private static ChemistryRecipe jerky(Supplier<Item> raw) {
        return new ChemistryRecipe(raw, 2, () -> Items.SUGAR, 1, ModItems.JERKY::get, 2, JERKY_TICKS);
    }

    /** Receita que as entradas atendem, em qualquer ordem. */
    public static Optional<ChemistryRecipe> find(ItemStack a, ItemStack b) {
        return find(ALL, a, b);
    }

    /** Receita desta lista que as entradas atendem, em qualquer ordem; as de duas entradas têm preferência. */
    public static Optional<ChemistryRecipe> find(List<ChemistryRecipe> recipes, ItemStack a, ItemStack b) {
        return recipes.stream().filter(recipe -> recipe.second != null)
                .filter(recipe -> recipe.matches(a, b) || recipe.matches(b, a)).findFirst()
                .or(() -> recipes.stream().filter(recipe -> recipe.second == null)
                        .filter(recipe -> recipe.matches(a, b) || recipe.matches(b, a)).findFirst());
    }

    public boolean matches(ItemStack a, ItemStack b) {
        if (!a.is(first.get()) || a.getCount() < firstCount) {
            return false;
        }
        // Entrada única: a outra fica vazia.
        return second == null ? b.isEmpty() : b.is(second.get()) && b.getCount() >= secondCount;
    }

    public ItemStack output() {
        return new ItemStack(result.get(), resultCount);
    }

    /** Se o item é ingrediente de alguma receita (para o que a tela aceita nas entradas). */
    public static boolean isIngredient(ItemStack stack) {
        return isIngredient(ALL, stack);
    }

    public static boolean isIngredient(List<ChemistryRecipe> recipes, ItemStack stack) {
        return recipes.stream().anyMatch(recipe -> stack.is(recipe.first.get())
                || recipe.second != null && stack.is(recipe.second.get()));
    }
}
