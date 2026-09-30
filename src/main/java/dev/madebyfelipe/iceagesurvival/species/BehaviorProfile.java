package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Como uma espécie selvagem se comporta.
 *
 * @param aggressive         ataca jogadores que entram no raio de percepção
 * @param aggroRadius        raio em que percebe e até onde mantém um alvo
 * @param territoryRadius    raio do território em torno de onde nasceu; não vagueia nem
 *                           persegue além dele. 0 = sem território
 * @param fleeHealthFraction abaixo desta fração de vida, recua de quem a feriu. 0 = nunca recua,
 *                           1 = foge assim que é ferida
 * @param herdRadius         distância máxima que se afasta do líder da manada. 0 = solitária
 * @param groupDefense       quando uma é atacada, as outras da mesma espécie por perto revidam juntas
 * @param prey               tag de tipos de entidade que ela caça
 */
public record BehaviorProfile(
        boolean aggressive,
        double aggroRadius,
        int territoryRadius,
        double fleeHealthFraction,
        int herdRadius,
        boolean groupDefense,
        Optional<TagKey<EntityType<?>>> prey) {

    /** Espécie sem bloco de comportamento: passiva, solitária, sem território. */
    public static final BehaviorProfile PASSIVE = new BehaviorProfile(false, 16.0, 0, 0.0, 0, false, Optional.empty());

    public static final Codec<BehaviorProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("aggressive", PASSIVE.aggressive()).forGetter(BehaviorProfile::aggressive),
            Codec.doubleRange(1, 128).optionalFieldOf("aggro_radius", PASSIVE.aggroRadius()).forGetter(BehaviorProfile::aggroRadius),
            Codec.intRange(0, 512).optionalFieldOf("territory_radius", PASSIVE.territoryRadius()).forGetter(BehaviorProfile::territoryRadius),
            Codec.doubleRange(0, 1).optionalFieldOf("flee_health_fraction", PASSIVE.fleeHealthFraction()).forGetter(BehaviorProfile::fleeHealthFraction),
            Codec.intRange(0, 64).optionalFieldOf("herd_radius", PASSIVE.herdRadius()).forGetter(BehaviorProfile::herdRadius),
            Codec.BOOL.optionalFieldOf("group_defense", PASSIVE.groupDefense()).forGetter(BehaviorProfile::groupDefense),
            TagKey.hashedCodec(Registries.ENTITY_TYPE).optionalFieldOf("prey").forGetter(BehaviorProfile::prey)
    ).apply(instance, BehaviorProfile::new));
}
