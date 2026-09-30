package dev.madebyfelipe.iceagesurvival.core.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class GeneticsTest {
    private static final Genetics.Tuning NO_MUTATION = new Genetics.Tuning(0.0, 3, 0.0);
    private static final Genetics.Tuning ALWAYS_MUTATE = new Genetics.Tuning(1.0, 3, 0.0);

    private static Genome parent(int health, int attack, int torpor, int armor, boolean gene) {
        StatPoints points = StatPoints.NONE.with(Stat.HEALTH, health).with(Stat.ATTACK, attack)
                .with(Stat.TORPOR, torpor).with(Stat.ARMOR, armor);
        return Genome.wild(points, gene);
    }

    @Test
    void eachStatComesWholeFromOneParent() {
        Genome mother = parent(10, 0, 10, 0, false);
        Genome father = parent(0, 10, 0, 10, false);
        RandomGenerator random = new SplittableRandom(1);
        for (int i = 0; i < 200; i++) {
            Genome child = Genetics.inherit(mother, father, random, NO_MUTATION);
            for (Stat stat : Stat.values()) {
                int value = child.points().get(stat);
                assertTrue(value == mother.points().get(stat) || value == father.points().get(stat), stat + " = " + value);
            }
        }
    }

    @Test
    void inheritanceIsFiftyFiftyAndIndependent() {
        Genome mother = parent(10, 10, 0, 0, false);
        Genome father = parent(0, 0, 0, 0, false);
        RandomGenerator random = new SplittableRandom(7);
        int healthFromMother = 0;
        int both = 0;
        int runs = 10_000;
        for (int i = 0; i < runs; i++) {
            Genome child = Genetics.inherit(mother, father, random, NO_MUTATION);
            boolean health = child.points().get(Stat.HEALTH) == 10;
            boolean attack = child.points().get(Stat.ATTACK) == 10;
            healthFromMother += health ? 1 : 0;
            both += health && attack ? 1 : 0;
        }
        assertEquals(0.5, healthFromMother / (double) runs, 0.02);
        assertEquals(0.25, both / (double) runs, 0.02, "atributos deveriam ser sorteados um a um");
    }

    @Test
    void withoutTheHealthGeneHealthNeverMutates() {
        Genome mother = parent(5, 5, 5, 5, false);
        Genome father = parent(5, 5, 5, 5, false);
        RandomGenerator random = new SplittableRandom(3);
        for (int i = 0; i < 500; i++) {
            Genome child = Genetics.inherit(mother, father, random, ALWAYS_MUTATE);
            assertEquals(0, child.mutations(Stat.HEALTH));
            assertEquals(5, child.points().get(Stat.HEALTH));
            assertEquals(3, child.totalMutations(), "três tentativas, todas certeiras, sem teto atingido");
        }
    }

    @Test
    void theHealthGeneIsInheritedAndLetsHealthMutate() {
        Genome mother = parent(5, 5, 5, 5, true);
        Genome father = parent(5, 5, 5, 5, false);
        RandomGenerator random = new SplittableRandom(4);
        boolean mutatedHealth = false;
        for (int i = 0; i < 200; i++) {
            Genome child = Genetics.inherit(mother, father, random, ALWAYS_MUTATE);
            assertTrue(child.healthGene(), "o gene de um dos pais passa sempre");
            mutatedHealth |= child.mutations(Stat.HEALTH) > 0;
        }
        assertTrue(mutatedHealth);
    }

    @Test
    void attackMutationsAddPointsAndHaveNoCeiling() {
        Genome lineage = parent(0, 0, 0, 0, false);
        RandomGenerator random = new SplittableRandom(5);
        for (int generation = 0; generation < 200; generation++) {
            lineage = Genetics.inherit(lineage, lineage, random, ALWAYS_MUTATE);
        }
        assertTrue(lineage.mutations(Stat.ATTACK) > 100, "ataque parou de mutar: " + lineage.mutations(Stat.ATTACK));
        assertEquals(lineage.mutations(Stat.ATTACK) * Genome.POINTS_PER_MUTATION, lineage.points().get(Stat.ATTACK));
    }

    @Test
    void speedTopsOutAtThirtyPercent() {
        Genome lineage = parent(0, 0, 0, 0, false);
        RandomGenerator random = new SplittableRandom(6);
        for (int generation = 0; generation < 200; generation++) {
            lineage = Genetics.inherit(lineage, lineage, random, ALWAYS_MUTATE);
        }
        assertEquals(Genome.MAX_SPEED_MUTATIONS, lineage.mutations(Stat.SPEED));
        assertEquals(1.3, lineage.speedMultiplier(), 1e-9);
        assertEquals(0, lineage.points().get(Stat.SPEED), "velocidade não ganha pontos");
    }

    @Test
    void torporAndArmorNeverMutate() {
        Genome lineage = parent(0, 0, 3, 3, true);
        RandomGenerator random = new SplittableRandom(8);
        for (int generation = 0; generation < 100; generation++) {
            lineage = Genetics.inherit(lineage, lineage, random, ALWAYS_MUTATE);
        }
        assertEquals(3, lineage.points().get(Stat.TORPOR));
        assertEquals(3, lineage.points().get(Stat.ARMOR));
        assertFalse(lineage.mutations(Stat.TORPOR) > 0 || lineage.mutations(Stat.ARMOR) > 0);
    }

    @Test
    void theGeneCanAppearOnItsOwn() {
        Genome plain = parent(0, 0, 0, 0, false);
        RandomGenerator random = new SplittableRandom(9);
        int carriers = 0;
        for (int i = 0; i < 10_000; i++) {
            carriers += Genetics.inherit(plain, plain, random, new Genetics.Tuning(0.0, 3, 0.01)).healthGene() ? 1 : 0;
        }
        assertEquals(0.01, carriers / 10_000.0, 0.004);
    }
}
