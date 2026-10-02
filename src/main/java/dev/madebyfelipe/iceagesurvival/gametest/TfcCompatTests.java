package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.compat.tfc.TfcCompat;
import dev.madebyfelipe.iceagesurvival.world.IceAgeMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * A integração com o TerraFirmaCraft é opcional. Aqui o TFC não está instalado: tudo o que depende
 * dele precisa ficar desligado, e as tags com entradas opcionais do TFC continuam carregando.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TfcCompatTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "tfc_absent";
    private static final String PACK_ID = "iceagesurvival_tfc_compat";

    @GameTest(template = EMPTY, batch = BATCH)
    public static void tfcIsNotLoaded(GameTestHelper helper) {
        helper.assertTrue(!TfcCompat.isLoaded(), "TFC dado como carregado sem estar instalado");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void noTfcTemperatureWithoutTfc(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(TfcCompat.biomeTemperature(helper.getLevel(), pos).isEmpty(),
                "temperatura do TFC presente sem o TFC");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void overworldGeneratorIsNeitherTfcNorIceAge(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ChunkGenerator generator = server.overworld().getChunkSource().getGenerator();
        helper.assertTrue(!TfcCompat.isTfcGenerator(generator), "gerador do Overworld de teste dado como do TFC");
        helper.assertTrue(!TfcCompat.isIceAgeGenerator(generator),
                "gerador do Overworld de teste dado como Era do Gelo");
        helper.assertTrue(!IceAgeMode.isIceAgePreset(server), "o mundo do GameTest não é Era do Gelo");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void biomeChoicesAreUntouchedWithoutTfc(GameTestHelper helper) {
        int[] choices = {0, 1, 2, 3};
        helper.assertTrue(TfcCompat.weighChoices(choices) == choices, "sorteio de biomas mudou sem o TFC");
        helper.assertTrue(choices[0] == 0 && choices[1] == 1 && choices[2] == 2 && choices[3] == 3,
                "lista de biomas alterada");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void compatDatapackIsAbsentWithoutTfc(GameTestHelper helper) {
        var packs = helper.getLevel().getServer().getPackRepository();
        helper.assertTrue(!packs.getAvailableIds().contains(PACK_ID), "datapack do TFC disponível sem o TFC");
        helper.assertTrue(!packs.getSelectedIds().contains(PACK_ID), "datapack do TFC ativo sem o TFC");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void biomeTagSkipsOptionalTfcEntries(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, IceAgeSurvival.id("spawns_brontosaurus"));
        var named = biomes.getTag(tag);
        helper.assertTrue(named.isPresent(), "tag spawns_brontosaurus não carregou");
        helper.assertTrue(biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS).is(tag),
                "snowy_plains fora de spawns_brontosaurus");
        HolderSet.Named<Biome> set = named.get();
        for (Holder<Biome> holder : set) {
            String namespace = holder.unwrapKey().map(ResourceKey::location).map(ResourceLocation::getNamespace)
                    .orElse("?");
            helper.assertTrue(!TfcCompat.MODID.equals(namespace),
                    "bioma do TFC na tag sem o TFC: " + holder.unwrapKey().orElse(null));
        }
        helper.assertTrue(biomes.keySet().stream().noneMatch(id -> TfcCompat.MODID.equals(id.getNamespace())),
                "bioma do TFC registrado sem o TFC");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void entityTagSkipsOptionalTfcEntries(GameTestHelper helper) {
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("tyrannosaurus_prey"));
        var named = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        helper.assertTrue(named.isPresent(), "tag tyrannosaurus_prey não carregou");
        helper.assertTrue(named.get().size() > 0, "tag tyrannosaurus_prey vazia");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                        .noneMatch(id -> TfcCompat.MODID.equals(id.getNamespace())),
                "tipo de entidade do TFC registrado sem o TFC");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void rawRedMeatTagKeepsVanillaBeefWithoutTfc(GameTestHelper helper) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, IceAgeSurvival.id("taming/raw_red_meat"));
        helper.assertTrue(BuiltInRegistries.ITEM.getTag(tag).isPresent(), "tag taming/raw_red_meat não carregou");
        helper.assertTrue(new ItemStack(Items.BEEF).is(tag), "carne crua de vaca fora de taming/raw_red_meat");
        helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream()
                        .noneMatch(id -> TfcCompat.MODID.equals(id.getNamespace())),
                "item do TFC registrado sem o TFC");
        helper.succeed();
    }
}
