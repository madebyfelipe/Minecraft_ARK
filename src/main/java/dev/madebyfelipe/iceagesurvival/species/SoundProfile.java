package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sons de uma espécie, por id de evento de {@code sounds.json}. Cada campo é opcional; sem
 * ele a criatura fica muda naquela situação. Os eventos não passam pelo registro de sons:
 * o id vai direto no pacote e o cliente resolve pelo {@code sounds.json}.
 *
 * @param ambient som ocioso, de tempos em tempos
 * @param hurt    ao levar dano
 * @param death   ao morrer
 * @param alert   ao escolher um alvo (o rugido do T-Rex)
 * @param volume  volume de todos os sons da espécie; criaturas grandes se ouvem de mais longe
 */
public record SoundProfile(
        Optional<ResourceLocation> ambient,
        Optional<ResourceLocation> hurt,
        Optional<ResourceLocation> death,
        Optional<ResourceLocation> alert,
        float volume) {
    public static final Codec<SoundProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("ambient").forGetter(SoundProfile::ambient),
            ResourceLocation.CODEC.optionalFieldOf("hurt").forGetter(SoundProfile::hurt),
            ResourceLocation.CODEC.optionalFieldOf("death").forGetter(SoundProfile::death),
            ResourceLocation.CODEC.optionalFieldOf("alert").forGetter(SoundProfile::alert),
            Codec.floatRange(0, 16).optionalFieldOf("volume", 1.0F).forGetter(SoundProfile::volume)
    ).apply(instance, SoundProfile::new));

    public static SoundEvent event(ResourceLocation id) {
        return SoundEvent.createVariableRangeEvent(id);
    }
}
