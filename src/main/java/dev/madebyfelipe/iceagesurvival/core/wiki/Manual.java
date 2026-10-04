package dev.madebyfelipe.iceagesurvival.core.wiki;

import java.util.List;
import java.util.Optional;

/**
 * O manual do Analisador, lido de {@code assets/iceagesurvival/wiki/manual.json} (gerado por {@code tools/gen_wiki.py}
 * a partir do texto de lore em {@code tools/wiki_lore/}): capítulos com páginas para a aba MANUAL e uma ficha por
 * espécie para a DINO FILE, e as séries de registros (postos, base, dossiê), cada uma na ordem em que se destrava.
 *
 * <p>Sem classes do Minecraft (D10): as espécies são o id da entidade em texto ({@code iceagesurvival:smilodon}).
 */
public record Manual(List<Chapter> chapters, List<Sheet> sheets, List<Series> series) {
    public static final Manual EMPTY = new Manual(List.of(), List.of(), List.of());
    /** As séries de registros: dos postos, da base (ambas destravadas por terminais) e o dossiê (fim da luta). */
    public static final String POSTS = "postos";
    public static final String BASE = "base";
    public static final String DOSSIER = "dossie";
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

    /** Um registro militar: uma nota recuperada de um terminal ({@code source} diz de onde ela veio). */
    public record Record(String id, String title, String source, List<Block> blocks) {
    }

    /** Uma série de registros, na ordem em que se destravam. */
    public record Series(String id, String title, List<Record> records) {
    }

    /** Os registros da série {@code id}; vazio se a série não existe. */
    public List<Record> records(String id) {
        return series.stream().filter(entry -> entry.id().equals(id)).findFirst().map(Series::records)
                .orElse(List.of());
    }

    /** As fichas que entram na DINO FILE (as das espécies que nascem), na ordem do manual. */
    public List<Sheet> dexEntries() {
        return sheets.stream().filter(sheet -> !sheet.disabled()).toList();
    }

    public Optional<Sheet> sheet(String species) {
        return sheets.stream().filter(sheet -> sheet.species().equals(species)).findFirst();
    }
}
