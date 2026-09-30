package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Montaria de uma espécie. A ausência do bloco {@code mount} no JSON significa que a
 * espécie não pode ser montada — é assim que o lobo-terrível fica de fora.
 *
 * @param seatHeight      altura do assento em blocos a partir dos pés; 0 = 85% da altura da colisão
 * @param seatForward     quanto o assento fica à frente do centro, em blocos (à frente da vela do espinossauro)
 * @param minAffinity     afinidade mínima para deixar montar (0 a {@code MAX_AFFINITY})
 * @param speedMultiplier multiplicador da velocidade quando montada
 * @param jumpStrength     impulso de pulo em blocos/tick; 0 = não pula
 * @param breakHardness    dureza máxima dos blocos que a mordida de quem monta quebra; 0 = não quebra
 *                         (terra 0,5; pedra 1,5; tronco e pedregulho 2)
 * @param breakBlocks      se presente, a mordida só quebra blocos desta tag — é o que faz do mamute um
 *                         coletor de madeira sem que ele cave pedra
 */
public record MountProfile(double seatHeight, double seatForward, float minAffinity, double speedMultiplier, double jumpStrength,
                           float breakHardness, Optional<TagKey<Block>> breakBlocks) {
    public static final MountProfile DEFAULT = new MountProfile(0.0, 0.0, 25.0F, 1.0, 0.5, 0.0F, Optional.empty());

    public static final Codec<MountProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 16).optionalFieldOf("seat_height", DEFAULT.seatHeight())
                    .forGetter(MountProfile::seatHeight),
            Codec.doubleRange(-16, 16).optionalFieldOf("seat_forward", DEFAULT.seatForward())
                    .forGetter(MountProfile::seatForward),
            Codec.floatRange(0, 100).optionalFieldOf("min_affinity", DEFAULT.minAffinity())
                    .forGetter(MountProfile::minAffinity),
            Codec.doubleRange(0.1, 5).optionalFieldOf("speed_multiplier", DEFAULT.speedMultiplier())
                    .forGetter(MountProfile::speedMultiplier),
            Codec.doubleRange(0, 2).optionalFieldOf("jump_strength", DEFAULT.jumpStrength())
                    .forGetter(MountProfile::jumpStrength),
            Codec.floatRange(0, 50).optionalFieldOf("break_hardness", DEFAULT.breakHardness())
                    .forGetter(MountProfile::breakHardness),
            TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("break_blocks")
                    .forGetter(MountProfile::breakBlocks)
    ).apply(instance, MountProfile::new));

    /** Altura do assento para uma criatura com esta caixa de colisão. */
    public double seatHeight(float collisionHeight) {
        return seatHeight > 0 ? seatHeight : collisionHeight * 0.85;
    }
}
