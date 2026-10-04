package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
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
 * Os postos militares e a base (D51, D52): postos espalhados pelo mundo, cada um com um
 * {@link MilitaryTerminalBlock terminal} que o Analisador lê para destravar o próximo registro, e a base militar, uma
 * por mundo, com terminais da série da base e a contenção do espécime ({@link ContainmentCoreBlock núcleo} e
 * {@link StasisGeneratorBlock geradores}). Registros próprios, fora dos {@code Mod*}.
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

    /** O emissor do campo de êxtase: indestrutível (só o console desliga o campo), sem drop, um pouco de luz. */
    public static final RegistryObject<StasisGeneratorBlock> STASIS_GENERATOR = BLOCKS.register("stasis_generator",
            () -> new StasisGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.BEDROCK).sound(SoundType.METAL)
                    .noLootTable().lightLevel(state -> 10)));
    public static final RegistryObject<BlockItem> STASIS_GENERATOR_ITEM = ITEMS.register("stasis_generator",
            () -> new BlockItem(STASIS_GENERATOR.get(), new Item.Properties()));
    /** O console de operação do campo: indestrutível, com o terminal que derruba o campo. */
    public static final RegistryObject<ContainmentConsoleBlock> CONTAINMENT_CONSOLE = BLOCKS.register(
            "containment_console", () -> new ContainmentConsoleBlock(BlockBehaviour.Properties.copy(Blocks.BEDROCK)
                    .sound(SoundType.METAL).noLootTable().noOcclusion().lightLevel(state -> 7)));
    public static final RegistryObject<BlockItem> CONTAINMENT_CONSOLE_ITEM = ITEMS.register("containment_console",
            () -> new BlockItem(CONTAINMENT_CONSOLE.get(), new Item.Properties()));
    /** O núcleo da contenção: indestrutível como a rocha-mãe. */
    public static final RegistryObject<ContainmentCoreBlock> CONTAINMENT_CORE = BLOCKS.register("containment_core",
            () -> new ContainmentCoreBlock(BlockBehaviour.Properties.copy(Blocks.BEDROCK).noLootTable()
                    .lightLevel(state -> 12)));
    public static final RegistryObject<BlockItem> CONTAINMENT_CORE_ITEM = ITEMS.register("containment_core",
            () -> new BlockItem(CONTAINMENT_CORE.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ContainmentCoreBlockEntity>> CONTAINMENT_CORE_ENTITY =
            BLOCK_ENTITIES.register("containment_core", () -> BlockEntityType.Builder.of(
                    ContainmentCoreBlockEntity::new, CONTAINMENT_CORE.get()).build(null));

    public static final RegistryObject<StructureType<OutpostStructure>> OUTPOST = STRUCTURE_TYPES.register(
            "military_outpost", () -> () -> OutpostStructure.CODEC);
    public static final RegistryObject<StructurePieceType> OUTPOST_PIECE = PIECE_TYPES.register("military_outpost",
            () -> (StructurePieceType.StructureTemplateType) OutpostPiece::new);

    /** As variantes em JSON ({@code data/iceagesurvival/worldgen/structure/}): a torre e o complexo. */
    public static final ResourceKey<Structure> OUTPOST_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, IceAgeSurvival.id("military_outpost"));
    public static final ResourceKey<Structure> OUTPOST_COMPLEX_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, IceAgeSurvival.id("military_outpost_complex"));
    /** A base militar, uma por mundo, com a contenção do espécime. */
    public static final ResourceKey<Structure> BASE_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, IceAgeSurvival.id("military_base"));

    private Outposts() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        modEventBus.addListener(Outposts::addToCreativeTabs);
        MinecraftForge.EVENT_BUS.addListener(Outposts::onContainedDeath);
    }

    /**
     * Morreu o que estava preso na contenção: quem estiver perto (a até 64 blocos) recupera o dossiê, uma vez. O aviso
     * vai como uma leitura de registro, e o Analisador abre nele.
     */
    public static void onContainedDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide
                || !entity.getPersistentData().getBoolean(ContainmentCoreBlockEntity.CONTAINED_TAG)) {
            return;
        }
        for (ServerPlayer player : entity.level().getEntitiesOfClass(ServerPlayer.class,
                entity.getBoundingBox().inflate(64.0))) {
            if (DinoFileData.records(player, Manual.DOSSIER) == 0) {
                int records = DinoFileData.unlockRecord(player, Manual.DOSSIER);
                ModPayloads.sendToPlayer(player, new TerminalReadPayload(Manual.DOSSIER, records, true));
            }
        }
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(MILITARY_TERMINAL_ITEM);
            event.accept(STASIS_GENERATOR_ITEM);
            event.accept(CONTAINMENT_CORE_ITEM);
            event.accept(CONTAINMENT_CONSOLE_ITEM);
        }
    }
}
