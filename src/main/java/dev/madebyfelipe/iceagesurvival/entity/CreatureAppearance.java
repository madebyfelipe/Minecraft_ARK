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

    /** Modelos do Revival cujas animações não têm o prefixo {@code animation.<modelo>.}. */
    private static final java.util.Set<String> UNPREFIXED_ANIMATIONS = java.util.Set.of("quetzalcoatlus");

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
            Map.entry("triceratops", new CreatureAppearance("triceratops", "triceratops/triceratops_male.png",
                    "idle", "walk", "attack_1", "sleep_1", "walk", "run")),
            Map.entry("kelenken", new CreatureAppearance("kelenken", "kelenken/kelenken_male.png",
                    "idle_1", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("ornitholestes", new CreatureAppearance("ornitholestes", "ornitholestes/ornitholestes_male.png",
                    "idle", "walk", "attack", "sleep", "walk", "run")),
            Map.entry("pteranodon", new CreatureAppearance("pteranodon", "pteranodon/pteranodon_male.png",
                    "idle", "walk", "attack", "sleep", "fly", "walk", "dive")),
            Map.entry("quetzalcoatlus", new CreatureAppearance("quetzalcoatlus",
                    "quetzalcoatlus/quetzalcoatlus_male.png", "idle", "walk", "attack", "sleep", "fly", "run", "dive")),
            Map.entry("megalania", new CreatureAppearance("megalania", "megalania/megalania_male.png",
                    "idle", "walk", "attack_1", "sleep", "walk", "run")),
            // O golpe é a cauda: a clavada para trás (TailClubStrike).
            Map.entry("ankylosaurus", new CreatureAppearance("ankylosaurus", "ankylosaurus/ankylosaurus_male.png",
                    "idle", "walk", "attack_back_right", "sleep", "walk", "run"))
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

    /**
     * O prefixo dos nomes das animações no arquivo do Revival: {@code animation.<modelo>.}, menos nos modelos em que
     * elas vêm sem prefixo ({@code fly}, {@code idle}…), como o do Quetzalcoatlus.
     */
    public String animationPrefix() {
        return UNPREFIXED_ANIMATIONS.contains(model) ? "" : "animation." + model + ".";
    }

    private static ResourceLocation revivalResource(String path) {
        return ResourceLocation.fromNamespaceAndPath("fossil", path);
    }
}
