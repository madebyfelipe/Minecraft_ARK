package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class EcologyTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void everyLandCreatureHasASpecies(GameTestHelper helper) {
        for (var creature : ModEntities.LAND_CREATURES) {
            helper.assertTrue(Species.of(helper.getLevel().registryAccess(), creature.get()).isPresent(),
                    "sem espécie: " + creature.getId());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyLandCreatureSpawnsWithStats(GameTestHelper helper) {
        for (var creature : ModEntities.LAND_CREATURES) {
            LandCreature spawned = helper.spawnWithNoFreeWill(creature.get(), 1, 2, 1);
            helper.assertTrue(spawned.creatureLevel() >= 10, creature.getId() + " sem nível");
            helper.assertTrue(spawned.maxTorpor() > 0, creature.getId() + " sem torpor máximo");
            spawned.discard();
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void herdMembersDefendEachOther(GameTestHelper helper) {
        LandCreature first = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 1, 2, 1);
        LandCreature second = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 1, 2, 1);
        Pig attacker = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);

        first.alertHerd(attacker);

        helper.assertTrue(second.getTarget() == attacker, "o outro mamute não reagiu");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void groupDefenseDoesNotCrossSpeciesOrReachTamedOnes(GameTestHelper helper) {
        LandCreature mammoth = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 1, 2, 1);
        LandCreature tamedMammoth = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 1, 2, 1);
        tamedMammoth.tame(helper.makeMockSurvivalPlayer());
        LandCreature wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 1, 2, 1);
        Pig attacker = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);

        mammoth.alertHerd(attacker);

        helper.assertTrue(wolf.getTarget() == null, "espécie diferente entrou na defesa");
        helper.assertTrue(tamedMammoth.getTarget() == null, "mamute domesticado entrou na defesa da manada selvagem");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void solitarySpeciesDoNotCallForHelp(GameTestHelper helper) {
        LandCreature first = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        LandCreature second = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        first.alertHerd(helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1));
        helper.assertTrue(second.getTarget() == null, "Smilodon solitário pediu ajuda");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void behaviorDataMatchesTheDesign(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BehaviorProfile mammoth = Species.of(registries, ModEntities.MAMMOTH.get()).orElseThrow().behavior().orElseThrow();
        BehaviorProfile wolf = Species.of(registries, ModEntities.DIRE_WOLF.get()).orElseThrow().behavior().orElseThrow();
        helper.assertTrue(!mammoth.aggressive() && mammoth.herdRadius() > 0 && mammoth.groupDefense(),
                "mamute deveria ser pacífico, de manada e com defesa em grupo");
        helper.assertTrue(wolf.aggressive() && wolf.prey().isPresent() && wolf.herdRadius() > 0,
                "lobo deveria ser agressivo, caçador e de matilha");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void spawnRuleAcceptsOpenGround(GameTestHelper helper) {
        BlockPos onFloor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(SpawnPlacements.checkSpawnRules(ModEntities.MAMMOTH.get(), helper.getLevel(),
                MobSpawnType.NATURAL, onFloor, helper.getLevel().random), "spawn recusado em chão firme a céu aberto");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void spawnRuleRejectsCoveredGround(GameTestHelper helper) {
        // Um bloco sólido logo acima equivale a uma caverna.
        helper.setBlock(1, 3, 1, Blocks.STONE);
        BlockPos onFloor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(ModEntities.MAMMOTH.get(), helper.getLevel(),
                MobSpawnType.NATURAL, onFloor, helper.getLevel().random), "spawn aceito sob teto");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void coldBiomesListTheNewFauna(GameTestHelper helper) {
        assertSpawnsIn(helper, Biomes.SNOWY_PLAINS, ModEntities.MAMMOTH.get());
        assertSpawnsIn(helper, Biomes.SNOWY_PLAINS, ModEntities.DIRE_WOLF.get());
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.SMILODON.get());
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.DIRE_WOLF.get());
        assertSpawnsIn(helper, Biomes.SNOWY_PLAINS, ModEntities.TYRANNOSAURUS.get());
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.TYRANNOSAURUS.get());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tyrannosaurusIsASoloApexPredator(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Species rex = Species.of(registries, ModEntities.TYRANNOSAURUS.get()).orElseThrow();
        Species smilodon = Species.of(registries, ModEntities.SMILODON.get()).orElseThrow();
        BehaviorProfile behavior = rex.behavior().orElseThrow();
        helper.assertTrue(behavior.aggressive() && behavior.prey().isPresent(), "T-Rex deveria ser agressivo e caçador");
        helper.assertTrue(behavior.herdRadius() == 0 && !behavior.groupDefense(), "T-Rex deveria ser solitário");
        helper.assertTrue(behavior.territoryRadius() > smilodon.behavior().orElseThrow().territoryRadius(),
                "território do T-Rex deveria ser maior que o do Smilodon");

        LandCreature spawned = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        helper.assertTrue(spawned.getMaxHealth() > 200, "T-Rex com pouca vida: " + spawned.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void warmBiomesDoNotListTheNewFauna(GameTestHelper helper) {
        Biome desert = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
        for (var creature : ModEntities.LAND_CREATURES) {
            boolean listed = desert.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                    .anyMatch(spawner -> spawner.type == creature.get());
            helper.assertTrue(!listed, creature.getId() + " nasce no deserto");
        }
        helper.succeed();
    }


    // ---- Reposição de fauna ----

    @GameTest(template = EMPTY)
    public static void everySpawningSpeciesHasARepopulationBlock(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (var creature : ModEntities.LAND_CREATURES) {
            SpawnProfile spawn = Species.of(registries, creature.get()).orElseThrow().spawn()
                    .orElseThrow(() -> new AssertionError("sem bloco spawn: " + creature.getId()));
            helper.assertTrue(spawn.weight() > 0, creature.getId() + " com peso zero");
            helper.assertTrue(spawn.maxNearby() > 0, creature.getId() + " sem teto de densidade");
            helper.assertTrue(spawn.groupMax() >= spawn.groupMin(), creature.getId() + " com grupo invertido");
            helper.assertTrue(spawn.maxNearby() >= spawn.groupMax(),
                    creature.getId() + " nasce em grupo maior que o próprio teto");
        }
        // A criatura de teste não nasce sozinha, nem na geração nem na reposição.
        helper.assertTrue(Species.of(registries, ModEntities.TEST_CREATURE.get()).orElseThrow().spawn().isEmpty(),
                "criatura de teste virou fauna");
        helper.succeed();
    }

    /** Num lote próprio: o raio da contagem (128) pegaria as criaturas dos testes vizinhos. */
    @GameTest(template = "arena", batch = "population_cap")
    public static void populationCapCountsEverySpeciesBeyondTheSpawnRing(GameTestHelper helper) {
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        helper.assertTrue(WildSpawner.densityRadius() > ServerConfig.WILD_SPAWN_MAX_DISTANCE.get(),
                "o raio da contagem precisa passar de onde a reposição faz nascer");
        int before = WildSpawner.totalWildNearby(helper.getLevel(), player);
        helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 4, 1, 4);
        helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 12, 1, 12);
        LandCreature tamed = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 1, 8);
        tamed.tame(player);
        helper.assertTrue(WildSpawner.totalWildNearby(helper.getLevel(), player) == before + 2,
                "esperava contar os 2 selvagens (lobo e mamute; a domesticada não conta), antes "
                        + before + ", agora "
                        + WildSpawner.totalWildNearby(helper.getLevel(), player));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dangerZonesKeepPredatorsAwayFromSpawn(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        java.util.function.ToIntFunction<EntityType<?>> min = type ->
                Species.of(registries, type).orElseThrow().spawn().orElseThrow().minDistance();
        helper.assertTrue(min.applyAsInt(ModEntities.DIRE_WOLF.get()) == 0
                && min.applyAsInt(ModEntities.MAMMOTH.get()) == 0
                && min.applyAsInt(ModEntities.BRONTOSAURUS.get()) == 0, "comuns deveriam nascer já no spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.TYRANNOSAURUS.get()) >= 1500, "T-Rex perto demais do spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.ALLOSAURUS.get()) >= 1000, "Alossauro perto demais do spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.SMILODON.get()) > 0, "Smilodon no spawn");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationRespectsTheBiomeOfTheSpecies(GameTestHelper helper) {
        // O mundo do gametest é de bioma temperado: nenhuma espécie do mod nasce aqui.
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        List<WildSpawner.Report> reports = WildSpawner.survey(helper.getLevel(), player);
        helper.assertTrue(reports.isEmpty(), "reposição ofereceu " + reports.size() + " espécie(s) em bioma temperado");
        helper.assertTrue(WildSpawner.trySpawnAround(helper.getLevel(), player) == 0, "repôs fauna em bioma temperado");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationSpawnsOnOpenGround(GameTestHelper helper) {
        // Tag de bioma que contém o bioma do gametest, para exercitar o caminho de posição
        // sem depender de um bioma nevado, que o mundo plano do teste não tem.
        SpawnProfile anywhere = new SpawnProfile(BiomeTags.IS_OVERWORLD, 10, 1, 1, 4, 0);
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(floor.below(), Blocks.STONE.defaultBlockState());
        var type = ModEntities.DIRE_WOLF.get();

        helper.assertTrue(WildSpawner.canSpawnAt(helper.getLevel(), type, floor, anywhere),
                "posição de chão firme a céu aberto recusada pela reposição");
        helper.assertTrue(WildSpawner.spawnAt(helper.getLevel(), type, floor), "a reposição não fez nascer nada");

        LandCreature spawned = helper.getLevel().getEntitiesOfClass(LandCreature.class,
                new net.minecraft.world.phys.AABB(floor).inflate(2.0)).get(0);
        helper.assertTrue(spawned.creatureLevel() >= 10, "nasceu sem nível: " + spawned.creatureLevel());
        helper.assertTrue(!spawned.isTame(), "a reposição nasceu domesticada");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationRefusesTheWrongBiome(GameTestHelper helper) {
        SpawnProfile jungleOnly = new SpawnProfile(BiomeTags.IS_JUNGLE, 10, 1, 1, 4, 0);
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(!WildSpawner.canSpawnAt(helper.getLevel(), ModEntities.SMILODON.get(), floor, jungleOnly),
                "a reposição aceitou um bioma fora da tag da espécie");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationFindsTheSurfaceOfTheColumn(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos found = WildSpawner.surfacePos(helper.getLevel(), floor.getX(), floor.getZ(),
                ModEntities.SMILODON.get());
        helper.assertTrue(found != null, "não achou a superfície de uma coluna carregada");
        helper.assertTrue(found.getY() == floor.getY(), "superfície em " + found.getY() + ", chão em " + floor.getY());
        helper.succeed();
    }

    private static void assertSpawnsIn(GameTestHelper helper, ResourceKey<Biome> biomeKey, EntityType<?> type) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biomeKey);
        boolean listed = biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(spawner -> spawner.type == type);
        helper.assertTrue(listed, EntityType.getKey(type) + " não nasce em " + biomeKey.location());
    }
}
