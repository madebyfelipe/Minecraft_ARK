package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.core.locator.RadarMath;
import dev.madebyfelipe.iceagesurvival.network.CreatureLocationsPayload;
import dev.madebyfelipe.iceagesurvival.network.LocateRequestPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;

/**
 * A criatura que o jogador mandou localizar: guarda a lista recebida do servidor e desenha, no canto
 * superior direito, um radar de proa para cima — anel de bússola que gira com o olhar, varredura, o
 * marcador da criatura (preso na borda além de {@link RadarMath#RANGE} blocos) — e, embaixo, nome, nível,
 * distância e diferença de altura. Enquanto há alguém sendo localizado, a posição é pedida de novo a cada
 * 2 s; perto o bastante, o radar some.
 */
public final class CreatureTracker {
    private static final int REFRESH_TICKS = 40;
    private static final double ARRIVED_DISTANCE = 6.0;

    static final int ACCENT = 0xFF5FB36B;
    static final int BRIGHT = 0xFF8CF0A8;
    static final int TARGET = 0xFFFFC857;
    static final int TEXT = 0xFFFFFFFF;
    static final int SUBTLE = 0xFFA0A8B0;
    static final int PANEL = 0xD0071510;
    static final int GRID = 0x355FB36B;

    private static final int BEZEL = 0xE00C1A14;
    private static final int TICK = 0x90A0E0B0;
    private static final int LETTER = 0xFFD8F5E0;
    private static final int NORTH = 0xFFFF6B5B;
    private static final int SCANLINE = 0x0C8CF0A8;

    /** Raio do disco do radar, por dentro do anel da bússola. */
    private static final float SCOPE = 28.0F;
    /** Borda de fora do anel da bússola. */
    private static final float RING = 37.0F;
    /** O espaço que o radar ocupa em volta do centro, com o índice e a marca da criatura por fora do anel. */
    private static final int REACH = 43;
    private static final int MARGIN = 6;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_MAX_WIDTH = 140;
    /** Uma volta da varredura, em milissegundos. */
    private static final float SWEEP_MILLIS = 2400.0F;
    /** Rastro da varredura, em graus. */
    private static final float SWEEP_TRAIL = 70.0F;
    private static final float SMALL = 0.75F;

    private static final String[] CARDINALS = {"n", "e", "s", "w"};
    private static final float[] CARDINAL_YAWS = {RadarMath.NORTH, RadarMath.EAST, RadarMath.SOUTH, RadarMath.WEST};

    private static List<CreatureLocator.Entry> entries = List.of();
    @Nullable
    private static UUID tracked;
    private static int refreshCountdown;

    /**
     * Onde a criatura está em relação ao jogador agora: ângulo no radar (0 à frente, horário), distância e
     * diferença de altura. Em outra dimensão, só o aviso.
     */
    public record Reading(boolean sameDimension, float bearing, double distance, int height) {
        static final Reading OTHER_DIMENSION = new Reading(false, 0.0F, 0.0, 0);
    }

    private CreatureTracker() {
    }

    public static List<CreatureLocator.Entry> entries() {
        return entries;
    }

    @Nullable
    public static UUID tracked() {
        return tracked;
    }

    /** Pede a lista ao servidor (a tela chama ao abrir). */
    public static void requestList() {
        ModPayloads.sendToServer(new LocateRequestPayload(Optional.empty()));
    }

    /** Começa a localizar a criatura; se estiver carregada no servidor, ela brilha. */
    public static void track(UUID creature) {
        tracked = creature;
        refreshCountdown = REFRESH_TICKS;
        ModPayloads.sendToServer(new LocateRequestPayload(Optional.of(creature)));
    }

    public static void stop() {
        tracked = null;
    }

    public static void receive(CreatureLocationsPayload payload) {
        entries = List.copyOf(payload.entries());
        if (tracked != null && entries.stream().noneMatch(entry -> entry.creature().equals(tracked))) {
            tracked = null; // morreu, foi solta ou mudou de dono
        }
        if (Minecraft.getInstance().screen instanceof CreatureLocatorScreen screen) {
            screen.refresh();
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || tracked == null || minecraft.player == null) {
            return;
        }
        if (--refreshCountdown <= 0) {
            refreshCountdown = REFRESH_TICKS;
            requestList();
        }
        CreatureLocator.Entry entry = trackedEntry();
        if (entry != null && entry.dimension().equals(minecraft.player.level().dimension())
                && position(entry).distanceTo(minecraft.player.position()) < ARRIVED_DISTANCE) {
            tracked = null;
        }
    }

    @Nullable
    private static CreatureLocator.Entry trackedEntry() {
        for (CreatureLocator.Entry entry : entries) {
            if (entry.creature().equals(tracked)) {
                return entry;
            }
        }
        return null;
    }

    /** Posição de agora se a criatura estiver carregada no cliente; senão a última que o servidor mandou. */
    public static Vec3 position(CreatureLocator.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension().equals(entry.dimension())) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity.getUUID().equals(entry.creature())) {
                    return entity.position();
                }
            }
        }
        return Vec3.atBottomCenterOf(entry.pos());
    }

    /** A leitura da criatura para o jogador de agora; {@code null} sem jogador. */
    @Nullable
    public static Reading read(CreatureLocator.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        if (!entry.dimension().equals(minecraft.player.level().dimension())) {
            return Reading.OTHER_DIMENSION;
        }
        Vec3 target = position(entry);
        Vec3 from = minecraft.player.position();
        return new Reading(true, RadarMath.bearing(target.x - from.x, target.z - from.z, minecraft.player.getYRot()),
                target.distanceTo(from), RadarMath.heightDifference(target.y - from.y));
    }

    static Component distanceText(Reading reading) {
        return reading.sameDimension()
                ? Component.translatable("iceagesurvival.locator.distance", Math.round(reading.distance()))
                : Component.translatable("iceagesurvival.locator.other_dimension");
    }

    /** "▲ 14 m", "▼ 30 m" ou "mesmo nível"; vazio em outra dimensão. */
    static Component heightText(Reading reading) {
        if (!reading.sameDimension()) {
            return Component.empty();
        }
        if (reading.height() == 0) {
            return Component.translatable("iceagesurvival.locator.same_level");
        }
        return Component.translatable(reading.height() > 0 ? "iceagesurvival.locator.above" : "iceagesurvival.locator.below",
                Math.abs(reading.height()));
    }

    /** Ângulo de tela da varredura agora: uma volta a cada {@link #SWEEP_MILLIS}. */
    static float sweepAngle() {
        return (Util.getMillis() % (long) SWEEP_MILLIS) / SWEEP_MILLIS * 360.0F;
    }

    /** Liga e desliga duas vezes por segundo, para o ponto de "rastreando" e o "sem sinal". */
    static boolean blink() {
        return Util.getMillis() / 500 % 2 == 0;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        CreatureLocator.Entry entry = tracked == null ? null : trackedEntry();
        if (entry == null || minecraft.options.hideGui || minecraft.options.renderDebug || minecraft.player == null) {
            return;
        }
        Reading reading = read(entry);
        if (reading == null) {
            return;
        }
        float centerX = screenWidth - MARGIN - REACH;
        float centerY = MARGIN + effectRows(minecraft) + REACH;
        drawRadar(graphics, minecraft.font, centerX, centerY, minecraft.player.getYRot(), reading);
        drawPanel(graphics, minecraft.font, screenWidth - MARGIN, Math.round(centerY) + REACH + 1, entry, reading);
    }

    /** Os ícones de efeito do vanilla ficam no canto superior direito: o radar desce uma fileira por linha usada. */
    private static int effectRows(Minecraft minecraft) {
        boolean beneficial = false;
        boolean harmful = false;
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (effect.showIcon()) {
                if (effect.getEffect().isBeneficial()) {
                    beneficial = true;
                } else {
                    harmful = true;
                }
            }
        }
        return harmful ? 52 : beneficial ? 26 : 0;
    }

    private static void drawRadar(GuiGraphics graphics, Font font, float x, float y, float yaw, Reading reading) {
        // Anel da bússola e o disco.
        HudShapes.ring(graphics, x, y, RING, RING + 1.0F, ACCENT);
        HudShapes.ring(graphics, x, y, SCOPE, RING, BEZEL);
        HudShapes.disc(graphics, x, y, SCOPE, PANEL);
        HudShapes.ring(graphics, x, y, SCOPE - 0.5F, SCOPE + 0.5F, HudShapes.fade(ACCENT, 0.6F));

        // Grade fixa: anel dos 50 blocos (meio raio, pela raiz), cruz e linhas de varredura de tela.
        HudShapes.ring(graphics, x, y, SCOPE / 2.0F - 0.5F, SCOPE / 2.0F, GRID);
        for (int angle = 0; angle < 360; angle += 90) {
            HudShapes.spoke(graphics, x, y, 2.0F, SCOPE - 1.0F, angle, 0.5F, GRID);
        }
        for (float row = -SCOPE + 2.0F; row < SCOPE - 1.0F; row += 3.0F) {
            float half = (float) Math.sqrt(SCOPE * SCOPE - row * row) - 1.0F;
            HudShapes.line(graphics, x - half, y + row, x + half, y + row, 0.5F, SCANLINE);
        }

        // Marcas a cada 15° e os pontos cardeais, que giram com o olhar.
        float middle = (SCOPE + RING) / 2.0F;
        for (int degrees = 0; degrees < 360; degrees += 15) {
            float angle = RadarMath.screenAngle(degrees, yaw);
            if (degrees % 90 == 0) {
                continue;
            }
            boolean major = degrees % 45 == 0;
            HudShapes.spoke(graphics, x, y, major ? SCOPE + 2.0F : RING - 3.0F, RING - 0.5F, angle, major ? 1.0F : 0.6F,
                    TICK);
        }
        for (int index = 0; index < CARDINALS.length; index++) {
            float angle = RadarMath.screenAngle(CARDINAL_YAWS[index], yaw);
            Component letter = Component.translatable("iceagesurvival.locator.cardinal." + CARDINALS[index]);
            drawSmallCentered(graphics, font, letter, HudShapes.pointX(x, middle, angle),
                    HudShapes.pointY(y, middle, angle), index == 0 ? NORTH : LETTER);
        }

        // Varredura com rastro.
        float sweep = sweepAngle();
        HudShapes.arc(graphics, x, y, 0.0F, SCOPE - 1.0F, sweep - SWEEP_TRAIL, sweep, 0x005FE38A, 0x605FE38A);
        HudShapes.spoke(graphics, x, y, 0.0F, SCOPE - 1.0F, sweep, 1.0F, HudShapes.fade(BRIGHT, 0.8F));

        // Índice da proa, no topo e por fora do anel.
        HudShapes.triangle(graphics, x, y - RING - 0.5F, x - 3.5F, y - RING - 5.0F, x + 3.5F, y - RING - 5.0F, BRIGHT);

        if (reading.sameDimension()) {
            drawTarget(graphics, x, y, sweep, reading);
        } else {
            drawNoSignal(graphics, font, x, y);
        }

        // O jogador, no centro, olhando para cima.
        HudShapes.arrowHead(graphics, x, y - 3.5F, 0.0F, 6.0F, 3.0F, TEXT);
    }

    private static void drawTarget(GuiGraphics graphics, float x, float y, float sweep, Reading reading) {
        float bearing = reading.bearing();
        // Marca da direção por fora do anel: alinhada com o índice, a criatura está bem à frente.
        HudShapes.arrowHead(graphics, HudShapes.pointX(x, RING + 0.5F, bearing), HudShapes.pointY(y, RING + 0.5F, bearing),
                bearing + 180.0F, 4.5F, 3.0F, TARGET);

        float glow = RadarMath.sweepGlow(sweep, bearing);
        if (RadarMath.outOfRange(reading.distance())) {
            // Longe: seta na borda do disco apontando para fora.
            float tip = SCOPE - 1.5F;
            HudShapes.arrowHead(graphics, HudShapes.pointX(x, tip, bearing), HudShapes.pointY(y, tip, bearing), bearing,
                    6.0F, 4.0F, HudShapes.fade(TARGET, 0.55F + 0.45F * glow));
            return;
        }
        float radius = RadarMath.radial(reading.distance()) * (SCOPE - 5.0F);
        float markerX = HudShapes.pointX(x, radius, bearing);
        float markerY = HudShapes.pointY(y, radius, bearing);
        HudShapes.disc(graphics, markerX, markerY, 2.5F + 3.5F * glow, HudShapes.fade(TARGET, 0.35F * glow));
        HudShapes.diamond(graphics, markerX, markerY, 3.0F, HudShapes.fade(TARGET, 0.6F + 0.4F * glow));
    }

    /** Outra dimensão: chuvisco no disco e "sem sinal" piscando. */
    private static void drawNoSignal(GuiGraphics graphics, Font font, float x, float y) {
        Random noise = new Random(Util.getMillis() / 80);
        for (int dot = 0; dot < 40; dot++) {
            float angle = noise.nextFloat() * 360.0F;
            float radius = (float) Math.sqrt(noise.nextFloat()) * (SCOPE - 2.0F);
            float dotX = HudShapes.pointX(x, radius, angle);
            float dotY = HudShapes.pointY(y, radius, angle);
            HudShapes.line(graphics, dotX, dotY, dotX + 1.0F, dotY, 1.0F, HudShapes.fade(BRIGHT, 0.2F + 0.4F * noise.nextFloat()));
        }
        if (blink()) {
            drawSmallCentered(graphics, font, Component.translatable("iceagesurvival.locator.no_signal"), x, y + 9.0F,
                    TARGET);
        }
    }

    /** Embaixo do radar, alinhado à direita: "● RASTREANDO", nome e nível, distância e altura. */
    private static void drawPanel(GuiGraphics graphics, Font font, int right, int top, CreatureLocator.Entry entry,
                                  Reading reading) {
        Component level = Component.translatable("iceagesurvival.hud.level", entry.level());
        Component distance = distanceText(reading);
        Component height = heightText(reading);
        int lineWidth = Math.max(font.width(entry.name()) + 6 + font.width(level),
                font.width(distance) + 8 + font.width(height));
        int width = Math.min(PANEL_MAX_WIDTH, Math.max(REACH * 2, lineWidth + PANEL_PADDING * 2 + 2));
        int left = right - width;
        int bottom = top + PANEL_PADDING + 7 + 2 + font.lineHeight * 2 + 1 + PANEL_PADDING;
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 1, HudShapes.fade(ACCENT, 0.6F));
        graphics.fill(left, top, left + 2, bottom, ACCENT);

        int x = left + 2 + PANEL_PADDING;
        int textRight = right - PANEL_PADDING;
        int y = top + PANEL_PADDING;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SMALL, SMALL, 1.0F);
        graphics.drawString(font, blink() ? "●" : "○", 0, 0, BRIGHT, false);
        graphics.drawString(font, Component.translatable("iceagesurvival.locator.tracking"), 10, 0, ACCENT, false);
        graphics.pose().popPose();

        y += 7 + 2;
        int nameWidth = textRight - x - font.width(level) - 6;
        graphics.drawString(font, font.plainSubstrByWidth(entry.name(), nameWidth), x, y, TEXT);
        graphics.drawString(font, level, textRight - font.width(level), y, TARGET);
        y += font.lineHeight + 1;
        graphics.drawString(font, distance, x, y, reading.sameDimension() ? BRIGHT : SUBTLE);
        graphics.drawString(font, height, textRight - font.width(height), y, SUBTLE);
    }

    /** Texto a 3/4 do tamanho, centrado em (x, y). */
    static void drawSmallCentered(GuiGraphics graphics, Font font, Component text, float x, float y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SMALL, SMALL, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2 + 1, color, false);
        graphics.pose().popPose();
    }
}
