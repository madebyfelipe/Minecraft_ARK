package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Os postos militares em volta da base (D51): construções pequenas de concreto espalhadas pelo mundo, cada uma com um
 * {@link MilitaryTerminalBlock terminal} que o Analisador lê para destravar o próximo registro militar. Registros
 * próprios, fora dos {@code Mod*}.
 */
public final class Outposts {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IceAgeSurvival.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IceAgeSurvival.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, IceAgeSurvival.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, IceAgeSurvival.MODID);

    /** Decorativo e sem drop: quebrado, o terminal (e quem já o leu) se perde. */
    public static final RegistryObject<MilitaryTerminalBlock> MILITARY_TERMINAL = BLOCKS.register("military_terminal",
            () -> new MilitaryTerminalBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noLootTable()
                    .lightLevel(state -> 6)));
    public static final RegistryObject<BlockItem> MILITARY_TERMINAL_ITEM = ITEMS.register("military_terminal",
            () -> new BlockItem(MILITARY_TERMINAL.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MilitaryTerminalBlockEntity>> MILITARY_TERMINAL_ENTITY =
            BLOCK_ENTITIES.register("military_terminal", () -> BlockEntityType.Builder.of(
                    MilitaryTerminalBlockEntity::new, MILITARY_TERMINAL.get()).build(null));

    public static final RegistryObject<StructureType<OutpostStructure>> OUTPOST = STRUCTURE_TYPES.register(
            "military_outpost", () -> () -> OutpostStructure.CODEC);
    public static final RegistryObject<StructurePieceType> OUTPOST_PIECE = PIECE_TYPES.register("military_outpost",
            () -> (StructurePieceType.ContextlessType) OutpostPiece::new);

    /** A estrutura em JSON ({@code data/iceagesurvival/worldgen/structure/military_outpost.json}). */
    public static final ResourceKey<Structure> OUTPOST_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, IceAgeSurvival.id("military_outpost"));

    private Outposts() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        modEventBus.addListener(Outposts::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(MILITARY_TERMINAL_ITEM);
        }
    }
}
