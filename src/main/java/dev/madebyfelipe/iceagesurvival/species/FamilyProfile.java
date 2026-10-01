package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Família no spawn de uma espécie solitária: a maioria nasce sozinha; às vezes uma mãe com
 * filhote; mais raro, o casal com o filhote.
 *
 * @param calfChance chance de um adulto selvagem nascer acompanhado de um filhote
 * @param mateChance com filhote, chance de o outro adulto também estar junto
 */
public record FamilyProfile(double calfChance, double mateChance) {
    public static final Codec<FamilyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("calf_chance", 0.0).forGetter(FamilyProfile::calfChance),
            Codec.doubleRange(0, 1).optionalFieldOf("mate_chance", 0.0).forGetter(FamilyProfile::mateChance)
    ).apply(instance, FamilyProfile::new));
}
