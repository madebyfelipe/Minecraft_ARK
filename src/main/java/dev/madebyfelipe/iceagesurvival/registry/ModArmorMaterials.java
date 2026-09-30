package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, IceAgeSurvival.MODID);

    /**
     * Pele: defesa de couro, o que ela tem de melhor é o isolamento (data map
     * {@code iceagesurvival:insulation}), não a proteção contra golpes.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FUR = ARMOR_MATERIALS.register("fur", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.CHESTPLATE, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 2);
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.BODY, 3);
        return new ArmorMaterial(defense, 12, SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(ModItems.PELT.get()),
                List.of(new ArmorMaterial.Layer(IceAgeSurvival.id("fur"))), 0.0F, 0.0F);
    });

    private ModArmorMaterials() {
    }
}
