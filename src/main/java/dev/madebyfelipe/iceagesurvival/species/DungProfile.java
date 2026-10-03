package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Esterco ({@code iceagesurvival:dung}, que adubo como farinha de osso). Bloco {@code dung} do JSON; ausente = a
 * espécie não deixa esterco. O Anquilossauro, "barriga grande" (<i>magniventris</i>), fermentava a vegetação rasteira
 * no intestino: quanto mais come, mais esterco.
 *
 * @param intervalSeconds de quanto em quanto tempo o adulto acordado deixa um esterco sozinho, selvagem ou domesticado
 *                        (o pasto que o mod não simula); 0 = só pelo que come
 * @param foodPerDung     valor de alimento (o {@code value} da domesticação) que vira um esterco: o que ele come
 *                        domesticando, alimentado pelo dono ou do próprio inventário
 */
public record DungProfile(int intervalSeconds, double foodPerDung) {
    public static final Codec<DungProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("interval_seconds", 600).forGetter(DungProfile::intervalSeconds),
            Codec.doubleRange(1, 10000).optionalFieldOf("food_per_dung", 60.0).forGetter(DungProfile::foodPerDung)
    ).apply(instance, DungProfile::new));
}
