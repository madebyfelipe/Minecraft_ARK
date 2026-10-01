package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
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
 * @param migrates           líder faz viagens longas e o restante da manada o acompanha
 * @param prey               tag de tipos de entidade que ela caça
 * @param huntStyle          {@code chase}: vai direto no alvo; {@code stalk}: espreita e só dá o bote
 *                           quando chega perto ou quando o alvo a vê
 * @param satedSeconds       depois de abater uma presa, o predador passa este tempo sem caçar
 * @param huntsPlayers       se, faminto, o predador também vê o jogador como presa; falso no
 *                           Velociraptor, pequeno demais para caçar gente
 * @param wariness           reação a ameaças (lutar ou fugir); ausente = ignora quem chega perto
 * @param ecology            fome, raio de caça, rivais e temperamento
 */
public record BehaviorProfile(
        boolean aggressive,
        double aggroRadius,
        int territoryRadius,
        double fleeHealthFraction,
        int herdRadius,
        boolean groupDefense,
        boolean migrates,
        Optional<TagKey<EntityType<?>>> prey,
        HuntStyle huntStyle,
        int satedSeconds,
        boolean huntsPlayers,
        Optional<WarinessProfile> wariness,
        EcologyProfile ecology) {

    public enum HuntStyle implements StringRepresentable {
        CHASE("chase"),
        STALK("stalk");

        public static final Codec<HuntStyle> CODEC = StringRepresentable.fromEnum(HuntStyle::values);
        private final String id;

        HuntStyle(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Espécie sem bloco de comportamento: passiva, solitária, sem território. */
    public static final BehaviorProfile PASSIVE =
            new BehaviorProfile(false, 16.0, 0, 0.0, 0, false, false, Optional.empty(), HuntStyle.CHASE, 180,
                    true, Optional.empty(), EcologyProfile.DEFAULT);

    public static final Codec<BehaviorProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("aggressive", PASSIVE.aggressive()).forGetter(BehaviorProfile::aggressive),
            Codec.doubleRange(1, 128).optionalFieldOf("aggro_radius", PASSIVE.aggroRadius()).forGetter(BehaviorProfile::aggroRadius),
            Codec.intRange(0, 512).optionalFieldOf("territory_radius", PASSIVE.territoryRadius()).forGetter(BehaviorProfile::territoryRadius),
            Codec.doubleRange(0, 1).optionalFieldOf("flee_health_fraction", PASSIVE.fleeHealthFraction()).forGetter(BehaviorProfile::fleeHealthFraction),
            Codec.intRange(0, 64).optionalFieldOf("herd_radius", PASSIVE.herdRadius()).forGetter(BehaviorProfile::herdRadius),
            Codec.BOOL.optionalFieldOf("group_defense", PASSIVE.groupDefense()).forGetter(BehaviorProfile::groupDefense),
            Codec.BOOL.optionalFieldOf("migrates", PASSIVE.migrates()).forGetter(BehaviorProfile::migrates),
            TagKey.hashedCodec(Registries.ENTITY_TYPE).optionalFieldOf("prey").forGetter(BehaviorProfile::prey),
            HuntStyle.CODEC.optionalFieldOf("hunt_style", HuntStyle.CHASE).forGetter(BehaviorProfile::huntStyle),
            Codec.intRange(0, 3600).optionalFieldOf("sated_seconds", PASSIVE.satedSeconds())
                    .forGetter(BehaviorProfile::satedSeconds),
            Codec.BOOL.optionalFieldOf("hunts_players", true).forGetter(BehaviorProfile::huntsPlayers),
            WarinessProfile.CODEC.optionalFieldOf("wariness").forGetter(BehaviorProfile::wariness),
            EcologyProfile.CODEC.optionalFieldOf("ecology", EcologyProfile.DEFAULT).forGetter(BehaviorProfile::ecology)
    ).apply(instance, BehaviorProfile::new));
}
