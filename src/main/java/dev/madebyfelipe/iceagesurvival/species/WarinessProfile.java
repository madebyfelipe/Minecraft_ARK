package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Como uma espécie selvagem reage a ameaças ({@link ThreatResponse}). Bloco {@code wariness} do
 * comportamento; ausente = a espécie não liga para quem chega perto (só revida se ferida).
 *
 * @param threats       tag de tipos de entidade que ela teme, além dos jogadores (os predadores)
 * @param players       se jogadores são ameaça
 * @param chargeSpeed   modificador de velocidade da investida (e da perseguição de quem a feriu)
 * @param fleeSpeed     modificador de velocidade da fuga
 * @param knockback     empurrão horizontal do golpe de chifre/cabeça, em blocos/tick; 0 = golpe comum
 * @param lift          impulso para cima do mesmo golpe — o "jogar para o alto" do rinoceronte
 */
public record WarinessProfile(Optional<TagKey<EntityType<?>>> threats, boolean players, double alertRadius,
                              double chargeRadius, double bluffChance, double retreatChance, double chargeChance,
                              double sneakFactor, double calfRadius, double chargeSpeed, double fleeSpeed,
                              double knockback, double lift) {
    public static final Codec<WarinessProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.hashedCodec(Registries.ENTITY_TYPE).optionalFieldOf("threats").forGetter(WarinessProfile::threats),
            Codec.BOOL.optionalFieldOf("players", true).forGetter(WarinessProfile::players),
            Codec.doubleRange(1, 64).optionalFieldOf("alert_radius", 12.0).forGetter(WarinessProfile::alertRadius),
            Codec.doubleRange(0, 32).optionalFieldOf("charge_radius", 0.0).forGetter(WarinessProfile::chargeRadius),
            Codec.doubleRange(0, 1).optionalFieldOf("bluff_chance", 0.0).forGetter(WarinessProfile::bluffChance),
            Codec.doubleRange(0, 1).optionalFieldOf("retreat_chance", 0.0).forGetter(WarinessProfile::retreatChance),
            Codec.doubleRange(0, 1).optionalFieldOf("charge_chance", 0.0).forGetter(WarinessProfile::chargeChance),
            Codec.doubleRange(0, 1).optionalFieldOf("sneak_factor", 0.5).forGetter(WarinessProfile::sneakFactor),
            Codec.doubleRange(0, 64).optionalFieldOf("calf_radius", 12.0).forGetter(WarinessProfile::calfRadius),
            Codec.doubleRange(0.1, 5).optionalFieldOf("charge_speed", 1.6).forGetter(WarinessProfile::chargeSpeed),
            Codec.doubleRange(0.1, 5).optionalFieldOf("flee_speed", 1.4).forGetter(WarinessProfile::fleeSpeed),
            Codec.doubleRange(0, 5).optionalFieldOf("knockback", 0.0).forGetter(WarinessProfile::knockback),
            Codec.doubleRange(0, 3).optionalFieldOf("lift", 0.0).forGetter(WarinessProfile::lift)
    ).apply(instance, WarinessProfile::new));

    public ThreatResponse.Tuning tuning() {
        return new ThreatResponse.Tuning(alertRadius, chargeRadius, bluffChance, retreatChance, chargeChance,
                sneakFactor, calfRadius);
    }
}
