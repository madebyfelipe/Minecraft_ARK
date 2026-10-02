package dev.madebyfelipe.iceagesurvival.primal;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
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
 * Receita de estação que trabalha com o tempo: um item entra, fica {@code time} ticks na estação e
 * vira {@code result}. Serve à grelha, ao forno de olaria e ao varal de secagem, cada um com o seu
 * {@link RecipeType}.
 *
 * <pre>{"type": "iceagesurvival:drying", "ingredient": {...}, "result": {"item": "...", "count": 1}, "time": 600}</pre>
 */
public class TimedRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int time;
    private final Serializer serializer;

    public TimedRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int time, Serializer serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.time = time;
        this.serializer = serializer;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** Ticks que o item fica na estação. */
    public int time() {
        return time;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return ingredient.test(container.getItem(0));
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
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** Fora do livro de receitas: a estação não tem tela. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return serializer.type.get();
    }

    /** Um serializador por estação: guarda o tipo e o tempo padrão quando o JSON não diz. */
    public static class Serializer implements RecipeSerializer<TimedRecipe> {
        private final Supplier<RecipeType<TimedRecipe>> type;
        private final int defaultTime;

        public Serializer(Supplier<RecipeType<TimedRecipe>> type, int defaultTime) {
            this.type = type;
            this.defaultTime = defaultTime;
        }

        @Override
        public TimedRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient")
                    ? GsonHelper.getAsJsonArray(json, "ingredient")
                    : GsonHelper.getAsJsonObject(json, "ingredient"), false);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int time = GsonHelper.getAsInt(json, "time", defaultTime);
            if (time <= 0) {
                throw new IllegalArgumentException("time deve ser positivo em " + id);
            }
            return new TimedRecipe(id, ingredient, result, time, this);
        }

        @Override
        public TimedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int time = buffer.readVarInt();
            return new TimedRecipe(id, ingredient, result, time, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TimedRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.time);
        }
    }
}
