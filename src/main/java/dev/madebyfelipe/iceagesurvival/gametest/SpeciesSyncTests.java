package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * As espécies vão ao cliente no login, antes das tags. Decodificar sem registros (NbtOps puro) simula o cliente de
 * outro PC: se alguma referência dependesse das tags carregadas, ele cairia ao entrar no mundo.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class SpeciesSyncTests {
    private static final String EMPTY = "empty";

    private static Species roundTrip(GameTestHelper helper, ResourceKey<Species> key, Species species) {
        Tag encoded = Species.CODEC.encodeStart(NbtOps.INSTANCE, species)
                .getOrThrow(false, error -> helper.fail(key.location() + " não codificou: " + error));
        return Species.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, error -> helper.fail(key.location() + " não decodificou sem tags: " + error));
    }

    @GameTest(template = EMPTY)
    public static void everySpeciesDecodesWithoutRegistries(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Species.REGISTRY_KEY);
        helper.assertTrue(registry.size() > 0, "registro de espécies vazio");
        for (Map.Entry<ResourceKey<Species>, Species> entry : registry.entrySet()) {
            roundTrip(helper, entry.getKey(), entry.getValue());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void decodedTamingTagStillMatches(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Species.REGISTRY_KEY);
        ResourceKey<Species> wolf = ResourceKey.create(Species.REGISTRY_KEY, IceAgeSurvival.id("dire_wolf"));
        Species decoded = roundTrip(helper, wolf, registry.getOrThrow(wolf));
        boolean eatsBeef = decoded.taming().flatMap(taming -> taming.foodFor(new ItemStack(Items.BEEF))).isPresent();
        helper.assertTrue(eatsBeef, "lobo-terrível decodificado não aceita carne de vaca (#taming/raw_red_meat)");
        helper.assertTrue(decoded.taming().map(TamingProfile::foods).map(foods -> !foods.isEmpty()).orElse(false),
                "lobo-terrível sem alimentos de domesticação");
        helper.succeed();
    }
}
