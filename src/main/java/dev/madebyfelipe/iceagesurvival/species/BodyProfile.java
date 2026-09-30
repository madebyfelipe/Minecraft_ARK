package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Características físicas de uma espécie.
 *
 * @param knockbackResistance de 0 a 1; espécies maiores que o jogador usam 1 (um golpe não as arremessa)
 * @param stepHeight          altura que sobe sem pular, em blocos; animais grandes usam 1 ou mais
 * @param breaksLeaves        atravessa copas de árvore destruindo as folhas, em vez de ficar presa
 */
public record BodyProfile(double knockbackResistance, double stepHeight, boolean breaksLeaves) {
    public static final BodyProfile DEFAULT = new BodyProfile(0.0, 0.6, false);

    public static final Codec<BodyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("knockback_resistance", DEFAULT.knockbackResistance())
                    .forGetter(BodyProfile::knockbackResistance),
            Codec.doubleRange(0, 10).optionalFieldOf("step_height", DEFAULT.stepHeight())
                    .forGetter(BodyProfile::stepHeight),
            Codec.BOOL.optionalFieldOf("breaks_leaves", DEFAULT.breaksLeaves()).forGetter(BodyProfile::breaksLeaves)
    ).apply(instance, BodyProfile::new));
}
