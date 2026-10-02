package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.madebyfelipe.iceagesurvival.core.ecology.Fishing;

/**
 * O pescador ({@code behavior.habits.fishing}; regras em {@link Fishing}). Bloco ausente = a espécie não pesca.
 *
 * <pre>"fishing": { "chance": 0.35, "radius": 24, "tame_radius": 6 }</pre>
 *
 * @param chance     chance de apanhar um peixe por bote quando não há peixe vivo a até
 *                   {@link Fishing#LIVE_FISH_REACH} blocos (com peixe vivo ali, a captura é certa)
 * @param radius     selvagem, com fome, procura água pescável a até esta distância
 * @param tameRadius domesticada com a ordem "Parar", pesca na água a até esta distância de onde parou
 */
public record FishingProfile(double chance, double radius, double tameRadius) {
    public static final Codec<FishingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("chance", 0.35).forGetter(FishingProfile::chance),
            Codec.doubleRange(4, 64).optionalFieldOf("radius", 24.0).forGetter(FishingProfile::radius),
            Codec.doubleRange(1, 32).optionalFieldOf("tame_radius", Fishing.DEFAULT_TAME_RADIUS)
                    .forGetter(FishingProfile::tameRadius)
    ).apply(instance, FishingProfile::new));
}
