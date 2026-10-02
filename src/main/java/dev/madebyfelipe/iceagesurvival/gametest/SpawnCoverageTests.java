package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.spawn.WildSpawnRules;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Toda espécie que nasce precisa ter onde nascer nos dois modos de mundo, e os carnívoros precisam de vaga.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class SpawnCoverageTests {
    private static final String EMPTY = "empty";

    /**
     * No preset Era do Gelo, todo bioma de terra vira planície nevada: a espécie que não a tem na tag de
     * spawn nunca nasce lá (era o caso do Utahraptor e do Urso-terrível).
     */
    @GameTest(template = EMPTY, batch = "spawn_coverage")
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
    @GameTest(template = EMPTY, batch = "spawn_coverage")
    public static void predatorsCountAsCarnivoresForTheReservedCap(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(WildSpawner.isCarnivore(level, ModEntities.SMILODON.get()), "Smilodon deveria ser carnívoro");
        helper.assertTrue(WildSpawner.isCarnivore(level, ModEntities.UTAHRAPTOR.get()), "Utahraptor deveria ser carnívoro");
        helper.assertFalse(WildSpawner.isCarnivore(level, ModEntities.DODO.get()), "Dodô não é carnívoro");
        helper.assertTrue(WildSpawnRules.herbivoreCap(WildSpawner.effectiveMaximumPopulation())
                < WildSpawner.effectiveMaximumPopulation(), "não sobrou vaga para carnívoros");
        helper.succeed();
    }
}
