package dev.madebyfelipe.iceagesurvival.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.CreatureAppearance;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Os nomes de assets que o código aponta existem de fato. Um nome errado não quebra nada no servidor: a criatura fica
 * parada sem animação, ou o item aparece sem textura, e só se vê em jogo.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class AssetReferenceTests {
    private static final String EMPTY = "empty";

    /** Toda animação que a criatura pede ao modelo do Revival está no arquivo de animações dele, com o prefixo certo. */
    @GameTest(template = EMPTY)
    public static void everyRevivalAnimationExists(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (var creature : ModEntities.LAND_CREATURES) {
            Optional<CreatureAppearance> found = CreatureAppearance.forEntity(creature.getId());
            if (found.isEmpty()) {
                continue; // modelo de outra fonte (Jurassic Reborn) ou próprio
            }
            CreatureAppearance appearance = found.get();
            JsonObject animations = json(helper, "fossil", "assets/fossil/animations/entity/" + appearance.model()
                    + ".animation.json").getAsJsonObject("animations");
            for (String name : List.of(appearance.idle(), appearance.walk(), appearance.attack(),
                    appearance.unconscious(), appearance.fly(), appearance.run(), appearance.dive())) {
                String key = appearance.animationPrefix() + name;
                if (!animations.has(key)) {
                    missing.add(creature.getId().getPath() + ": " + key);
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "animações que não existem no Revival: " + missing);
        helper.succeed();
    }

    /** Todo ovo gerador tem modelo de item; sem ele, aparece com a textura que falta. */
    @GameTest(template = EMPTY)
    public static void everySpawnEggHasAnItemModel(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (var item : ModItems.ITEMS.getEntries()) {
            String path = item.getId().getPath();
            if (path.endsWith("_spawn_egg") && resource(IceAgeSurvival.MODID,
                    "assets/" + IceAgeSurvival.MODID + "/models/item/" + path + ".json") == null) {
                missing.add(path);
            }
        }
        helper.assertTrue(missing.isEmpty(), "ovos sem modelo de item: " + missing);
        helper.succeed();
    }

    private static JsonObject json(GameTestHelper helper, String modId, String file) {
        Path path = resource(modId, file);
        helper.assertTrue(path != null, "não achou " + modId + "/" + file);
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("não leu " + file, e);
        }
    }

    private static Path resource(String modId, String file) {
        var mod = ModList.get().getModFileById(modId);
        if (mod == null) {
            return null;
        }
        Path path = mod.getFile().findResource(file);
        return Files.exists(path) ? path : null;
    }
}
