package dev.madebyfelipe.iceagesurvival.core.genetics;

import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import java.util.Arrays;

/**
 * O que um indivíduo passa aos filhotes: os pontos de cada atributo, quantas mutações cada
 * atributo acumulou na linhagem e se ela carrega o gene de mutação de vida.
 *
 * <p>A velocidade não recebe pontos: muda só por mutação, em {@link #SPEED_PER_MUTATION} por
 * mutação até {@link #MAX_SPEED_MUTATIONS}, o teto de +30% do design (seção 17 do CLOUD.md).
 *
 * @param points     pontos por atributo; o nível é {@code points.level()}
 * @param mutations  mutações por atributo, na ordem de {@link Stat#values()}
 * @param healthGene se a linhagem pode mutar a vida
 */
public record Genome(StatPoints points, int[] mutations, boolean healthGene) {
    public static final double SPEED_PER_MUTATION = 0.03;
    public static final int MAX_SPEED_MUTATIONS = 10;
    /** Pontos que uma mutação de ataque ou vida acrescenta ao atributo (e ao nível). */
    public static final int POINTS_PER_MUTATION = 2;

    public Genome {
        if (mutations.length != Stat.values().length) {
            throw new IllegalArgumentException("Uma contagem de mutação por atributo");
        }
        mutations = mutations.clone();
    }

    /** Indivíduo selvagem: só os pontos sorteados, sem mutações. */
    public static Genome wild(StatPoints points, boolean healthGene) {
        return new Genome(points, new int[Stat.values().length], healthGene);
    }

    public int mutations(Stat stat) {
        return mutations[stat.ordinal()];
    }

    public int totalMutations() {
        return Arrays.stream(mutations).sum();
    }

    /** Multiplicador da velocidade base da espécie; 1,0 a 1,3. */
    public double speedMultiplier() {
        return 1.0 + SPEED_PER_MUTATION * Math.min(mutations(Stat.SPEED), MAX_SPEED_MUTATIONS);
    }

    public int level() {
        return points.level();
    }

    @Override
    public int[] mutations() {
        return mutations.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Genome that && points.equals(that.points)
                && Arrays.equals(mutations, that.mutations) && healthGene == that.healthGene;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * points.hashCode() + Arrays.hashCode(mutations)) + Boolean.hashCode(healthGene);
    }
}
