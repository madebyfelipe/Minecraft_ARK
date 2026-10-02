package dev.madebyfelipe.iceagesurvival.core.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

class BiomeWeightsTest {
    private static final int A = 10;
    private static final int B = 20;
    private static final int C = 30;
    private static final int D = 40;

    @Test
    void favoredEntriesAreRepeatedInPlace() {
        int[] result = BiomeWeights.weighted(new int[] {A, B, C, D}, id -> id == A || id == C, 2);
        assertArrayEquals(new int[] {A, A, B, C, C, D}, result, "favorecidas repetidas no lugar, ordem mantida");
    }

    @Test
    void factorThreeRepeatsThreeTimes() {
        int[] result = BiomeWeights.weighted(new int[] {A, B, C}, id -> id == B, 3);
        assertArrayEquals(new int[] {A, B, B, B, C}, result);
    }

    @Test
    void noFavoredEntryReturnsTheSameInstance() {
        int[] choices = {A, B, C, D};
        assertSame(choices, BiomeWeights.weighted(choices, id -> false, 2), "sem favorecidas deve devolver a mesma lista");
    }

    @Test
    void emptyListReturnsTheSameInstance() {
        int[] choices = {};
        assertSame(choices, BiomeWeights.weighted(choices, id -> true, 2), "lista vazia não tem favorecidas");
    }

    @Test
    void factorBelowOneIsRejected() {
        int[] choices = {A, B};
        assertThrows(IllegalArgumentException.class, () -> BiomeWeights.weighted(choices, id -> true, 0));
        assertThrows(IllegalArgumentException.class, () -> BiomeWeights.weighted(choices, id -> true, -1));
        assertThrows(IllegalArgumentException.class, () -> BiomeWeights.weighted(choices, id -> false, 0),
                "fator inválido mesmo sem favorecidas");
    }

    @Test
    void factorOneKeepsTheContent() {
        int[] choices = {A, B, C, D};
        assertArrayEquals(new int[] {A, B, C, D}, BiomeWeights.weighted(choices, id -> id == A, 1));
    }

    @Test
    void inputIsNotModified() {
        int[] choices = {A, B, C, D};
        BiomeWeights.weighted(choices, id -> id != B, 2);
        assertArrayEquals(new int[] {A, B, C, D}, choices, "o array recebido foi alterado");
    }

    @Test
    void duplicatedFavoredEntriesAreEachRepeated() {
        int[] result = BiomeWeights.weighted(new int[] {A, B, A}, id -> id == A, 2);
        assertArrayEquals(new int[] {A, A, B, A, A}, result);
    }

    @Test
    void singleFavoredOutOfFourGetsTwoFifths() {
        int[] result = BiomeWeights.weighted(new int[] {A, B, C, D}, id -> id == A, 2);
        assertEquals(5, result.length);
        assertEquals(2, count(result, id -> id == A), "A deve ocupar 2 de 5 posições");
    }

    @Test
    void favoredShareMatchesTheFormula() {
        int[] choices = {1, 2, 3, 4, 5, 6, 7};
        IntPredicate favored = id -> id % 3 == 0; // 3 e 6: f = 2, n = 5
        int[] result = BiomeWeights.weighted(choices, favored, 2);
        int f = 2;
        int n = 5;
        assertEquals(2 * f + n, result.length);
        assertEquals((double) (2 * f) / (2 * f + n), (double) count(result, favored) / result.length, 1e-12,
                "fração de sorteios nos favorecidos fora de (2f)/(2f+n)");
    }

    private static int count(int[] values, IntPredicate predicate) {
        int total = 0;
        for (int value : values) {
            if (predicate.test(value)) {
                total++;
            }
        }
        return total;
    }
}
