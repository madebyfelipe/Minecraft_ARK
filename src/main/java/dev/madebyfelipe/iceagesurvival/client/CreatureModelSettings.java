package dev.madebyfelipe.iceagesurvival.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.HashMap;
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
 * <pre>{ "scale": 2.0 }</pre>
 *
 * Ficam nos assets, junto do modelo a que se referem, e não nos dados da espécie: assim um
 * resource pack que troca o modelo de uma criatura também acerta o tamanho dele.
 */
public final class CreatureModelSettings extends SimpleJsonResourceReloadListener {
    public static final CreatureModelSettings INSTANCE = new CreatureModelSettings();
    private static final String DIRECTORY = "creature_models";
    private static final float DEFAULT_SCALE = 1.0F;
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;

    private Map<ResourceLocation, Float> scales = Map.of();

    private CreatureModelSettings() {
        super(new Gson(), DIRECTORY);
    }

    /** Fator aplicado ao modelo da espécie; 1 se não houver arquivo. */
    public float scale(ResourceLocation typeId) {
        return scales.getOrDefault(typeId, DEFAULT_SCALE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Float> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                float scale = GsonHelper.getAsFloat(GsonHelper.convertToJsonObject(json, DIRECTORY), "scale", DEFAULT_SCALE);
                loaded.put(id, Math.clamp(scale, MIN_SCALE, MAX_SCALE));
            } catch (JsonParseException e) {
                IceAgeSurvival.LOGGER.error("Ajuste de modelo inválido em {}: {}", id, e.getMessage());
            }
        });
        scales = loaded;
    }
}
