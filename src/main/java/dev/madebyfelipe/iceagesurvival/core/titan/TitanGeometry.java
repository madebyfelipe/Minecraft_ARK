package dev.madebyfelipe.iceagesurvival.core.titan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A geometria do Titanovenator, derivada em runtime do Tyrannosaurus do Fossils and Archeology: Revival. Nada do
 * Revival é copiado para o jar: o cliente lê o {@code tyrannosaurus.geo.json} do mod instalado e aplica aqui os mesmos
 * ajustes que o estudo local de {@code tools/study_titanovenator.py} (passos 1 e 2, aprovados pelo Felipe) e do passo 4
 * ({@code tools/titanovenator_step4.py}). Lógica pura sobre JSON, sem Minecraft, para dar para testar.
 *
 * <ol>
 *   <li><b>Box UV para UV por face</b>, antes de mudar qualquer tamanho: a arte não se desloca.</li>
 *   <li><b>Passo 1</b>: tronco, pescoço, cauda e coxas mais profundos e largos (só dimensões dos cubos existentes).</li>
 *   <li><b>Passo 2</b>: focinho e mandíbula mais largos, cristas baixas, bochechas e lábios articulados nos ossos
 *   originais.</li>
 *   <li><b>Passo 4</b>: cerdas na nuca, no dorso e na base da cauda; cicatrizes por decalque. Os dentes ficam como
 *   estão.</li>
 * </ol>
 *
 * Hierarquia, pivôs, nomes e rotações de repouso de todos os ossos originais ficam intactos, então as animações
 * (que só nomeiam ossos) continuam valendo.
 */
public final class TitanGeometry {
    /** Mesma região opaca de pele que os lábios usam na textura original (a cor sai da recoloração). */
    private static final double[] SKIN = {109, 32, 2, 2};
    private static final double[] CHEEK_SKIN = {0, 8, 2, 3};
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    private record Resize(double sx, double sy, double sz, boolean anchorTop) {
    }

    /** Passo 1: escala por osso; {@code anchorTop} ancora o eixo Y no dorso (o ventre cresce para baixo). */
    private static final Map<String, Resize> PASS_1 = new LinkedHashMap<>();

    static {
        PASS_1.put("lowerBodyBreathing", new Resize(1.12, 1.10, 1, true));
        PASS_1.put("upperBodyBreathing", new Resize(1.10, 1.06, 1, true));
        PASS_1.put("neck", new Resize(1.10, 1.04, 1, false));
        PASS_1.put("head", new Resize(1.06, 1.03, 1, false));
        PASS_1.put("upperJaw", new Resize(1.06, 1, 1, false));
        PASS_1.put("lowerJaw", new Resize(1.06, 1.10, 1, true));
        PASS_1.put("tail1", new Resize(1.12, 1.08, 1, true));
        PASS_1.put("tail2", new Resize(1.06, 1.04, 1, true));
        PASS_1.put("leftThigh", new Resize(1.08, 1.04, 1.05, false));
        PASS_1.put("rightThigh", new Resize(1.08, 1.04, 1.05, false));
    }

    /** Onde cada cicatriz e cada cerda fica; a pintura delas está em {@link TitanTexture}. */
    public record Scar(String bone, boolean positiveX, TitanTexture.ScarKind kind, double z, double y, double width,
                       double height) {
    }

    public static final List<Scar> SCARS = List.of(
            new Scar("upperBodyBreathing", true, TitanTexture.ScarKind.SLASH, -10.5, 11.5, 6.0, 6.0),
            new Scar("upperBodyBreathing", false, TitanTexture.ScarKind.BITE, -12.0, 12.0, 4.5, 5.2),
            new Scar("upperJaw", true, TitanTexture.ScarKind.JAW, -31.5, 17.3, 5.0, 1.6),
            new Scar("neck", true, TitanTexture.ScarKind.BITE, -19.0, 15.0, 4.5, 5.2),
            new Scar("tail2", true, TitanTexture.ScarKind.SLASH, 13.0, 15.0, 4.0, 4.0));

    private record Bristles(String bone, double[] zs, double height) {
    }

    private static final List<Bristles> BRISTLES = List.of(
            new Bristles("neck", new double[] {-20.6, -19.3, -18.1, -16.6, -15.4, -14.0, -12.8}, 1.25),
            new Bristles("upperBodyBreathing", new double[] {-12.9, -11.5, -10.0, -8.7, -7.2, -5.9}, 1.45),
            new Bristles("lowerBodyBreathing", new double[] {-3.6, -2.0, -0.6, 1.0, 2.5, 4.0}, 1.25),
            new Bristles("tail1", new double[] {5.5, 7.4, 9.3, 11.2}, 0.95));

    private TitanGeometry() {
    }

    /** O geo do Titanovenator a partir do geo do Tyrannosaurus. O original não é alterado. */
    public static JsonObject derive(JsonObject revivalRex) {
        JsonObject geo = revivalRex.deepCopy();
        Map<String, JsonObject> bones = bonesOf(geo);
        for (JsonObject bone : bones.values()) {
            boxUvToFaces(bone);
        }
        resizeBody(bones);
        reshapeHead(bones);
        widenStance(bones);
        addBristles(bones);
        addScars(bones);
        return geo;
    }

    /** Quantos cubos o osso tem (para os testes). */
    public static int cubeCount(JsonObject geo) {
        int count = 0;
        for (JsonObject bone : bonesOf(geo).values()) {
            if (bone.has("cubes")) {
                count += bone.getAsJsonArray("cubes").size();
            }
        }
        return count;
    }

    public static Map<String, JsonObject> bonesOf(JsonObject geo) {
        Map<String, JsonObject> bones = new LinkedHashMap<>();
        JsonArray list = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (JsonElement element : list) {
            JsonObject bone = element.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }
        return bones;
    }

    // ---- Box UV para UV por face ----

    private static void boxUvToFaces(JsonObject bone) {
        if (!bone.has("cubes")) {
            return;
        }
        for (JsonElement element : bone.getAsJsonArray("cubes")) {
            JsonObject cube = element.getAsJsonObject();
            if (!cube.has("uv") || !cube.get("uv").isJsonArray()) {
                continue;
            }
            double[] size = vec(cube.getAsJsonArray("size"));
            double[] uv = vec(cube.getAsJsonArray("uv"));
            boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
            double w = size[0];
            double h = size[1];
            double d = size[2];
            // {x, y, largura, altura} de cada face dentro do box UV; up/down vêm invertidas.
            Map<String, double[]> layout = new LinkedHashMap<>();
            layout.put("east", new double[] {0, d, d, h});
            layout.put("west", new double[] {d + w, d, d, h});
            layout.put("up", new double[] {d + w, d, -w, -d});
            layout.put("down", new double[] {d + 2 * w, 0, -w, d});
            layout.put("south", new double[] {2 * d + w, d, w, h});
            layout.put("north", new double[] {d, d, w, h});
            if (mirror) {
                for (double[] f : layout.values()) {
                    f[0] += f[2];
                    f[2] *= -1;
                }
                double[] east = layout.get("east");
                layout.put("east", layout.get("west"));
                layout.put("west", east);
            }
            JsonObject faces = new JsonObject();
            for (Map.Entry<String, double[]> face : layout.entrySet()) {
                double[] f = face.getValue();
                double u = uv[0] + f[0];
                double v = uv[1] + f[1];
                double bigU = u + f[2];
                double bigV = v + f[3];
                if (face.getKey().equals("up") || face.getKey().equals("down")) {
                    double tu = u;
                    double tv = v;
                    u = bigU;
                    v = bigV;
                    bigU = tu;
                    bigV = tv;
                }
                faces.add(face.getKey(), faceUv(u, v, bigU - u, bigV - v));
            }
            cube.add("uv", faces);
            cube.remove("mirror");
        }
    }

    private static JsonObject faceUv(double u, double v, double width, double height) {
        JsonObject face = new JsonObject();
        face.add("uv", array(u, v));
        face.add("uv_size", array(width, height));
        return face;
    }

    // ---- Passo 1 ----

    private static void resizeBody(Map<String, JsonObject> bones) {
        for (Map.Entry<String, Resize> entry : PASS_1.entrySet()) {
            JsonObject bone = bones.get(entry.getKey());
            if (bone == null || !bone.has("cubes")) {
                continue;
            }
            Resize resize = entry.getValue();
            for (JsonElement element : bone.getAsJsonArray("cubes")) {
                scaleCube(element.getAsJsonObject(), new double[] {resize.sx(), resize.sy(), resize.sz()},
                        resize.anchorTop() ? 1.0 : 0.5);
            }
        }
    }

    /** Escala o cubo e reposiciona a origem; {@code yAnchor} é 1 para ancorar o topo, 0,5 para o centro. */
    private static void scaleCube(JsonObject cube, double[] scale, double yAnchor) {
        double[] old = vec(cube.getAsJsonArray("size"));
        double[] origin = vec(cube.getAsJsonArray("origin"));
        double[] size = new double[3];
        double[] moved = new double[3];
        for (int i = 0; i < 3; i++) {
            size[i] = round5(old[i] * scale[i]);
            double anchor = i == 1 ? yAnchor : 0.5;
            moved[i] = round5(origin[i] + (old[i] - size[i]) * anchor);
        }
        cube.add("size", array(size));
        cube.add("origin", array(moved));
    }

    // ---- Passo 2 ----

    private static void reshapeHead(Map<String, JsonObject> bones) {
        resizeHeadPart(bones, "head", new double[] {1.10, 1.025, 1}, 0.5);
        resizeHeadPart(bones, "upperJaw", new double[] {1.16, 1.02, 1}, 1);
        resizeHeadPart(bones, "lowerJaw", new double[] {1.46, 1.27, 1}, 1);
        for (String crest : List.of("bigkeratincrestL", "bigkeratincrestR")) {
            resizeHeadPart(bones, crest, new double[] {0.9, 0.45, 0.9}, 0);
            // Baixo-relevo sobre o crânio, não uma quina sobre o olho.
            for (JsonElement element : bones.get(crest).getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                double[] origin = vec(cube.getAsJsonArray("origin"));
                double[] size = vec(cube.getAsJsonArray("size"));
                cube.add("origin", array(origin[0], 22.0, origin[2]));
                cube.add("size", array(size[0], 0.65, size[2]));
            }
        }

        JsonObject head = bones.get("head").getAsJsonArray("cubes").get(0).getAsJsonObject();
        double half = vec(head.getAsJsonArray("size"))[0] / 2;
        for (int side : new int[] {-1, 1}) {
            double[] origin = {round5(side * (half - 0.15) - 0.48), 14.3, -24.5};
            bones.get("head").getAsJsonArray("cubes").add(skinCube(side < 0 ? "cheek_left" : "cheek_right", origin,
                    new double[] {0.96, 3.5, 3.6}, CHEEK_SKIN, new double[] {0, 0, -side * 8}));
        }
        addLips(bones.get("upperJaw"), "upperJaw", 0.30, 0.60);
        addLips(bones.get("lowerJaw"), "lowerJaw", 0.24, 0.38);
    }

    private static void resizeHeadPart(Map<String, JsonObject> bones, String name, double[] scale, double yAnchor) {
        for (JsonElement element : bones.get(name).getAsJsonArray("cubes")) {
            scaleCube(element.getAsJsonObject(), scale, yAnchor);
        }
    }

    private static void addLips(JsonObject bone, String name, double thickness, double height) {
        JsonObject cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
        double[] origin = vec(cube.getAsJsonArray("origin"));
        double[] size = vec(cube.getAsJsonArray("size"));
        double x = origin[0];
        double y = origin[1];
        double z = origin[2];
        double yy = name.equals("upperJaw") ? y - 0.18 : y + size[1] - 0.12;
        JsonArray cubes = bone.getAsJsonArray("cubes");
        for (int side : new int[] {-1, 1}) {
            double xx = side < 0 ? x - 0.08 : x + size[0] - thickness + 0.08;
            cubes.add(skinCube(name + "_lip_" + side, new double[] {round5(xx), round5(yy), z + 0.10},
                    new double[] {thickness, height, size[2] - 0.2}, SKIN, null));
        }
        cubes.add(skinCube(name + "_lip_front", new double[] {x + 0.08, round5(yy), z - 0.06},
                new double[] {size[0] - 0.16, height, 0.24}, SKIN, null));
    }

    // ---- Passo 4 ----

    /** Quanto cada perna sai para fora do tronco, em unidades do modelo (pedido do Felipe: corpo de aparência mais larga). */
    public static final double STANCE_WIDEN = 1.0;

    /**
     * Afasta as pernas do tronco: coxa, canela e pé de cada lado deslocam {@link #STANCE_WIDEN} para fora, com os
     * pivôs. Só muda o x, então as passadas (rotações em X) e a hierarquia ficam como estão.
     */
    private static void widenStance(Map<String, JsonObject> bones) {
        for (String side : new String[] {"left", "right"}) {
            for (String part : new String[] {"Thigh", "Leg", "Foot"}) {
                JsonObject bone = bones.get(side + part);
                if (bone == null || !bone.has("cubes")) {
                    continue;
                }
                double outward = side.equals("right") ? 1.0 : -1.0;
                JsonObject first = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
                double centerX = vec(first.getAsJsonArray("origin"))[0] + vec(first.getAsJsonArray("size"))[0] / 2;
                if (centerX * outward < 0) {
                    outward = -outward; // o modelo desta versão tem o lado trocado: vai para fora do mesmo jeito
                }
                double shift = outward * STANCE_WIDEN;
                for (JsonElement element : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = element.getAsJsonObject();
                    double[] origin = vec(cube.getAsJsonArray("origin"));
                    cube.add("origin", array(round5(origin[0] + shift), origin[1], origin[2]));
                    if (cube.has("pivot")) {
                        double[] pivot = vec(cube.getAsJsonArray("pivot"));
                        cube.add("pivot", array(round5(pivot[0] + shift), pivot[1], pivot[2]));
                    }
                }
                if (bone.has("pivot")) {
                    double[] pivot = vec(bone.getAsJsonArray("pivot"));
                    bone.add("pivot", array(round5(pivot[0] + shift), pivot[1], pivot[2]));
                }
            }
        }
    }

    private static void addBristles(Map<String, JsonObject> bones) {
        for (Bristles row : BRISTLES) {
            JsonObject bone = bones.get(row.bone());
            if (bone == null || !bone.has("cubes")) {
                continue;
            }
            JsonObject top = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
            double[] origin = vec(top.getAsJsonArray("origin"));
            double[] size = vec(top.getAsJsonArray("size"));
            double centerX = origin[0] + size[0] / 2;
            double topY = origin[1] + size[1];
            for (int i = 0; i < row.zs().length; i++) {
                // Altura, deslocamento lateral e inclinação variam: filamentos, não uma fileira de espetos.
                double h = round3(row.height() * (0.85 + 0.3 * ((i * 7 + row.bone().length()) % 5) / 4.0));
                double lateral = 0.35 * ((i % 3) - 1);
                double z = row.zs()[i];
                JsonObject cube = skinCube(row.bone() + "_bristle_" + i,
                        new double[] {round3(centerX + lateral - 0.2), round3(topY - 0.25), round3(z - 0.2)},
                        new double[] {0.4, h + 0.25, 0.4}, SKIN, new double[] {18 + 4 * (i % 3), 0, 3 * ((i % 3) - 1)});
                // Penteadas para trás, em torno da base.
                cube.add("pivot", array(round3(centerX + lateral), round3(topY - 0.25), round3(z)));
                bone.getAsJsonArray("cubes").add(cube);
            }
        }
    }

    private static void addScars(Map<String, JsonObject> bones) {
        for (Scar scar : SCARS) {
            JsonObject bone = bones.get(scar.bone());
            if (bone == null || !bone.has("cubes")) {
                continue;
            }
            JsonObject cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
            double[] origin = vec(cube.getAsJsonArray("origin"));
            double[] size = vec(cube.getAsJsonArray("size"));
            double x = scar.positiveX() ? origin[0] + size[0] + 0.03 : origin[0] - 0.03 - 0.08;
            JsonObject decal = new JsonObject();
            decal.addProperty("name", scar.bone() + "_scar_" + scar.kind().name().toLowerCase());
            decal.add("origin", array(round3(x), round3(scar.y()), round3(scar.z())));
            decal.add("size", array(0.08, scar.height(), scar.width()));
            JsonObject uv = new JsonObject();
            // Só a face de fora leva a pintura; as outras apontam para um trecho vazio da textura.
            String outer = scar.positiveX() ? "east" : "west";
            for (String face : FACES) {
                double[] rect = face.equals(outer) ? scar.kind().rect() : TitanTexture.EMPTY_RECT;
                uv.add(face, faceUv(rect[0], rect[1], rect[2], rect[3]));
            }
            decal.add("uv", uv);
            bone.getAsJsonArray("cubes").add(decal);
        }
    }

    // ---- Utilidades ----

    private static JsonObject skinCube(String name, double[] origin, double[] size, double[] skin, double[] rotation) {
        JsonObject cube = new JsonObject();
        cube.addProperty("name", name);
        cube.add("origin", array(origin));
        cube.add("size", array(size));
        JsonObject uv = new JsonObject();
        for (String face : FACES) {
            uv.add(face, faceUv(skin[0], skin[1], skin[2], skin[3]));
        }
        cube.add("uv", uv);
        if (rotation != null) {
            cube.add("rotation", array(rotation));
            cube.add("pivot", array(origin[0] + size[0] / 2, origin[1] + size[1] / 2, origin[2] + size[2] / 2));
        }
        return cube;
    }

    private static double[] vec(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    private static JsonArray array(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
    }

    private static double round5(double value) {
        return Math.round(value * 1.0E5) / 1.0E5;
    }

    private static double round3(double value) {
        return Math.round(value * 1.0E3) / 1.0E3;
    }
}
