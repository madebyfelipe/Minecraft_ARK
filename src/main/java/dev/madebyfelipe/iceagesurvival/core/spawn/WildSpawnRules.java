package dev.madebyfelipe.iceagesurvival.core.spawn;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Regras de reposição de fauna selvagem, sem Minecraft: sorteio de espécie por peso,
 * tamanho do grupo e posição num anel em volta do jogador.
 *
 * <p>A reposição existe porque criaturas da categoria {@code CREATURE} do vanilla nascem
 * praticamente só quando o terreno é gerado: num mundo já explorado, matar a fauna a
 * esgota para sempre. Em vez de mexer na categoria, o mod repõe por conta própria,
 * com densidade limitada por espécie.
 */
public final class WildSpawnRules {
    private WildSpawnRules() {
    }

    /**
     * Fração do teto total que os herbívoros podem ocupar. O resto fica para os carnívoros: com pesos e
     * manadas maiores, os herbívoros enchiam o teto e o Smilodon e o Utahraptor quase nunca tinham vaga.
     */
    public static final double HERBIVORE_SHARE = 0.75;

    /** Quantos herbívoros cabem neste teto total (o resto é dos carnívoros). */
    public static int herbivoreCap(int totalCap) {
        return (int) Math.floor(Math.max(0, totalCap) * HERBIVORE_SHARE);
    }

    /** Quantas vagas de herbívoro ainda há, com tantos herbívoros já por perto. */
    public static int herbivoreRoom(int totalCap, int herbivoresNearby) {
        return Math.max(0, herbivoreCap(totalCap) - herbivoresNearby);
    }

    /** Deslocamento horizontal em blocos, em relação ao jogador. */
    public record Offset(int x, int z) {
    }

    /**
     * Um candidato do sorteio: o peso da espécie e quantas dela já existem perto do jogador.
     *
     * @param weight    peso relativo no sorteio; 0 tira a espécie do sorteio
     * @param nearby    quantas criaturas dessa espécie já existem no raio de densidade
     * @param maxNearby quantas podem existir nesse raio
     */
    public record Candidate(int weight, int nearby, int maxNearby) {
        public boolean hasRoom() {
            return weight > 0 && nearby < maxNearby;
        }
    }

    /**
     * Sorteia um candidato com vaga, proporcionalmente ao peso.
     *
     * @return o índice sorteado, ou -1 se nenhum candidato tem vaga
     */
    public static int pick(List<Candidate> candidates, RandomGenerator random) {
        int total = 0;
        for (Candidate candidate : candidates) {
            if (candidate.hasRoom()) {
                total += candidate.weight();
            }
        }
        if (total <= 0) {
            return -1;
        }
        int roll = random.nextInt(total);
        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            if (!candidate.hasRoom()) {
                continue;
            }
            roll -= candidate.weight();
            if (roll < 0) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Tamanho do grupo a nascer, limitado pela vaga que a espécie ainda tem.
     *
     * @return de 0 (sem vaga) a {@code max}
     */
    public static int groupSize(int min, int max, int room, RandomGenerator random) {
        int low = Math.max(1, min);
        int high = Math.max(low, max);
        if (room <= 0) {
            return 0;
        }
        return Math.min(room, low + random.nextInt(high - low + 1));
    }

    /**
     * Posição num anel em volta do jogador: longe o bastante para não nascer à vista,
     * perto o bastante para o jogador encontrar a criatura.
     */
    public static Offset ringOffset(int minDistance, int maxDistance, RandomGenerator random) {
        int min = Math.max(0, minDistance);
        int max = Math.max(min + 1, maxDistance);
        double angle = random.nextDouble() * Math.PI * 2;
        // Raio pela raiz da área para a densidade ficar uniforme no anel, e não concentrada no centro.
        double minSq = (double) min * min;
        double distance = Math.sqrt(minSq + random.nextDouble() * ((double) max * max - minSq));
        return new Offset((int) Math.round(Math.cos(angle) * distance), (int) Math.round(Math.sin(angle) * distance));
    }
}
