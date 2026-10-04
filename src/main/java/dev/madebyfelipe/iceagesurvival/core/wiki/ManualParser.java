package dev.madebyfelipe.iceagesurvival.core.wiki;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lê o {@code manual.json} no formato de {@code tools/gen_wiki.py}. Tolerante: bloco de tipo desconhecido ou campo
 * faltando é pulado, para um manual gerado por uma versão mais nova do script não derrubar a tela.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class ManualParser {
    private ManualParser() {
    }

    public static Manual parse(Reader reader) {
        return parse(JsonParser.parseReader(reader).getAsJsonObject());
    }

    public static Manual parse(JsonObject root) {
        List<Manual.Chapter> chapters = new ArrayList<>();
        for (JsonElement element : array(root, "chapters")) {
            JsonObject chapter = element.getAsJsonObject();
            List<Manual.Page> pages = new ArrayList<>();
            for (JsonElement pageElement : array(chapter, "pages")) {
                JsonObject page = pageElement.getAsJsonObject();
                pages.add(new Manual.Page(string(page, "id"), string(page, "title"), blocks(page)));
            }
            chapters.add(new Manual.Chapter(string(chapter, "id"), string(chapter, "title"), string(chapter, "subtitle"),
                    List.copyOf(pages)));
        }
        List<Manual.Sheet> sheets = new ArrayList<>();
        if (root.has("species") && root.get("species").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("species").entrySet()) {
                JsonObject sheet = entry.getValue().getAsJsonObject();
                sheets.add(new Manual.Sheet(entry.getKey(), string(sheet, "name"), strings(array(sheet, "badges")),
                        blocks(sheet)));
            }
        }
        List<Manual.Series> series = new ArrayList<>();
        for (JsonElement element : array(root, "series")) {
            JsonObject entry = element.getAsJsonObject();
            List<Manual.Record> records = new ArrayList<>();
            for (JsonElement recordElement : array(entry, "records")) {
                JsonObject record = recordElement.getAsJsonObject();
                records.add(new Manual.Record(string(record, "id"), string(record, "title"), string(record, "source"),
                        blocks(record)));
            }
            series.add(new Manual.Series(string(entry, "id"), string(entry, "title"), List.copyOf(records)));
        }
        return new Manual(List.copyOf(chapters), List.copyOf(sheets), List.copyOf(series));
    }

    private static List<Manual.Block> blocks(JsonObject owner) {
        List<Manual.Block> blocks = new ArrayList<>();
        for (JsonElement element : array(owner, "blocks")) {
            JsonObject block = element.getAsJsonObject();
            Manual.Block parsed = switch (string(block, "type")) {
                case "heading" -> new Manual.Heading(string(block, "text"));
                case "paragraph" -> new Manual.Paragraph(string(block, "text"));
                case "note" -> new Manual.Note(string(block, "text"));
                case "hologram" -> new Manual.Hologram(strings(array(block, "species")));
                case "list" -> new Manual.Bullets(block.has("ordered") && block.get("ordered").getAsBoolean(),
                        strings(array(block, "items")));
                case "table" -> {
                    List<List<String>> rows = new ArrayList<>();
                    for (JsonElement row : array(block, "rows")) {
                        rows.add(strings(row.getAsJsonArray()));
                    }
                    yield new Manual.Table(strings(array(block, "columns")), List.copyOf(rows));
                }
                default -> null;
            };
            if (parsed != null) {
                blocks.add(parsed);
            }
        }
        return List.copyOf(blocks);
    }

    private static JsonArray array(JsonObject owner, String key) {
        return owner.has(key) && owner.get(key).isJsonArray() ? owner.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonObject owner, String key) {
        return owner.has(key) && owner.get(key).isJsonPrimitive() ? owner.get(key).getAsString() : "";
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(element.isJsonPrimitive() ? element.getAsString() : "");
        }
        return List.copyOf(values);
    }
}
