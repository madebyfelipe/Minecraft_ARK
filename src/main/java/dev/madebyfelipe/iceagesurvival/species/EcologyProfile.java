package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Como a espécie vive no mundo selvagem: fome, caçada, disputa reprodutiva e temperamento. Bloco
 * {@code behavior.ecology} do JSON; tudo opcional.
 *
 * @param huntRadius    até onde o predador faminto procura presa (faro, não só vista), em blocos
 * @param hungerSeconds tempo desde a última refeição até sair caçando
 * @param chaseSeconds  quanto fôlego tem a perseguição; depois disso a presa escapou
 * @param rivalRadius   a que distância machos adultos da mesma espécie disputam uma fêmea
 * @param nervousness   temperamento: multiplica o que estressa (dodô 1,8; T-Rex 0,4)
 * @param carcassPortions porções de carne da carcaça que fica quando morre selvagem sem jogador; 0 = não deixa
 *                        carcaça (presa pequena). Pelo peso do animal real: Galimimo 6, Tricerátopo 30, Bronto 60
 */
public record EcologyProfile(double huntRadius, int hungerSeconds, int chaseSeconds,
                             double rivalRadius, double nervousness, int carcassPortions) {
    public static final EcologyProfile DEFAULT = new EcologyProfile(48.0, 360, 25, 32.0, 1.0, 0);

    public static final Codec<EcologyProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(4, 128).optionalFieldOf("hunt_radius", DEFAULT.huntRadius())
                    .forGetter(EcologyProfile::huntRadius),
            Codec.intRange(10, 7200).optionalFieldOf("hunger_seconds", DEFAULT.hungerSeconds())
                    .forGetter(EcologyProfile::hungerSeconds),
            Codec.intRange(3, 300).optionalFieldOf("chase_seconds", DEFAULT.chaseSeconds())
                    .forGetter(EcologyProfile::chaseSeconds),
            Codec.doubleRange(4, 128).optionalFieldOf("rival_radius", DEFAULT.rivalRadius())
                    .forGetter(EcologyProfile::rivalRadius),
            Codec.doubleRange(0, 5).optionalFieldOf("nervousness", DEFAULT.nervousness())
                    .forGetter(EcologyProfile::nervousness),
            Codec.intRange(0, 200).optionalFieldOf("carcass_portions", DEFAULT.carcassPortions())
                    .forGetter(EcologyProfile::carcassPortions)
    ).apply(instance, EcologyProfile::new));
}
