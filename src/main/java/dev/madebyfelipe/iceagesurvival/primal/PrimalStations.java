package dev.madebyfelipe.iceagesurvival.primal;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Estações primitivas portadas do Primal Stage (nanokulon, MIT), reimplementadas para Forge: grelha,
 * forno de olaria, varal de secagem, tora de corte e bigorna de pedra. Sem tela: o item fica à vista
 * em cima e as receitas são por dados, um tipo por estação ({@code iceagesurvival:grill}, {@code kiln},
 * {@code drying}, {@code cutting}, {@code forging}). Registros próprios, fora dos {@code Mod*}.
 */
public final class PrimalStations {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IceAgeSurvival.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IceAgeSurvival.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, IceAgeSurvival.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, IceAgeSurvival.MODID);

    /** Tempo padrão (ticks) e golpes padrão quando o JSON da receita não diz; os do Primal Stage. */
    public static final int GRILL_TICKS = 600;
    public static final int KILN_TICKS = 1200;
    public static final int DRYING_TICKS = 600;
    public static final int CUTTING_HITS = 1;
    public static final int FORGING_HITS = 5;

    // Receitas

    public static final RegistryObject<RecipeType<TimedRecipe>> GRILL_RECIPE = RECIPE_TYPES.register("grill",
            () -> RecipeType.simple(IceAgeSurvival.id("grill")));
    public static final RegistryObject<RecipeType<TimedRecipe>> KILN_RECIPE = RECIPE_TYPES.register("kiln",
            () -> RecipeType.simple(IceAgeSurvival.id("kiln")));
    public static final RegistryObject<RecipeType<TimedRecipe>> DRYING_RECIPE = RECIPE_TYPES.register("drying",
            () -> RecipeType.simple(IceAgeSurvival.id("drying")));
    public static final RegistryObject<RecipeType<HitRecipe>> CUTTING_RECIPE = RECIPE_TYPES.register("cutting",
            () -> RecipeType.simple(IceAgeSurvival.id("cutting")));
    public static final RegistryObject<RecipeType<HitRecipe>> FORGING_RECIPE = RECIPE_TYPES.register("forging",
            () -> RecipeType.simple(IceAgeSurvival.id("forging")));

    public static final RegistryObject<TimedRecipe.Serializer> GRILL_SERIALIZER = RECIPE_SERIALIZERS.register("grill",
            () -> new TimedRecipe.Serializer(GRILL_RECIPE, GRILL_TICKS));
    public static final RegistryObject<TimedRecipe.Serializer> KILN_SERIALIZER = RECIPE_SERIALIZERS.register("kiln",
            () -> new TimedRecipe.Serializer(KILN_RECIPE, KILN_TICKS));
    public static final RegistryObject<TimedRecipe.Serializer> DRYING_SERIALIZER = RECIPE_SERIALIZERS.register("drying",
            () -> new TimedRecipe.Serializer(DRYING_RECIPE, DRYING_TICKS));
    public static final RegistryObject<HitRecipe.Serializer> CUTTING_SERIALIZER = RECIPE_SERIALIZERS.register("cutting",
            () -> new HitRecipe.Serializer(CUTTING_RECIPE, CUTTING_HITS));
    public static final RegistryObject<HitRecipe.Serializer> FORGING_SERIALIZER = RECIPE_SERIALIZERS.register("forging",
            () -> new HitRecipe.Serializer(FORGING_RECIPE, FORGING_HITS));

    // Blocos

    public static final RegistryObject<PrimitiveGrillBlock> PRIMITIVE_GRILL = BLOCKS.register("primitive_grill",
            () -> new PrimitiveGrillBlock(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE).strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> KILN_BRICKS = BLOCKS.register("kiln_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
    public static final RegistryObject<KilnBlock> KILN = BLOCKS.register("kiln",
            () -> new KilnBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS).noOcclusion()));
    public static final RegistryObject<CuttingLogBlock> CUTTING_LOG = BLOCKS.register("cutting_log",
            () -> new CuttingLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
                    // A cor de mapa do tronco lê o eixo, que a tora de corte não tem.
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD).noOcclusion()));
    public static final RegistryObject<StoneAnvilBlock> STONE_ANVIL = BLOCKS.register("stone_anvil",
            () -> new StoneAnvilBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2.0F).noOcclusion()));

    /** Madeiras do varal: a do Primal Stage mais cerejeira e bambu, que ele não tinha. */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");

    /** Varal de secagem por madeira, na ordem de {@link #WOODS}. */
    public static final Map<String, RegistryObject<DryingRackBlock>> DRYING_RACKS = new LinkedHashMap<>();

    static {
        for (String wood : WOODS) {
            Block planks = switch (wood) {
                case "crimson", "warped" -> Blocks.CRIMSON_PLANKS;
                case "bamboo" -> Blocks.BAMBOO_PLANKS;
                case "cherry" -> Blocks.CHERRY_PLANKS;
                default -> Blocks.OAK_PLANKS;
            };
            DRYING_RACKS.put(wood, BLOCKS.register(wood + "_drying_rack",
                    () -> new DryingRackBlock(BlockBehaviour.Properties.copy(planks).noOcclusion())));
        }
    }

    // Block entities

    public static final RegistryObject<BlockEntityType<PrimitiveGrillBlock.Station>> GRILL_ENTITY =
            BLOCK_ENTITIES.register("primitive_grill", () -> BlockEntityType.Builder
                    .of(PrimitiveGrillBlock.Station::new, PRIMITIVE_GRILL.get()).build(null));
    public static final RegistryObject<BlockEntityType<KilnBlock.Station>> KILN_ENTITY =
            BLOCK_ENTITIES.register("kiln", () -> BlockEntityType.Builder
                    .of(KilnBlock.Station::new, KILN.get()).build(null));
    public static final RegistryObject<BlockEntityType<DryingRackBlock.Station>> DRYING_RACK_ENTITY =
            BLOCK_ENTITIES.register("drying_rack", () -> BlockEntityType.Builder
                    .of(DryingRackBlock.Station::new, DRYING_RACKS.values().stream()
                            .map(RegistryObject::get).toArray(Block[]::new))
                    .build(null));
    public static final RegistryObject<BlockEntityType<CuttingLogBlock.Station>> CUTTING_LOG_ENTITY =
            BLOCK_ENTITIES.register("cutting_log", () -> BlockEntityType.Builder
                    .of(CuttingLogBlock.Station::new, CUTTING_LOG.get()).build(null));
    public static final RegistryObject<BlockEntityType<StoneAnvilBlock.Station>> STONE_ANVIL_ENTITY =
            BLOCK_ENTITIES.register("stone_anvil", () -> BlockEntityType.Builder
                    .of(StoneAnvilBlock.Station::new, STONE_ANVIL.get()).build(null));

    // Itens dos blocos, na ordem da aba do criativo

    private static final List<RegistryObject<BlockItem>> STATION_ITEMS = new java.util.ArrayList<>();
    public static final RegistryObject<BlockItem> KILN_BRICKS_ITEM = blockItem(KILN_BRICKS);

    static {
        STATION_ITEMS.add(blockItem(PRIMITIVE_GRILL));
        STATION_ITEMS.add(blockItem(KILN));
        for (RegistryObject<DryingRackBlock> rack : DRYING_RACKS.values()) {
            STATION_ITEMS.add(blockItem(rack));
        }
        STATION_ITEMS.add(blockItem(CUTTING_LOG));
        STATION_ITEMS.add(blockItem(STONE_ANVIL));
    }

    private PrimalStations() {
    }

    private static RegistryObject<BlockItem> blockItem(RegistryObject<? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(PrimalStations::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            STATION_ITEMS.forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(KILN_BRICKS_ITEM);
        }
    }
}
