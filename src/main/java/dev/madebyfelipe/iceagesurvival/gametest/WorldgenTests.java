package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Etapa 9: o world preset glacial. Gerar um mundo com ele é teste manual; aqui, os biomas que ele pode dar. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WorldgenTests {
    private static final String EMPTY = "empty";
    /** Cavernas guardam a temperatura delas: são abrigo do frio. E o oceano profundo congelado, abaixo. */
    private static final Set<ResourceKey<Biome>> CAVES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK,
            // Temperatura base 0,5, mas com o modificador "frozen" do vanilla: a superfície congela.
            Biomes.DEEP_FROZEN_OCEAN);
    /** Acima disto nada neva; taigas (0,25) e morros ventosos (0,2) são as regiões frias sem nevasca. */
    private static final float WARMEST_ALLOWED = 0.3F;

    @GameTest(template = EMPTY)
    public static void iceAgePresetOnlyHasColdBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        WorldPreset preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(IceAgeSurvival.id("ice_age"));
        helper.assertTrue(preset != null, "preset iceagesurvival:ice_age não carregou");
        LevelStem overworld = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD);
        Set<Holder<Biome>> biomes = overworld.generator().getBiomeSource().possibleBiomes();

        List<String> warm = biomes.stream()
                .filter(biome -> biome.unwrapKey().map(key -> !CAVES.contains(key)).orElse(true))
                .filter(biome -> biome.value().getBaseTemperature() > WARMEST_ALLOWED)
                .map(biome -> biome.unwrapKey().map(key -> key.location().toString()).orElse("?"))
                .collect(Collectors.toList());
        helper.assertTrue(warm.isEmpty(), "biomas quentes no mundo glacial: " + warm);
        for (ResourceKey<Biome> gone : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.JUNGLE, Biomes.OCEAN, Biomes.RIVER)) {
            helper.assertTrue(biomes.stream().noneMatch(biome -> biome.is(gone)), gone.location() + " sobrou");
        }
        for (ResourceKey<Biome> kept : List.of(Biomes.SNOWY_PLAINS, Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN, Biomes.TAIGA)) {
            helper.assertTrue(biomes.stream().anyMatch(biome -> biome.is(kept)), kept.location() + " faltando");
        }
        helper.succeed();
    }
}
