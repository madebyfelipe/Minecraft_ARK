package dev.madebyfelipe.iceagesurvival.client.tabula;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Carrega, a cada recarga de recursos do cliente, os modelos Tabula e as poses das espécies de
 * {@link JurassicRebornAppearance}, pelo {@code ResourceManager} (do mod Jurassic Reborn instalado, ou de um
 * resource pack que os troque). A leitura dos arquivos roda fora da thread principal; a montagem das partes, nela.
 * Sem o mod ou com arquivo faltando, avisa uma vez no log e a espécie fica sem modelo (não é desenhada).
 */
public final class TabulaModels extends SimplePreparableReloadListener<Map<ResourceLocation, TabulaModels.Loaded>> {
    public static final TabulaModels INSTANCE = new TabulaModels();

    /** O que a leitura produz, ainda sem nada do Minecraft além do id. */
    record Loaded(JurassicRebornAppearance appearance, TabulaModelData base, TabulaRig rig,
                  Map<String, PoseClip> clips) {
    }

    private volatile Map<ResourceLocation, BakedTabulaModel> models = Map.of();

    private TabulaModels() {
    }

    /** O modelo pronto da espécie, ou vazio se o Jurassic Reborn não está instalado ou o modelo não carregou. */
    public static Optional<BakedTabulaModel> get(ResourceLocation entityId) {
        return Optional.ofNullable(INSTANCE.models.get(entityId));
    }

    @Override
    protected Map<ResourceLocation, Loaded> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Loaded> loaded = new HashMap<>();
        for (String path : JurassicRebornAppearance.entityPaths()) {
            ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(IceAgeSurvival.MODID, path);
            JurassicRebornAppearance appearance = JurassicRebornAppearance.forEntity(entityId).orElseThrow();
            try {
                load(resources, appearance).ifPresent(species -> loaded.put(entityId, species));
            } catch (IOException | RuntimeException e) {
                IceAgeSurvival.LOGGER.warn("Modelo do Jurassic Reborn de {} não carregou ({}); a criatura não será "
                        + "desenhada.", entityId, e.toString());
            }
        }
        return loaded;
    }

    private static Optional<Loaded> load(ResourceManager resources, JurassicRebornAppearance appearance)
            throws IOException {
        ResourceLocation posesId = appearance.posesResource();
        Optional<Resource> posesResource = resources.getResource(posesId);
        if (posesResource.isEmpty()) {
            IceAgeSurvival.LOGGER.warn("{} não encontrado: o Jurassic Reborn não está instalado? A espécie {} não será "
                    + "desenhada.", posesId, appearance.species());
            return Optional.empty();
        }
        Map<String, List<PoseStep>> animations;
        try (Reader reader = posesResource.get().openAsReader()) {
            animations = TabulaReader.readPoses(reader);
        }
        if (animations.isEmpty()) {
            throw new IOException(posesId + " sem animações");
        }

        Map<String, TabulaModelData> poses = new HashMap<>();
        for (List<PoseStep> steps : animations.values()) {
            for (PoseStep step : steps) {
                if (!poses.containsKey(step.pose())) {
                    poses.put(step.pose(), readPose(resources, appearance.poseResource(step.pose())));
                }
            }
        }

        // A malha vem da primeira pose de IDLE: todas as poses repetem a mesma árvore de cubos.
        List<PoseStep> idle = animations.getOrDefault(CreaturePoses.IDLE, animations.values().iterator().next());
        TabulaModelData base = poses.get(idle.get(0).pose());
        TabulaRig rig = TabulaRig.of(base);

        Map<String, float[]> frames = new HashMap<>();
        for (Map.Entry<String, TabulaModelData> pose : poses.entrySet()) {
            int matched = rig.matchedByIdentifier(pose.getValue());
            if (matched < rig.size()) {
                IceAgeSurvival.LOGGER.warn("Pose {} de {}: só {} de {} cubos acham par na malha base.",
                        pose.getKey(), appearance.species(), matched, rig.size());
            }
            frames.put(pose.getKey(), rig.frameOf(pose.getValue()));
        }
        Map<String, PoseClip> clips = new HashMap<>();
        for (Map.Entry<String, List<PoseStep>> animation : animations.entrySet()) {
            List<float[]> clipFrames = new ArrayList<>();
            float[] ticks = new float[animation.getValue().size()];
            for (int i = 0; i < ticks.length; i++) {
                PoseStep step = animation.getValue().get(i);
                clipFrames.add(frames.get(step.pose()));
                ticks[i] = step.ticks();
            }
            clips.put(animation.getKey(), new PoseClip(clipFrames, ticks));
        }
        return Optional.of(new Loaded(appearance, base, rig, clips));
    }

    private static TabulaModelData readPose(ResourceManager resources, ResourceLocation id) throws IOException {
        Resource resource = resources.getResource(id).orElseThrow(() -> new IOException(id + " não encontrado"));
        try (InputStream stream = resource.open()) {
            return TabulaReader.readTbl(stream);
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, Loaded> loaded, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, BakedTabulaModel> baked = new HashMap<>();
        loaded.forEach((id, species) -> {
            try {
                baked.put(id, BakedTabulaModel.bake(species.appearance(), species.base(), species.rig(),
                        species.clips()));
            } catch (RuntimeException e) {
                IceAgeSurvival.LOGGER.warn("Modelo do Jurassic Reborn de {} não montou ({}); a criatura não será "
                        + "desenhada.", id, e.toString());
            }
        });
        models = Map.copyOf(baked);
    }
}
