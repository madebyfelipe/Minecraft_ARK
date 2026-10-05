package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.cryo.CryoStorage;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Cápsula criogênica (legado do MC-19). A criogenia agora é só pelo menu da tecla O ({@link CryoStorage}); o item
 * continua registrado para as cápsulas que já existem nos mundos não virarem ar com a criatura dentro. Sem receita,
 * fora da aba criativa e sem uso: ao entrar no mundo e enquanto estiver no inventário de um jogador, a cápsula cheia
 * vira uma entrada na criogenia desse jogador e some; a vazia só some.
 */
public class CryoCapsuleItem extends Item {
    private static final String TAG_CREATURE = "Creature";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_NAME = "Name";

    public CryoCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Se a cápsula guarda uma criatura. */
    public static boolean holdsCreature(ItemStack stack) {
        return stack.getItem() instanceof CryoCapsuleItem && stack.hasTag() && stack.getTag().contains(TAG_CREATURE)
                && stack.getTag().contains(TAG_TYPE);
    }

    /** Espécie guardada, para o nome traduzido. */
    public static Optional<ResourceLocation> creatureType(ItemStack stack) {
        return holdsCreature(stack) ? Optional.ofNullable(ResourceLocation.tryParse(stack.getTag().getString(TAG_TYPE)))
                : Optional.empty();
    }

    /** Uma cápsula cheia como era gravada antes da criogenia por menu; para testes e conversão. */
    public static ItemStack legacyCapsule(ResourceLocation type, CompoundTag frozen, int level, String name) {
        ItemStack stack = new ItemStack(dev.madebyfelipe.iceagesurvival.registry.ModItems.CRYO_CAPSULE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_CREATURE, frozen.copy());
        tag.putString(TAG_TYPE, type.toString());
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_NAME, name);
        return stack;
    }

    /**
     * Passa a cápsula para a criogenia do jogador: a cheia vira entrada, e o stack some (cheio ou vazio). Limpa o
     * stack depois de guardar, então a mesma cápsula não converte duas vezes. Se não há onde guardar (cliente), não
     * mexe em nada. Só no servidor.
     */
    public static boolean convert(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof CryoCapsuleItem) || stack.isEmpty() || player.level().isClientSide) {
            return false;
        }
        if (holdsCreature(stack)) {
            CompoundTag tag = stack.getTag();
            ResourceLocation type = ResourceLocation.tryParse(tag.getString(TAG_TYPE));
            if (type == null || !CryoStorage.adopt(player, type, tag.getCompound(TAG_CREATURE), tag.getInt(TAG_LEVEL),
                    tag.getString(TAG_NAME))) {
                return false;
            }
            player.displayClientMessage(Component.translatable("iceagesurvival.cryo.migrated", tag.getString(TAG_NAME)),
                    false);
        }
        stack.setTag(null);
        stack.setCount(0);
        return true;
    }

    /** Converte as cápsulas que estão em qualquer espaço do contêiner (inventário, baú do Ender). */
    public static void convertAll(Player player, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (convert(player, stack)) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Ao entrar no mundo: as cápsulas antigas do inventário e do baú do Ender vão para a criogenia. */
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            convertAll(player, player.getInventory());
            convertAll(player, player.getEnderChestInventory());
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            convert(player, stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (holdsCreature(stack)) {
            Component species = creatureType(stack)
                    .<Component>map(type -> Component.translatable("entity." + type.getNamespace() + "." + type.getPath()))
                    .orElse(Component.literal("?"));
            tooltip.add(Component.literal(stack.getTag().getString(TAG_NAME)).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.iceagesurvival.implant.creature", species,
                    stack.getTag().getInt(TAG_LEVEL)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.iceagesurvival.cryo_capsule.legacy")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holdsCreature(stack);
    }
}
