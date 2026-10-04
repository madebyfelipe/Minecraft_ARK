package dev.madebyfelipe.iceagesurvival.core.titan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Testa a derivação contra um "Rex" sintético com os mesmos nomes de osso do modelo do Revival, mas números próprios:
 * nada do modelo de terceiros entra no repositório.
 */
class TitanGeometryTest {
    private static JsonObject cube(double[] origin, double[] size, double[] uv, boolean mirror) {
        JsonObject cube = new JsonObject();
        cube.add("origin", array(origin));
        cube.add("size", array(size));
        cube.add("uv", array(uv));
        if (mirror) {
            cube.addProperty("mirror", true);
        }
        return cube;
    }

    private static JsonArray array(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject bone(String name, String parent, double[] pivot, double[] rotation, JsonObject... cubes) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parent != null) {
            bone.addProperty("parent", parent);
        }
        bone.add("pivot", array(pivot));
        if (rotation != null) {
            bone.add("rotation", array(rotation));
        }
        if (cubes.length > 0) {
            JsonArray list = new JsonArray();
            for (JsonObject cube : cubes) {
                list.add(cube);
            }
            bone.add("cubes", list);
        }
        return bone;
    }

    private static JsonObject syntheticRex() {
        JsonArray bones = new JsonArray();
        double[] p = {0, 20, 0};
        bones.add(bone("rex", null, p, null));
        bones.add(bone("lowerBodyBreathing", "rex", p, null,
                cube(new double[] {-4.5, 9, -6}, new double[] {9, 12, 13}, new double[] {56, 33}, false)));
        bones.add(bone("rider_pos", "lowerBodyBreathing", new double[] {0, 21, -1}, null));
        bones.add(bone("tail1", "rex", p, new double[] {-2, 0, 0},
                cube(new double[] {-3.5, 13, 3}, new double[] {7, 8, 10}, new double[] {3, 46}, false)));
        bones.add(bone("tail2", "tail1", p, new double[] {4, 0, 0},
                cube(new double[] {-2.5, 14, 12}, new double[] {5, 7, 10}, new double[] {35, 47}, false)));
        bones.add(bone("tail3", "tail2", p, new double[] {-2, 0, 0},
                cube(new double[] {-1.5, 15, 21}, new double[] {3, 5, 11}, new double[] {66, 45}, false)));
        bones.add(bone("upperBodyBreathing", "rex", p, null,
                cube(new double[] {-5, 10, -14}, new double[] {10, 11, 10}, new double[] {87, 10}, false)));
        bones.add(bone("neck", "upperBodyBreathing", new double[] {0, 17, -12},
                new double[] {-30, 0, 0}, cube(new double[] {-2.5, 13, -22}, new double[] {5, 8, 10},
                        new double[] {53, 10}, false)));
        bones.add(bone("head", "neck", new double[] {0, 19, -20}, new double[] {32, 0, 0},
                cube(new double[] {-3, 14, -25}, new double[] {6, 8, 5}, new double[] {0, 5}, false)));
        bones.add(bone("lowerJaw", "head", new double[] {0, 17, -24}, null,
                cube(new double[] {-1.5, 14.6, -32.7}, new double[] {3, 2, 8}, new double[] {27, 21}, false)));
        bones.add(bone("upperJaw", "head", new double[] {0, 19, -25}, new double[] {2.6, 0, 0},
                cube(new double[] {-2, 16.6, -32.9}, new double[] {4, 5, 8}, new double[] {100, 31}, false)));
        bones.add(bone("bigkeratincrestR", "head", new double[] {2, 21, -26}, new double[] {0, 10, 0},
                cube(new double[] {1, 21, -26}, new double[] {2, 2, 4}, new double[] {47, 26}, false)));
        bones.add(bone("bigkeratincrestL", "head", new double[] {-2, 21, -26}, new double[] {0, -13, 0},
                cube(new double[] {-3, 21, -26}, new double[] {2, 2, 4}, new double[] {47, 26}, true)));
        bones.add(bone("leftThigh", "rex", new double[] {-2.7, 16, 2}, new double[] {-20, 0, 0},
                cube(new double[] {-6.8, 8, -2}, new double[] {4, 11, 8}, new double[] {1, 21}, true)));
        bones.add(bone("rightThigh", "rex", new double[] {2.7, 16, 2}, new double[] {-20, 0, 0},
                cube(new double[] {2.6, 8, -2}, new double[] {4, 11, 8}, new double[] {1, 21}, false)));
        JsonObject geometry = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("texture_width", 128);
        description.addProperty("texture_height", 64);
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray list = new JsonArray();
        list.add(geometry);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.12.0");
        root.add("minecraft:geometry", list);
        return root;
    }

    private static JsonObject cubeOf(Map<String, JsonObject> bones, String bone, int index) {
        return bones.get(bone).getAsJsonArray("cubes").get(index).getAsJsonObject();
    }

    private static double[] numbers(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    @Test
    void theOriginalIsNotChanged() {
        JsonObject rex = syntheticRex();
        String before = rex.toString();
        TitanGeometry.derive(rex);
        assertEquals(before, rex.toString());
    }

    @Test
    void everyOriginalBoneKeepsItsParentPivotAndRestRotation() {
        JsonObject rex = syntheticRex();
        Map<String, JsonObject> before = TitanGeometry.bonesOf(rex);
        Map<String, JsonObject> after = TitanGeometry.bonesOf(TitanGeometry.derive(rex));
        assertEquals(before.keySet(), after.keySet(), "nenhum osso a mais ou a menos: as animações só nomeiam ossos");
        for (String name : before.keySet()) {
            for (String key : new String[] {"parent", "pivot", "rotation"}) {
                if (key.equals("pivot") && name.matches("(left|right)(Thigh|Leg|Foot)")) {
                    // As pernas se afastam do tronco: só o x do pivô muda.
                    double[] was = numbers(before.get(name), "pivot");
                    double[] now = numbers(after.get(name), "pivot");
                    assertEquals(was[1], now[1], 1.0E-9, name + ".pivot.y");
                    assertEquals(was[2], now[2], 1.0E-9, name + ".pivot.z");
                    continue;
                }
                assertEquals(before.get(name).get(key), after.get(name).get(key), name + "." + key);
            }
        }
    }

    @Test
    void boxUvBecomesPerFaceUvBeforeAnythingIsResized() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        // tail3 não é redimensionado: sobra só a conversão. Box UV [66,45], tamanho 3×5×11.
        JsonObject uv = cubeOf(bones, "tail3", 0).getAsJsonObject("uv");
        assertEquals(66.0, numbers(uv.getAsJsonObject("east"), "uv")[0]);
        assertEquals(45.0 + 11.0, numbers(uv.getAsJsonObject("east"), "uv")[1]);
        assertEquals(11.0, numbers(uv.getAsJsonObject("east"), "uv_size")[0]);
        assertEquals(5.0, numbers(uv.getAsJsonObject("east"), "uv_size")[1]);
        assertEquals(66.0 + 2 * 11.0 + 3.0, numbers(uv.getAsJsonObject("south"), "uv")[0], "a face sul vem depois de leste, norte e oeste");
        // A face de baixo sai com a altura invertida, como no Blockbench; a de cima, não.
        assertTrue(numbers(uv.getAsJsonObject("down"), "uv_size")[1] < 0);
        assertTrue(numbers(uv.getAsJsonObject("up"), "uv_size")[1] > 0);
        assertFalse(cubeOf(bones, "tail3", 0).has("mirror"));
    }

    @Test
    void mirroredCubesSwapEastAndWest() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        JsonObject mirrored = cubeOf(bones, "leftThigh", 0).getAsJsonObject("uv");
        JsonObject plain = cubeOf(bones, "rightThigh", 0).getAsJsonObject("uv");
        // Espelhado: a face leste usa onde a oeste estaria, com a largura invertida.
        assertEquals(numbers(plain.getAsJsonObject("west"), "uv")[0] + numbers(plain.getAsJsonObject("west"), "uv_size")[0],
                numbers(mirrored.getAsJsonObject("east"), "uv")[0], 1.0E-6);
        assertTrue(numbers(mirrored.getAsJsonObject("east"), "uv_size")[0] < 0);
        assertTrue(numbers(plain.getAsJsonObject("east"), "uv_size")[0] > 0);
    }

    @Test
    void theBodyGrowsDeeperAndWiderKeepingTheBackLine() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        JsonObject body = cubeOf(bones, "lowerBodyBreathing", 0);
        double[] size = numbers(body, "size");
        double[] origin = numbers(body, "origin");
        assertEquals(9 * 1.12, size[0], 1.0E-4);
        assertEquals(12 * 1.10, size[1], 1.0E-4);
        assertEquals(13.0, size[2], 1.0E-4);
        assertEquals(9.0 + 12.0, origin[1] + size[1], 1.0E-4, "o dorso fica onde estava; o ventre desce");
        assertEquals(0.0, origin[0] + size[0] / 2, 1.0E-4, "continua centrado");
    }

    @Test
    void legsMoveOutwardFromTheTorsoForAWiderLook() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        JsonObject right = cubeOf(bones, "rightThigh", 0);
        JsonObject left = cubeOf(bones, "leftThigh", 0);
        double rightCenter = numbers(right, "origin")[0] + numbers(right, "size")[0] / 2;
        double leftCenter = numbers(left, "origin")[0] + numbers(left, "size")[0] / 2;
        // O centro original era 4,6 e -4,8; o passo 1 alarga em torno do centro, então só o afastamento o desloca.
        assertEquals(4.6 + TitanGeometry.STANCE_WIDEN, rightCenter, 1.0E-4);
        assertEquals(-4.8 - TitanGeometry.STANCE_WIDEN, leftCenter, 1.0E-4);
        assertEquals(2.7 + TitanGeometry.STANCE_WIDEN, numbers(bones.get("rightThigh"), "pivot")[0], 1.0E-4,
                "o pivô acompanha, para a passada girar no mesmo lugar");
        assertEquals(-2.7 - TitanGeometry.STANCE_WIDEN, numbers(bones.get("leftThigh"), "pivot")[0], 1.0E-4);
        assertEquals(16.0, numbers(bones.get("rightThigh"), "pivot")[1], 1.0E-4, "só o x muda");
    }

    @Test
    void theHeadGetsWiderWithCheeksLipsAndLowCrests() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        assertEquals(3, bones.get("head").getAsJsonArray("cubes").size(), "crânio e duas bochechas");
        assertEquals("cheek_left", cubeOf(bones, "head", 1).get("name").getAsString());
        assertEquals("cheek_right", cubeOf(bones, "head", 2).get("name").getAsString());
        assertEquals(5, bones.get("upperJaw").getAsJsonArray("cubes").size(), "focinho, três peças de lábio e a cicatriz");
        assertEquals(4, bones.get("lowerJaw").getAsJsonArray("cubes").size());
        assertEquals(6.0 * 1.06 * 1.10, numbers(cubeOf(bones, "head", 0), "size")[0], 1.0E-3);
        for (String crest : new String[] {"bigkeratincrestL", "bigkeratincrestR"}) {
            JsonObject cube = cubeOf(bones, crest, 0);
            assertEquals(0.65, numbers(cube, "size")[1], 1.0E-6);
            assertEquals(22.0, numbers(cube, "origin")[1], 1.0E-6);
        }
    }

    @Test
    void bristlesLineTheNeckBackAndTailBaseOnTheExistingBones() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        int bristles = 0;
        for (String bone : new String[] {"neck", "upperBodyBreathing", "lowerBodyBreathing", "tail1"}) {
            for (JsonElement element : bones.get(bone).getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                if (cube.has("name") && cube.get("name").getAsString().contains("_bristle_")) {
                    bristles++;
                    double[] size = numbers(cube, "size");
                    assertTrue(size[0] <= 0.4 && size[2] <= 0.4, "finas");
                    assertTrue(size[1] < 2.2, "curtas: filamentos, não espetos");
                    assertNotNull(cube.get("pivot"));
                }
            }
        }
        assertEquals(23, bristles);
    }

    @Test
    void scarsAreThinDecalsPaintedOnlyOnTheirOuterFace() {
        Map<String, JsonObject> bones = TitanGeometry.bonesOf(TitanGeometry.derive(syntheticRex()));
        int decals = 0;
        for (TitanGeometry.Scar scar : TitanGeometry.SCARS) {
            JsonObject decal = null;
            for (JsonElement element : bones.get(scar.bone()).getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                if (cube.has("name") && cube.get("name").getAsString().equals(
                        scar.bone() + "_scar_" + scar.kind().name().toLowerCase())
                        && (numbers(cube, "origin")[0] > 0) == scar.positiveX()) {
                    decal = cube;
                }
            }
            assertNotNull(decal, scar.toString());
            decals++;
            assertEquals(0.08, numbers(decal, "size")[0], 1.0E-6, "fina como um decalque");
            JsonObject uv = decal.getAsJsonObject("uv");
            String outer = scar.positiveX() ? "east" : "west";
            double[] rect = scar.kind().rect();
            assertEquals(rect[0], numbers(uv.getAsJsonObject(outer), "uv")[0], 1.0E-6);
            assertEquals(TitanTexture.EMPTY_RECT[0], numbers(uv.getAsJsonObject(scar.positiveX() ? "west" : "east"),
                    "uv")[0], 1.0E-6, "a face de trás não leva a pintura");
        }
        assertEquals(5, decals);
    }

    @Test
    void everyUvStaysInsideTheAtlas() {
        JsonObject geo = TitanGeometry.derive(syntheticRex());
        for (JsonObject bone : TitanGeometry.bonesOf(geo).values()) {
            if (!bone.has("cubes")) {
                continue;
            }
            for (JsonElement element : bone.getAsJsonArray("cubes")) {
                JsonObject uv = element.getAsJsonObject().getAsJsonObject("uv");
                for (Map.Entry<String, JsonElement> face : uv.entrySet()) {
                    double[] origin = numbers(face.getValue().getAsJsonObject(), "uv");
                    double[] size = numbers(face.getValue().getAsJsonObject(), "uv_size");
                    double minX = Math.min(origin[0], origin[0] + size[0]);
                    double maxX = Math.max(origin[0], origin[0] + size[0]);
                    double minY = Math.min(origin[1], origin[1] + size[1]);
                    double maxY = Math.max(origin[1], origin[1] + size[1]);
                    assertTrue(minX >= 0 && maxX <= TitanTexture.WIDTH && minY >= 0 && maxY <= TitanTexture.HEIGHT,
                            bone.get("name") + "." + face.getKey() + " fora do atlas");
                }
            }
        }
    }

    @Test
    void theDerivedGeoSerialisesToValidJson() {
        JsonObject geo = TitanGeometry.derive(syntheticRex());
        assertEquals(geo, JsonParser.parseString(geo.toString()));
        assertTrue(TitanGeometry.cubeCount(geo) > TitanGeometry.cubeCount(syntheticRex()) + 30);
    }
}
