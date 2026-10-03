package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.spawn.WildSpawnRules;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Toda espécie que nasce precisa ter onde nascer nos dois modos de mundo, e os carnívoros precisam de vaga.
 *
 * <p>Convenção de spawn (decisão do Felipe, 2026-10-02): toda espécie nasce em qualquer bioma do Overworld com o
 * peso base, e com o triplo no bioma ideal (o habitat real, mais a planície nevada, para o preset Era do Gelo
 * manter a proporção entre as espécies). Os testes abaixo valem para toda espécie com bloco {@code spawn}, inclusive
 * as que vierem depois; só o que está em {@code #iceagesurvival:disabled} fica de fora.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class SpawnCoverageTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "spawn_coverage";

    /** Biomas fora do habitat de quase todas: o teste usa o primeiro que não for ideal da espécie. */
    private static final List<ResourceKey<Biome>> AWAY_FROM_HOME = List.of(
            Biomes.JUNGLE, Biomes.DESERT, Biomes.MUSHROOM_FIELDS, Biomes.DARK_FOREST, Biomes.FROZEN_PEAKS);

    /**
     * No preset Era do Gelo, todo bioma de terra vira planície nevada: a espécie que não a tem na tag de
     * spawn nunca nasce lá (era o caso do Utahraptor e do Urso-terrível).
     */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void everySpeciesCanSpawnInTheIceAge(GameTestHelper helper) {
        var snowyPlains = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.SNOWY_PLAINS);
        List<String> missing = new ArrayList<>();
        for (var holder : ModEntities.LAND_CREATURES) {
            var type = holder.get();
            if (type.is(ModTags.DISABLED)) {
                continue;
            }
            Species.of(helper.getLevel().registryAccess(), type).flatMap(Species::spawn)
                    .filter(spawn -> !snowyPlains.is(spawn.biomes()))
                    .ifPresent(spawn -> missing.add(holder.getId().getPath()));
        }
        helper.assertTrue(missing.isEmpty(), "não nascem na planície nevada (o mundo Era do Gelo): " + missing);
        helper.succeed();
    }

    /** O Smilodon e o Utahraptor são carnívoros: entram na quarta parte do teto que é deles. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void predatorsCountAsCarnivoresForTheReservedCap(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(WildSpawner.isCarnivore(level, ModEntities.SMILODON.get()), "Smilodon deveria ser carnívoro");
        helper.assertTrue(WildSpawner.isCarnivore(level, ModEntities.UTAHRAPTOR.get()), "Utahraptor deveria ser carnívoro");
        helper.assertFalse(WildSpawner.isCarnivore(level, ModEntities.DODO.get()), "Dodô não é carnívoro");
        helper.assertTrue(WildSpawnRules.herbivoreCap(WildSpawner.effectiveMaximumPopulation())
                < WildSpawner.effectiveMaximumPopulation(), "não sobrou vaga para carnívoros");
        helper.succeed();
    }

    // ---- Qualquer bioma com o peso base, o triplo no ideal ----

    /**
     * A reposição oferece a espécie em todo bioma do Overworld; fora do ideal, com o peso base. A selva (ou o
     * deserto, se a selva for o ideal dela) é o exemplo de bioma longe de casa.
     */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void everySpeciesSpawnsAnywhereWithTheBaseWeight(GameTestHelper helper) {
        Registry<Biome> biomes = biomes(helper);
        List<String> problems = new ArrayList<>();
        forEachSpawningSpecies(helper, (type, spawn) -> {
            String id = EntityType.getKey(type).getPath();
            if (spawn.favored().isEmpty()) {
                problems.add(id + " sem spawn.favored");
                return;
            }
            var ideal = spawn.favored().get().biomes();
            Holder<Biome> away = awayFromHome(biomes, ideal);
            if (away == null) {
                problems.add(id + ": todo bioma de " + AWAY_FROM_HOME + " é ideal");
            } else if (!away.is(spawn.biomes()) || spawn.weightIn(away) != spawn.weight()) {
                problems.add(id + " em " + key(away) + ": nasce " + away.is(spawn.biomes()) + ", peso "
                        + spawn.weightIn(away) + " (base " + spawn.weight() + ")");
            }
            for (Holder<Biome> biome : biomes.getTagOrEmpty(BiomeTags.IS_OVERWORLD)) {
                if (!biome.is(spawn.biomes())) {
                    problems.add(id + " não nasce em " + key(biome));
                } else if (!biome.is(ideal) && spawn.weightIn(biome) != spawn.weight()) {
                    problems.add(id + " com peso " + spawn.weightIn(biome) + " fora do ideal, em " + key(biome));
                }
            }
        });
        helper.assertTrue(problems.isEmpty(), "espécies fora da convenção de spawn: " + problems);
        helper.succeed();
    }

    /** No bioma ideal, o sorteio da reposição dá o triplo do peso base. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theIdealBiomeTriplesTheWeight(GameTestHelper helper) {
        Registry<Biome> biomes = biomes(helper);
        List<String> problems = new ArrayList<>();
        forEachSpawningSpecies(helper, (type, spawn) -> {
            String id = EntityType.getKey(type).getPath();
            if (spawn.favored().isEmpty()) {
                problems.add(id + " sem spawn.favored");
                return;
            }
            var ideal = spawn.favored().get().biomes();
            int count = 0;
            for (Holder<Biome> biome : biomes.getTagOrEmpty(ideal)) {
                count++;
                if (!biome.is(spawn.biomes())) {
                    problems.add(id + " não nasce no próprio ideal " + key(biome));
                } else if (spawn.weightIn(biome) != 3 * spawn.weight()) {
                    problems.add(id + " em " + key(biome) + ": peso " + spawn.weightIn(biome) + ", esperado "
                            + 3 * spawn.weight());
                }
            }
            if (count == 0) {
                problems.add(id + " com ideal vazio (" + ideal.location() + ")");
            }
        });
        helper.assertTrue(problems.isEmpty(), "ideal sem o triplo do peso: " + problems);
        helper.succeed();
    }

    /** A planície nevada é ideal de todas: no preset Era do Gelo, só com ela, a proporção entre as espécies fica a mesma. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void snowyPlainsIsIdealForEverySpecies(GameTestHelper helper) {
        var snowyPlains = biomes(helper).getHolderOrThrow(Biomes.SNOWY_PLAINS);
        List<String> missing = new ArrayList<>();
        forEachSpawningSpecies(helper, (type, spawn) -> {
            if (!spawn.favored().map(favored -> snowyPlains.is(favored.biomes())).orElse(false)) {
                missing.add(EntityType.getKey(type).getPath());
            }
        });
        helper.assertTrue(missing.isEmpty(), "planície nevada fora do ideal de: " + missing);
        helper.succeed();
    }

    /**
     * Geração do terreno ({@code forge:add_spawns}): a espécie entra em todo bioma do Overworld com o peso w, e os
     * modificadores somam 3w nos biomas ideais — a mesma regra da reposição.
     */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void biomeModifiersAddThreeTimesTheBaseInTheIdeal(GameTestHelper helper) {
        Registry<Biome> biomes = biomes(helper);
        List<String> problems = new ArrayList<>();
        forEachSpawningSpecies(helper, (type, spawn) -> {
            String id = EntityType.getKey(type).getPath();
            if (spawn.favored().isEmpty()) {
                problems.add(id + " sem spawn.favored");
                return;
            }
            var ideal = spawn.favored().get().biomes();
            Holder<Biome> away = awayFromHome(biomes, ideal);
            if (away == null) {
                problems.add(id + ": todo bioma de " + AWAY_FROM_HOME + " é ideal");
                return;
            }
            int base = generationWeight(away, type);
            if (base <= 0) {
                problems.add(id + " não entra na geração de " + key(away));
                return;
            }
            for (Holder<Biome> biome : biomes.getTagOrEmpty(BiomeTags.IS_OVERWORLD)) {
                int expected = biome.is(ideal) ? 3 * base : base;
                int actual = generationWeight(biome, type);
                if (actual != expected) {
                    problems.add(id + " em " + key(biome) + ": peso " + actual + ", esperado " + expected);
                }
            }
        });
        helper.assertTrue(problems.isEmpty(), "geração fora da convenção (w fora, 3w no ideal): " + problems);
        helper.succeed();
    }

    private static Registry<Biome> biomes(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
    }

    /**
     * Cada espécie que nasce sozinha (tem bloco {@code spawn}), menos as desligadas. Falha se não achar nenhuma, para
     * o teste não passar em branco.
     */
    private static void forEachSpawningSpecies(GameTestHelper helper, BiConsumer<EntityType<?>, SpawnProfile> check) {
        int checked = 0;
        for (var holder : ModEntities.LAND_CREATURES) {
            EntityType<?> type = holder.get();
            if (type.is(ModTags.DISABLED)) {
                continue;
            }
            var spawn = Species.of(helper.getLevel().registryAccess(), type).flatMap(Species::spawn);
            if (spawn.isPresent()) {
                check.accept(type, spawn.get());
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "nenhuma espécie com bloco spawn");
    }

    private static Holder<Biome> awayFromHome(Registry<Biome> biomes, TagKey<Biome> ideal) {
        for (ResourceKey<Biome> key : AWAY_FROM_HOME) {
            Holder<Biome> biome = biomes.getHolderOrThrow(key);
            if (!biome.is(ideal)) {
                return biome;
            }
        }
        return null;
    }

    /** Soma dos pesos da espécie na lista de spawn do bioma, já com os biome modifiers aplicados. */
    private static int generationWeight(Holder<Biome> biome, EntityType<?> type) {
        int total = 0;
        for (MobCategory category : MobCategory.values()) {
            for (var spawner : biome.value().getMobSettings().getMobs(category).unwrap()) {
                if (spawner.type == type) {
                    total += spawner.getWeight().asInt();
                }
            }
        }
        return total;
    }

    private static String key(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }
}
