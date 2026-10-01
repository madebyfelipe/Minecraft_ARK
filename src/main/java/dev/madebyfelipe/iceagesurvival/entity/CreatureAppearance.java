package dev.madebyfelipe.iceagesurvival.entity;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Referências a assets que o Fossils and Archeology Revival fornece em runtime. */
public record CreatureAppearance(
        String model,
        String texture,
        String idle,
        String walk,
        String attack,
        String unconscious) {
    private static final Map<String, CreatureAppearance> REVIVAL = Map.of(
            "smilodon", new CreatureAppearance("smilodon", "smilodon/smilodon_male.png", "idle", "walk", "attack", "sleep"),
            "mammoth", new CreatureAppearance("mammoth", "mammoth/mammoth_male.png", "idle_1_90", "walk", "attack", "rest/sleep"),
            "tyrannosaurus", new CreatureAppearance("tyrannosaurus", "tyrannosaurus/tyrannosaurus_male.png",
                    "idle", "walk", "attack_normal_1", "sleep_1"),
            "velociraptor", new CreatureAppearance("velociraptor", "velociraptor/velociraptor_male.png",
                    "idle", "walk", "attack", "sleep"),
            "utahraptor", new CreatureAppearance("deinonychus", "deinonychus/deinonychus_male.png",
                    "idle", "walk", "attack", "sleep"),
            "spinosaurus", new CreatureAppearance("spinosaurus", "spinosaurus/spinosaurus_male.png",
                    "idle", "walk", "attack", "sleep"),
            "allosaurus", new CreatureAppearance("allosaurus", "allosaurus/allosaurus_male.png",
                    "idle", "walk", "attack", "sleep"),
            "brontosaurus", new CreatureAppearance("diplodocus", "diplodocus/diplodocus_male.png",
                    "idle", "walk", "attack", "sleep"),
            "stegosaurus", new CreatureAppearance("stegosaurus", "stegosaurus/stegosaurus_male.png",
                    "idle", "walk", "attack_back_left", "sleep_1"),
            "pteranodon", new CreatureAppearance("pteranodon", "pteranodon/pteranodon_male.png",
                    "idle", "fly", "attack", "sleep")
    );

    public static Optional<CreatureAppearance> forEntity(ResourceLocation entityId) {
        return Optional.ofNullable(REVIVAL.get(entityId.getPath()));
    }

    public ResourceLocation modelResource() {
        return revivalResource("geo/entity/" + model + ".geo.json");
    }

    public ResourceLocation textureResource() {
        return revivalResource("textures/entity/" + texture);
    }

    public ResourceLocation animationResource() {
        return revivalResource("animations/entity/" + model + ".animation.json");
    }

    public String animationPrefix() {
        return "animation." + model + ".";
    }

    private static ResourceLocation revivalResource(String path) {
        return ResourceLocation.fromNamespaceAndPath("fossil", path);
    }
}
