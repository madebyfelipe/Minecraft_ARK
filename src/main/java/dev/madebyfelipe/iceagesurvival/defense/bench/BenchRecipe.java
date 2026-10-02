package dev.madebyfelipe.iceagesurvival.defense.bench;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Receita de bancada (Construção ou Armeiro): ingredientes com quantidade, tirados direto do inventário do
 * jogador, viram o resultado. Sem grade nem forma; a bancada mostra a lista e o clique fabrica.
 *
 * <pre>{
 *   "type": "iceagesurvival:bench",
 *   "bench": "armory",
 *   "ingredients": [{"item": "minecraft:iron_ingot", "count": 3}, {"tag": "minecraft:planks", "count": 2}],
 *   "result": {"item": "iceagesurvival:tranq_rifle", "count": 1}
 * }</pre>
 *
 * Cada ingrediente é um ingrediente comum (item, tag ou tipo do Forge) com {@code "count"} opcional (1). Aceita
 * {@code "conditions"} do Forge, como qualquer receita.
 */
public class BenchRecipe implements Recipe<Container> {
    /** Um ingrediente e quantos dele a receita pede. */
    public record Cost(Ingredient ingredient, int count) {
    }

    private final ResourceLocation id;
    private final BenchKind bench;
    private final List<Cost> costs;
    private final ItemStack result;

    public BenchRecipe(ResourceLocation id, BenchKind bench, List<Cost> costs, ItemStack result) {
        this.id = id;
        this.bench = bench;
        this.costs = List.copyOf(costs);
        this.result = result;
    }

    public BenchKind bench() {
        return bench;
    }

    public List<Cost> costs() {
        return costs;
    }

    public ItemStack result() {
        return result;
    }

    /**
     * Quantos itens tirar de cada espaço para pagar a receita uma vez, ou {@code null} se faltar algo. Paga os
     * ingredientes na ordem do JSON, cada um do primeiro espaço que servir.
     */
    @Nullable
    public int[] plan(List<ItemStack> slots) {
        int[] taken = new int[slots.size()];
        for (Cost cost : costs) {
            int missing = cost.count();
            for (int i = 0; i < slots.size() && missing > 0; i++) {
                ItemStack stack = slots.get(i);
                int free = stack.getCount() - taken[i];
                if (free > 0 && cost.ingredient().test(stack)) {
                    int take = Math.min(free, missing);
                    taken[i] += take;
                    missing -= take;
                }
            }
            if (missing > 0) {
                return null;
            }
        }
        return taken;
    }

    /** Quantos itens que servem a este ingrediente há nos espaços (para a tela mostrar "tem/precisa"). */
    public static int available(List<ItemStack> slots, Cost cost) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && cost.ingredient().test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public boolean matches(Container container, Level level) {
        List<ItemStack> slots = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            slots.add(container.getItem(i));
        }
        return plan(slots) != null;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        costs.forEach(cost -> list.add(cost.ingredient()));
        return list;
    }

    /** Fora do livro de receitas: a bancada tem a própria lista. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(bench.block());
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Benches.BENCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Benches.BENCH_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<BenchRecipe> {
        @Override
        public BenchRecipe fromJson(ResourceLocation id, JsonObject json) {
            BenchKind bench = BenchKind.byName(GsonHelper.getAsString(json, "bench"));
            List<Cost> costs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
                JsonObject entry = GsonHelper.convertToJsonObject(element, "ingredient");
                int count = GsonHelper.getAsInt(entry, "count", 1);
                if (count <= 0) {
                    throw new JsonParseException("count deve ser positivo em " + id);
                }
                JsonObject ingredient = entry.deepCopy();
                ingredient.remove("count");
                costs.add(new Cost(Ingredient.fromJson(ingredient, false), count));
            }
            if (costs.isEmpty()) {
                throw new JsonParseException("receita de bancada sem ingredientes: " + id);
            }
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new BenchRecipe(id, bench, costs, result);
        }

        @Override
        public BenchRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            BenchKind bench = buffer.readEnum(BenchKind.class);
            int size = buffer.readVarInt();
            List<Cost> costs = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Ingredient ingredient = Ingredient.fromNetwork(buffer);
                costs.add(new Cost(ingredient, buffer.readVarInt()));
            }
            return new BenchRecipe(id, bench, costs, buffer.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BenchRecipe recipe) {
            buffer.writeEnum(recipe.bench);
            buffer.writeVarInt(recipe.costs.size());
            for (Cost cost : recipe.costs) {
                cost.ingredient().toNetwork(buffer);
                buffer.writeVarInt(cost.count());
            }
            buffer.writeItem(recipe.result);
        }
    }
}
