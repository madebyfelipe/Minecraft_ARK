package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A árvore de cubos de um modelo, achatada em ordem de árvore (pai antes dos filhos). Uma pose é um "quadro":
 * {@value #STRIDE} números por cubo, na mesma ordem — ponto de rotação (x, y, z) e rotação (x, y, z) em radianos.
 * As poses de uma espécie do Jurassic Reborn repetem a mesma árvore com os mesmos {@code identifier}; é por ele que
 * cada cubo da pose acha o seu lugar (o nome se repete: há vários "spike" irmãos).
 */
public final class TabulaRig {
    /** Números por cubo num quadro. */
    public static final int STRIDE = 6;

    private final List<TabulaCube> cubes;
    private final int[] parents;
    private final Map<String, Integer> byIdentifier;
    private final Map<String, Integer> byName;
    private final float[] rest;

    private TabulaRig(List<TabulaCube> cubes, int[] parents) {
        this.cubes = Collections.unmodifiableList(cubes);
        this.parents = parents;
        Map<String, Integer> identifiers = new HashMap<>();
        Map<String, Integer> names = new HashMap<>();
        for (int i = 0; i < cubes.size(); i++) {
            TabulaCube cube = cubes.get(i);
            if (cube.identifier() != null) {
                identifiers.putIfAbsent(cube.identifier(), i);
            }
            names.putIfAbsent(cube.name(), i);
        }
        this.byIdentifier = identifiers;
        this.byName = names;
        this.rest = new float[cubes.size() * STRIDE];
        for (int i = 0; i < cubes.size(); i++) {
            write(cubes.get(i), rest, i);
        }
    }

    public static TabulaRig of(TabulaModelData model) {
        List<TabulaCube> cubes = new ArrayList<>();
        List<Integer> parents = new ArrayList<>();
        flatten(model.roots(), -1, cubes, parents);
        return new TabulaRig(cubes, parents.stream().mapToInt(Integer::intValue).toArray());
    }

    private static void flatten(List<TabulaCube> level, int parent, List<TabulaCube> cubes, List<Integer> parents) {
        for (TabulaCube cube : level) {
            int index = cubes.size();
            cubes.add(cube);
            parents.add(parent);
            flatten(cube.children(), index, cubes, parents);
        }
    }

    public int size() {
        return cubes.size();
    }

    public TabulaCube cube(int index) {
        return cubes.get(index);
    }

    /** Índice do pai, ou -1 numa raiz. */
    public int parent(int index) {
        return parents[index];
    }

    /** Índice do primeiro cubo com esse nome (ordem de árvore), ou -1. */
    public int indexOf(String name) {
        return byName.getOrDefault(name, -1);
    }

    public int frameSize() {
        return rest.length;
    }

    /** O quadro do próprio modelo base. */
    public float[] restFrame() {
        return rest.clone();
    }

    /**
     * O quadro de uma pose. Cada cubo é achado pelo {@code identifier}; sem ele, pela posição na árvore, se as duas
     * árvores tiverem o mesmo tamanho. Cubo da base que a pose não traz fica como na base.
     */
    public float[] frameOf(TabulaModelData pose) {
        float[] frame = rest.clone();
        List<TabulaCube> poseCubes = new ArrayList<>();
        flatten(pose.roots(), -1, poseCubes, new ArrayList<>());
        boolean sameShape = poseCubes.size() == cubes.size();
        for (int i = 0; i < poseCubes.size(); i++) {
            TabulaCube cube = poseCubes.get(i);
            Integer index = cube.identifier() == null ? null : byIdentifier.get(cube.identifier());
            if (index == null && sameShape) {
                index = i;
            }
            if (index != null) {
                write(cube, frame, index);
            }
        }
        return frame;
    }

    /** Quantos cubos da pose acham par na base pelo {@code identifier} (para avisar de pose de outra árvore). */
    public int matchedByIdentifier(TabulaModelData pose) {
        List<TabulaCube> poseCubes = new ArrayList<>();
        flatten(pose.roots(), -1, poseCubes, new ArrayList<>());
        int matched = 0;
        for (TabulaCube cube : poseCubes) {
            if (cube.identifier() != null && byIdentifier.containsKey(cube.identifier())) {
                matched++;
            }
        }
        return matched;
    }

    private static void write(TabulaCube cube, float[] frame, int index) {
        int at = index * STRIDE;
        frame[at] = cube.position()[0];
        frame[at + 1] = cube.position()[1];
        frame[at + 2] = cube.position()[2];
        frame[at + 3] = (float) Math.toRadians(cube.rotation()[0]);
        frame[at + 4] = (float) Math.toRadians(cube.rotation()[1]);
        frame[at + 5] = (float) Math.toRadians(cube.rotation()[2]);
    }
}
