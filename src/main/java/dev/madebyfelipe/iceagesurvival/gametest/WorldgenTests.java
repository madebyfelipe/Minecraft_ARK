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
    /** Cavernas guardam a temperatura delas: são abrigo do frio. E o oceano profundo congelado, abaixo. */
    private static final Set<ResourceKey<Biome>> CAVES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK,
            // Temperatura base 0,5, mas com o modificador "frozen" do vanilla: a superfície congela.
            Biomes.DEEP_FROZEN_OCEAN);
    /**
     * Limite de neve do vanilla ({@code Biome.coldEnoughToSnow}): acima disto chove em vez de nevar.
     * Taiga (0,25), costa de pedra e morros ventosos (0,2) passavam no limite antigo de 0,3 e o mundo
     * saía verde em boa parte; agora toda a superfície neva.
     */
    private static final float WARMEST_ALLOWED = 0.15F;

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
        for (ResourceKey<Biome> gone : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.JUNGLE, Biomes.OCEAN, Biomes.RIVER,
                Biomes.TAIGA, Biomes.STONY_SHORE, Biomes.WINDSWEPT_HILLS, Biomes.OLD_GROWTH_PINE_TAIGA)) {
            helper.assertTrue(biomes.stream().noneMatch(biome -> biome.is(gone)), gone.location() + " sobrou");
        }
        for (ResourceKey<Biome> kept : List.of(Biomes.SNOWY_PLAINS, Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN, Biomes.SNOWY_TAIGA)) {
            helper.assertTrue(biomes.stream().anyMatch(biome -> biome.is(kept)), kept.location() + " faltando");
        }
        helper.succeed();
    }

    /**
     * A Era do Gelo é a estepe do mamute: tundra aberta, com manchas de floresta. As florestas
     * temperadas viravam todas taiga nevada, e ~75% da terra saía floresta de pinheiro fechada —
     * árvores demais e bicho grande sem espaço para nascer. Amostra o ruído real do preset numa
     * área de ~16 mil blocos de lado, com três sementes.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void iceAgeIsMostlyOpenTundra(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        WorldPreset preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(IceAgeSurvival.id("ice_age"));
        var source = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD).generator().getBiomeSource();
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD);
        int land = 0;
        int plains = 0;
        int taiga = 0;
        for (long seed : new long[] {1L, 42L, 20261001L}) {
            var sampler = net.minecraft.world.level.levelgen.RandomState.create(settings,
                    registries.lookupOrThrow(Registries.NOISE), seed).sampler();
            for (int x = -32; x < 32; x++) {
                for (int z = -32; z < 32; z++) {
                    // Passo de 256 blocos (64 em coordenada de quarto), na altura do nível do mar.
                    Holder<Biome> biome = source.getNoiseBiome(x * 64, 16, z * 64, sampler);
                    if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)
                            || biome.unwrapKey().map(CAVES::contains).orElse(false)) {
                        continue;
                    }
                    land++;
                    if (biome.is(Biomes.SNOWY_PLAINS)) {
                        plains++;
                    } else if (biome.is(Biomes.SNOWY_TAIGA)) {
                        taiga++;
                    }
                }
            }
        }
        double plainsShare = (double) plains / land;
        double taigaShare = (double) taiga / land;
        helper.assertTrue(plainsShare >= 0.40, String.format("tundra aberta só em %.0f%% da terra", plainsShare * 100));
        helper.assertTrue(taigaShare <= 0.30, String.format("taiga nevada em %.0f%% da terra", taigaShare * 100));
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
