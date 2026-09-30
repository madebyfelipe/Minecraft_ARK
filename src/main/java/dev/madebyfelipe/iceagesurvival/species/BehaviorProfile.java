package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Como uma espécie selvagem se comporta.
 *
 * @param aggressive         ataca jogadores que entram no raio de percepção
 * @param aggroRadius        raio em que percebe e até onde mantém um alvo
 * @param territoryRadius    raio do território em torno de onde nasceu; não vagueia nem
 *                           persegue além dele. 0 = sem território
 * @param fleeHealthFraction abaixo desta fração de vida, recua de quem a feriu. 0 = nunca recua
 */
public record BehaviorProfile(boolean aggressive, double aggroRadius, int territoryRadius, double fleeHealthFraction) {
    public static final Codec<BehaviorProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("aggressive", false).forGetter(BehaviorProfile::aggressive),
            Codec.doubleRange(1, 128).optionalFieldOf("aggro_radius", 16.0).forGetter(BehaviorProfile::aggroRadius),
            Codec.intRange(0, 512).optionalFieldOf("territory_radius", 0).forGetter(BehaviorProfile::territoryRadius),
            Codec.doubleRange(0, 1).optionalFieldOf("flee_health_fraction", 0.0).forGetter(BehaviorProfile::fleeHealthFraction)
    ).apply(instance, BehaviorProfile::new));
}
