package dev.madebyfelipe.iceagesurvival.core.stats;

import java.util.List;

/**
 * Pontos que o dono distribui à mão, como os {@value #INCUBATOR_GRANT} do filhote da incubadora
 * ao terminar de crescer. Cada ponto vale um ponto de nível no atributo escolhido; ficam fora do
 * genoma e não são herdados.
 *
 * @param unspent quantos ainda não foram distribuídos
 * @param spent   os já distribuídos, por atributo
 */
public record BonusPoints(int unspent, StatPoints spent) {
    public static final BonusPoints NONE = new BonusPoints(0, StatPoints.NONE);
    /** Quantos pontos ganha o filhote da incubadora ao terminar de crescer. */
    public static final int INCUBATOR_GRANT = 10;

    public BonusPoints {
        if (unspent < 0) {
            throw new IllegalArgumentException("Saldo negativo: " + unspent);
        }
    }

    public BonusPoints grant(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Quantidade negativa: " + count);
        }
        return new BonusPoints(unspent + count, spent);
    }

    /** Se há saldo e o atributo está entre os que recebem pontos nesta espécie. */
    public boolean canSpend(Stat stat, List<Stat> eligible) {
        return unspent > 0 && eligible.contains(stat);
    }

    /** Põe um ponto no atributo; falha sem saldo ou num atributo que não recebe pontos. */
    public BonusPoints spend(Stat stat, List<Stat> eligible) {
        if (!canSpend(stat, eligible)) {
            throw new IllegalArgumentException("Não dá para pôr ponto em " + stat.id() + " com saldo " + unspent);
        }
        return new BonusPoints(unspent - 1, spent.with(stat, spent.get(stat) + 1));
    }
}
