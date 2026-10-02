package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Implante: fica no corpo da criatura domesticada que morreu e guarda quem ela era — espécie, nível, pontos,
 * genes, nome e dono. Na mesa de reviver, com um diamante, ela volta ({@link #revive}).
 */
public class ImplantItem extends Item {
    private static final String TAG_CREATURE = "Creature";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_NAME = "Name";
    /** A criatura revive com esta fração da vida. */
    public static final float REVIVE_HEALTH = 0.25F;

    public ImplantItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** O implante desta criatura, já sem o inventário (que fica no corpo). */
    public static ItemStack of(PrehistoricCreature creature, Item item) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_CREATURE, creature.implantData());
        tag.putString(TAG_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()).toString());
        tag.putInt(TAG_LEVEL, creature.creatureLevel());
        tag.putString(TAG_NAME, creature.getName().getString());
        return stack;
    }

    /** Se este implante guarda uma criatura. */
    public static boolean holdsCreature(ItemStack stack) {
        return stack.getItem() instanceof ImplantItem && stack.hasTag() && stack.getTag().contains(TAG_CREATURE)
                && stack.getTag().contains(TAG_TYPE);
    }

    /** Traz a criatura de volta nesta posição, com {@link #REVIVE_HEALTH} da vida. */
    public static Optional<PrehistoricCreature> revive(ServerLevel level, ItemStack implant, Vec3 position) {
        if (!holdsCreature(implant)) {
            return Optional.empty();
        }
        CompoundTag tag = implant.getTag();
        Optional<EntityType<?>> type = EntityType.byString(tag.getString(TAG_TYPE));
        if (type.isEmpty() || !(type.get().create(level) instanceof PrehistoricCreature creature)) {
            return Optional.empty();
        }
        creature.load(tag.getCompound(TAG_CREATURE).copy());
        creature.moveTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
        creature.setHealth(creature.getMaxHealth() * REVIVE_HEALTH);
        level.addFreshEntity(creature);
        return Optional.of(creature);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!holdsCreature(stack)) {
            tooltip.add(Component.translatable("item.iceagesurvival.implant.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation type = ResourceLocation.tryParse(tag.getString(TAG_TYPE));
        Component species = type == null ? Component.literal("?")
                : Component.translatable("entity." + type.getNamespace() + "." + type.getPath());
        tooltip.add(Component.literal(tag.getString(TAG_NAME)).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.iceagesurvival.implant.creature", species, tag.getInt(TAG_LEVEL))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceagesurvival.implant.hint").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holdsCreature(stack);
    }
}
