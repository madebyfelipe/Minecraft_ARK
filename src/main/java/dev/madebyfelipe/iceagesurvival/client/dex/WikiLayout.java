package dev.madebyfelipe.iceagesurvival.client.dex;

import dev.madebyfelipe.iceagesurvival.client.HudShapes;
import dev.madebyfelipe.iceagesurvival.client.TechStyle;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Diagrama os blocos do manual numa largura: títulos com o losango âmbar, parágrafos quebrados em linhas, listas com
 * marcador ciano, avisos com a barra âmbar e tabelas com as colunas repartidas pelo conteúdo. As tabelas sem
 * cabeçalho (rótulo e valor, as das fichas) saem como ficha de dados: rótulo em ciano, valor em branco. O holograma
 * desenha os modelos das espécies lado a lado ({@link ComparisonHologram}).
 */
final class WikiLayout {
    static final int PARAGRAPH = 0xFFD3DEE4;
    private static final int LINE = 10;
    private static final int GAP = 5;
    private static final int CELL_PAD = 3;
    private static final int MIN_COLUMN = 26;
    private static final int ROW_EVEN = 0x18A8F4FF;
    private static final int ROW_ODD = 0x08A8F4FF;
    private static final int NOTE_BACK = 0x20FFB21E;
    private static final int NOTE_TEXT = 0xFFFFD98A;

    /** Um pedaço desenhável, com a altura que ocupa. */
    private interface Element {
        int height();

        void draw(GuiGraphics graphics, Font font, int x, int y);
    }

    private final List<Element> elements;
    private final int height;

    private WikiLayout(List<Element> elements) {
        this.elements = elements;
        this.height = elements.stream().mapToInt(Element::height).sum();
    }

    static WikiLayout of(Font font, List<Manual.Block> blocks, int width) {
        List<Element> elements = new ArrayList<>();
        for (Manual.Block block : blocks) {
            if (block instanceof Manual.Heading heading) {
                elements.add(heading(font, heading.text(), width));
            } else if (block instanceof Manual.Paragraph paragraph) {
                elements.add(text(font, paragraph.text(), width, 0, PARAGRAPH, ""));
            } else if (block instanceof Manual.Note note) {
                elements.add(note(font, note.text(), width));
            } else if (block instanceof Manual.Bullets bullets) {
                for (int index = 0; index < bullets.items().size(); index++) {
                    String marker = bullets.ordered() ? (index + 1) + "." : "▪";
                    elements.add(text(font, bullets.items().get(index), width, bullets.ordered() ? 14 : 10, PARAGRAPH, marker));
                }
                elements.add(spacer(GAP - 2));
            } else if (block instanceof Manual.Table table) {
                elements.add(table(font, table, width));
            } else if (block instanceof Manual.Hologram hologram) {
                elements.add(hologram(hologram.species(), width));
            }
        }
        return new WikiLayout(List.copyOf(elements));
    }

    int height() {
        return height;
    }

    /** Desenha a partir de (x, y), pulando o que cai fora de [clipTop, clipBottom) (o recorte segura o resto). */
    void draw(GuiGraphics graphics, Font font, int x, int y, int clipTop, int clipBottom) {
        int at = y;
        for (Element element : elements) {
            int bottom = at + element.height();
            if (bottom > clipTop && at < clipBottom) {
                element.draw(graphics, font, x, at);
            }
            at = bottom;
        }
    }

    private static Element spacer(int size) {
        return new Element() {
            @Override
            public int height() {
                return size;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
            }
        };
    }

    /** O holograma comparativo ({@link ComparisonHologram}), na largura toda. */
    private static Element hologram(List<String> species, int width) {
        return new Element() {
            @Override
            public int height() {
                return ComparisonHologram.HEIGHT + GAP;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
                ComparisonHologram.draw(graphics, font, species, x, y, x + width, y + ComparisonHologram.HEIGHT);
            }
        };
    }

    private static Element heading(Font font, String text, int width) {
        List<FormattedCharSequence> lines = font.split(TechStyle.titleText(Component.literal(text)), width - 10);
        return new Element() {
            @Override
            public int height() {
                return 4 + lines.size() * LINE + 3;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
                TechStyle.marker(graphics, x, y + 4);
                for (int line = 0; line < lines.size(); line++) {
                    graphics.drawString(font, lines.get(line), x + 10, y + 4 + line * LINE, TechStyle.TEXT);
                }
                graphics.fill(x + 10, y + 4 + lines.size() * LINE, x + width, y + 5 + lines.size() * LINE,
                        HudShapes.fade(TechStyle.ACCENT, 0.35F));
            }
        };
    }

    /** Texto quebrado em linhas, com recuo e, opcionalmente, um marcador na primeira linha. */
    private static Element text(Font font, String text, int width, int indent, int color, String marker) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width - indent);
        return new Element() {
            @Override
            public int height() {
                return lines.size() * LINE + (marker.isEmpty() ? GAP : 1);
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
                if (!marker.isEmpty()) {
                    graphics.drawString(font, marker, x + 1, y, TechStyle.ACCENT, false);
                }
                for (int line = 0; line < lines.size(); line++) {
                    graphics.drawString(font, lines.get(line), x + indent, y + line * LINE, color, false);
                }
            }
        };
    }

    private static Element note(Font font, String text, int width) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width - 10);
        return new Element() {
            @Override
            public int height() {
                return lines.size() * LINE + 6 + GAP;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
                int bottom = y + lines.size() * LINE + 6;
                graphics.fill(x, y, x + width, bottom, NOTE_BACK);
                graphics.fill(x, y, x + 2, bottom, TechStyle.AMBER);
                for (int line = 0; line < lines.size(); line++) {
                    graphics.drawString(font, lines.get(line), x + 7, y + 3 + line * LINE, NOTE_TEXT, false);
                }
            }
        };
    }

    private static Element table(Font font, Manual.Table table, int width) {
        boolean header = !table.columns().isEmpty();
        List<List<String>> rows = new ArrayList<>();
        if (header) {
            rows.add(table.columns());
        }
        rows.addAll(table.rows());
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0) {
            return spacer(0);
        }
        int[] widths = columnWidths(font, rows, columns, width);
        List<List<List<FormattedCharSequence>>> cells = new ArrayList<>();
        int[] heights = new int[rows.size()];
        for (int row = 0; row < rows.size(); row++) {
            List<List<FormattedCharSequence>> line = new ArrayList<>();
            int tallest = 1;
            for (int column = 0; column < columns; column++) {
                String value = column < rows.get(row).size() ? rows.get(row).get(column) : "";
                List<FormattedCharSequence> wrapped = font.split(Component.literal(value), widths[column] - CELL_PAD * 2);
                line.add(wrapped);
                tallest = Math.max(tallest, wrapped.size());
            }
            cells.add(line);
            heights[row] = tallest * LINE + 3;
        }
        int total = 0;
        for (int rowHeight : heights) {
            total += rowHeight;
        }
        int tableHeight = total;
        return new Element() {
            @Override
            public int height() {
                return tableHeight + GAP + 1;
            }

            @Override
            public void draw(GuiGraphics graphics, Font font, int x, int y) {
                int top = y;
                for (int row = 0; row < cells.size(); row++) {
                    boolean isHeader = header && row == 0;
                    int bottom = top + heights[row];
                    graphics.fill(x, top, x + width, bottom, isHeader ? TechStyle.METAL_DARK : row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    int cellX = x;
                    for (int column = 0; column < columns; column++) {
                        int color = isHeader ? TechStyle.BRIGHT : !header && column == 0 ? TechStyle.ACCENT : TechStyle.TEXT;
                        List<FormattedCharSequence> wrapped = cells.get(row).get(column);
                        for (int line = 0; line < wrapped.size(); line++) {
                            graphics.drawString(font, wrapped.get(line), cellX + CELL_PAD, top + 2 + line * LINE, color, false);
                        }
                        cellX += widths[column];
                        if (column < columns - 1) {
                            graphics.fill(cellX - 1, top, cellX, bottom, 0x30A8F4FF);
                        }
                    }
                    top = bottom;
                }
                TechStyle.border(graphics, x - 1, y - 1, x + width + 1, top + 1, HudShapes.fade(TechStyle.METAL, 0.8F));
            }
        };
    }

    /**
     * Larguras das colunas: a natural de cada uma (o texto mais largo numa linha só) se couber; senão, cada coluna
     * começa no mínimo (a palavra mais longa, ou {@link #MIN_COLUMN}) e reparte o que sobra pela falta de cada uma.
     */
    private static int[] columnWidths(Font font, List<List<String>> rows, int columns, int width) {
        int[] natural = new int[columns];
        int[] minimum = new int[columns];
        for (List<String> row : rows) {
            for (int column = 0; column < row.size() && column < columns; column++) {
                String value = row.get(column);
                natural[column] = Math.max(natural[column], font.width(value) + CELL_PAD * 2);
                for (String word : value.split("\\s+")) {
                    minimum[column] = Math.max(minimum[column], font.width(word) + CELL_PAD * 2);
                }
            }
        }
        int naturalSum = 0;
        int minimumSum = 0;
        for (int column = 0; column < columns; column++) {
            minimum[column] = Math.max(MIN_COLUMN, Math.min(minimum[column], natural[column]));
            natural[column] = Math.max(natural[column], minimum[column]);
            naturalSum += natural[column];
            minimumSum += minimum[column];
        }
        int[] widths = new int[columns];
        if (naturalSum <= width) {
            System.arraycopy(natural, 0, widths, 0, columns);
            widths[columns - 1] += width - naturalSum;
        } else if (minimumSum >= width) {
            for (int column = 0; column < columns; column++) {
                widths[column] = width / columns;
            }
            widths[columns - 1] += width - width / columns * columns;
        } else {
            int spare = width - minimumSum;
            int want = naturalSum - minimumSum;
            int used = 0;
            for (int column = 0; column < columns; column++) {
                widths[column] = minimum[column] + (natural[column] - minimum[column]) * spare / want;
                used += widths[column];
            }
            widths[columns - 1] += width - used;
        }
        return widths;
    }
}
