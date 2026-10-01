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
 * @param bodyHeat            proteção contra o frio que o corpo da criatura dá, na escala do frio
 *                            (§15 do CLOUD.md): inteira para quem monta, caindo até 0 na borda de
 *                            {@code bodyHeatRadius} para quem está perto. 0 = não aquece
 * @param bodyHeatRadius      alcance do calor do corpo, em blocos a partir da colisão
 */
public record BodyProfile(double knockbackResistance, double stepHeight, boolean breaksLeaves, float plowHardness,
                          double bodyHeat, double bodyHeatRadius) {
    public static final BodyProfile DEFAULT = new BodyProfile(0.0, 0.6, false, 0.0F, 0.0, 4.0);

    public static final Codec<BodyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("knockback_resistance", DEFAULT.knockbackResistance())
                    .forGetter(BodyProfile::knockbackResistance),
            Codec.doubleRange(0, 10).optionalFieldOf("step_height", DEFAULT.stepHeight())
                    .forGetter(BodyProfile::stepHeight),
            Codec.BOOL.optionalFieldOf("breaks_leaves", DEFAULT.breaksLeaves()).forGetter(BodyProfile::breaksLeaves),
            Codec.floatRange(0, 50).optionalFieldOf("plow_hardness", DEFAULT.plowHardness())
                    .forGetter(BodyProfile::plowHardness),
            Codec.doubleRange(0, 2).optionalFieldOf("body_heat", DEFAULT.bodyHeat()).forGetter(BodyProfile::bodyHeat),
            Codec.doubleRange(0, 16).optionalFieldOf("body_heat_radius", DEFAULT.bodyHeatRadius())
                    .forGetter(BodyProfile::bodyHeatRadius)
    ).apply(instance, BodyProfile::new));
}
