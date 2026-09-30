package dev.madebyfelipe.iceagesurvival.core.genetics;

import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Herança e mutação (seção 17 do CLOUD.md):
 *
 * <ul>
 *   <li>cada atributo — pontos e mutações juntos — vem de um dos pais, 50/50, independente dos outros;
 *   <li>a cada cria há {@code attempts} tentativas de mutação, cada uma com {@code chance};
 *   <li>ataque muta sem teto; velocidade até +30%; vida só se a linhagem carregar o gene;
 *   <li>o gene de vida passa se um dos pais o tiver, e surge sozinho com {@code geneChance}.
 * </ul>
 */
public final class Genetics {
    private Genetics() {
    }

    /**
     * @param chance     chance de cada tentativa de mutação dar certo
     * @param attempts   tentativas por cria
     * @param geneChance chance de o gene de vida surgir numa cria de pais sem ele
     */
    public record Tuning(double chance, int attempts, double geneChance) {
    }

    public static Genome inherit(Genome mother, Genome father, RandomGenerator random, Tuning tuning) {
        StatPoints points = StatPoints.NONE;
        int[] mutations = new int[Stat.values().length];
        for (Stat stat : Stat.values()) {
            Genome from = random.nextBoolean() ? mother : father;
            points = points.with(stat, from.points().get(stat));
            mutations[stat.ordinal()] = from.mutations(stat);
        }
        boolean healthGene = mother.healthGene() || father.healthGene() || random.nextDouble() < tuning.geneChance();

        for (int attempt = 0; attempt < tuning.attempts(); attempt++) {
            if (random.nextDouble() >= tuning.chance()) {
                continue;
            }
            List<Stat> mutable = new ArrayList<>(List.of(Stat.ATTACK, Stat.SPEED));
            if (healthGene) {
                mutable.add(Stat.HEALTH);
            }
            Stat stat = mutable.get(random.nextInt(mutable.size()));
            if (stat == Stat.SPEED) {
                // No teto a mutação se perde, como no ARK depois do limite.
                if (mutations[stat.ordinal()] < Genome.MAX_SPEED_MUTATIONS) {
                    mutations[stat.ordinal()]++;
                }
            } else {
                points = points.with(stat, points.get(stat) + Genome.POINTS_PER_MUTATION);
                mutations[stat.ordinal()]++;
            }
        }
        return new Genome(points, mutations, healthGene);
    }
}
