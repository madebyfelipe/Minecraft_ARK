package dev.madebyfelipe.iceagesurvival.core.world;

import java.util.function.IntPredicate;

/**
 * Peso de bioma num sorteio por lista: quem sorteia pega uma posição uniforme, então repetir uma entrada aumenta a
 * chance dela. Usado para dar mais área aos biomas primitivos do TerraFirmaCraft no mundo padrão.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class BiomeWeights {
    private BiomeWeights() {
    }

    /**
     * A lista com cada entrada favorecida repetida {@code factor} vezes, no lugar dela; as outras ficam uma vez, na
     * mesma ordem. Sem favorecida, devolve a própria lista.
     */
    public static int[] weighted(int[] choices, IntPredicate favored, int factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("factor < 1: " + factor);
        }
        int size = 0;
        for (int choice : choices) {
            size += favored.test(choice) ? factor : 1;
        }
        if (size == choices.length) {
            return choices;
        }
        int[] result = new int[size];
        int index = 0;
        for (int choice : choices) {
            int copies = favored.test(choice) ? factor : 1;
            for (int copy = 0; copy < copies; copy++) {
                result[index++] = choice;
            }
        }
        return result;
    }
}
