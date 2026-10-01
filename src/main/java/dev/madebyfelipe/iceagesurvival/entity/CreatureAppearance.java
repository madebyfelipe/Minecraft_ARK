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
        String unconscious,
        String fly) {
    /** Espécie que não voa: a animação de voo é a de andar. */
    public CreatureAppearance(String model, String texture, String idle, String walk, String attack, String unconscious) {
        this(model, texture, idle, walk, attack, unconscious, walk);
    }

    private static final Map<String, CreatureAppearance> REVIVAL = Map.ofEntries(Map.entry("dodo",
                    new CreatureAppearance("dodo", "dodo/dodo_male.png", "idle", "walk", "attack_1", "sleep_1")),
            Map.entry("elasmotherium", new CreatureAppearance("elasmotherium", "elasmotherium/elasmotherium_male.png",
                    "idle", "walk", "attack", "sit/sleep")),
            Map.entry("smilodon", new CreatureAppearance("smilodon", "smilodon/smilodon_male.png", "idle", "walk", "attack", "sleep")),
            Map.entry("mammoth", new CreatureAppearance("mammoth", "mammoth/mammoth_male.png", "idle_1_90", "walk", "attack", "rest/sleep")),
            Map.entry("tyrannosaurus", new CreatureAppearance("tyrannosaurus", "tyrannosaurus/tyrannosaurus_male.png",
                    "idle", "walk", "attack_normal_1", "sleep_1")),
            Map.entry("velociraptor", new CreatureAppearance("velociraptor", "velociraptor/velociraptor_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("utahraptor", new CreatureAppearance("deinonychus", "deinonychus/deinonychus_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("spinosaurus", new CreatureAppearance("spinosaurus", "spinosaurus/spinosaurus_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("allosaurus", new CreatureAppearance("allosaurus", "allosaurus/allosaurus_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("brontosaurus", new CreatureAppearance("diplodocus", "diplodocus/diplodocus_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("stegosaurus", new CreatureAppearance("stegosaurus", "stegosaurus/stegosaurus_male.png",
                    "idle", "walk", "attack_back_left", "sleep_1")),
            Map.entry("pteranodon", new CreatureAppearance("pteranodon", "pteranodon/pteranodon_male.png",
                    "idle", "walk", "attack", "sleep", "fly"))
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
