package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.CaveTrackerItem;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModStructures;
import dev.madebyfelipe.iceagesurvival.world.cave.ArenaCaveLayout;
import dev.madebyfelipe.iceagesurvival.world.cave.ArenaCaveStructure;
import dev.madebyfelipe.iceagesurvival.world.cave.CavePieces;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Etapa 10 (D47): a caverna da arena e o rastreador. O mundo do GameTest é plano e não tem a nossa estrutura, então a
 * busca do rastreador num mundo de verdade é conferida pelos pontos do anel e pela geração da estrutura sobre o
 * gerador do overworld normal (e do preset Era do Gelo), sem carregar chunks.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CaveTests {
    private static final String EMPTY = "empty";
    private static final int MIN_DISTANCE_FROM_SPAWN = 1500;
    private static final long[] SEEDS = {0L, 1L, 42L, 20261003L, -7_331_552_101L};

    @GameTest(template = EMPTY)
    public static void arenaCaveIsRegisteredWithConcentricRings(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).get(ModStructures.ARENA_CAVE_STRUCTURE);
        helper.assertTrue(structure != null, "estrutura iceagesurvival:arena_cave não carregou");
        helper.assertTrue(structure.type() == ModStructures.ARENA_CAVE.get(), "tipo da estrutura errado");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE)
                        .getHolderOrThrow(ModStructures.ARENA_CAVE_STRUCTURE).is(ModStructures.ARENA_CAVES),
                "a tag #iceagesurvival:arena_cave (a que o rastreador procura) não tem a caverna");
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(ModStructures.ARENA_CAVE_SET);
        helper.assertTrue(set != null, "structure_set iceagesurvival:arena_caves não carregou");
        helper.assertTrue(set.placement() instanceof ConcentricRingsStructurePlacement,
                "a caverna precisa de anéis concêntricos, como as fortalezas");
        ConcentricRingsStructurePlacement rings = (ConcentricRingsStructurePlacement) set.placement();
        // Pior caso do anel interno: 4·d − 1,25·d chunks (sorteio do raio), menos o desvio de bioma (112 blocos).
        double nearestPossible = rings.distance() * 2.75 * 16 - 112;
        helper.assertTrue(nearestPossible >= MIN_DISTANCE_FROM_SPAWN,
                "o anel pode cair a " + (int) nearestPossible + " blocos do spawn");
        helper.assertTrue(rings.count() >= 3 && rings.count() <= 6, "cavernas por mundo: " + rings.count());
        helper.succeed();
    }

    /** Os pontos reais do anel, no overworld normal e no preset Era do Gelo, para algumas sementes. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void everyCaveIsAtLeast1500BlocksFromSpawn(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ConcentricRingsStructurePlacement rings = (ConcentricRingsStructurePlacement) registries
                .registryOrThrow(Registries.STRUCTURE_SET).get(ModStructures.ARENA_CAVE_SET).placement();
        for (ResourceKey<WorldPreset> preset : List.of(WorldPresets.NORMAL,
                ResourceKey.create(Registries.WORLD_PRESET, IceAgeSurvival.id("ice_age")))) {
            ChunkGenerator generator = overworld(registries, preset);
            for (long seed : SEEDS) {
                List<ChunkPos> positions = structureState(registries, generator, seed).getRingPositionsFor(rings);
                helper.assertTrue(positions != null && positions.size() == rings.count(),
                        preset.location() + ": anel sem as " + rings.count() + " cavernas (semente " + seed + ")");
                for (ChunkPos chunk : positions) {
                    double distance = Math.sqrt((double) chunk.getMiddleBlockX() * chunk.getMiddleBlockX()
                            + (double) chunk.getMiddleBlockZ() * chunk.getMiddleBlockZ());
                    helper.assertTrue(distance >= MIN_DISTANCE_FROM_SPAWN, preset.location() + ": caverna a "
                            + (int) distance + " blocos do spawn (semente " + seed + ")");
                }
            }
        }
        helper.succeed();
    }

    /**
     * A estrutura gerada de verdade sobre o relevo do overworld normal, nos pontos do anel: pelo menos uma caverna por
     * mundo, entrada em terra seca acima do mar, arena funda com o altar no centro e o resto ao alcance das referências
     * de estrutura (8 chunks).
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void caveGeneratesOnRealTerrain(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).get(ModStructures.ARENA_CAVE_STRUCTURE);
        ConcentricRingsStructurePlacement rings = (ConcentricRingsStructurePlacement) registries
                .registryOrThrow(Registries.STRUCTURE_SET).get(ModStructures.ARENA_CAVE_SET).placement();
        for (ResourceKey<WorldPreset> preset : List.of(WorldPresets.NORMAL,
                ResourceKey.create(Registries.WORLD_PRESET, IceAgeSurvival.id("ice_age")))) {
            ChunkGenerator generator = overworld(registries, preset);
            LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
            for (long seed : new long[] {SEEDS[0], SEEDS[2]}) {
                RandomState randomState = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD,
                        seed);
                int valid = 0;
                for (ChunkPos chunk : structureState(registries, generator, seed).getRingPositionsFor(rings)) {
                    StructureStart start = structure.generate(registries, generator, generator.getBiomeSource(),
                            randomState, helper.getLevel().getStructureManager(), seed, chunk, 0, height,
                            biome -> structure.biomes().contains(biome));
                    if (!start.isValid()) {
                        continue;
                    }
                    valid++;
                    String where = preset.location() + " semente " + seed + " chunk " + chunk;
                    CavePieces.Entrance entrance = find(start.getPieces(), CavePieces.Entrance.class);
                    CavePieces.Arena arena = find(start.getPieces(), CavePieces.Arena.class);
                    helper.assertTrue(entrance != null && arena != null, where + ": faltou a entrada ou a arena");
                    BlockPos mouth = entrance.mouth();
                    helper.assertTrue(mouth.equals(ArenaCaveStructure.entranceAt(generator, height, randomState, chunk)),
                            where + ": o rastreador aponta para outro lugar que não a boca " + mouth);
                    int surface = generator.getFirstOccupiedHeight(mouth.getX(), mouth.getZ(),
                            Heightmap.Types.WORLD_SURFACE_WG, height, randomState);
                    int floor = generator.getFirstOccupiedHeight(mouth.getX(), mouth.getZ(),
                            Heightmap.Types.OCEAN_FLOOR_WG, height, randomState);
                    helper.assertTrue(surface == floor && floor > generator.getSeaLevel(),
                            where + ": entrada na água ou abaixo do mar (" + floor + "/" + surface + ")");
                    helper.assertTrue(arena.floorY() <= ArenaCaveLayout.DEFAULT_ARENA_FLOOR,
                            where + ": arena rasa em y " + arena.floorY());
                    helper.assertTrue(start.getPieces().get(start.getPieces().size() - 1) == arena,
                            where + ": a arena precisa ser a última peça");
                    assertWithinReferenceRange(helper, start.getPieces(), chunk, where);
                }
                helper.assertTrue(valid > 0, preset.location() + " semente " + seed + ": nenhuma caverna gerou");
            }
        }
        helper.succeed();
    }

    /** O traçado com relevo plano: túnel contínuo da boca até a porta, nada atravessando a arena por cima. */
    @GameTest(template = EMPTY)
    public static void layoutConnectsTheMouthToTheArenaDoor(GameTestHelper helper) {
        for (int seed = 0; seed < 40; seed++) {
            RandomSource random = RandomSource.create(seed);
            BlockPos entrance = new BlockPos(8 + (seed % 7) * 8 - 24, 90, 8 - (seed % 5) * 10);
            ArenaCaveLayout.Plan plan = ArenaCaveLayout.plan(random, 8, 8, entrance,
                    ArenaCaveLayout.arenaFloorFor(90, -64), (x, z) -> 90);
            String where = "semente " + seed;
            List<CavePieces.Tunnel> tunnels = plan.tunnels();
            helper.assertTrue(tunnels.get(0).from().distanceTo(Vec3.atCenterOf(entrance.above())) < 0.01,
                    where + ": o primeiro túnel não sai da boca");
            for (int i = 1; i < tunnels.size(); i++) {
                helper.assertTrue(tunnels.get(i - 1).to().distanceTo(tunnels.get(i).from()) < 0.01,
                        where + ": túnel " + i + " desconectado");
            }
            CavePieces.Arena arena = plan.arena();
            int roofTop = arena.floorY() + CavePieces.Arena.HEIGHT + CavePieces.Arena.SHELL;
            for (CavePieces.Tunnel tunnel : tunnels) {
                if (plan.finalDescent().contains(tunnel)) {
                    continue;
                }
                double bottom = Math.min(tunnel.from().y, tunnel.to().y) - tunnel.radius();
                helper.assertTrue(bottom > roofTop + 1, where + ": túnel desce até y " + bottom + " antes da porta");
            }
            for (StructurePiece piece : plan.pieces()) {
                if (piece instanceof CavePieces.Room room) {
                    helper.assertTrue(room.floorY() - 1 > roofTop + 1, where + ": sala encosta no teto da arena");
                }
            }
            // A descida final chega à porta pelo lado de fora da parede.
            Direction door = plan.door();
            CavePieces.Tunnel last = plan.finalDescent().get(plan.finalDescent().size() - 1);
            double out = (last.to().x - arena.altarPos().getX() - 0.5) * door.getStepX()
                    + (last.to().z - arena.altarPos().getZ() - 0.5) * door.getStepZ();
            helper.assertTrue(out - last.radius() >= CavePieces.Arena.HALF + CavePieces.Arena.SHELL - 0.5,
                    where + ": o fim do túnel fura a parede da arena");
            BlockPos end = BlockPos.containing(last.to());
            helper.assertTrue(arena.isDoorway(end.getX() - door.getStepX() * 2, end.getY(), end.getZ()
                    - door.getStepZ() * 2), where + ": o túnel não encontra o corredor da porta");
            helper.assertTrue(last.to().y - last.radius() < arena.floorY() + 1.5,
                    where + ": o túnel chega acima do chão da arena");
            assertWithinReferenceRange(helper, plan.pieces(), new ChunkPos(0, 0), where);
            helper.assertTrue(plan.pieces().get(plan.pieces().size() - 1) == arena, where + ": arena não é a última");
        }
        helper.succeed();
    }

    /** A arena escavada num nível de verdade: altar no centro com ar em volta, chão e paredes sólidos, porta aberta. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void arenaPieceBuildsAClosedArenaAroundTheAltar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO);
        int floor = 200;
        CavePieces.Arena arena = new CavePieces.Arena(base.getX() + 40, floor, base.getZ() + 40, Direction.EAST);
        BoundingBox box = arena.getBoundingBox();
        arena.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box,
                new ChunkPos(arena.altarPos()), arena.altarPos());
        try {
            BlockPos altar = arena.altarPos();
            helper.assertTrue(level.getBlockState(altar).is(ModBlocks.ARENA_ALTAR.get()), "sem altar no centro");
            helper.assertTrue(level.getBlockState(altar.below()).isSolidRender(level, altar.below()),
                    "chão sob o altar não é sólido");
            int half = CavePieces.Arena.HALF;
            for (int dx = -half + 3; dx < half - 3; dx += 4) {
                for (int dz = -half + 3; dz < half - 3; dz += 4) {
                    BlockPos column = new BlockPos(altar.getX() + dx, floor, altar.getZ() + dz);
                    helper.assertTrue(level.getBlockState(column).isSolidRender(level, column),
                            "buraco no chão em " + column);
                    for (int dy = 1; dy < CavePieces.Arena.HEIGHT; dy++) {
                        if (dx == 0 && dz == 0 && dy == 1) {
                            continue;
                        }
                        helper.assertTrue(level.getBlockState(column.above(dy)).isAir(),
                                "bloco no meio da arena em " + column.above(dy));
                    }
                    BlockPos ceiling = column.above(CavePieces.Arena.HEIGHT + 1);
                    helper.assertTrue(level.getBlockState(ceiling).isSolidRender(level, ceiling), "teto aberto em " + ceiling);
                }
            }
            // Paredes fechadas, menos a porta a leste.
            BlockPos west = new BlockPos(altar.getX() - half - 1, floor + 2, altar.getZ());
            BlockPos north = new BlockPos(altar.getX(), floor + 2, altar.getZ() - half - 1);
            helper.assertTrue(level.getBlockState(west).isSolidRender(level, west), "parede oeste aberta");
            helper.assertTrue(level.getBlockState(north).isSolidRender(level, north), "parede norte aberta");
            for (int out = half; out < half + CavePieces.Arena.SHELL + CavePieces.Arena.DOOR_REACH; out++) {
                for (int dy = 1; dy <= CavePieces.Arena.DOOR_HEIGHT; dy++) {
                    helper.assertTrue(level.getBlockState(new BlockPos(altar.getX() + out, floor + dy, altar.getZ()))
                            .isAir(), "porta fechada a " + out + " blocos do centro");
                }
            }
            BlockPos lintel = new BlockPos(altar.getX() + half + 1, floor + CavePieces.Arena.DOOR_HEIGHT + 1, altar.getZ());
            BlockPos jamb = new BlockPos(altar.getX() + half + 1, floor + 1, altar.getZ() + 2);
            helper.assertTrue(level.getBlockState(lintel).isSolidRender(level, lintel)
                    && level.getBlockState(jamb).isSolidRender(level, jamb), "a porta é maior que 3×4");
        } finally {
            BlockPos.betweenClosedStream(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())
                    .forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void trackerRecipeUsesTheApexTrophies(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(IceAgeSurvival.id("cave_tracker"));
        helper.assertTrue(recipe.isPresent(), "receita iceagesurvival:cave_tracker não carregou");
        Recipe<?> tracker = recipe.get();
        ItemStack result = tracker.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModItems.CAVE_TRACKER.get()) && result.getCount() == 1,
                "a receita devia dar 1 rastreador: " + result);
        List<Ingredient> ingredients = tracker.getIngredients();
        helper.assertTrue(ingredients.size() == 3, "ingredientes: " + ingredients.size());
        for (ItemStack needed : List.of(new ItemStack(ModItems.TYRANNOSAURUS_HEAD.get()),
                new ItemStack(ModItems.SPINOSAURUS_HEAD.get()), new ItemStack(Items.COMPASS))) {
            helper.assertTrue(ingredients.stream().anyMatch(ingredient -> ingredient.test(needed)),
                    "falta " + needed.getItem() + " na receita");
        }
        helper.succeed();
    }

    /** O rastreador é um aparelho de mão: usar não o gasta nem lança nada, e ele vale em qualquer das mãos. */
    @GameTest(template = EMPTY)
    public static void trackerIsAHandheldDeviceThatIsNotSpent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        ItemStack tracker = new ItemStack(ModItems.CAVE_TRACKER.get());
        helper.assertTrue(tracker.getMaxStackSize() == 1, "o rastreador devia ser 1 por espaço");
        helper.assertFalse(CaveTrackerItem.isHolding(player), "segurando sem nada na mão");
        player.setItemInHand(InteractionHand.OFF_HAND, tracker);
        helper.assertTrue(CaveTrackerItem.isHolding(player), "na mão secundária também vale");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, tracker);
        helper.assertTrue(CaveTrackerItem.isHolding(player), "na mão principal não contou");
        tracker.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ModItems.CAVE_TRACKER.get())
                && player.getMainHandItem().getCount() == 1, "usar gastou o rastreador");
        helper.assertTrue(level.getEntitiesOfClass(EyeOfEnder.class, new AABB(at).inflate(8)).isEmpty(),
                "o rastreador não é mais jogado como o Olho do Ender");
        helper.succeed();
    }

    /**
     * A busca do rastreador no mundo de verdade das GameTests: responde sem travar e, se acha uma boca, ela está no
     * anel, longe do spawn (o ponto do anel a 1500+, a boca a até 3 chunks dele).
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void trackerSearchAnswersInTheRealWorld(GameTestHelper helper) {
        BlockPos mouth = CaveTrackerItem.nearestMouth(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        if (mouth != null) {
            double fromSpawn = Math.sqrt((double) mouth.getX() * mouth.getX() + (double) mouth.getZ() * mouth.getZ());
            helper.assertTrue(fromSpawn >= MIN_DISTANCE_FROM_SPAWN - 16 * 4,
                    "boca da caverna a " + (int) fromSpawn + " blocos do spawn: " + mouth);
        }
        helper.succeed();
    }

    private static <T> T find(List<StructurePiece> pieces, Class<T> type) {
        return pieces.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    /** As referências de estrutura só olham 8 chunks em volta do início; as peças precisam caber nisso. */
    private static void assertWithinReferenceRange(GameTestHelper helper, List<StructurePiece> pieces, ChunkPos start,
            String where) {
        int limit = 8 * 16 - 8;
        for (StructurePiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            int reach = Math.max(Math.max(Math.abs(box.minX() - start.getMiddleBlockX()),
                            Math.abs(box.maxX() - start.getMiddleBlockX())),
                    Math.max(Math.abs(box.minZ() - start.getMiddleBlockZ()), Math.abs(box.maxZ() - start.getMiddleBlockZ())));
            helper.assertTrue(reach <= limit, where + ": peça " + piece.getClass().getSimpleName() + " a " + reach
                    + " blocos do início");
        }
    }

    private static ChunkGenerator overworld(RegistryAccess registries, ResourceKey<WorldPreset> key) {
        WorldPreset preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(key);
        return preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD).generator();
    }

    private static ChunkGeneratorStructureState structureState(RegistryAccess registries, ChunkGenerator generator,
            long seed) {
        RandomState randomState = RandomState.create(registries.asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(randomState, seed,
                generator.getBiomeSource(), registries.lookupOrThrow(Registries.STRUCTURE_SET));
        state.ensureStructuresGenerated();
        return state;
    }
}
