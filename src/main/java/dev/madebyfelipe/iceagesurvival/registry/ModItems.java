package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.CreatureEggItem;
import dev.madebyfelipe.iceagesurvival.item.TranqArrowItem;
import dev.madebyfelipe.iceagesurvival.item.TranqGunItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> TEST_CREATURE_SPAWN_EGG = ITEMS.register(
            "test_creature_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TEST_CREATURE, 0xB8D8E8, 0x4A6572, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SMILODON_SPAWN_EGG = ITEMS.register(
            "smilodon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SMILODON, 0xC49654, 0xF0EAD6, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> MAMMOTH_SPAWN_EGG = ITEMS.register(
            "mammoth_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MAMMOTH, 0x60402A, 0xECE4CC, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> DIRE_WOLF_SPAWN_EGG = ITEMS.register(
            "dire_wolf_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DIRE_WOLF, 0x76767C, 0x4A4A52, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> TYRANNOSAURUS_SPAWN_EGG = ITEMS.register(
            "tyrannosaurus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TYRANNOSAURUS, 0x58683F, 0xB2AA80, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> VELOCIRAPTOR_SPAWN_EGG = ITEMS.register(
            "velociraptor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.VELOCIRAPTOR, 0x9A7B4F, 0x3E3226, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> UTAHRAPTOR_SPAWN_EGG = ITEMS.register(
            "utahraptor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.UTAHRAPTOR, 0x7A5A3A, 0xC9B38A, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SPINOSAURUS_SPAWN_EGG = ITEMS.register(
            "spinosaurus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SPINOSAURUS, 0x5B6B4E, 0xB04A2E, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ALLOSAURUS_SPAWN_EGG = ITEMS.register(
            "allosaurus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ALLOSAURUS, 0x8C3B2A, 0x2F2A26, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BRONTOSAURUS_SPAWN_EGG = ITEMS.register(
            "brontosaurus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BRONTOSAURUS, 0x6F7A6A, 0xA8A38D, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> STEGOSAURUS_SPAWN_EGG = ITEMS.register(
            "stegosaurus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STEGOSAURUS, 0x8A6A3F, 0xC9A55A, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> PTERANODON_SPAWN_EGG = ITEMS.register(
            "pteranodon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PTERANODON, 0x7E6958, 0xD5C8B0, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> DIREBEAR_SPAWN_EGG = ITEMS.register(
            "direbear_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DIREBEAR, 0x684428, 0xA67E52, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> GALLIMIMUS_SPAWN_EGG = ITEMS.register(
            "gallimimus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GALLIMIMUS, 0x9C7B4E, 0xE0C89A, new Item.Properties()));
    public static final RegistryObject<ForgeSpawnEggItem> TRICERATOPS_SPAWN_EGG = ITEMS.register(
            "triceratops_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TRICERATOPS, 0x6E5A3A, 0xB8432F, new Item.Properties()));
    public static final RegistryObject<ForgeSpawnEggItem> DODO_SPAWN_EGG = ITEMS.register(
            "dodo_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DODO, 0x8E8A7C, 0xD9C27A, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ELASMOTHERIUM_SPAWN_EGG = ITEMS.register(
            "elasmotherium_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ELASMOTHERIUM, 0x5E4632, 0xB89A74, new Item.Properties()));
    public static final RegistryObject<ForgeSpawnEggItem> KELENKEN_SPAWN_EGG = ITEMS.register(
            "kelenken_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KELENKEN, 0x4A3B2C, 0xD8C29A, new Item.Properties()));
    public static final RegistryObject<ForgeSpawnEggItem> ORNITHOLESTES_SPAWN_EGG = ITEMS.register(
            "ornitholestes_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ORNITHOLESTES, 0x5B6B3A, 0xC9B98A, new Item.Properties()));

    /** Pele grossa dos animais da era do gelo: vira a roupa que segura o frio. */
    public static final RegistryObject<Item> PELT = simpleItem("pelt");

    public static final RegistryObject<ArmorItem> FUR_HELMET = furArmor("fur_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<ArmorItem> FUR_CHESTPLATE = furArmor("fur_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<ArmorItem> FUR_LEGGINGS = furArmor("fur_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<ArmorItem> FUR_BOOTS = furArmor("fur_boots", ArmorItem.Type.BOOTS);

    /** Pena de dodô: a primeira roupa contra o frio, um degrau abaixo da pele. */
    public static final RegistryObject<Item> DODO_FEATHER = simpleItem("dodo_feather");

    public static final RegistryObject<ArmorItem> FEATHER_HELMET = featherArmor("feather_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<ArmorItem> FEATHER_CHESTPLATE = featherArmor("feather_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<ArmorItem> FEATHER_LEGGINGS = featherArmor("feather_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<ArmorItem> FEATHER_BOOTS = featherArmor("feather_boots", ArmorItem.Type.BOOTS);

    /** Carne de dodô: a comida fácil do começo, como o frango (sem o risco de intoxicação). */
    public static final RegistryObject<Item> DODO_MEAT = ITEMS.register("dodo_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(3).saturationMod(0.3F).meat().build())));
    public static final RegistryObject<Item> COOKED_DODO_MEAT = ITEMS.register("cooked_dodo_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationMod(0.7F).meat().build())));

    public static final RegistryObject<TranqArrowItem> TRANQ_ARROW =
            ITEMS.register("tranq_arrow", () -> new TranqArrowItem(new Item.Properties()));

    /** Dardo sedativo: pepita de ferro com narcótico. Munição do rifle e da besta de dardos. */
    public static final RegistryObject<Item> TRANQ_DART = simpleItem("tranq_dart");

    /** Rifle tranquilizante: 8× o torpor da flecha (200) a cada 2,5 s — 80/s, o maior de todos, acima do arco Força V (62,5/s). */
    public static final RegistryObject<TranqGunItem> TRANQ_RIFLE = ITEMS.register("tranq_rifle",
            () -> new TranqGunItem(new Item.Properties().durability(400), 8.0, 6.0F, 0.2F, 50,
                    stack -> stack.is(TRANQ_DART.get()), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST_FAR));

    /** Besta de dardos: 2× o torpor da flecha (50) a cada 2 s — 25/s, nunca acima do arco Força V; sem puxar e mais precisa. */
    public static final RegistryObject<TranqGunItem> TRANQ_CROSSBOW = ITEMS.register("tranq_crossbow",
            () -> new TranqGunItem(new Item.Properties().durability(326), 2.0, 4.0F, 0.6F, 40,
                    stack -> stack.is(TRANQ_DART.get()) || stack.is(TRANQ_ARROW.get()),
                    net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT));

    /** Fruta da árvore de fruta-negra; ingrediente do narcótico. */
    public static final RegistryObject<Item> BLACK_FRUIT = simpleItem("black_fruit");

    /** Dado a uma criatura inconsciente, aumenta o torpor sem causar dano. */
    public static final RegistryObject<Item> NARCOTIC = simpleItem("narcotic");

    public static final RegistryObject<BlockItem> BLACK_FRUIT_LEAVES =
            blockItem("black_fruit_leaves", ModBlocks.BLACK_FRUIT_LEAVES);

    public static final RegistryObject<BlockItem> INCUBATOR = blockItem("incubator", ModBlocks.INCUBATOR);
    public static final RegistryObject<BlockItem> CHEMISTRY_BENCH = blockItem("chemistry_bench", ModBlocks.CHEMISTRY_BENCH);
    public static final RegistryObject<BlockItem> PREP_STATION = blockItem("prep_station", ModBlocks.PREP_STATION);
    public static final RegistryObject<BlockItem> TYRANNOSAURUS_HEAD = ITEMS.register("tyrannosaurus_head",
            () -> new BlockItem(ModBlocks.TYRANNOSAURUS_HEAD.get(), new Item.Properties().stacksTo(16)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<BlockItem> SPINOSAURUS_HEAD = ITEMS.register("spinosaurus_head",
            () -> new BlockItem(ModBlocks.SPINOSAURUS_HEAD.get(), new Item.Properties().stacksTo(16)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<BlockItem> REVIVE_TABLE = blockItem("revive_table", ModBlocks.REVIVE_TABLE);

    /** Fica no corpo da criatura domesticada que morreu; revive-a na mesa de reviver com um diamante. */
    public static final RegistryObject<dev.madebyfelipe.iceagesurvival.item.ImplantItem> IMPLANT = ITEMS.register(
            "implant", () -> new dev.madebyfelipe.iceagesurvival.item.ImplantItem(new Item.Properties()));

    /** Charque: carne com açúcar na estação de preparação. Não estraga e sacia mais que a carne assada. */
    public static final RegistryObject<Item> JERKY = ITEMS.register("jerky", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(8).saturationMod(0.9F).meat().build())));

    /** Ovo fecundado: espécie, genoma e dono do filhote. */
    public static final RegistryObject<CreatureEggItem> CREATURE_EGG =
            ITEMS.register("creature_egg", () -> new CreatureEggItem(new Item.Properties()));

    /** Dado a uma criatura, tira torpor: acorda uma criatura sua derrubada. Feito na mesa química. */
    public static final RegistryObject<Item> STIMULANT = simpleItem("stimulant");

    /** Muda: plante perto da base para ter fruta-negra sem sair procurando a árvore. */
    public static final RegistryObject<BlockItem> BLACK_FRUIT_SAPLING =
            blockItem("black_fruit_sapling", ModBlocks.BLACK_FRUIT_SAPLING);

    private ModItems() {
    }

    /** Durabilidade de couro × 1,6: mais grossa, dura mais. */
    private static final int FUR_DURABILITY_MULTIPLIER = 8;

    private static RegistryObject<ArmorItem> furArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.FUR, type,
                new Item.Properties().durability(ModArmorMaterials.FUR.getDurabilityForType(type))));
    }

    private static RegistryObject<ArmorItem> featherArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.FEATHER, type,
                new Item.Properties().durability(ModArmorMaterials.FEATHER.getDurabilityForType(type))));
    }

    private static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<BlockItem> blockItem(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(TEST_CREATURE_SPAWN_EGG);
            event.accept(SMILODON_SPAWN_EGG);
            event.accept(MAMMOTH_SPAWN_EGG);
            event.accept(TYRANNOSAURUS_SPAWN_EGG);
            event.accept(VELOCIRAPTOR_SPAWN_EGG);
            event.accept(UTAHRAPTOR_SPAWN_EGG);
            event.accept(SPINOSAURUS_SPAWN_EGG);
            event.accept(ALLOSAURUS_SPAWN_EGG);
            event.accept(BRONTOSAURUS_SPAWN_EGG);
            event.accept(STEGOSAURUS_SPAWN_EGG);
            event.accept(PTERANODON_SPAWN_EGG);
            event.accept(DIREBEAR_SPAWN_EGG);
            event.accept(DODO_SPAWN_EGG);
            event.accept(GALLIMIMUS_SPAWN_EGG);
            event.accept(TRICERATOPS_SPAWN_EGG);
            event.accept(ELASMOTHERIUM_SPAWN_EGG);
            event.accept(KELENKEN_SPAWN_EGG);
            event.accept(ORNITHOLESTES_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TRANQ_ARROW);
            event.accept(TRANQ_DART);
            event.accept(TRANQ_CROSSBOW);
            event.accept(TRANQ_RIFLE);
            event.accept(FEATHER_HELMET);
            event.accept(FEATHER_CHESTPLATE);
            event.accept(FEATHER_LEGGINGS);
            event.accept(FEATHER_BOOTS);
            event.accept(FUR_HELMET);
            event.accept(FUR_CHESTPLATE);
            event.accept(FUR_LEGGINGS);
            event.accept(FUR_BOOTS);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(BLACK_FRUIT);
            event.accept(PELT);
            event.accept(DODO_FEATHER);
            event.accept(STIMULANT);
            event.accept(NARCOTIC);
        } else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(DODO_MEAT);
            event.accept(COOKED_DODO_MEAT);
            event.accept(JERKY);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(INCUBATOR);
            event.accept(CHEMISTRY_BENCH);
            event.accept(PREP_STATION);
            event.accept(REVIVE_TABLE);
            event.accept(TYRANNOSAURUS_HEAD);
            event.accept(SPINOSAURUS_HEAD);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(BLACK_FRUIT_LEAVES);
            event.accept(BLACK_FRUIT_SAPLING);
        }
    }
}
