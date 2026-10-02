package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.madebyfelipe.iceagesurvival.core.ecology.Activity;
import dev.madebyfelipe.iceagesurvival.core.ecology.Camouflage;
import java.util.Locale;
import java.util.Optional;

/**
 * Hábitos da espécie fora da caçada e da luta. Bloco {@code behavior.habits} do JSON; tudo opcional.
 *
 * @param activity       horário: {@code always} (padrão), {@code nocturnal} (dorme escondido de dia) ou
 *                       {@code diurnal} ({@link Activity})
 * @param camouflage     escondida no sub-bosque, quem a procura a nota a esta fração do raio; 1 = sem camuflagem
 *                       ({@link Camouflage})
 * @param scavenges      com fome, come carne crua largada no chão ({@code core/ecology/Carrion})
 * @param sentinelRadius domesticada, avisa o dono do predador selvagem a até este raio; 0 = não avisa
 *                       ({@code core/ecology/Sentinel})
 * @param fishing        pescador: vadeia a água rasa e apanha peixe ({@link FishingProfile}); ausente = não pesca
 */
public record HabitsProfile(Activity.Pattern activity, double camouflage, boolean scavenges, double sentinelRadius,
                            Optional<FishingProfile> fishing) {
    public static final HabitsProfile DEFAULT =
            new HabitsProfile(Activity.Pattern.ALWAYS, Camouflage.NONE, false, 0.0, Optional.empty());

    public HabitsProfile(Activity.Pattern activity, double camouflage, boolean scavenges, double sentinelRadius) {
        this(activity, camouflage, scavenges, sentinelRadius, Optional.empty());
    }

    private static final Codec<Activity.Pattern> ACTIVITY_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return com.mojang.serialization.DataResult.success(
                            Activity.Pattern.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return com.mojang.serialization.DataResult.error(() -> "activity desconhecida: " + name);
                }
            },
            pattern -> pattern.name().toLowerCase(Locale.ROOT));

    public static final Codec<HabitsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ACTIVITY_CODEC.optionalFieldOf("activity", DEFAULT.activity()).forGetter(HabitsProfile::activity),
            Codec.doubleRange(0.1, 1).optionalFieldOf("camouflage", DEFAULT.camouflage())
                    .forGetter(HabitsProfile::camouflage),
            Codec.BOOL.optionalFieldOf("scavenges", DEFAULT.scavenges()).forGetter(HabitsProfile::scavenges),
            Codec.doubleRange(0, 64).optionalFieldOf("sentinel_radius", DEFAULT.sentinelRadius())
                    .forGetter(HabitsProfile::sentinelRadius),
            FishingProfile.CODEC.optionalFieldOf("fishing").forGetter(HabitsProfile::fishing)
    ).apply(instance, HabitsProfile::new));
}
