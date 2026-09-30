package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
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
import net.minecraft.world.level.GameType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        tamedMammoth.tame(helper.makeMockPlayer(GameType.SURVIVAL));
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

    @GameTest(template = EMPTY, skyAccess = true)
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

    @GameTest(template = EMPTY)
    public static void repopulationRespectsTheBiomeOfTheSpecies(GameTestHelper helper) {
        // O mundo do gametest é de bioma temperado: nenhuma espécie do mod nasce aqui.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<WildSpawner.Report> reports = WildSpawner.survey(helper.getLevel(), player);
        helper.assertTrue(reports.isEmpty(), "reposição ofereceu " + reports.size() + " espécie(s) em bioma temperado");
        helper.assertTrue(WildSpawner.trySpawnAround(helper.getLevel(), player) == 0, "repôs fauna em bioma temperado");
        helper.succeed();
    }

    @GameTest(template = EMPTY, skyAccess = true)
    public static void repopulationSpawnsOnOpenGround(GameTestHelper helper) {
        // Tag de bioma que contém o bioma do gametest, para exercitar o caminho de posição
        // sem depender de um bioma nevado, que o mundo plano do teste não tem.
        SpawnProfile anywhere = new SpawnProfile(BiomeTags.IS_OVERWORLD, 10, 1, 1, 4);
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        var type = ModEntities.SMILODON.get();

        helper.assertTrue(WildSpawner.canSpawnAt(helper.getLevel(), type, floor, anywhere),
                "posição de chão firme a céu aberto recusada pela reposição");
        helper.assertTrue(WildSpawner.spawnAt(helper.getLevel(), type, floor), "a reposição não fez nascer nada");

        LandCreature spawned = helper.getLevel().getEntitiesOfClass(LandCreature.class,
                new net.minecraft.world.phys.AABB(floor).inflate(2.0)).getFirst();
        helper.assertTrue(spawned.creatureLevel() >= 10, "nasceu sem nível: " + spawned.creatureLevel());
        helper.assertTrue(!spawned.isTame(), "a reposição nasceu domesticada");
        helper.succeed();
    }

    @GameTest(template = EMPTY, skyAccess = true)
    public static void repopulationRefusesTheWrongBiome(GameTestHelper helper) {
        SpawnProfile jungleOnly = new SpawnProfile(BiomeTags.IS_JUNGLE, 10, 1, 1, 4);
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(!WildSpawner.canSpawnAt(helper.getLevel(), ModEntities.SMILODON.get(), floor, jungleOnly),
                "a reposição aceitou um bioma fora da tag da espécie");
        helper.succeed();
    }

    @GameTest(template = EMPTY, skyAccess = true)
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
