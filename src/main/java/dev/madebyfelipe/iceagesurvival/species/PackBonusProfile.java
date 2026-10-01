package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PackBonusProfile(int radius, int minimumAllies, double speedMultiplier, double attackMultiplier) {
    public static final Codec<PackBonusProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 64).fieldOf("radius").forGetter(PackBonusProfile::radius),
            Codec.intRange(1, 16).optionalFieldOf("minimum_allies", 1).forGetter(PackBonusProfile::minimumAllies),
            Codec.doubleRange(0, 2).fieldOf("speed_multiplier").forGetter(PackBonusProfile::speedMultiplier),
            Codec.doubleRange(0, 2).fieldOf("attack_multiplier").forGetter(PackBonusProfile::attackMultiplier)
    ).apply(instance, PackBonusProfile::new));
}
