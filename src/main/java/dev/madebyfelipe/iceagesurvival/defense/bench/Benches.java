package dev.madebyfelipe.iceagesurvival.defense.bench;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Bancadas de Construção e de Armeiro: blocos, menus e o tipo de receita de lista {@code iceagesurvival:bench}
 * (dados em {@code recipes/bench/<bancada>/}). Registros próprios, fora dos {@code Mod*}.
 */
public final class Benches {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IceAgeSurvival.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IceAgeSurvival.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, IceAgeSurvival.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, IceAgeSurvival.MODID);

    public static final RegistryObject<RecipeType<BenchRecipe>> BENCH_RECIPE = RECIPE_TYPES.register("bench",
            () -> RecipeType.simple(IceAgeSurvival.id("bench")));
    public static final RegistryObject<BenchRecipe.Serializer> BENCH_SERIALIZER = RECIPE_SERIALIZERS.register("bench",
            BenchRecipe.Serializer::new);

    public static final RegistryObject<BenchBlock> CONSTRUCTION_BENCH = BLOCKS.register("construction_bench",
            () -> new BenchBlock(BenchKind.CONSTRUCTION, BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));
    public static final RegistryObject<BenchBlock> ARMORY_BENCH = BLOCKS.register("armory_bench",
            () -> new BenchBlock(BenchKind.ARMORY, BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));

    public static final RegistryObject<BlockItem> CONSTRUCTION_BENCH_ITEM = ITEMS.register("construction_bench",
            () -> new BlockItem(CONSTRUCTION_BENCH.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> ARMORY_BENCH_ITEM = ITEMS.register("armory_bench",
            () -> new BlockItem(ARMORY_BENCH.get(), new Item.Properties()));

    /** Um tipo de menu por bancada: o cliente sabe qual lista mostrar sem dado extra no pacote de abertura. */
    public static final RegistryObject<MenuType<BenchMenu>> CONSTRUCTION_MENU = MENUS.register("construction_bench",
            () -> new MenuType<>((id, inventory) -> new BenchMenu(BenchKind.CONSTRUCTION, id, inventory,
                    ContainerLevelAccess.NULL), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<BenchMenu>> ARMORY_MENU = MENUS.register("armory_bench",
            () -> new MenuType<>((id, inventory) -> new BenchMenu(BenchKind.ARMORY, id, inventory,
                    ContainerLevelAccess.NULL), FeatureFlags.DEFAULT_FLAGS));

    private Benches() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(Benches::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CONSTRUCTION_BENCH_ITEM);
            event.accept(ARMORY_BENCH_ITEM);
        }
    }
}
