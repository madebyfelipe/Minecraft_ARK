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
 * Receita de estação que trabalha com golpes: um item entra e, depois de {@code hits} golpes da
 * ferramenta certa, salta como {@code result}. Serve à tora de corte (machado) e à bigorna de pedra
 * (martelo), cada uma com o seu {@link RecipeType}.
 *
 * <pre>{"type": "iceagesurvival:cutting", "ingredient": {...}, "result": {"item": "...", "count": 6}, "hits": 3}</pre>
 */
public class HitRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int hits;
    private final Serializer serializer;

    public HitRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int hits, Serializer serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.hits = hits;
        this.serializer = serializer;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** Golpes até o item ficar pronto. */
    public int hits() {
        return hits;
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

    /** Um serializador por estação: guarda o tipo e os golpes padrão quando o JSON não diz. */
    public static class Serializer implements RecipeSerializer<HitRecipe> {
        private final Supplier<RecipeType<HitRecipe>> type;
        private final int defaultHits;

        public Serializer(Supplier<RecipeType<HitRecipe>> type, int defaultHits) {
            this.type = type;
            this.defaultHits = defaultHits;
        }

        @Override
        public HitRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient")
                    ? GsonHelper.getAsJsonArray(json, "ingredient")
                    : GsonHelper.getAsJsonObject(json, "ingredient"), false);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int hits = GsonHelper.getAsInt(json, "hits", defaultHits);
            if (hits <= 0) {
                throw new IllegalArgumentException("hits deve ser positivo em " + id);
            }
            return new HitRecipe(id, ingredient, result, hits, this);
        }

        @Override
        public HitRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int hits = buffer.readVarInt();
            return new HitRecipe(id, ingredient, result, hits, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, HitRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.hits);
        }
    }
}
