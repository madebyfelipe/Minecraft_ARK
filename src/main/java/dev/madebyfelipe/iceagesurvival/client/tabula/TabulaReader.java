package dev.madebyfelipe.iceagesurvival.client.tabula;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Leitura pura (só Gson) dos arquivos do Jurassic Reborn: o {@code .tbl} (um zip com {@code model.json}) e o JSON
 * de poses ({@code {"version":0,"poses":{"IDLE":[{"pose":"...","time":20}], ...}}}). Não conhece o Minecraft, para
 * poder ser testada em JUnit; quem abre os arquivos pelo {@code ResourceManager} é o {@link TabulaModels}.
 */
public final class TabulaReader {
    private static final String MODEL_ENTRY = "model.json";

    private TabulaReader() {
    }

    /** Lê um {@code .tbl}. Não fecha o fluxo. */
    public static TabulaModelData readTbl(InputStream tbl) throws IOException {
        ZipInputStream zip = new ZipInputStream(tbl);
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (MODEL_ENTRY.equals(entry.getName())) {
                return readModelJson(new InputStreamReader(zip, StandardCharsets.UTF_8));
            }
        }
        throw new IOException("Sem " + MODEL_ENTRY + " no .tbl");
    }

    /** Lê o {@code model.json} de dentro do {@code .tbl}. */
    public static TabulaModelData readModelJson(Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        int width = intOr(json, "textureWidth", 64);
        int height = intOr(json, "textureHeight", 32);
        List<TabulaCube> roots = new ArrayList<>(cubes(json.getAsJsonArray("cubes")));
        // Grupos de cubos não têm transformação própria: os cubos deles entram como raízes.
        addGroups(json.getAsJsonArray("cubeGroups"), roots);
        return new TabulaModelData(width, height, List.copyOf(roots));
    }

    /** Lê o JSON de poses: nome da animação para a sequência de passos, na ordem do arquivo. */
    public static Map<String, List<PoseStep>> readPoses(Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject poses = json.getAsJsonObject("poses");
        if (poses == null) {
            throw new JsonParseException("Sem \"poses\"");
        }
        Map<String, List<PoseStep>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> animation : poses.entrySet()) {
            List<PoseStep> steps = new ArrayList<>();
            for (JsonElement element : animation.getValue().getAsJsonArray()) {
                JsonObject step = element.getAsJsonObject();
                steps.add(new PoseStep(step.get("pose").getAsString(), floatOr(step, "time", 1.0F)));
            }
            if (!steps.isEmpty()) {
                result.put(animation.getKey(), List.copyOf(steps));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static void addGroups(JsonArray groups, List<TabulaCube> roots) {
        if (groups == null) {
            return;
        }
        for (JsonElement element : groups) {
            JsonObject group = element.getAsJsonObject();
            roots.addAll(cubes(group.getAsJsonArray("cubes")));
            addGroups(group.getAsJsonArray("cubeGroups"), roots);
        }
    }

    private static List<TabulaCube> cubes(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<TabulaCube> cubes = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            cubes.add(cube(element.getAsJsonObject()));
        }
        return List.copyOf(cubes);
    }

    private static TabulaCube cube(JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "";
        String identifier = json.has("identifier") ? json.get("identifier").getAsString() : null;
        float[] tex = vector(json, "txOffset", 2);
        return new TabulaCube(name, identifier,
                vector(json, "dimensions", 3),
                vector(json, "position", 3),
                vector(json, "offset", 3),
                vector(json, "rotation", 3),
                (int) tex[0], (int) tex[1],
                json.has("txMirror") && json.get("txMirror").getAsBoolean(),
                floatOr(json, "mcScale", 0.0F),
                json.has("hidden") && json.get("hidden").getAsBoolean(),
                cubes(json.getAsJsonArray("children")));
    }

    private static float[] vector(JsonObject json, String key, int size) {
        float[] values = new float[size];
        JsonArray array = json.getAsJsonArray(key);
        if (array != null) {
            for (int i = 0; i < size && i < array.size(); i++) {
                values[i] = array.get(i).getAsFloat();
            }
        }
        return values;
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static float floatOr(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }
}
