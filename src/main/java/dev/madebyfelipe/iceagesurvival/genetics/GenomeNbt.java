package dev.madebyfelipe.iceagesurvival.genetics;

import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import net.minecraft.nbt.CompoundTag;

/** Genoma em NBT: nas criaturas e nos ovos. Atributos por id, para o formato sobreviver a atributos novos. */
public final class GenomeNbt {
    private static final String POINTS = "Points";
    private static final String MUTATIONS = "Mutations";
    private static final String HEALTH_GENE = "HealthGene";

    private GenomeNbt() {
    }

    public static CompoundTag write(Genome genome) {
        CompoundTag points = new CompoundTag();
        CompoundTag mutations = new CompoundTag();
        for (Stat stat : Stat.values()) {
            points.putInt(stat.id(), genome.points().get(stat));
            if (genome.mutations(stat) > 0) {
                mutations.putInt(stat.id(), genome.mutations(stat));
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put(POINTS, points);
        tag.put(MUTATIONS, mutations);
        tag.putBoolean(HEALTH_GENE, genome.healthGene());
        return tag;
    }

    public static Genome read(CompoundTag tag) {
        return read(tag.getCompound(POINTS), tag.getCompound(MUTATIONS), tag.getBoolean(HEALTH_GENE));
    }

    /** Lê também o formato das criaturas antigas, que só guardavam os pontos. */
    public static Genome read(CompoundTag points, CompoundTag mutations, boolean healthGene) {
        StatPoints statPoints = StatPoints.NONE;
        int[] counts = new int[Stat.values().length];
        for (Stat stat : Stat.values()) {
            statPoints = statPoints.with(stat, Math.max(0, points.getInt(stat.id())));
            counts[stat.ordinal()] = Math.max(0, mutations.getInt(stat.id()));
        }
        return new Genome(statPoints, counts, healthGene);
    }
}
