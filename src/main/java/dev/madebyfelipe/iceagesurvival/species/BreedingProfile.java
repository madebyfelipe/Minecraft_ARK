package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/**
 * Reprodução de uma espécie. Sem o bloco {@code breeding} no JSON a espécie não se reproduz.
 *
 * @param offspring          {@code egg}: a fêmea põe um ovo, chocado na incubadora;
 *                           {@code live}: gestação na própria fêmea, e o filhote nasce dela
 * @param incubationSeconds  tempo para o ovo chocar na incubadora, ou de gestação
 * @param maturationSeconds  tempo de filhote até adulto
 * @param cooldownSeconds    espera da fêmea entre duas crias
 */
public record BreedingProfile(Offspring offspring, int incubationSeconds, int maturationSeconds, int cooldownSeconds) {
    public enum Offspring implements StringRepresentable {
        EGG("egg"),
        LIVE("live");

        public static final Codec<Offspring> CODEC = StringRepresentable.fromEnum(Offspring::values);
        private final String id;

        Offspring(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final Codec<BreedingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Offspring.CODEC.fieldOf("offspring").forGetter(BreedingProfile::offspring),
            Codec.intRange(1, 86_400).optionalFieldOf("incubation_seconds", 600).forGetter(BreedingProfile::incubationSeconds),
            Codec.intRange(1, 864_000).optionalFieldOf("maturation_seconds", 3600).forGetter(BreedingProfile::maturationSeconds),
            Codec.intRange(0, 86_400).optionalFieldOf("cooldown_seconds", 1200).forGetter(BreedingProfile::cooldownSeconds)
    ).apply(instance, BreedingProfile::new));
}
