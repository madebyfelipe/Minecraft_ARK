package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Características físicas de uma espécie.
 *
 * @param knockbackResistance de 0 a 1; espécies maiores que o jogador usam 1 (um golpe não as arremessa)
 * @param stepHeight          altura que sobe sem pular, em blocos; animais grandes usam 1 ou mais
 * @param breaksLeaves        atravessa copas de árvore destruindo as folhas, em vez de ficar presa
 * @param plowHardness        ao esbarrar, quebra blocos da superfície da tag {@code iceagesurvival:plowable}
 *                            (troncos, folhas, plantas, neve) com dureza até este valor; 0 = não quebra.
 *                            É o que impede os grandes predadores de ficarem presos no mato
 */
public record BodyProfile(double knockbackResistance, double stepHeight, boolean breaksLeaves, float plowHardness) {
    public static final BodyProfile DEFAULT = new BodyProfile(0.0, 0.6, false, 0.0F);

    public static final Codec<BodyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("knockback_resistance", DEFAULT.knockbackResistance())
                    .forGetter(BodyProfile::knockbackResistance),
            Codec.doubleRange(0, 10).optionalFieldOf("step_height", DEFAULT.stepHeight())
                    .forGetter(BodyProfile::stepHeight),
            Codec.BOOL.optionalFieldOf("breaks_leaves", DEFAULT.breaksLeaves()).forGetter(BodyProfile::breaksLeaves),
            Codec.floatRange(0, 50).optionalFieldOf("plow_hardness", DEFAULT.plowHardness())
                    .forGetter(BodyProfile::plowHardness)
    ).apply(instance, BodyProfile::new));
}
