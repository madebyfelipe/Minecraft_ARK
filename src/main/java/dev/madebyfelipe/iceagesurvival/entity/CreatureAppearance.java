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
        String fly,
        String run,
        String dive) {
    /** Sem animação de mergulho própria: mergulha com a de voo. */
    public CreatureAppearance(String model, String texture, String idle, String walk, String attack, String unconscious,
                              String fly, String run) {
        this(model, texture, idle, walk, attack, unconscious, fly, run, fly);
    }

    /** Espécie que não voa nem tem corrida própria: as duas são a animação de andar. */
    public CreatureAppearance(String model, String texture, String idle, String walk, String attack, String unconscious) {
        this(model, texture, idle, walk, attack, unconscious, walk, walk);
    }

    public CreatureAppearance(String model, String texture, String idle, String walk, String attack, String unconscious,
                              String fly) {
        this(model, texture, idle, walk, attack, unconscious, fly, walk);
    }

    private static final Map<String, CreatureAppearance> REVIVAL = Map.ofEntries(Map.entry("dodo",
                    new CreatureAppearance("dodo", "dodo/dodo_male.png", "idle", "walk", "attack_1", "sleep_1",
                            "walk", "run")),
            Map.entry("elasmotherium", new CreatureAppearance("elasmotherium", "elasmotherium/elasmotherium_male.png",
                    "idle", "walk", "attack", "sit/sleep", "walk", "run")),
            Map.entry("smilodon", new CreatureAppearance("smilodon", "smilodon/smilodon_male.png", "idle", "walk", "attack", "sleep", "walk", "sprint")),
            Map.entry("mammoth", new CreatureAppearance("mammoth", "mammoth/mammoth_male.png", "idle_1_90", "walk", "attack", "rest/sleep", "walk", "run")),
            Map.entry("tyrannosaurus", new CreatureAppearance("tyrannosaurus", "tyrannosaurus/tyrannosaurus_male.png",
                    "idle", "walk", "attack_normal_1", "sleep_1", "walk", "run")),
            Map.entry("velociraptor", new CreatureAppearance("velociraptor", "velociraptor/velociraptor_male.png",
                    "idle", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("utahraptor", new CreatureAppearance("deinonychus", "deinonychus/deinonychus_male.png",
                    "idle", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("spinosaurus", new CreatureAppearance("spinosaurus", "spinosaurus/spinosaurus_male.png",
                    "idle", "walk", "attack", "sleep")),
            Map.entry("allosaurus", new CreatureAppearance("allosaurus", "allosaurus/allosaurus_male.png",
                    "idle", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("brontosaurus", new CreatureAppearance("diplodocus", "diplodocus/diplodocus_male.png",
                    "idle", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("stegosaurus", new CreatureAppearance("stegosaurus", "stegosaurus/stegosaurus_male.png",
                    "idle", "walk", "attack_back_left", "sleep_1", "walk", "run")),
            Map.entry("gallimimus", new CreatureAppearance("gallimimus", "gallimimus/gallimimus_male.png",
                    "idle", "walk", "attack", "sleep/sit", "walk", "run")),
            Map.entry("pteranodon", new CreatureAppearance("pteranodon", "pteranodon/pteranodon_male.png",
                    "idle", "walk", "attack", "sleep", "fly", "walk", "dive"))
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
