package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.TranqArrowItem;
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

    public static final DeferredItem<TranqArrowItem> TRANQ_ARROW =
            ITEMS.register("tranq_arrow", () -> new TranqArrowItem(new Item.Properties()));

    private ModItems() {
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(TEST_CREATURE_SPAWN_EGG);
            event.accept(SMILODON_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TRANQ_ARROW);
        }
    }
}
