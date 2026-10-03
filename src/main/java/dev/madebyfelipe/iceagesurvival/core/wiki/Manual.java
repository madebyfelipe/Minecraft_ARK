package dev.madebyfelipe.iceagesurvival.core.wiki;

import java.util.List;
import java.util.Optional;

/**
 * O manual do Analisador, lido de {@code assets/iceagesurvival/wiki/manual.json} (gerado por {@code tools/gen_wiki.py}
 * a partir do texto de lore em {@code tools/wiki_lore/}): capítulos com páginas para a aba MANUAL e uma ficha por
 * espécie para a DINO FILE.
 *
 * <p>Sem classes do Minecraft (D10): as espécies são o id da entidade em texto ({@code iceagesurvival:smilodon}).
 */
public record Manual(List<Chapter> chapters, List<Sheet> sheets) {
    public static final Manual EMPTY = new Manual(List.of(), List.of());
    /** O selo de espécie desligada (não nasce): fica fora da DINO FILE. */
    public static final String DISABLED_BADGE = "Desligado";

    public sealed interface Block permits Heading, Paragraph, Bullets, Table, Note {
    }

    public record Heading(String text) implements Block {
    }

    public record Paragraph(String text) implements Block {
    }

    public record Bullets(boolean ordered, List<String> items) implements Block {
    }

    /** {@code columns} vazio quando a tabela não tem cabeçalho. */
    public record Table(List<String> columns, List<List<String>> rows) implements Block {
    }

    public record Note(String text) implements Block {
    }

    public record Page(String id, String title, List<Block> blocks) {
    }

    public record Chapter(String id, String title, String subtitle, List<Page> pages) {
    }

    public record Sheet(String species, String name, List<String> badges, List<Block> blocks) {
        public boolean disabled() {
            return badges.contains(DISABLED_BADGE);
        }
    }

    /** As fichas que entram na DINO FILE (as das espécies que nascem), na ordem do manual. */
    public List<Sheet> dexEntries() {
        return sheets.stream().filter(sheet -> !sheet.disabled()).toList();
    }

    public Optional<Sheet> sheet(String species) {
        return sheets.stream().filter(sheet -> sheet.species().equals(species)).findFirst();
    }
}
