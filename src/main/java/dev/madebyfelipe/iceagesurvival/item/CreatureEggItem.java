package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.genetics.GenomeNbt;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Ovo fecundado de uma criatura do mod: carrega a espécie, o genoma do filhote e o dono. Só
 * choca na incubadora. Um item para todas as espécies; a cor vem do ovo gerador de cada uma.
 */
public class CreatureEggItem extends Item {
    private static final String SPECIES = "Species";
    private static final String GENOME = "Genome";
    private static final String OWNER = "Owner";

    public CreatureEggItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(EntityType<?> species, Genome genome, @Nullable UUID owner) {
        CompoundTag tag = new CompoundTag();
        tag.putString(SPECIES, BuiltInRegistries.ENTITY_TYPE.getKey(species).toString());
        tag.put(GENOME, GenomeNbt.write(genome));
        if (owner != null) {
            tag.putUUID(OWNER, owner);
        }
        ItemStack stack = new ItemStack(ModItems.CREATURE_EGG.get());
        stack.getOrCreateTag().merge(tag);
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static Optional<EntityType<?>> species(ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(data(stack).getString(SPECIES));
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static Genome genome(ItemStack stack) {
        return GenomeNbt.read(data(stack).getCompound(GENOME));
    }

    @Nullable
    public static UUID owner(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return species(stack)
                .map(type -> (Component) Component.translatable("item.iceagesurvival.creature_egg.of", type.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (species(stack).isEmpty()) {
            return;
        }
        Genome genome = genome(stack);
        tooltip.add(Component.translatable("iceagesurvival.egg.level", genome.level()).withStyle(ChatFormatting.GRAY));
        if (genome.totalMutations() > 0) {
            tooltip.add(Component.translatable("iceagesurvival.egg.mutations", genome.totalMutations())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("iceagesurvival.egg.incubate").withStyle(ChatFormatting.DARK_GRAY));
    }
}
