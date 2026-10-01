package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Como a espécie vive no mundo selvagem: fome, caçada, rivais e temperamento. Bloco
 * {@code behavior.ecology} do JSON; tudo opcional.
 *
 * @param huntRadius    até onde o predador faminto procura presa (faro, não só vista), em blocos
 * @param hungerSeconds tempo desde a última refeição até sair caçando
 * @param chaseSeconds  quanto fôlego tem a perseguição; depois disso a presa escapou
 * @param rivals        espécies com que disputa território (inclusive a própria, se estiver na tag)
 * @param rivalRadius   a que distância um rival incomoda
 * @param nervousness   temperamento: multiplica o que estressa (dodô 1,8; T-Rex 0,4)
 */
public record EcologyProfile(double huntRadius, int hungerSeconds, int chaseSeconds,
                             Optional<TagKey<EntityType<?>>> rivals, double rivalRadius, double nervousness) {
    public static final EcologyProfile DEFAULT = new EcologyProfile(48.0, 360, 25, Optional.empty(), 32.0, 1.0);

    public static final Codec<EcologyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(4, 128).optionalFieldOf("hunt_radius", DEFAULT.huntRadius())
                    .forGetter(EcologyProfile::huntRadius),
            Codec.intRange(10, 7200).optionalFieldOf("hunger_seconds", DEFAULT.hungerSeconds())
                    .forGetter(EcologyProfile::hungerSeconds),
            Codec.intRange(3, 300).optionalFieldOf("chase_seconds", DEFAULT.chaseSeconds())
                    .forGetter(EcologyProfile::chaseSeconds),
            TagKey.hashedCodec(Registries.ENTITY_TYPE).optionalFieldOf("rivals").forGetter(EcologyProfile::rivals),
            Codec.doubleRange(4, 128).optionalFieldOf("rival_radius", DEFAULT.rivalRadius())
                    .forGetter(EcologyProfile::rivalRadius),
            Codec.doubleRange(0, 5).optionalFieldOf("nervousness", DEFAULT.nervousness())
                    .forGetter(EcologyProfile::nervousness)
    ).apply(instance, EcologyProfile::new));
}
