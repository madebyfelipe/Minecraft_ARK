package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Como a espécie aguenta as armas de fogo do estilo Dino Crisis 2 ({@code core/firearms/Ballistics}, D58). Bloco
 * {@code firearm} do JSON; ausente = a escala do raptor ({@code Ballistics.GENERIC_DC2_PER_HEALTH}).
 *
 * @param dc2Health vida do bicho equivalente no DC2 (Normal): 400 Compsognathus/Oviraptor, 800 raptor marrom, 1000
 *                  Oviraptor nível 2, 1600 raptor vermelho, 2300 Inostrancevia, 5000 Alossauro, 10 000 o porte do
 *                  Rex (proposto; no DC2 ele não morre)
 * @param armored   tem o couro que segura 80% das armas leves (o Alossauro do DC2)
 */
public record FirearmProfile(double dc2Health, boolean armored) {
    public static final Codec<FirearmProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, 1_000_000).fieldOf("dc2_health").forGetter(FirearmProfile::dc2Health),
            Codec.BOOL.optionalFieldOf("armored", false).forGetter(FirearmProfile::armored)
    ).apply(instance, FirearmProfile::new));
}
