package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Família no spawn de uma espécie solitária: a maioria nasce sozinha; às vezes uma mãe com
 * filhote; mais raro, o casal com o filhote. Sem filhote, {@code pairChance} de nascer em casal.
 *
 * @param calfChance chance de um adulto selvagem nascer acompanhado de um filhote
 * @param mateChance com filhote, chance de o outro adulto também estar junto
 * @param pairChance sem filhote, chance de nascer junto do parceiro do outro sexo — o casal não
 *                   disputa território, que só acontece entre machos
 */
public record FamilyProfile(double calfChance, double mateChance, double pairChance) {
    public FamilyProfile(double calfChance, double mateChance) {
        this(calfChance, mateChance, 0.0);
    }

    public static final Codec<FamilyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("calf_chance", 0.0).forGetter(FamilyProfile::calfChance),
            Codec.doubleRange(0, 1).optionalFieldOf("mate_chance", 0.0).forGetter(FamilyProfile::mateChance),
            Codec.doubleRange(0, 1).optionalFieldOf("pair_chance", 0.0).forGetter(FamilyProfile::pairChance)
    ).apply(instance, FamilyProfile::new));
}
