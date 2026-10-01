package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * "Localizar criatura": as criaturas domesticadas do jogador, da mais perto para a mais longe, com
 * nível, direção e distância. "Localizar" liga a bússola da tela ({@link CreatureTracker}) e faz a
 * criatura brilhar se estiver carregada; na mesma linha, "Parar" desliga.
 */
public class CreatureLocatorScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER = 28;
    private static final int FOOTER = 8;
    private static final int BUTTON_WIDTH = 64;
    private static final int PANEL = 0xE0101418;
    private static final int BORDER = 0xFF5FB36B;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTLE = 0xFFA0A8B0;
    private static final int LEVEL = 0xFFFFC857;

    private List<CreatureLocator.Entry> rows = List.of();
    private int scroll;
    private int left;
    private int top;
    private int visibleRows;

    public CreatureLocatorScreen() {
        super(Component.translatable("iceagesurvival.locator.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CreatureLocatorScreen());
        CreatureTracker.requestList();
    }

    @Override
    protected void init() {
        visibleRows = Math.max(1, Math.min(8, (height - 40 - HEADER - FOOTER) / ROW_HEIGHT));
        left = (width - WIDTH) / 2;
        top = (height - (HEADER + visibleRows * ROW_HEIGHT + FOOTER)) / 2;
        refresh();
    }

    /** Lista nova do servidor: reordena pela distância e refaz os botões. */
    void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        List<CreatureLocator.Entry> sorted = new ArrayList<>(CreatureTracker.entries());
        if (minecraft.player != null) {
            var here = minecraft.player.level().dimension();
            sorted.sort(Comparator.<CreatureLocator.Entry>comparingInt(entry -> entry.dimension().equals(here) ? 0 : 1)
                    .thenComparingDouble(entry -> CreatureTracker.position(entry).distanceToSqr(minecraft.player.position())));
        }
        rows = sorted;
        scroll = Math.max(0, Math.min(scroll, rows.size() - visibleRows));
        clearWidgets();
        for (int index = 0; index < visibleRows && scroll + index < rows.size(); index++) {
            CreatureLocator.Entry entry = rows.get(scroll + index);
            boolean tracking = entry.creature().equals(CreatureTracker.tracked());
            int y = top + HEADER + index * ROW_HEIGHT;
            addRenderableWidget(Button.builder(
                    Component.translatable(tracking ? "iceagesurvival.locator.stop" : "iceagesurvival.locator.locate"),
                    button -> {
                        if (tracking) {
                            CreatureTracker.stop();
                            refresh();
                        } else {
                            CreatureTracker.track(entry.creature());
                            onClose();
                        }
                    }).bounds(left + WIDTH - 8 - BUTTON_WIDTH, y + 1, BUTTON_WIDTH, ROW_HEIGHT - 4).build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, rows.size() - visibleRows);
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            refresh();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int bottom = top + HEADER + visibleRows * ROW_HEIGHT + FOOTER;
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, bottom + 1, BORDER);
        graphics.fill(left, top, left + WIDTH, bottom, PANEL);
        graphics.drawString(font, title, left + 8, top + 8, TEXT);
        if (rows.size() > visibleRows) {
            Component page = Component.literal((scroll + 1) + "–" + Math.min(rows.size(), scroll + visibleRows)
                    + " / " + rows.size());
            graphics.drawString(font, page, left + WIDTH - 8 - font.width(page), top + 8, SUBTLE);
        }
        graphics.fill(left + 8, top + 20, left + WIDTH - 8, top + 21, 0x40FFFFFF);
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("iceagesurvival.locator.empty"),
                    left + WIDTH / 2, top + HEADER + 6, SUBTLE);
        }
        int textRight = left + WIDTH - 16 - BUTTON_WIDTH;
        for (int index = 0; index < visibleRows && scroll + index < rows.size(); index++) {
            CreatureLocator.Entry entry = rows.get(scroll + index);
            int y = top + HEADER + index * ROW_HEIGHT;
            Component level = Component.translatable("iceagesurvival.hud.level", entry.level());
            Component where = CreatureTracker.direction(entry);
            int nameWidth = textRight - (left + 8) - font.width(level) - 6;
            graphics.drawString(font, font.plainSubstrByWidth(entry.name(), nameWidth), left + 8, y + 2, TEXT);
            graphics.drawString(font, level, textRight - font.width(level), y + 2, LEVEL);
            graphics.drawString(font, where, left + 8, y + 11, SUBTLE);
            Component coords = Component.literal(entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ());
            graphics.drawString(font, coords, textRight - font.width(coords), y + 11, SUBTLE);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
