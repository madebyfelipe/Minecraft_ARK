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
 * @param huntStyle          {@code stalk} (padrão de todo carnívoro): ronda a manada sem ser notado e
 *                           só dispara na hora certa; {@code chase}: vai direto no alvo
 * @param satedSeconds       depois de abater uma presa, o predador passa este tempo sem caçar
 * @param diet               tabela de preferência de presas ({@link DietEntry}); a primeira linha que
 *                           casa vale, e presa da tag fora da tabela tem preferência 1
 * @param wariness           reação a ameaças (lutar ou fugir); ausente = ignora quem chega perto
 * @param ecology            fome, raio de caça, rivais e temperamento
 * @param huntSpecial        o golpe próprio do caçador: {@code ambush} (Smilodon), {@code pack_leap} (Utahraptor),
 *                           {@code beak_strike} (ave-terrível), {@code grab} (Ornitholestes), {@code gaff} (Baryonyx),
 *                           {@code swallow} (Quetzalcoatlus), {@code venom} (Megalania); {@code none} por padrão
 * @param habits             horário, camuflagem, carniça, sentinela e pesca ({@link HabitsProfile})
 * @param fearsWater         selvagem, tem pavor de água: não entra nem segue presa para dentro dela e, se cair, nada
 *                           direto para a margem (Smilodon)
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
        java.util.List<DietEntry> diet,
        Optional<WarinessProfile> wariness,
        EcologyProfile ecology,
        HuntSpecial huntSpecial,
        HabitsProfile habits,
        boolean fearsWater) {

    /** O golpe próprio de cada caçador (ver {@code core/ecology/HuntSpecials}). */
    public enum HuntSpecial implements StringRepresentable {
        /** Nenhum. */
        NONE("none"),
        /** Emboscada: arrancada curta no bote e o primeiro golpe agarra (a presa fica lenta). */
        AMBUSH("ambush"),
        /** Salto: no bote, pula sobre a presa a média distância. */
        PACK_LEAP("pack_leap"),
        /** Bicada: parte do dano ignora armadura, e depois de acertar recua um instante (golpe e recua). */
        BEAK_STRIKE("beak_strike"),
        /** Agarrão: sem arrancada; o primeiro golpe do bote prende a presa pequena (a presa fica lenta). */
        GRAB("grab"),
        /**
         * Garra-gancho: o primeiro golpe depois do bote (da caçada ou da pesca) fisga a presa na água, ou a do porte do
         * caçador para baixo, puxa-a para perto e a prende (a presa fica lenta).
         */
        GAFF("gaff"),
        /**
         * Engole inteira (Quetzalcoatlus, caçador a pé como a cegonha): o golpe na presa pequena a engole de uma vez
         * ({@code entity/SwallowStrike}).
         */
        SWALLOW("swallow"),
        /**
         * Peçonha (Megalania): morde, solta e segue o rastro da presa envenenada até ela cair
         * ({@code entity/VenomBite}).
         */
        VENOM("venom");

        public static final Codec<HuntSpecial> CODEC = StringRepresentable.fromEnum(HuntSpecial::values);
        private final String id;

        HuntSpecial(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * Se o jogador é presa: só quando está na tabela de dieta ({@code minecraft:player}). O jogador é um
     * animal como os outros — o Velociraptor, pequeno demais, não o tem na tabela.
     */
    public boolean huntsPlayers() {
        return diet.stream().anyMatch(entry -> entry.prey().contains(EntityType.PLAYER.builtInRegistryHolder()));
    }

    /** Preferência pela presa deste tipo, pela tabela de dieta. */
    public int preference(EntityType<?> type) {
        for (DietEntry entry : diet) {
            if (entry.prey().contains(type.builtInRegistryHolder())) {
                return entry.preference();
            }
        }
        return DietEntry.DEFAULT_PREFERENCE;
    }

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
            new BehaviorProfile(false, 16.0, 0, 0.0, 0, false, false, Optional.empty(), HuntStyle.STALK, 180,
                    java.util.List.of(), Optional.empty(), EcologyProfile.DEFAULT, HuntSpecial.NONE,
                    HabitsProfile.DEFAULT, false);

    public static final Codec<BehaviorProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("aggressive", PASSIVE.aggressive()).forGetter(BehaviorProfile::aggressive),
            Codec.doubleRange(1, 128).optionalFieldOf("aggro_radius", PASSIVE.aggroRadius()).forGetter(BehaviorProfile::aggroRadius),
            Codec.intRange(0, 512).optionalFieldOf("territory_radius", PASSIVE.territoryRadius()).forGetter(BehaviorProfile::territoryRadius),
            Codec.doubleRange(0, 1).optionalFieldOf("flee_health_fraction", PASSIVE.fleeHealthFraction()).forGetter(BehaviorProfile::fleeHealthFraction),
            Codec.intRange(0, 64).optionalFieldOf("herd_radius", PASSIVE.herdRadius()).forGetter(BehaviorProfile::herdRadius),
            Codec.BOOL.optionalFieldOf("group_defense", PASSIVE.groupDefense()).forGetter(BehaviorProfile::groupDefense),
            Codec.BOOL.optionalFieldOf("migrates", PASSIVE.migrates()).forGetter(BehaviorProfile::migrates),
            TagKey.hashedCodec(Registries.ENTITY_TYPE).optionalFieldOf("prey").forGetter(BehaviorProfile::prey),
            HuntStyle.CODEC.optionalFieldOf("hunt_style", PASSIVE.huntStyle()).forGetter(BehaviorProfile::huntStyle),
            Codec.intRange(0, 3600).optionalFieldOf("sated_seconds", PASSIVE.satedSeconds())
                    .forGetter(BehaviorProfile::satedSeconds),
            DietEntry.CODEC.listOf().optionalFieldOf("diet", java.util.List.of()).forGetter(BehaviorProfile::diet),
            WarinessProfile.CODEC.optionalFieldOf("wariness").forGetter(BehaviorProfile::wariness),
            EcologyProfile.CODEC.optionalFieldOf("ecology", EcologyProfile.DEFAULT).forGetter(BehaviorProfile::ecology),
            HuntSpecial.CODEC.optionalFieldOf("hunt_special", HuntSpecial.NONE).forGetter(BehaviorProfile::huntSpecial),
            HabitsProfile.CODEC.optionalFieldOf("habits", HabitsProfile.DEFAULT).forGetter(BehaviorProfile::habits),
            Codec.BOOL.optionalFieldOf("fears_water", false).forGetter(BehaviorProfile::fearsWater)
    ).apply(instance, BehaviorProfile::new));
}
