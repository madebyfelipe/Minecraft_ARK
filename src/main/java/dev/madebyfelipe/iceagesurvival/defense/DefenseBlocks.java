package dev.madebyfelipe.iceagesurvival.defense;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Muros altos, portões e armadilhas: registros próprios (blocos, itens, block entities), fora dos {@code Mod*}.
 *
 * <p>Regras comuns: quem colocou é o dono (guardado na block entity); madeira cede só aos gigantes de
 * {@link #WALL_BREAKERS}, por golpes contados ({@link DefenseDamage}); pedra, nenhuma criatura quebra; e nenhuma
 * criatura quebra bloco de defesa esbarrando (nenhum está em {@code #iceagesurvival:plowable}).
 */
public final class DefenseBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IceAgeSurvival.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IceAgeSurvival.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IceAgeSurvival.MODID);

    /** Os gigantes que derrubam muro e portão de madeira: T-Rex, Espinossauro e Bronto. */
    public static final TagKey<EntityType<?>> WALL_BREAKERS =
            TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("wall_breakers"));

    /** Dano das armadilhas (espetos, espinhos, urso); tipo de dano por dados em {@code damage_type/defense_trap.json}. */
    public static final ResourceKey<DamageType> TRAP_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, IceAgeSurvival.id("defense_trap"));

    // Muros altos

    public static final RegistryObject<DefenseWallBlock> WOOD_WALL = BLOCKS.register("wood_wall",
            () -> new DefenseWallBlock(true, wood()));
    public static final RegistryObject<DefenseWallBlock> STONE_WALL = BLOCKS.register("stone_wall",
            () -> new DefenseWallBlock(false, stone()));

    // Portões: comum 1 × 2, grande 5 × 5

    public static final RegistryObject<DefenseGateBlock> WOOD_GATE = BLOCKS.register("wood_gate",
            () -> new DefenseGateBlock(true, 1, 2, wood()));
    public static final RegistryObject<DefenseGateBlock> STONE_GATE = BLOCKS.register("stone_gate",
            () -> new DefenseGateBlock(false, 1, 2, stone()));
    public static final RegistryObject<DefenseGateBlock> LARGE_WOOD_GATE = BLOCKS.register("large_wood_gate",
            () -> new DefenseGateBlock(true, 5, 5, wood()));
    public static final RegistryObject<DefenseGateBlock> LARGE_STONE_GATE = BLOCKS.register("large_stone_gate",
            () -> new DefenseGateBlock(false, 5, 5, stone()));

    // Armadilhas e cobertura

    public static final RegistryObject<SpikeTrapBlock> SPIKE_TRAP = BLOCKS.register("spike_trap",
            () -> new SpikeTrapBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.5F)
                    .noCollission().noOcclusion()));
    public static final RegistryObject<ThornPalisadeBlock> THORN_PALISADE = BLOCKS.register("thorn_palisade",
            () -> new ThornPalisadeBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.5F, 3.0F)
                    .noOcclusion()));
    public static final RegistryObject<BearTrapBlock> BEAR_TRAP = BLOCKS.register("bear_trap",
            () -> new BearTrapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noCollission().noOcclusion()));
    public static final RegistryObject<FoliageCoverBlock> FOLIAGE_COVER = BLOCKS.register("foliage_cover",
            () -> new FoliageCoverBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.3F)
                    .sound(SoundType.GRASS).ignitedByLava().dynamicShape()
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

    // Block entities

    /** Só o dono: portões (a parte mestra), espetos, espinhos e cobertura. */
    public static final RegistryObject<BlockEntityType<DefenseOwnerBlockEntity>> OWNER_ENTITY =
            BLOCK_ENTITIES.register("defense_owner", () -> BlockEntityType.Builder.of(DefenseOwnerBlockEntity::new,
                    WOOD_GATE.get(), STONE_GATE.get(), LARGE_WOOD_GATE.get(), LARGE_STONE_GATE.get(),
                    SPIKE_TRAP.get(), THORN_PALISADE.get(), FOLIAGE_COVER.get()).build(null));
    public static final RegistryObject<BlockEntityType<BearTrapBlockEntity>> BEAR_TRAP_ENTITY =
            BLOCK_ENTITIES.register("bear_trap", () -> BlockEntityType.Builder.of(BearTrapBlockEntity::new,
                    BEAR_TRAP.get()).build(null));

    // Itens, na ordem das abas do criativo

    private static final List<RegistryObject<BlockItem>> BUILDING_ITEMS = new ArrayList<>();
    private static final List<RegistryObject<BlockItem>> COMBAT_ITEMS = new ArrayList<>();

    static {
        for (RegistryObject<? extends Block> block : List.of(WOOD_WALL, STONE_WALL, WOOD_GATE, STONE_GATE,
                LARGE_WOOD_GATE, LARGE_STONE_GATE)) {
            BUILDING_ITEMS.add(blockItem(block));
        }
        for (RegistryObject<? extends Block> block : List.of(SPIKE_TRAP, THORN_PALISADE, BEAR_TRAP, FOLIAGE_COVER)) {
            COMBAT_ITEMS.add(blockItem(block));
        }
    }

    private DefenseBlocks() {
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F, 4.0F);
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(2.5F, 9.0F).requiresCorrectToolForDrops();
    }

    private static RegistryObject<BlockItem> blockItem(RegistryObject<? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(DefenseBlocks::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            BUILDING_ITEMS.forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            COMBAT_ITEMS.forEach(event::accept);
        }
    }

    /** Se o bloco é uma das defesas deste pacote. */
    public static boolean isDefense(BlockState state) {
        return state.getBlock() instanceof DefenseBlock;
    }

    /** O dano das armadilhas. */
    public static DamageSource trapDamage(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TRAP_DAMAGE));
    }

    /**
     * A mordida montada ({@link PrehistoricCreature#attackAsMount}) pegou um bloco de defesa: nunca o destrói
     * direto; se a montaria é um gigante ({@link #WALL_BREAKERS}) e o bloco é de madeira, conta um golpe nele.
     * Qualquer outra criatura não faz nada. Respeita a proteção do spawn como a mordida faz com o resto.
     */
    public static void biteHit(PrehistoricCreature mount, ServerPlayer rider, BlockPos pos, BlockState state) {
        if (mount.level().mayInteract(rider, pos)) {
            DefenseDamage.hit(mount, pos, state);
        }
    }
}
