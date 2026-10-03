package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.core.locator.RadarMath;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * "Localizar criatura": as criaturas domesticadas do jogador, da mais perto para a mais longe, num painel
 * no estilo do radar ({@link RadarHud}). Cada linha tem uma bússola pequena apontando para a
 * criatura em relação ao olhar, nome, nível, distância, diferença de altura e coordenadas. "Localizar"
 * liga o radar e faz a criatura brilhar se estiver carregada; na mesma linha, "Parar" desliga.
 */
public class CreatureLocatorScreen extends Screen {
    private static final int WIDTH = 284;
    private static final int ROW_HEIGHT = 26;
    private static final int HEADER = 28;
    private static final int FOOTER = 8;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 16;
    /** Raio da bússola de cada linha. */
    private static final float DIAL = 9.0F;
    private static final int TEXT_LEFT = 10 + 2 * 9 + 8;
    private static final int SCROLLBAR = 4;
    private static final int CORNER = 8;

    private static final int PANEL = 0xEA060C0A;
    private static final int DIAL_FACE = 0xFF0A1410;
    private static final int ROW_TRACKED = 0x285FB36B;
    private static final int ROW_HOVER = 0x10FFFFFF;
    private static final int SCANLINE = 0x068CF0A8;
    private static final int NORTH = 0xFFFF6B5B;

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
            int y = top + HEADER + index * ROW_HEIGHT + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
            addRenderableWidget(new TechButton(left + WIDTH - 6 - SCROLLBAR - BUTTON_WIDTH, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.translatable(tracking ? "iceagesurvival.locator.stop" : "iceagesurvival.locator.locate"),
                    tracking ? RadarHud.TARGET : RadarHud.BRIGHT,
                    button -> {
                        if (tracking) {
                            CreatureTracker.stop();
                            refresh();
                        } else {
                            CreatureTracker.track(entry.creature());
                            onClose();
                        }
                    }));
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
        int right = left + WIDTH;
        int bottom = top + HEADER + visibleRows * ROW_HEIGHT + FOOTER;
        drawFrame(graphics, right, bottom);
        drawHeader(graphics, right);

        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("iceagesurvival.locator.empty"),
                    left + WIDTH / 2, top + HEADER + 8, RadarHud.SUBTLE);
        }
        int textRight = right - 12 - SCROLLBAR - BUTTON_WIDTH;
        for (int index = 0; index < visibleRows && scroll + index < rows.size(); index++) {
            CreatureLocator.Entry entry = rows.get(scroll + index);
            int y = top + HEADER + index * ROW_HEIGHT;
            if (entry.creature().equals(CreatureTracker.tracked())) {
                graphics.fill(left + 1, y, right - 1, y + ROW_HEIGHT, ROW_TRACKED);
                graphics.fill(left + 1, y, left + 3, y + ROW_HEIGHT, RadarHud.ACCENT);
            } else if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                graphics.fill(left + 1, y, right - 1, y + ROW_HEIGHT, ROW_HOVER);
            }
            drawRow(graphics, entry, y, textRight);
            if (index > 0) {
                graphics.fill(left + TEXT_LEFT, y, right - 8, y + 1, HudShapes.fade(RadarHud.ACCENT, 0.15F));
            }
        }
        drawScrollbar(graphics, right);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Fundo, borda fina, cantoneiras e linhas de varredura. */
    private void drawFrame(GuiGraphics graphics, int right, int bottom) {
        graphics.fill(left, top, right, bottom, PANEL);
        for (int y = top + 2; y < bottom - 1; y += 3) {
            graphics.fill(left + 1, y, right - 1, y + 1, SCANLINE);
        }
        int edge = HudShapes.fade(RadarHud.ACCENT, 0.45F);
        graphics.fill(left, top, right, top + 1, edge);
        graphics.fill(left, bottom - 1, right, bottom, edge);
        graphics.fill(left, top, left + 1, bottom, edge);
        graphics.fill(right - 1, top, right, bottom, edge);
        int corner = RadarHud.BRIGHT;
        graphics.fill(left - 1, top - 1, left + CORNER, top + 1, corner);
        graphics.fill(left - 1, top - 1, left + 1, top + CORNER, corner);
        graphics.fill(right - CORNER, top - 1, right + 1, top + 1, corner);
        graphics.fill(right - 1, top - 1, right + 1, top + CORNER, corner);
        graphics.fill(left - 1, bottom - 1, left + CORNER, bottom + 1, corner);
        graphics.fill(left - 1, bottom - CORNER, left + 1, bottom + 1, corner);
        graphics.fill(right - CORNER, bottom - 1, right + 1, bottom + 1, corner);
        graphics.fill(right - 1, bottom - CORNER, right + 1, bottom + 1, corner);
    }

    /** Ponto piscando, título, a página e a linha com um brilho correndo por ela. */
    private void drawHeader(GuiGraphics graphics, int right) {
        graphics.drawString(font, RadarHud.blink() ? "●" : "○", left + 8, top + 8, RadarHud.BRIGHT);
        graphics.drawString(font, title, left + 18, top + 8, RadarHud.TEXT);
        if (rows.size() > visibleRows) {
            Component page = Component.literal((scroll + 1) + "–" + Math.min(rows.size(), scroll + visibleRows)
                    + " / " + rows.size());
            graphics.drawString(font, page, right - 8 - font.width(page), top + 8, RadarHud.SUBTLE);
        }
        int lineLeft = left + 8;
        int lineRight = right - 8;
        int lineY = top + 20;
        graphics.fill(lineLeft, lineY, lineRight, lineY + 1, HudShapes.fade(RadarHud.ACCENT, 0.5F));
        int span = lineRight - lineLeft;
        int glowWidth = 28;
        int glowX = lineLeft + (int) (Util.getMillis() / 6 % (span + glowWidth)) - glowWidth;
        graphics.fill(Math.max(lineLeft, glowX), lineY, Math.min(lineRight, glowX + glowWidth), lineY + 1,
                RadarHud.BRIGHT);
    }

    private void drawRow(GuiGraphics graphics, CreatureLocator.Entry entry, int y, int textRight) {
        RadarHud.Reading reading = CreatureTracker.read(entry);
        float dialX = left + 10 + DIAL;
        float dialY = y + ROW_HEIGHT / 2.0F;
        drawDial(graphics, dialX, dialY, reading);

        int x = left + TEXT_LEFT;
        Component level = Component.translatable("iceagesurvival.hud.level", entry.level());
        String name = font.plainSubstrByWidth(entry.name(), textRight - x - font.width(level) - 6);
        graphics.drawString(font, name, x, y + 4, RadarHud.TEXT);
        graphics.drawString(font, level, x + font.width(name) + 6, y + 4, RadarHud.TARGET);

        int used = x;
        if (reading != null) {
            Component distance = CreatureTracker.distanceText(reading);
            Component height = RadarHud.heightText(reading);
            graphics.drawString(font, distance, x, y + 14, reading.signal() ? RadarHud.BRIGHT
                    : RadarHud.SUBTLE);
            graphics.drawString(font, height, x + font.width(distance) + 8, y + 14, RadarHud.SUBTLE);
            used += font.width(distance) + 8 + font.width(height);
        }
        Component coords = Component.literal(entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ());
        if (used + 6 + font.width(coords) * 0.75F > textRight) {
            return; // não cabe ao lado da distância
        }
        graphics.pose().pushPose();
        graphics.pose().translate(textRight, y + 15, 0.0F);
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawString(font, coords, -font.width(coords), 0, RadarHud.SUBTLE, false);
        graphics.pose().popPose();
    }

    /** A bússola da linha: proa para cima, um ponto vermelho no norte e a agulha na criatura. */
    private void drawDial(GuiGraphics graphics, float x, float y, RadarHud.Reading reading) {
        HudShapes.disc(graphics, x, y, DIAL, DIAL_FACE);
        HudShapes.ring(graphics, x, y, DIAL - 1.0F, DIAL, HudShapes.fade(RadarHud.ACCENT, 0.8F));
        if (reading == null || !reading.signal()) {
            RadarHud.drawSmallCentered(graphics, font, Component.literal("?"), x, y, RadarHud.SUBTLE);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            float north = RadarMath.screenAngle(RadarMath.NORTH, minecraft.player.getYRot());
            HudShapes.disc(graphics, HudShapes.pointX(x, DIAL - 2.5F, north), HudShapes.pointY(y, DIAL - 2.5F, north),
                    1.2F, NORTH);
        }
        float bearing = reading.bearing();
        HudShapes.spoke(graphics, x, y, 0.0F, DIAL - 5.0F, bearing, 1.2F, RadarHud.TARGET);
        HudShapes.arrowHead(graphics, HudShapes.pointX(x, DIAL - 2.0F, bearing), HudShapes.pointY(y, DIAL - 2.0F, bearing),
                bearing, 4.0F, 2.5F, RadarHud.TARGET);
        HudShapes.disc(graphics, x, y, 1.3F, RadarHud.TEXT);
    }

    private void drawScrollbar(GuiGraphics graphics, int right) {
        if (rows.size() <= visibleRows) {
            return;
        }
        int trackTop = top + HEADER;
        int trackHeight = visibleRows * ROW_HEIGHT;
        int x = right - 3 - SCROLLBAR / 2;
        graphics.fill(x, trackTop, x + 2, trackTop + trackHeight, HudShapes.fade(RadarHud.ACCENT, 0.2F));
        int thumb = Math.max(8, trackHeight * visibleRows / rows.size());
        int thumbTop = trackTop + (trackHeight - thumb) * scroll / Math.max(1, rows.size() - visibleRows);
        graphics.fill(x, thumbTop, x + 2, thumbTop + thumb, RadarHud.BRIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Botão chapado com borda, no estilo do painel, em vez da textura de pedra do vanilla. */
    private static final class TechButton extends Button {
        private final int color;

        TechButton(int x, int y, int width, int height, Component message, int color, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.color = color;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hot = isHoveredOrFocused();
            int x = getX();
            int y = getY();
            int right = x + width;
            int bottom = y + height;
            graphics.fill(x, y, right, bottom, HudShapes.fade(color, hot ? 0.35F : 0.12F));
            int edge = HudShapes.fade(color, hot ? 1.0F : 0.6F);
            graphics.fill(x, y, right, y + 1, edge);
            graphics.fill(x, bottom - 1, right, bottom, edge);
            graphics.fill(x, y, x + 1, bottom, edge);
            graphics.fill(right - 1, y, right, bottom, edge);
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), x + width / 2, y + (height - 8) / 2,
                    hot ? RadarHud.TEXT : color);
        }
    }
}
