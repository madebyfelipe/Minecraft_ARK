package dev.madebyfelipe.iceagesurvival.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Ajustes de renderização por espécie, lidos de
 * {@code assets/<namespace>/creature_models/<especie>.json}:
 *
 * <pre>{ "scale": 1.3, "look_bone": "neck_control", "baby_hidden_bones": ["Tusk1", "Tusk2"] }</pre>
 *
 * <ul>
 *   <li>{@code scale}: fator aplicado ao modelo (padrão 1);
 *   <li>{@code look_bone}: osso que gira para acompanhar o olhar (padrão {@code head}); cada modelo
 *       de origem nomeia o seu — {@code Head}, {@code neck}, {@code neck_control}…;
 *   <li>{@code baby_hidden_bones}: ossos escondidos no filhote (as presas do mamute).
 * </ul>
 *
 * Ficam nos assets, junto do modelo a que se referem, e não nos dados da espécie: assim um
 * resource pack que troca o modelo de uma criatura também acerta o tamanho e os ossos dele.
 */
public final class CreatureModelSettings extends SimpleJsonResourceReloadListener {
    public static final CreatureModelSettings INSTANCE = new CreatureModelSettings();
    private static final String DIRECTORY = "creature_models";
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;

    public record Settings(float scale, String lookBone, List<String> babyHiddenBones) {
        public static final Settings DEFAULT = new Settings(1.0F, "head", List.of());
    }

    private Map<ResourceLocation, Settings> settings = Map.of();

    private CreatureModelSettings() {
        super(new Gson(), DIRECTORY);
    }

    /** Ajustes da espécie; os padrões se não houver arquivo. */
    public Settings get(ResourceLocation typeId) {
        return settings.getOrDefault(typeId, Settings.DEFAULT);
    }

    public float scale(ResourceLocation typeId) {
        return get(typeId).scale();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Settings> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                JsonObject object = GsonHelper.convertToJsonObject(json, DIRECTORY);
                float scale = GsonHelper.getAsFloat(object, "scale", Settings.DEFAULT.scale());
                String lookBone = GsonHelper.getAsString(object, "look_bone", Settings.DEFAULT.lookBone());
                List<String> hidden = new ArrayList<>();
                GsonHelper.getAsJsonArray(object, "baby_hidden_bones", new com.google.gson.JsonArray())
                        .forEach(element -> hidden.add(element.getAsString()));
                loaded.put(id, new Settings(Math.clamp(scale, MIN_SCALE, MAX_SCALE), lookBone, List.copyOf(hidden)));
            } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
                IceAgeSurvival.LOGGER.error("Ajuste de modelo inválido em {}: {}", id, e.getMessage());
            }
        });
        settings = loaded;
    }
}
