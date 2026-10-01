package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModArmorMaterials {
    /**
     * Armor materials are ordinary objects rather than registry entries in Forge 1.20.1.
     * Keep the registration hook because the mod bootstrap is outside the porting scope.
     */
    public static final Registration ARMOR_MATERIALS = new Registration();

    public static final ArmorMaterial FUR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return durabilityFactor(type) * 8;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 1;
                case CHESTPLATE -> 3;
                case LEGGINGS -> 2;
                case BOOTS -> 1;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.PELT.get());
        }

        @Override
        public String getName() {
            return IceAgeSurvival.MODID + ":fur";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    private ModArmorMaterials() {
    }

    private static int durabilityFactor(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
    }

    public static final class Registration {
        private Registration() {
        }

        public void register(IEventBus eventBus) {
        }
    }
}
