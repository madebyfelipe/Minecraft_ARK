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

    /**
     * Teto próprio dos voadores (Pteranodonte, Quetzalcoatlus) por jogador. Eles ficam fora do teto do chão: no céu
     * não tomam o lugar de ninguém, e dentro dele o Pteranodonte (até 8) disputava vaga com as manadas e os
     * predadores.
     */
    public static final int FLYER_CAP = 8;

    /** Espécie que ainda não tem ninguém por perto entra no sorteio com este peso a mais. */
    public static final int ABSENT_BOOST = 3;

    /** A partir desta fração do teto da categoria, a vaga é disputada: cada espécie fica com um bando por perto. */
    public static final double CONTESTED_FRACTION = 0.5;

    /** Se a categoria já está cheia o bastante para a vaga ser disputada. */
    public static boolean contested(int occupied, int cap) {
        return occupied >= cap * CONTESTED_FRACTION;
    }

    /**
     * Peso de uma espécie no sorteio, com a regra de diversidade. Antes o sorteio era só pelo peso e a fauna não some
     * sozinha: as espécies de peso alto e bando grande (dodô, mamute, Galimimo, Pteranodonte) enchiam o teto primeiro
     * e a composição congelava — cada espécie nova diluía as outras e as raras (Estegossauro, Tricerátopo, Quetzal)
     * quase nunca entravam.
     *
     * <ul>
     *   <li>ninguém dela por perto: peso × {@value #ABSENT_BOOST};</li>
     *   <li>vaga disputada e já com um bando inteiro ({@code groupMax}) por perto: fora do sorteio;</li>
     *   <li>senão, o peso.</li>
     * </ul>
     */
    public static int fairWeight(int weight, int nearby, int groupMax, boolean contested) {
        if (weight <= 0) {
            return 0;
        }
        if (nearby <= 0) {
            return weight * ABSENT_BOOST;
        }
        if (contested && nearby >= Math.max(1, groupMax)) {
            return 0;
        }
        return weight;
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
