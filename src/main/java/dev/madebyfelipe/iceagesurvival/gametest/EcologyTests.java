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
    public static void everyPredatorChoosesFightOrFlightBySize(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        // O Velociraptor fica de fora: pequeno, reage ao jogador como ameaça em vez de caçá-lo.
        for (var predator : List.of(ModEntities.DIRE_WOLF.get(), ModEntities.SMILODON.get(), ModEntities.DIREBEAR.get(),
                ModEntities.UTAHRAPTOR.get(), ModEntities.ALLOSAURUS.get(),
                ModEntities.SPINOSAURUS.get(), ModEntities.TYRANNOSAURUS.get())) {
            String name = EntityType.getKey(predator).toString();
            var wariness = Species.of(registries, predator).orElseThrow().behavior().orElseThrow().wariness()
                    .orElseThrow(() -> new AssertionError(name + " sem reação a outros carnívoros"));
            helper.assertTrue(!wariness.players(), name + " não deveria substituir sua agressão normal ao jogador");
            helper.assertTrue(wariness.chargeRadius() > 0 && wariness.fleeSpeed() > 0,
                    name + " precisa poder lutar ou fugir");
            helper.assertTrue(ModEntities.TYRANNOSAURUS.get().is(wariness.threats().orElseThrow()),
                    name + " não reconhece predadores como ameaça");
        }
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
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.SMILODON.get());
        assertSpawnsIn(helper, Biomes.SNOWY_PLAINS, ModEntities.TYRANNOSAURUS.get());
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.TYRANNOSAURUS.get());
        assertSpawnsIn(helper, Biomes.SNOWY_PLAINS, ModEntities.PTERANODON.get());
        assertSpawnsIn(helper, Biomes.TAIGA, ModEntities.PTERANODON.get());
        // Pteranodonte em qualquer bioma da superfície, não só nos frios.
        assertSpawnsIn(helper, Biomes.DESERT, ModEntities.PTERANODON.get());
        assertSpawnsIn(helper, Biomes.JUNGLE, ModEntities.PTERANODON.get());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void naturalSpawnWeightsAreReducedAndRareSpeciesRemain(GameTestHelper helper) {
        assertSpawnWeight(helper, Biomes.SNOWY_PLAINS, ModEntities.DODO.get(), 7);
        assertSpawnWeight(helper, Biomes.SNOWY_PLAINS, ModEntities.VELOCIRAPTOR.get(), 2);
        assertSpawnWeight(helper, Biomes.SNOWY_PLAINS, ModEntities.PTERANODON.get(), 4);
        assertSpawnWeight(helper, Biomes.TAIGA, ModEntities.SMILODON.get(), 2);
        // O Smilodon substitui o bando de Velociraptores na planície nevada do spawn.
        assertSpawnWeight(helper, Biomes.SNOWY_PLAINS, ModEntities.SMILODON.get(), 2);
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

    /**
     * Velociraptor próximo do real: do porte de um peru grande, sozinho ou em par, espreita bicho pequeno
     * e não caça gente — mas reage a quem chega perto (alerta, ameaça, recua ou morde encurralado).
     */
    @GameTest(template = EMPTY)
    public static void velociraptorIsASmallWaryHunter(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Species velociraptor = Species.of(registries, ModEntities.VELOCIRAPTOR.get()).orElseThrow();
        BehaviorProfile behavior = velociraptor.behavior().orElseThrow();
        var wariness = behavior.wariness().orElseThrow();
        helper.assertTrue(!behavior.aggressive() && !behavior.huntsPlayers(), "não caça gente");
        helper.assertTrue(wariness.players() && wariness.chargeRadius() > 0 && wariness.bluffChance() > 0,
                "reage ao jogador: ameaça e morde se encurralado");
        helper.assertTrue(!ModEntities.ELASMOTHERIUM.get().is(behavior.prey().orElseThrow())
                        && ModEntities.DODO.get().is(behavior.prey().orElseThrow()), "só bicho pequeno");
        helper.assertTrue(ModEntities.VELOCIRAPTOR.get().getHeight() <= 0.9F, "do porte de um peru grande");
        SpawnProfile spawn = velociraptor.spawn().orElseThrow();
        helper.assertTrue(spawn.groupMax() <= 2 && spawn.maxNearby() <= 4, "sozinho ou em par, sem superpopulação");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void normalWorldBiomesListTheFauna(GameTestHelper helper) {
        // As criaturas nascem em mundo normal; o preset Era do Gelo troca esses biomas por frios.
        assertSpawnsIn(helper, Biomes.PLAINS, ModEntities.DODO.get());
        assertSpawnsIn(helper, Biomes.PLAINS, ModEntities.BRONTOSAURUS.get());
        assertSpawnsIn(helper, Biomes.SAVANNA, ModEntities.TYRANNOSAURUS.get());
        assertSpawnsIn(helper, Biomes.SAVANNA, ModEntities.TRICERATOPS.get());
        assertSpawnsIn(helper, Biomes.FOREST, ModEntities.STEGOSAURUS.get());
        assertSpawnsIn(helper, Biomes.DESERT, ModEntities.GALLIMIMUS.get());
        assertSpawnsIn(helper, Biomes.RIVER, ModEntities.SPINOSAURUS.get());
        helper.succeed();
    }


    // ---- Reposição de fauna ----

    @GameTest(template = EMPTY)
    public static void everySpawningSpeciesHasARepopulationBlock(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (var creature : ModEntities.LAND_CREATURES) {
            if (creature.get().is(dev.madebyfelipe.iceagesurvival.registry.ModTags.DISABLED)) {
                continue;
            }
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
        helper.assertTrue(min.applyAsInt(ModEntities.MAMMOTH.get()) == 0
                && min.applyAsInt(ModEntities.BRONTOSAURUS.get()) == 0
                && min.applyAsInt(ModEntities.SMILODON.get()) == 0,
                "fauna comum e Smilodon solitário deveriam nascer já no spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.VELOCIRAPTOR.get()) >= 300,
                "raptores não podem nascer dentro dos 300 blocos do spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.PTERANODON.get()) == 0,
                "o Pteranodonte nasce em qualquer lugar, inclusive perto do spawn");
        helper.assertTrue(min.applyAsInt(ModEntities.TYRANNOSAURUS.get()) == 300,
                "o T-Rex natural deveria começar logo depois da zona inicial (o apex garantido fica dentro)");
        helper.assertTrue(min.applyAsInt(ModEntities.ALLOSAURUS.get()) == 400
                        && min.applyAsInt(ModEntities.SPINOSAURUS.get()) == 400,
                "os predadores grandes deveriam aparecer a partir de 400 blocos");
        helper.assertTrue(min.applyAsInt(ModEntities.UTAHRAPTOR.get()) == 350
                        && min.applyAsInt(ModEntities.DIREBEAR.get()) == 300,
                "os predadores médios deveriam aparecer mais perto do spawn");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationRespectsTheBiomeOfTheSpecies(GameTestHelper helper) {
        // Só as espécies cuja tag inclui o bioma do lugar entram no sorteio.
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        var biome = helper.getLevel().getBiome(player.blockPosition());
        List<WildSpawner.Report> reports = WildSpawner.survey(helper.getLevel(), player);
        helper.assertTrue(!reports.isEmpty(), "nenhuma espécie no bioma do mundo de teste");
        for (WildSpawner.Report report : reports) {
            helper.assertTrue(biome.is(report.profile().biomes()), report.type() + " oferecida fora do bioma");
        }
        helper.assertTrue(reports.stream().noneMatch(report -> report.type() == ModEntities.SPINOSAURUS.get())
                        || biome.is(net.minecraft.tags.BiomeTags.IS_RIVER) || biome.is(net.minecraft.tags.BiomeTags.IS_BEACH),
                "Espinossauro oferecido longe da água");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationSpawnsOnOpenGround(GameTestHelper helper) {
        // Tag de bioma que contém o bioma do gametest, para exercitar o caminho de posição
        // sem depender de um bioma nevado, que o mundo plano do teste não tem.
        SpawnProfile anywhere = new SpawnProfile(BiomeTags.IS_OVERWORLD, 10, 1, 1, 4, 0, 0, java.util.Optional.empty());
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

    /** À noite, sobre neve: a reposição falhava porque o Animal só nasce em grama ou na luz. */
    @GameTest(template = EMPTY)
    public static void repopulationWorksAtNightOnSnow(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(floor.below(), Blocks.SNOW_BLOCK.defaultBlockState());
        var wolf = ModEntities.DIRE_WOLF.get().create(helper.getLevel());
        wolf.moveTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        // Escuro de verdade não dá para garantir no gametest; o que importa é não depender da luz.
        helper.assertTrue(wolf.checkSpawnRules(helper.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL),
                "a regra de spawn ainda depende de grama ou luz");
        wolf.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void repopulationRefusesTheWrongBiome(GameTestHelper helper) {
        SpawnProfile jungleOnly = new SpawnProfile(BiomeTags.IS_JUNGLE, 10, 1, 1, 4, 0, 0, java.util.Optional.empty());
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

    private static void assertSpawnWeight(GameTestHelper helper, ResourceKey<Biome> biomeKey, EntityType<?> type,
                                          int expectedWeight) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biomeKey);
        var spawner = biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .filter(candidate -> candidate.type == type).findFirst()
                .orElseThrow(() -> new AssertionError(EntityType.getKey(type) + " não nasce em "
                        + biomeKey.location()));
        int actualWeight = spawner.getWeight().asInt();
        helper.assertTrue(actualWeight == expectedWeight, EntityType.getKey(type) + " peso "
                + actualWeight + ", esperado " + expectedWeight);
    }

    /**
     * O Estegossauro nasce desde o spawn e com peso de herbívoro comum: a 300+ blocos e com peso 6 ele
     * perdia todas as vagas do teto de população para dodôs, raptores e Pteranodontes e não aparecia.
     */
    @GameTest(template = EMPTY)
    public static void stegosaurusIsCommonFromTheSpawn(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        SpawnProfile stego = Species.of(registries, ModEntities.STEGOSAURUS.get()).orElseThrow().spawn().orElseThrow();
        SpawnProfile mammoth = Species.of(registries, ModEntities.MAMMOTH.get()).orElseThrow().spawn().orElseThrow();
        helper.assertTrue(stego.minDistance() == 0, "o Estegossauro deveria nascer já no spawn");
        helper.assertTrue(stego.weight() >= mammoth.weight(), "peso " + stego.weight() + " abaixo do mamute");
        assertSpawnWeight(helper, Biomes.SNOWY_PLAINS, ModEntities.STEGOSAURUS.get(), 4);
        helper.succeed();
    }

    /** O lobo-terrível está desligado: não nasce, nem na geração nem na reposição, e o selvagem que sobrou some. */
    @GameTest(template = EMPTY)
    public static void direWolfIsDisabled(GameTestHelper helper) {
        var wolfType = ModEntities.DIRE_WOLF.get();
        helper.assertTrue(wolfType.is(dev.madebyfelipe.iceagesurvival.registry.ModTags.DISABLED), "lobo-terrível na tag disabled");
        helper.assertTrue(Species.of(helper.getLevel().registryAccess(), wolfType).orElseThrow().spawn().isEmpty(),
                "sem bloco de reposição");
        for (var biome : List.of(Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.FOREST)) {
            Biome holder = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biome);
            helper.assertTrue(holder.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                    .noneMatch(spawner -> spawner.type == wolfType), "lobo-terrível na geração de " + biome.location());
        }
        LandCreature wild = (LandCreature) wolfType.create(helper.getLevel());
        wild.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        helper.getLevel().addFreshEntity(wild);
        LandCreature tame = helper.spawn(wolfType, 4, 2, 4);
        tame.tame(helper.makeMockPlayer());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(wild.isRemoved(), "o lobo-terrível selvagem deveria ter sumido");
            helper.assertTrue(!tame.isRemoved(), "o domesticado fica");
            helper.succeed();
        });
    }
}
