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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Etapa 9: o world preset glacial. Gerar um mundo com ele é teste manual; aqui, os biomas que ele pode dar. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WorldgenTests {
    private static final String EMPTY = "empty";
    private static final Set<ResourceKey<Biome>> ICE_AGE_BIOMES = Set.of(
            Biomes.SNOWY_PLAINS, Biomes.SNOWY_BEACH, Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN, Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK);

    @GameTest(template = EMPTY)
    public static void iceAgePresetOnlyHasColdBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        WorldPreset preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(IceAgeSurvival.id("ice_age"));
        helper.assertTrue(preset != null, "preset iceagesurvival:ice_age não carregou");
        LevelStem overworld = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD);
        Set<Holder<Biome>> biomes = overworld.generator().getBiomeSource().possibleBiomes();

        List<String> unexpected = biomes.stream()
                .filter(biome -> biome.unwrapKey().map(key -> !ICE_AGE_BIOMES.contains(key)).orElse(true))
                .map(biome -> biome.unwrapKey().map(key -> key.location().toString()).orElse("?"))
                .collect(Collectors.toList());
        helper.assertTrue(unexpected.isEmpty(), "biomas fora do conjunto gelado simples: " + unexpected);
        for (ResourceKey<Biome> gone : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.JUNGLE, Biomes.OCEAN, Biomes.RIVER)) {
            helper.assertTrue(biomes.stream().noneMatch(biome -> biome.is(gone)), gone.location() + " sobrou");
        }
        for (ResourceKey<Biome> kept : List.of(Biomes.SNOWY_PLAINS, Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN)) {
            helper.assertTrue(biomes.stream().anyMatch(biome -> biome.is(kept)), kept.location() + " faltando");
        }
        helper.succeed();
    }

    /** Do Revival só entram os assets: minérios, estátua moai e estruturas dele não são gerados. */
    @GameTest(template = EMPTY)
    public static void revivalWorldgenIsDisabled(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var structureSets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        for (String name : List.of("fossil_site", "tar_site", "aztec_temple", "aztec_weapon_shop", "egyptian_academy", "hell_boat")) {
            var set = structureSets.get(new net.minecraft.resources.ResourceLocation("fossil", name));
            helper.assertTrue(set != null, "conjunto fossil:" + name + " sumiu (o Revival mudou?)");
            helper.assertTrue(set.structures().isEmpty(), "fossil:" + name + " ainda gera estruturas");
        }
        var modifiers = registries.registryOrThrow(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS);
        helper.assertTrue(modifiers.containsKey(IceAgeSurvival.id("remove_revival_features")),
                "biome modifier que tira os minérios do Revival não carregou");
        var oreKey = net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE,
                new net.minecraft.resources.ResourceLocation("fossil", "ore_fossil_block_middle"));
        var plains = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.SNOWY_PLAINS);
        boolean hasOre = plains.value().getGenerationSettings().features().stream()
                .flatMap(net.minecraft.core.HolderSet::stream)
                .anyMatch(feature -> feature.is(oreKey));
        helper.assertFalse(hasOre, "minério de fóssil do Revival continua na planície nevada");
        helper.succeed();
    }
}
