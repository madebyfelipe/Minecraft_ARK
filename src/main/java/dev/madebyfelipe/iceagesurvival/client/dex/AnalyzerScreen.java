package dev.madebyfelipe.iceagesurvival.client.dex;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.madebyfelipe.iceagesurvival.client.HudShapes;
import dev.madebyfelipe.iceagesurvival.client.TechButton;
import dev.madebyfelipe.iceagesurvival.client.TechStyle;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import dev.madebyfelipe.iceagesurvival.network.ScanResultPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/**
 * O terminal do Analisador, no visual de Dino Crisis 2 do {@link TechStyle}. Três abas:
 * <ul>
 *   <li><b>DINO FILE</b>: a lista numerada das espécies (as não registradas como "???"), o modelo da escolhida girando
 *   numa plataforma (silhueta enquanto não registrada), a ficha dela do manual e a ANÁLISE DO INDIVÍDUO do último
 *   exemplar escaneado;</li>
 *   <li><b>MANUAL</b>: os capítulos e páginas do manual gerado de {@code tools/wiki_lore/};</li>
 *   <li><b>REGISTROS</b>: os registros militares recuperados nos terminais dos postos, na ordem de leitura (os ainda
 *   não recuperados como "???").</li>
 * </ul>
 * Aba, espécie, página e registro ficam lembrados entre uma abertura e outra.
 */
public class AnalyzerScreen extends Screen {
    private enum Tab { DEX, MANUAL, RECORDS }

    private static final int MAX_WIDTH = 404;
    private static final int MAX_HEIGHT = 232;
    private static final int LIST_WIDTH = 112;
    private static final int ROW = 11;
    private static final int VIEW_WIDTH = 100;
    private static final int TAB_WIDTH = 64;
    private static final int SCROLL_STEP = 20;
    /** Por quanto tempo o aviso de registro novo pisca, em ms. */
    private static final long NEW_ENTRY_MILLIS = 5000;

    private static final int LOCKED = 0xFF4E6370;
    private static final int VIEW_BACK = 0xFF040D12;
    private static final int VIEW_GRID = 0x1838C6D9;
    private static final int SELECTED = 0x40FFB21E;
    private static final int HEALTH = 0xFFD04848;
    private static final int TORPOR = 0xFF9B59D0;
    private static final int MEAT = 0xFFE8743B;
    private static final int PLANT = 0xFF7FD17F;

    private static Tab lastTab = Tab.DEX;
    private static String lastSpecies = "";
    private static String lastPage = "";
    private static String lastSeries = "";
    private static int lastRecord;
    private static final Map<String, LivingEntity> MODELS = new HashMap<>();
    @Nullable
    private static ClientLevel modelsLevel;

    private final long openedAt = Util.getMillis();
    private final boolean newEntry;
    private Tab tab;
    private int left;
    private int top;
    private int right;
    private int bottom;
    private int contentTop;
    private int contentBottom;
    private int listScroll;
    private int contentScroll;
    private int contentHeight;
    @Nullable
    private WikiLayout layout;
    private String layoutKey = "";
    private TechButton dexButton;
    private TechButton manualButton;
    private TechButton recordsButton;

    private AnalyzerScreen(Tab tab, boolean newEntry) {
        super(Component.translatable("iceagesurvival.analyzer.title"));
        this.tab = tab;
        this.newEntry = newEntry;
    }

    /** Clique no ar com o Analisador. */
    public static void open() {
        Minecraft.getInstance().setScreen(new AnalyzerScreen(lastTab, false));
    }

    /** Terminou um scan: a ficha da espécie, com o aviso se o registro é novo. */
    static void openOnScan(String species, boolean newEntry) {
        lastTab = Tab.DEX;
        lastSpecies = species;
        Minecraft.getInstance().setScreen(new AnalyzerScreen(Tab.DEX, newEntry));
    }

    /** Destravou um registro novo: abre REGISTROS nele, com o aviso. */
    static void openOnRecord(String series, int index) {
        lastTab = Tab.RECORDS;
        lastSeries = series;
        lastRecord = index;
        Minecraft.getInstance().setScreen(new AnalyzerScreen(Tab.RECORDS, true));
    }

    // ---- Dados ----

    private static List<Manual.Sheet> entries() {
        return WikiManual.get().dexEntries();
    }

    /** O índice do manual: cada capítulo seguido das páginas dele. */
    private record IndexRow(@Nullable Manual.Chapter chapter, @Nullable Manual.Page page) {
    }

    private static List<IndexRow> index() {
        List<IndexRow> rows = new ArrayList<>();
        for (Manual.Chapter chapter : WikiManual.get().chapters()) {
            rows.add(new IndexRow(chapter, null));
            chapter.pages().forEach(page -> rows.add(new IndexRow(chapter, page)));
        }
        return rows;
    }

    /** Uma linha da lista de REGISTROS: o título de uma série ({@code index} −1) ou um registro dela. */
    private record RecordRow(Manual.Series series, int index) {
        boolean header() {
            return index < 0;
        }

        boolean unlocked() {
            return index >= 0 && index < DinoFileClient.records(series.id());
        }
    }

    private static List<RecordRow> recordRows() {
        List<RecordRow> rows = new ArrayList<>();
        for (Manual.Series series : WikiManual.get().series()) {
            rows.add(new RecordRow(series, -1));
            for (int index = 0; index < series.records().size(); index++) {
                rows.add(new RecordRow(series, index));
            }
        }
        return rows;
    }

    private static int totalRecords() {
        return WikiManual.get().series().stream().mapToInt(series -> series.records().size()).sum();
    }

    private static int unlockedRecords() {
        return WikiManual.get().series().stream()
                .mapToInt(series -> Math.min(DinoFileClient.records(series.id()), series.records().size())).sum();
    }

    private static int registeredCount() {
        return (int) entries().stream().filter(sheet -> DinoFileClient.isRegistered(sheet.species())).count();
    }

    private Optional<Manual.Sheet> selectedSheet() {
        return entries().stream().filter(sheet -> sheet.species().equals(lastSpecies)).findFirst();
    }

    private Optional<IndexRow> selectedPage() {
        return index().stream().filter(row -> row.page() != null && row.page().id().equals(lastPage)).findFirst();
    }

    private void ensureSelection() {
        List<Manual.Sheet> entries = entries();
        if (!entries.isEmpty() && selectedSheet().isEmpty()) {
            lastSpecies = entries.get(0).species();
        }
        if (selectedPage().isEmpty()) {
            index().stream().filter(row -> row.page() != null).findFirst()
                    .ifPresent(row -> lastPage = row.page().id());
        }
    }

    // ---- Montagem ----

    @Override
    protected void init() {
        int panelWidth = Math.min(MAX_WIDTH, width - 20);
        int panelHeight = Math.min(MAX_HEIGHT, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        right = left + panelWidth;
        bottom = top + panelHeight;
        contentTop = top + 26;
        contentBottom = bottom - 16;
        dexButton = addRenderableWidget(new TechButton(right - 8 - TAB_WIDTH * 3 - 8, top + 5, TAB_WIDTH, 14,
                Component.translatable("iceagesurvival.analyzer.tab.dex"), TechStyle.ACCENT, true,
                button -> switchTab(Tab.DEX)));
        manualButton = addRenderableWidget(new TechButton(right - 8 - TAB_WIDTH * 2 - 4, top + 5, TAB_WIDTH, 14,
                Component.translatable("iceagesurvival.analyzer.tab.manual"), TechStyle.ACCENT, true,
                button -> switchTab(Tab.MANUAL)));
        recordsButton = addRenderableWidget(new TechButton(right - 8 - TAB_WIDTH, top + 5, TAB_WIDTH, 14,
                Component.translatable("iceagesurvival.analyzer.tab.records"), TechStyle.ACCENT, true,
                button -> switchTab(Tab.RECORDS)));
        ensureSelection();
        updateTabs();
        layoutKey = "";
        scrollListToSelection();
    }

    private void switchTab(Tab next) {
        tab = next;
        lastTab = next;
        contentScroll = 0;
        listScroll = 0;
        layoutKey = "";
        updateTabs();
        scrollListToSelection();
    }

    private void updateTabs() {
        dexButton.active = tab != Tab.DEX;
        manualButton.active = tab != Tab.MANUAL;
        recordsButton.active = tab != Tab.RECORDS;
    }

    private int listTop() {
        return tab == Tab.MANUAL ? contentTop : contentTop + 12;
    }

    private int visibleRows() {
        return Math.max(1, (contentBottom - listTop()) / ROW);
    }

    private int listSize() {
        return switch (tab) {
            case DEX -> entries().size();
            case MANUAL -> index().size();
            case RECORDS -> recordRows().size();
        };
    }

    private int selectedRow() {
        if (tab == Tab.RECORDS) {
            List<RecordRow> rows = recordRows();
            for (int row = 0; row < rows.size(); row++) {
                if (!rows.get(row).header() && rows.get(row).series().id().equals(lastSeries)
                        && rows.get(row).index() == lastRecord) {
                    return row;
                }
            }
            for (int row = 0; row < rows.size(); row++) {
                if (!rows.get(row).header()) {
                    return row;
                }
            }
            return 0;
        }
        if (tab == Tab.DEX) {
            List<Manual.Sheet> entries = entries();
            for (int row = 0; row < entries.size(); row++) {
                if (entries.get(row).species().equals(lastSpecies)) {
                    return row;
                }
            }
        } else {
            List<IndexRow> index = index();
            for (int row = 0; row < index.size(); row++) {
                if (index.get(row).page() != null && index.get(row).page().id().equals(lastPage)) {
                    return row;
                }
            }
        }
        return 0;
    }

    private void scrollListToSelection() {
        int row = selectedRow();
        int visible = visibleRows();
        if (row < listScroll) {
            listScroll = row;
        } else if (row >= listScroll + visible) {
            listScroll = row - visible + 1;
        }
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, listSize() - visible)));
    }

    private void select(int row) {
        if (tab == Tab.RECORDS) {
            List<RecordRow> rows = recordRows();
            if (row >= 0 && row < rows.size() && !rows.get(row).header()) {
                lastSeries = rows.get(row).series().id();
                lastRecord = rows.get(row).index();
            }
        } else if (tab == Tab.DEX) {
            List<Manual.Sheet> entries = entries();
            if (row >= 0 && row < entries.size()) {
                lastSpecies = entries.get(row).species();
            }
        } else {
            List<IndexRow> index = index();
            if (row >= 0 && row < index.size() && index.get(row).page() != null) {
                lastPage = index.get(row).page().id();
            }
        }
        contentScroll = 0;
        scrollListToSelection();
    }

    // ---- Entrada ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= left + 6 && mouseX < left + 6 + LIST_WIDTH && mouseY >= listTop()
                && mouseY < listTop() + visibleRows() * ROW) {
            int row = listScroll + (int) ((mouseY - listTop()) / ROW);
            if (row < listSize()) {
                select(row);
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 1.8F, 0.4F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < left + 6 + LIST_WIDTH) {
            int max = Math.max(0, listSize() - visibleRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
        } else {
            int max = Math.max(0, contentHeight - (contentBottom - contentTop));
            contentScroll = Math.max(0, Math.min(max, contentScroll - (int) Math.signum(delta) * SCROLL_STEP));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            int step = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            int row = selectedRow() + step;
            if (tab == Tab.MANUAL) {
                List<IndexRow> index = index();
                while (row >= 0 && row < index.size() && index.get(row).page() == null) {
                    row += step;
                }
            } else if (tab == Tab.RECORDS) {
                List<RecordRow> rows = recordRows();
                while (row >= 0 && row < rows.size() && rows.get(row).header()) {
                    row += step;
                }
            }
            select(row);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            switchTab(Tab.values()[(tab.ordinal() + 1) % Tab.values().length]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Desenho ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TechStyle.frame(graphics, left, top, right, bottom);
        TechStyle.marker(graphics, left + 8, top + 8);
        graphics.drawString(font, TechStyle.titleText(title), left + 18, top + 8, TechStyle.TEXT);
        TechStyle.rule(graphics, left + 8, right - 8, top + 22);
        if (WikiManual.get().sheets().isEmpty() && WikiManual.get().chapters().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("iceagesurvival.analyzer.no_manual"),
                    (left + right) / 2, (top + bottom) / 2, TechStyle.DANGER);
        } else if (tab == Tab.DEX) {
            renderDexList(graphics, mouseX, mouseY);
            renderDex(graphics);
        } else if (tab == Tab.RECORDS) {
            renderRecordList(graphics, mouseX, mouseY);
            renderRecord(graphics);
        } else {
            renderIndex(graphics, mouseX, mouseY);
            renderPage(graphics);
        }
        renderFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFooter(GuiGraphics graphics) {
        int y = bottom - 12;
        graphics.fill(left + 6, y - 3, right - 6, y - 2, HudShapes.fade(TechStyle.METAL, 0.8F));
        graphics.drawString(font, (TechStyle.blink() ? "▶ " : "  ") + Component.translatable(
                "iceagesurvival.analyzer.footer").getString(), left + 8, y, TechStyle.ACCENT, false);
        Component count = tab == Tab.RECORDS
                ? Component.translatable("iceagesurvival.analyzer.recovered", unlockedRecords(), totalRecords())
                : Component.translatable("iceagesurvival.analyzer.registered", registeredCount(), entries().size());
        graphics.drawString(font, count, right - 8 - font.width(count), y, TechStyle.AMBER, false);
    }

    private void renderListFrame(GuiGraphics graphics, int listTop, int rows) {
        int x = left + 6;
        graphics.fill(x, listTop - 1, x + LIST_WIDTH, listTop + rows * ROW + 1, 0x60040D12);
        TechStyle.border(graphics, x - 1, listTop - 2, x + LIST_WIDTH + 1, listTop + rows * ROW + 2,
                HudShapes.fade(TechStyle.METAL, 0.7F));
        if (listSize() > rows) {
            int trackTop = listTop;
            int trackHeight = rows * ROW;
            int thumb = Math.max(6, trackHeight * rows / listSize());
            int thumbTop = trackTop + (trackHeight - thumb) * listScroll / Math.max(1, listSize() - rows);
            graphics.fill(x + LIST_WIDTH - 3, trackTop, x + LIST_WIDTH - 1, trackTop + trackHeight, TechStyle.SLOT_EDGE);
            graphics.fill(x + LIST_WIDTH - 3, thumbTop, x + LIST_WIDTH - 1, thumbTop + thumb, TechStyle.AMBER);
        }
    }

    private void renderDexList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Manual.Sheet> entries = entries();
        int x = left + 6;
        graphics.drawString(font, Component.translatable("iceagesurvival.analyzer.tab.dex").getString()
                .toUpperCase(Locale.ROOT), x + 1, contentTop, TechStyle.ACCENT, false);
        int listTop = listTop();
        int rows = visibleRows();
        renderListFrame(graphics, listTop, rows);
        for (int index = 0; index < rows && listScroll + index < entries.size(); index++) {
            int row = listScroll + index;
            Manual.Sheet sheet = entries.get(row);
            int y = listTop + index * ROW;
            boolean selected = sheet.species().equals(lastSpecies);
            boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW;
            if (selected) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, SELECTED);
                if (TechStyle.blink()) {
                    graphics.drawString(font, "▶", x + 1, y + 2, TechStyle.AMBER, false);
                }
            } else if (hovered) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, TechStyle.HOVER);
            }
            boolean known = DinoFileClient.isRegistered(sheet.species());
            String number = String.format(Locale.ROOT, "No.%02d", row + 1);
            graphics.drawString(font, number, x + 9, y + 2, known ? TechStyle.AMBER : LOCKED, false);
            String name = known ? sheet.name().toUpperCase(Locale.ROOT) : "???";
            graphics.drawString(font, ellipsize(name, LIST_WIDTH - 48), x + 42, y + 2,
                    known ? (selected ? TechStyle.TEXT : TechStyle.BRIGHT) : LOCKED, false);
        }
    }

    private void renderDex(GuiGraphics graphics) {
        Optional<Manual.Sheet> sheet = selectedSheet();
        if (sheet.isEmpty()) {
            return;
        }
        boolean known = DinoFileClient.isRegistered(sheet.get().species());
        int viewLeft = left + 6 + LIST_WIDTH + 6;
        renderViewport(graphics, sheet.get(), known, viewLeft, contentTop, viewLeft + VIEW_WIDTH, contentBottom);

        int dataLeft = viewLeft + VIEW_WIDTH + 6;
        int dataRight = right - 12;
        int width = dataRight - dataLeft;
        String key = "dex:" + sheet.get().species() + ":" + known + ":" + width;
        if (!key.equals(layoutKey)) {
            layoutKey = key;
            layout = known ? WikiLayout.of(font, sheet.get().blocks(), width) : null;
        }
        Optional<ScanResultPayload> scan = known ? DinoFileClient.lastScan(sheet.get().species()) : Optional.empty();
        int headerHeight = 34;
        int analysisHeight = scan.isPresent() ? 76 : 0;
        int body = known ? (layout == null ? 0 : layout.height()) : 40;
        contentHeight = headerHeight + analysisHeight + body;
        clampContentScroll();

        graphics.enableScissor(dataLeft - 2, contentTop, dataRight + 2, contentBottom);
        int y = contentTop - contentScroll;
        renderSheetHeader(graphics, sheet.get(), known, dataLeft, y, dataRight);
        y += headerHeight;
        if (scan.isPresent()) {
            renderAnalysis(graphics, scan.get(), dataLeft, y, dataRight);
            y += analysisHeight;
        }
        if (known && layout != null) {
            layout.draw(graphics, font, dataLeft, y, contentTop, contentBottom);
        } else if (!known) {
            for (var line : font.split(Component.translatable("iceagesurvival.analyzer.locked"), width)) {
                graphics.drawString(font, line, dataLeft, y, TechStyle.SUBTLE, false);
                y += 10;
            }
        }
        graphics.disableScissor();
        renderContentScrollbar(graphics, dataRight + 4);
    }

    private void renderSheetHeader(GuiGraphics graphics, Manual.Sheet sheet, boolean known, int x, int y, int right) {
        int row = entries().indexOf(sheet) + 1;
        String number = String.format(Locale.ROOT, "No.%02d", row);
        graphics.drawString(font, number, right - font.width(number), y + 1, TechStyle.AMBER, false);
        Component name = TechStyle.titleText(Component.literal(known ? sheet.name() : "???"));
        graphics.drawString(font, name, x, y + 1, known ? TechStyle.TEXT : LOCKED);
        int badgeX = x;
        int badgeY = y + 14;
        if (known) {
            for (String badge : sheet.badges()) {
                int color = badgeColor(badge);
                String text = badge.toUpperCase(Locale.ROOT);
                int width = font.width(text) + 6;
                if (badgeX + width > right) {
                    break;
                }
                graphics.fill(badgeX, badgeY, badgeX + width, badgeY + 11, HudShapes.fade(color, 0.18F));
                TechStyle.border(graphics, badgeX, badgeY, badgeX + width, badgeY + 11, color);
                graphics.drawString(font, text, badgeX + 3, badgeY + 2, color, false);
                badgeX += width + 3;
            }
        }
        graphics.fill(x, y + 29, right, y + 30, HudShapes.fade(TechStyle.ACCENT, 0.35F));
    }

    private static int badgeColor(String badge) {
        String lower = badge.toLowerCase(Locale.ROOT);
        if (lower.startsWith("carn")) {
            return MEAT;
        }
        if (lower.startsWith("herb")) {
            return PLANT;
        }
        if (lower.startsWith("agress") || lower.startsWith("apex")) {
            return TechStyle.DANGER;
        }
        return TechStyle.ACCENT;
    }

    /** A leitura do último exemplar escaneado: nome, nível, sexo, vida, torpor, estado e o selo de perigo. */
    private void renderAnalysis(GuiGraphics graphics, ScanResultPayload scan, int x, int y, int right) {
        int boxBottom = y + 70;
        graphics.fill(x, y, right, boxBottom, 0x50040D12);
        TechStyle.border(graphics, x, y, right, boxBottom, HudShapes.fade(TechStyle.METAL, 0.9F));
        graphics.fill(x + 1, y + 1, right - 1, y + 11, TechStyle.METAL_DARK);
        graphics.drawString(font, Component.translatable("iceagesurvival.analyzer.individual").getString()
                .toUpperCase(Locale.ROOT), x + 4, y + 2, TechStyle.BRIGHT, false);
        Component danger = Component.translatable("iceagesurvival.analyzer.danger." + scan.danger());
        int dangerColor = scan.danger() == 2 ? (TechStyle.blink() ? TechStyle.DANGER : HudShapes.fade(TechStyle.DANGER, 0.5F))
                : scan.danger() == 1 ? TechStyle.AMBER : TechStyle.ACCENT;
        graphics.drawString(font, danger, right - 4 - font.width(danger), y + 2, dangerColor, false);

        int textY = y + 14;
        String sex = scan.female() ? "♀" : "♂";
        Component level = Component.translatable("iceagesurvival.hud.level", scan.level());
        String name = font.plainSubstrByWidth(scan.name(), right - x - 12 - font.width(level) - font.width(sex));
        graphics.drawString(font, name, x + 4, textY, TechStyle.TEXT, false);
        graphics.drawString(font, sex, x + 6 + font.width(name), textY, scan.female() ? 0xFFFF7BC4 : 0xFF5BA8FF, false);
        graphics.drawString(font, level, right - 4 - font.width(level), textY, TechStyle.AMBER, false);

        int barLeft = x + 4 + 34;
        int barWidth = right - 4 - barLeft - 46;
        textY += 11;
        graphics.drawString(font, Component.translatable("iceagesurvival.stat.health"), x + 4, textY, TechStyle.SUBTLE, false);
        TechStyle.bar(graphics, barLeft, textY + 2, barWidth, 4, scan.health() / Math.max(1.0F, scan.maxHealth()), HEALTH);
        String health = Math.round(scan.health()) + "/" + Math.round(scan.maxHealth());
        graphics.drawString(font, health, right - 4 - font.width(health), textY, TechStyle.TEXT, false);
        textY += 10;
        graphics.drawString(font, Component.translatable("iceagesurvival.stat.torpor"), x + 4, textY, TechStyle.SUBTLE, false);
        TechStyle.bar(graphics, barLeft, textY + 2, barWidth, 4, scan.torpor(), TORPOR);
        String torpor = Math.round(scan.torpor() * 100) + "%";
        graphics.drawString(font, torpor, right - 4 - font.width(torpor), textY, TechStyle.TEXT, false);
        textY += 11;
        Component state = switch (scan.state()) {
            case WILD -> Component.translatable("iceagesurvival.analyzer.state.wild",
                    Component.translatable("iceagesurvival.mood." + scan.mood()));
            case UNCONSCIOUS -> Component.translatable("iceagesurvival.analyzer.state.unconscious");
            case YOURS -> Component.translatable("iceagesurvival.analyzer.state.yours");
            case OTHERS -> Component.translatable("iceagesurvival.analyzer.state.others", scan.owner());
        };
        graphics.drawString(font, font.plainSubstrByWidth(state.getString(), right - x - 8), x + 4, textY,
                TechStyle.ACCENT, false);
        Component combat = Component.translatable("iceagesurvival.analyzer.combat",
                String.format(Locale.ROOT, "%.0f", scan.attack()), String.format(Locale.ROOT, "%.0f", scan.armor()));
        textY += 10;
        graphics.drawString(font, combat, x + 4, textY, TechStyle.SUBTLE, false);
    }

    /** A plataforma de holograma com o modelo girando; silhueta escura enquanto a espécie não está registrada. */
    private void renderViewport(GuiGraphics graphics, Manual.Sheet sheet, boolean known, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, VIEW_BACK);
        for (int gx = x1 + 8; gx < x2; gx += 8) {
            graphics.fill(gx, y1 + 1, gx + 1, y2 - 1, VIEW_GRID);
        }
        for (int gy = y2 - 14; gy > y1; gy -= 8) {
            graphics.fill(x1 + 1, gy, x2 - 1, gy + 1, VIEW_GRID);
        }
        TechStyle.border(graphics, x1, y1, x2, y2, TechStyle.SLOT_EDGE);
        TechStyle.corners(graphics, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 6);

        int centerX = (x1 + x2) / 2;
        int feetY = y2 - 16;
        // Plataforma: anéis achatados em perspectiva.
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, feetY, 0.0F);
        graphics.pose().scale(1.0F, 0.3F, 1.0F);
        HudShapes.disc(graphics, 0.0F, 0.0F, (x2 - x1) / 2.0F - 6.0F, HudShapes.fade(TechStyle.ACCENT, 0.12F));
        HudShapes.ring(graphics, 0.0F, 0.0F, (x2 - x1) / 2.0F - 7.0F, (x2 - x1) / 2.0F - 6.0F, TechStyle.ACCENT);
        HudShapes.ring(graphics, 0.0F, 0.0F, (x2 - x1) / 4.0F - 1.0F, (x2 - x1) / 4.0F, HudShapes.fade(TechStyle.ACCENT, 0.6F));
        graphics.pose().popPose();

        LivingEntity model = model(sheet.species());
        if (model != null) {
            float visualHeight = model.getBbHeight() * 1.25F;
            float visualLength = Math.max(model.getBbWidth(), model.getBbHeight() * 0.5F) * 2.2F;
            // Girando, o corpo comprido passa de frente para o lado: cabe pelo comprimento com folga.
            float fit = Math.min((y2 - y1 - 34) / visualHeight, (x2 - x1 - 10) / (visualLength * 1.35F));
            int scale = Math.max(3, Math.round(fit));
            float angle = (Util.getMillis() % 9000L) / 9000.0F * 360.0F;
            model.yBodyRot = angle;
            model.yBodyRotO = angle;
            model.setYRot(angle);
            model.yRotO = angle;
            model.yHeadRot = angle;
            model.yHeadRotO = angle;
            model.setXRot(0.0F);
            model.xRotO = 0.0F;
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateX((float) Math.toRadians(-12.0));
            graphics.enableScissor(x1 + 1, y1 + 1, x2 - 1, y2 - 1);
            if (!known) {
                RenderSystem.setShaderColor(0.03F, 0.09F, 0.12F, 1.0F);
            }
            try {
                InventoryScreen.renderEntityInInventory(graphics, centerX, feetY, scale, pose, null, model);
            } finally {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.disableScissor();
            }
        }
        if (!known) {
            graphics.drawCenteredString(font, "???", centerX, y1 + 22, TechStyle.blink() ? LOCKED : TechStyle.SUBTLE);
        }
        if (newEntry && sheet.species().equals(lastSpecies) && Util.getMillis() - openedAt < NEW_ENTRY_MILLIS
                && TechStyle.blink()) {
            Component banner = Component.translatable("iceagesurvival.analyzer.new_entry");
            int width = font.width(banner) + 8;
            graphics.fill(centerX - width / 2, y1 + 5, centerX + width / 2, y1 + 17, HudShapes.fade(TechStyle.AMBER, 0.85F));
            graphics.drawCenteredString(font, banner, centerX, y1 + 7, 0xFF1A1206);
        }
    }

    /** Um exemplar de mentira da espécie, para o modelo da ficha (um por espécie, refeito ao trocar de mundo). */
    @Nullable
    private static LivingEntity model(String species) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        if (modelsLevel != minecraft.level) {
            MODELS.clear();
            modelsLevel = minecraft.level;
        }
        return MODELS.computeIfAbsent(species, id -> {
            ResourceLocation location = ResourceLocation.tryParse(id);
            Optional<EntityType<?>> type = location == null ? Optional.empty()
                    : BuiltInRegistries.ENTITY_TYPE.getOptional(location);
            Entity entity = type.map(entityType -> entityType.create(minecraft.level)).orElse(null);
            return entity instanceof LivingEntity living ? living : null;
        });
    }

    private void renderIndex(GuiGraphics graphics, int mouseX, int mouseY) {
        List<IndexRow> index = index();
        int x = left + 6;
        int listTop = listTop();
        int rows = visibleRows();
        renderListFrame(graphics, listTop, rows);
        for (int position = 0; position < rows && listScroll + position < index.size(); position++) {
            IndexRow row = index.get(listScroll + position);
            int y = listTop + position * ROW;
            if (row.page() == null) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, TechStyle.METAL_DARK);
                graphics.drawString(font, row.chapter().title().toUpperCase(Locale.ROOT), x + 4, y + 2, TechStyle.AMBER, false);
                continue;
            }
            boolean selected = row.page().id().equals(lastPage);
            boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW;
            if (selected) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, SELECTED);
                if (TechStyle.blink()) {
                    graphics.drawString(font, "▶", x + 1, y + 2, TechStyle.AMBER, false);
                }
            } else if (hovered) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, TechStyle.HOVER);
            }
            graphics.drawString(font, ellipsize(row.page().title(), LIST_WIDTH - 16), x + 9, y + 2,
                    selected ? TechStyle.TEXT : TechStyle.BRIGHT, false);
        }
    }

    private void renderPage(GuiGraphics graphics) {
        Optional<IndexRow> selected = selectedPage();
        if (selected.isEmpty()) {
            return;
        }
        Manual.Chapter chapter = selected.get().chapter();
        Manual.Page page = selected.get().page();
        int pageLeft = left + 6 + LIST_WIDTH + 8;
        int pageRight = right - 12;
        int width = pageRight - pageLeft;
        String key = "page:" + page.id() + ":" + width;
        if (!key.equals(layoutKey)) {
            layoutKey = key;
            layout = WikiLayout.of(font, page.blocks(), width);
        }
        int headerHeight = 26;
        contentHeight = headerHeight + layout.height();
        clampContentScroll();
        graphics.enableScissor(pageLeft - 2, contentTop, pageRight + 2, contentBottom);
        int y = contentTop - contentScroll;
        graphics.drawString(font, chapter.title().toUpperCase(Locale.ROOT), pageLeft, y, TechStyle.AMBER, false);
        graphics.drawString(font, TechStyle.titleText(Component.literal(page.title())), pageLeft, y + 11, TechStyle.TEXT);
        graphics.fill(pageLeft, y + 22, pageRight, y + 23, HudShapes.fade(TechStyle.ACCENT, 0.5F));
        layout.draw(graphics, font, pageLeft, y + headerHeight, contentTop, contentBottom);
        graphics.disableScissor();
        renderContentScrollbar(graphics, pageRight + 4);
    }

    private void renderRecordList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<RecordRow> rows = recordRows();
        int x = left + 6;
        graphics.drawString(font, Component.translatable("iceagesurvival.analyzer.tab.records").getString()
                .toUpperCase(Locale.ROOT), x + 1, contentTop, TechStyle.ACCENT, false);
        int listTop = listTop();
        int visible = visibleRows();
        renderListFrame(graphics, listTop, visible);
        int selectedRow = selectedRow();
        for (int position = 0; position < visible && listScroll + position < rows.size(); position++) {
            int row = listScroll + position;
            RecordRow entry = rows.get(row);
            int y = listTop + position * ROW;
            if (entry.header()) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, TechStyle.METAL_DARK);
                graphics.drawString(font, ellipsize(entry.series().title().toUpperCase(Locale.ROOT), LIST_WIDTH - 10),
                        x + 4, y + 2, TechStyle.AMBER, false);
                continue;
            }
            boolean selected = row == selectedRow;
            boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW;
            if (selected) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, SELECTED);
                if (TechStyle.blink()) {
                    graphics.drawString(font, "▶", x + 1, y + 2, TechStyle.AMBER, false);
                }
            } else if (hovered) {
                graphics.fill(x, y, x + LIST_WIDTH - 4, y + ROW, TechStyle.HOVER);
            }
            boolean known = entry.unlocked();
            graphics.drawString(font, String.format(Locale.ROOT, "%02d", entry.index() + 1), x + 9, y + 2,
                    known ? TechStyle.AMBER : LOCKED, false);
            String title = known ? entry.series().records().get(entry.index()).title() : "???";
            graphics.drawString(font, ellipsize(title, LIST_WIDTH - 30), x + 24, y + 2,
                    known ? (selected ? TechStyle.TEXT : TechStyle.BRIGHT) : LOCKED, false);
        }
    }

    /** O registro escolhido: número, título, de onde veio e o texto; trancado, só o aviso de como recuperá-lo. */
    private void renderRecord(GuiGraphics graphics) {
        List<RecordRow> rows = recordRows();
        if (rows.isEmpty() || rows.get(selectedRow()).header()) {
            return;
        }
        RecordRow entry = rows.get(selectedRow());
        int row = entry.index();
        Manual.Record record = entry.series().records().get(row);
        boolean known = entry.unlocked();
        int pageLeft = left + 6 + LIST_WIDTH + 8;
        int pageRight = right - 12;
        int width = pageRight - pageLeft;
        String key = "record:" + record.id() + ":" + known + ":" + width;
        if (!key.equals(layoutKey)) {
            layoutKey = key;
            layout = known ? WikiLayout.of(font, record.blocks(), width) : null;
        }
        int headerHeight = 36;
        contentHeight = headerHeight + (layout == null ? 40 : layout.height());
        clampContentScroll();
        graphics.enableScissor(pageLeft - 2, contentTop, pageRight + 2, contentBottom);
        int y = contentTop - contentScroll;
        String number = String.format(Locale.ROOT, "REG.%02d", row + 1);
        graphics.drawString(font, number, pageRight - font.width(number), y, TechStyle.AMBER, false);
        graphics.drawString(font, ellipsize((known ? record.source() : "???").toUpperCase(Locale.ROOT),
                width - font.width(number) - 6), pageLeft, y, known ? TechStyle.SUBTLE : LOCKED, false);
        graphics.drawString(font, TechStyle.titleText(Component.literal(known ? record.title() : "???")), pageLeft,
                y + 12, known ? TechStyle.TEXT : LOCKED);
        if (newEntry && row == lastRecord && entry.series().id().equals(lastSeries) && Util.getMillis() - openedAt < NEW_ENTRY_MILLIS && TechStyle.blink()) {
            Component banner = Component.translatable("iceagesurvival.analyzer.new_record");
            int bannerWidth = font.width(banner) + 8;
            graphics.fill(pageRight - bannerWidth, y + 10, pageRight, y + 22, HudShapes.fade(TechStyle.AMBER, 0.85F));
            graphics.drawString(font, banner, pageRight - bannerWidth + 4, y + 12, 0xFF1A1206, false);
        }
        graphics.fill(pageLeft, y + 26, pageRight, y + 27, HudShapes.fade(TechStyle.ACCENT, 0.5F));
        y += headerHeight;
        if (layout != null) {
            layout.draw(graphics, font, pageLeft, y, contentTop, contentBottom);
        } else {
            for (var line : font.split(Component.translatable("iceagesurvival.analyzer.record_locked"), width)) {
                graphics.drawString(font, line, pageLeft, y, TechStyle.SUBTLE, false);
                y += 10;
            }
        }
        graphics.disableScissor();
        renderContentScrollbar(graphics, pageRight + 4);
    }

    /** Corta o texto na largura, com reticências quando não cabe. */
    private String ellipsize(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }

    private void clampContentScroll() {
        int max = Math.max(0, contentHeight - (contentBottom - contentTop));
        contentScroll = Math.max(0, Math.min(max, contentScroll));
    }

    private void renderContentScrollbar(GuiGraphics graphics, int x) {
        int visible = contentBottom - contentTop;
        if (contentHeight <= visible) {
            return;
        }
        graphics.fill(x, contentTop, x + 2, contentBottom, TechStyle.SLOT_EDGE);
        int thumb = Math.max(8, visible * visible / contentHeight);
        int thumbTop = contentTop + (visible - thumb) * contentScroll / Math.max(1, contentHeight - visible);
        graphics.fill(x - 1, thumbTop, x + 3, thumbTop + thumb, TechStyle.AMBER);
    }
}
