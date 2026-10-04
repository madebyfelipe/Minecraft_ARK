package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Como uma espécie é derrubada e domesticada. Espécie sem este bloco não acumula torpor.
 *
 * @param torporMultiplier     multiplica todo torpor recebido
 * @param torporDecayPerSecond quanto torpor se perde por segundo
 * @param requiredFood         valor total de alimento exigido no nível 1
 * @param requiredFoodPerLevel acréscimo fracionário de alimento por nível acima do 1
 * @param feedIntervalSeconds  espera entre uma alimentação e a próxima
 * @param foods                alimentos aceitos; o primeiro que casar com o item vale
 */
public record TamingProfile(
        double torporMultiplier,
        double torporDecayPerSecond,
        double requiredFood,
        double requiredFoodPerLevel,
        int feedIntervalSeconds,
        List<Food> foods) {

    /**
     * @param items   item, lista de itens ou tag ({@code "#namespace:tag"})
     * @param value   quanto cada unidade avança a domesticação
     * @param quality de 0 a 1; abaixo de 1 reduz a eficiência final
     */
    public record Food(LazyHolderSet<Item> items, double value, double quality) {
        public static final Codec<Food> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LazyHolderSet.codec(Registries.ITEM).fieldOf("items").forGetter(Food::items),
                Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE).fieldOf("value").forGetter(Food::value),
                Codec.doubleRange(0, 1).optionalFieldOf("quality", 1.0).forGetter(Food::quality)
        ).apply(instance, Food::new));
    }

    public static final Codec<TamingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("torpor_multiplier", 1.0).forGetter(TamingProfile::torporMultiplier),
            Codec.doubleRange(0, Double.MAX_VALUE).fieldOf("torpor_decay_per_second").forGetter(TamingProfile::torporDecayPerSecond),
            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE).fieldOf("required_food").forGetter(TamingProfile::requiredFood),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("required_food_per_level", 0.0).forGetter(TamingProfile::requiredFoodPerLevel),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("feed_interval_seconds").forGetter(TamingProfile::feedIntervalSeconds),
            Food.CODEC.listOf().fieldOf("foods").forGetter(TamingProfile::foods)
    ).apply(instance, TamingProfile::new));

    public Optional<Food> foodFor(ItemStack stack) {
        return foods.stream().filter(food -> stack.is(food.items()::contains)).findFirst();
    }
}
