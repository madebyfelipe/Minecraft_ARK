package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import java.util.Optional;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Reposição de fauna: como a espécie volta a nascer num mundo cujos chunks já foram gerados.
 * A ausência do bloco {@code spawn} no JSON significa que a espécie só nasce com o terreno
 * (é o caso da criatura de teste).
 *
 * <p>O bloco não substitui o {@code neoforge:add_spawns} — aquele povoa chunks novos, este
 * repõe o que morreu no que já existe. Por isso a mesma tag de biomas serve aos dois.
 *
 * @param biomes    tag de biomas em que a espécie pode nascer
 * @param weight    peso relativo no sorteio entre espécies com vaga
 * @param groupMin  menor número de indivíduos por grupo
 * @param groupMax  maior número de indivíduos por grupo
 * @param maxNearby quantos indivíduos da espécie podem existir no raio de densidade de um
 *                  jogador; é o que impede a reposição de encher o mundo
 * @param minDistance distância horizontal mínima do spawn do mundo, em blocos: o mundo fica mais
 *                    perigoso conforme se afasta dele (o T-Rex não nasce perto do spawn)
 * @param family      espécie solitária que às vezes nasce em família (mãe e filhote, ou o casal)
 */
public record SpawnProfile(TagKey<Biome> biomes, int weight, int groupMin, int groupMax, int maxNearby, int minDistance,
                           Optional<FamilyProfile> family) {
    public SpawnProfile {
        groupMin = Math.max(1, groupMin);
        groupMax = Math.max(groupMin, groupMax);
    }

    public static final Codec<SpawnProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.hashedCodec(Registries.BIOME).fieldOf("biomes").forGetter(SpawnProfile::biomes),
            Codec.intRange(0, 1000).optionalFieldOf("weight", 10).forGetter(SpawnProfile::weight),
            Codec.intRange(1, 16).optionalFieldOf("group_min", 1).forGetter(SpawnProfile::groupMin),
            Codec.intRange(1, 16).optionalFieldOf("group_max", 1).forGetter(SpawnProfile::groupMax),
            Codec.intRange(0, 64).optionalFieldOf("max_nearby", 4).forGetter(SpawnProfile::maxNearby),
            Codec.intRange(0, 1_000_000).optionalFieldOf("min_distance", 0).forGetter(SpawnProfile::minDistance),
            FamilyProfile.CODEC.optionalFieldOf("family").forGetter(SpawnProfile::family)
    ).apply(instance, SpawnProfile::new));
}
