package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
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

    private static void assertSpawnsIn(GameTestHelper helper, ResourceKey<Biome> biomeKey, EntityType<?> type) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biomeKey);
        boolean listed = biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(spawner -> spawner.type == type);
        helper.assertTrue(listed, EntityType.getKey(type) + " não nasce em " + biomeKey.location());
    }
}
