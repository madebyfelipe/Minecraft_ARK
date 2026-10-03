package dev.madebyfelipe.iceagesurvival.core.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O manual do Analisador: o {@code manual.json} de verdade (gerado por {@code tools/gen_wiki.py}) e a tolerância do
 * leitor a um arquivo de uma versão diferente do script. O diretório de trabalho do teste é a raiz do projeto.
 */
class ManualParserTest {
    private static final Path MANUAL = Path.of("src/main/resources/assets/iceagesurvival/wiki/manual.json");
    private static final Path SPECIES = Path.of("src/main/resources/data/iceagesurvival/iceagesurvival/species");
    private static final String DIRE_WOLF = "iceagesurvival:dire_wolf";

    @Test
    void realManualHasTheEcologyAndBehaviorChaptersWithPages() {
        Manual manual = realManual();
        List<String> ids = manual.chapters().stream().map(Manual.Chapter::id).toList();
        assertTrue(ids.contains("ecologia"), "falta o capítulo ecologia: " + ids);
        assertTrue(ids.contains("comportamento"), "falta o capítulo comportamento: " + ids);
        for (Manual.Chapter chapter : manual.chapters()) {
            assertFalse(chapter.title().isBlank(), "capítulo sem título: " + chapter.id());
            assertFalse(chapter.pages().isEmpty(), "capítulo sem páginas: " + chapter.id());
            for (Manual.Page page : chapter.pages()) {
                assertFalse(page.title().isBlank(), "página sem título: " + page.id());
                assertFalse(page.blocks().isEmpty(), "página vazia: " + page.id());
            }
        }
    }

    @Test
    void everySpeciesInTheDataHasASheet() throws IOException {
        Manual manual = realManual();
        List<String> species;
        try (Stream<Path> files = Files.list(SPECIES)) {
            species = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .filter(name -> !name.equals("test_creature"))
                    .sorted()
                    .toList();
        }
        assertFalse(species.isEmpty(), "nenhuma espécie em " + SPECIES);
        for (String id : species) {
            Optional<Manual.Sheet> sheet = manual.sheet("iceagesurvival:" + id);
            assertTrue(sheet.isPresent(), "espécie sem ficha no manual: " + id);
            assertFalse(sheet.get().name().isBlank(), "ficha sem nome: " + id);
            assertFalse(sheet.get().blocks().isEmpty(), "ficha sem conteúdo: " + id);
        }
    }

    @Test
    void dexEntriesLeaveOutTheDisabledSpeciesAndKeepTheFileOrder() {
        Manual manual = realManual();
        Optional<Manual.Sheet> wolf = manual.sheet(DIRE_WOLF);
        assertTrue(wolf.isPresent(), "o lobo-terrível devia ter ficha no manual, mesmo desligado");
        assertTrue(wolf.get().badges().contains("Desligado"), "o lobo-terrível não nasce: devia ter o selo Desligado");

        List<String> dex = manual.dexEntries().stream().map(Manual.Sheet::species).toList();
        assertFalse(dex.contains(DIRE_WOLF), "o lobo-terrível (desligado) entrou na DINO FILE");
        assertTrue(dex.contains("iceagesurvival:smilodon"), "o smilodon sumiu da DINO FILE");

        // A ordem das chaves como estão escritas no arquivo, menos as que têm o selo Desligado.
        List<String> expected = new ArrayList<>();
        for (String species : speciesKeysInFileOrder()) {
            if (!manual.sheet(species).orElseThrow().badges().contains("Desligado")) {
                expected.add(species);
            }
        }
        assertEquals(expected, dex, "a DINO FILE devia seguir a ordem do manual, sem as espécies desligadas");
    }

    @Test
    void dexEntriesKeepTheOrderOfASmallManualAndDropOnlyTheDisabled() {
        Manual manual = ManualParser.parse(new StringReader("""
                {"species": {
                  "iceagesurvival:zeta": {"name": "Zeta", "badges": ["Carnívoro"], "blocks": []},
                  "iceagesurvival:alfa": {"name": "Alfa", "badges": ["Herbívoro", "Desligado"], "blocks": []},
                  "iceagesurvival:mu": {"name": "Mu", "badges": [], "blocks": []}
                }}
                """));
        assertEquals(List.of("iceagesurvival:zeta", "iceagesurvival:alfa", "iceagesurvival:mu"),
                manual.sheets().stream().map(Manual.Sheet::species).toList(), "as fichas deviam vir na ordem do arquivo");
        assertEquals(List.of("iceagesurvival:zeta", "iceagesurvival:mu"),
                manual.dexEntries().stream().map(Manual.Sheet::species).toList(),
                "só a desligada devia sair, sem reordenar as outras");
        assertTrue(manual.sheet("iceagesurvival:alfa").isPresent(), "a ficha desligada ainda devia ser achada");
        assertTrue(manual.sheet("iceagesurvival:nada").isEmpty(), "achou ficha de espécie que não existe");
    }

    @Test
    void everyTableWithAHeaderHasRowsOfTheSameWidth() {
        Manual manual = realManual();
        List<List<Manual.Block>> owners = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Manual.Chapter chapter : manual.chapters()) {
            for (Manual.Page page : chapter.pages()) {
                owners.add(page.blocks());
                names.add(page.id());
            }
        }
        for (Manual.Sheet sheet : manual.sheets()) {
            owners.add(sheet.blocks());
            names.add(sheet.species());
        }
        int tables = 0;
        for (int i = 0; i < owners.size(); i++) {
            for (Manual.Block block : owners.get(i)) {
                if (block instanceof Manual.Table table && !table.columns().isEmpty()) {
                    tables++;
                    for (List<String> row : table.rows()) {
                        assertEquals(table.columns().size(), row.size(),
                                "linha de tamanho diferente do cabeçalho em " + names.get(i) + ": " + row);
                    }
                }
            }
        }
        assertTrue(tables > 0, "o manual devia ter tabelas com cabeçalho");
    }

    @Test
    void unknownBlocksAreSkippedAndMissingFieldsBecomeEmpty() {
        Manual manual = ManualParser.parse(new StringReader("""
                {"chapters": [{"id": "c1", "pages": [{"id": "c1/p", "blocks": [
                  {"type": "video", "src": "filme.mp4"},
                  {"type": "heading"},
                  {"type": "paragraph", "text": "Olá"},
                  {"type": "list", "ordered": true, "items": ["um", "dois"]},
                  {"type": "list", "items": ["a"]},
                  {"type": "table", "rows": [["x", "y"], ["z"]]},
                  {"type": "table", "columns": ["A", "B"]},
                  {"text": "bloco sem tipo"},
                  {"type": "note", "text": "cuidado"}
                ]}]}],
                 "species": {"iceagesurvival:sem_nada": {}}}
                """));

        assertEquals(1, manual.chapters().size());
        Manual.Chapter chapter = manual.chapters().get(0);
        assertEquals("c1", chapter.id());
        assertEquals("", chapter.title(), "título faltando devia virar texto vazio");
        assertEquals("", chapter.subtitle(), "subtítulo faltando devia virar texto vazio");
        Manual.Page page = chapter.pages().get(0);
        assertEquals("", page.title(), "título de página faltando devia virar texto vazio");
        assertEquals(List.of(
                new Manual.Heading(""),
                new Manual.Paragraph("Olá"),
                new Manual.Bullets(true, List.of("um", "dois")),
                new Manual.Bullets(false, List.of("a")),
                new Manual.Table(List.of(), List.of(List.of("x", "y"), List.of("z"))),
                new Manual.Table(List.of("A", "B"), List.of()),
                new Manual.Note("cuidado")), page.blocks(),
                "tipo desconhecido (ou sem tipo) devia ser pulado e o resto lido na ordem");

        Manual.Sheet sheet = manual.sheet("iceagesurvival:sem_nada").orElseThrow();
        assertEquals("", sheet.name(), "nome faltando devia virar texto vazio");
        assertEquals(List.of(), sheet.badges(), "selos faltando deviam virar lista vazia");
        assertEquals(List.of(), sheet.blocks(), "blocos faltando deviam virar lista vazia");
        assertEquals(List.of(sheet), manual.dexEntries(), "ficha sem selo nenhum não está desligada");
    }

    @Test
    void emptyDocumentGivesAnEmptyManual() {
        Manual manual = ManualParser.parse(new StringReader("{}"));
        assertTrue(manual.chapters().isEmpty(), "capítulos sem a chave chapters");
        assertTrue(manual.sheets().isEmpty(), "fichas sem a chave species");
        assertTrue(manual.dexEntries().isEmpty(), "DINO FILE sem a chave species");
    }

    private static Manual realManual() {
        try (Reader reader = Files.newBufferedReader(MANUAL, StandardCharsets.UTF_8)) {
            return ManualParser.parse(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** As chaves de {@code species} na ordem em que aparecem no texto do arquivo (cada id aparece uma vez só). */
    private static List<String> speciesKeysInFileOrder() {
        String text;
        try {
            text = Files.readString(MANUAL, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String species = text.substring(text.indexOf("\"species\""));
        Matcher key = Pattern.compile("\"(iceagesurvival:[a-z0-9_]+)\"\\s*:").matcher(species);
        List<String> keys = new ArrayList<>();
        while (key.find()) {
            keys.add(key.group(1));
        }
        assertEquals(keys.size(), Set.copyOf(keys).size(), "id de espécie repetido no texto do manual: " + keys);
        return keys;
    }
}
