package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StorageProfile(int slots) {
    public static final Codec<StorageProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(9, 216).fieldOf("slots").forGetter(StorageProfile::slots)
    ).apply(instance, StorageProfile::new));
}
