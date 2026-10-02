package dev.madebyfelipe.iceagesurvival.compat;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.common.loot.IGlobalLootModifier;

/**
 * Global loot modifier que não faz nada ({@code iceagesurvival:none}). Serve para desligar por dados
 * os modifiers de outros mods: o arquivo deles é sobrescrito, no mesmo caminho, por
 * {@code {"type": "iceagesurvival:none"}}. A lista compartilhada
 * {@code forge:loot_modifiers/global_loot_modifiers.json} continua citando o id, mas ele passa a
 * devolver o loot intacto. Qualquer outro campo do JSON é ignorado.
 */
public record DisabledLootModifier() implements IGlobalLootModifier {
    public static final Codec<DisabledLootModifier> CODEC = Codec.unit(DisabledLootModifier::new);

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
