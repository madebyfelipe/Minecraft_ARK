package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.TranqArrowItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IceAgeSurvival.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> TEST_CREATURE_SPAWN_EGG = ITEMS.register(
            "test_creature_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TEST_CREATURE, 0xB8D8E8, 0x4A6572, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SMILODON_SPAWN_EGG = ITEMS.register(
            "smilodon_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SMILODON, 0xC49654, 0xF0EAD6, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MAMMOTH_SPAWN_EGG = ITEMS.register(
            "mammoth_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MAMMOTH, 0x60402A, 0xECE4CC, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> DIRE_WOLF_SPAWN_EGG = ITEMS.register(
            "dire_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DIRE_WOLF, 0x76767C, 0x4A4A52, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> TYRANNOSAURUS_SPAWN_EGG = ITEMS.register(
            "tyrannosaurus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TYRANNOSAURUS, 0x58683F, 0xB2AA80, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> VELOCIRAPTOR_SPAWN_EGG = ITEMS.register(
            "velociraptor_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VELOCIRAPTOR, 0x9A7B4F, 0x3E3226, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> UTAHRAPTOR_SPAWN_EGG = ITEMS.register(
            "utahraptor_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.UTAHRAPTOR, 0x7A5A3A, 0xC9B38A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SPINOSAURUS_SPAWN_EGG = ITEMS.register(
            "spinosaurus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SPINOSAURUS, 0x5B6B4E, 0xB04A2E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CARNOTAURUS_SPAWN_EGG = ITEMS.register(
            "carnotaurus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CARNOTAURUS, 0x8C3B2A, 0x2F2A26, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BRONTOSAURUS_SPAWN_EGG = ITEMS.register(
            "brontosaurus_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BRONTOSAURUS, 0x6F7A6A, 0xA8A38D, new Item.Properties()));

    /** Pele grossa dos animais da era do gelo: vira a roupa que segura o frio. */
    public static final DeferredItem<Item> PELT = ITEMS.registerSimpleItem("pelt");

    public static final DeferredItem<ArmorItem> FUR_HELMET = furArmor("fur_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> FUR_CHESTPLATE = furArmor("fur_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> FUR_LEGGINGS = furArmor("fur_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> FUR_BOOTS = furArmor("fur_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<TranqArrowItem> TRANQ_ARROW =
            ITEMS.register("tranq_arrow", () -> new TranqArrowItem(new Item.Properties()));

    /** Fruta da árvore de fruta-negra; ingrediente do narcótico. */
    public static final DeferredItem<Item> BLACK_FRUIT = ITEMS.registerSimpleItem("black_fruit");

    /** Dado a uma criatura inconsciente, aumenta o torpor sem causar dano. */
    public static final DeferredItem<Item> NARCOTIC = ITEMS.registerSimpleItem("narcotic");

    public static final DeferredItem<BlockItem> BLACK_FRUIT_LEAVES =
            ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_FRUIT_LEAVES);

    /** Muda: plante perto da base para ter fruta-negra sem sair procurando a árvore. */
    public static final DeferredItem<BlockItem> BLACK_FRUIT_SAPLING =
            ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_FRUIT_SAPLING);

    private ModItems() {
    }

    /** Durabilidade de couro × 1,6: mais grossa, dura mais. */
    private static final int FUR_DURABILITY_MULTIPLIER = 8;

    private static DeferredItem<ArmorItem> furArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.FUR, type,
                new Item.Properties().durability(type.getDurability(FUR_DURABILITY_MULTIPLIER))));
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(TEST_CREATURE_SPAWN_EGG);
            event.accept(SMILODON_SPAWN_EGG);
            event.accept(MAMMOTH_SPAWN_EGG);
            event.accept(DIRE_WOLF_SPAWN_EGG);
            event.accept(TYRANNOSAURUS_SPAWN_EGG);
            event.accept(VELOCIRAPTOR_SPAWN_EGG);
            event.accept(UTAHRAPTOR_SPAWN_EGG);
            event.accept(SPINOSAURUS_SPAWN_EGG);
            event.accept(CARNOTAURUS_SPAWN_EGG);
            event.accept(BRONTOSAURUS_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TRANQ_ARROW);
            event.accept(FUR_HELMET);
            event.accept(FUR_CHESTPLATE);
            event.accept(FUR_LEGGINGS);
            event.accept(FUR_BOOTS);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(BLACK_FRUIT);
            event.accept(PELT);
            event.accept(NARCOTIC);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(BLACK_FRUIT_LEAVES);
            event.accept(BLACK_FRUIT_SAPLING);
        }
    }
}
