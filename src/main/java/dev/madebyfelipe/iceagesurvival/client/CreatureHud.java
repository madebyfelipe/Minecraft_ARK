package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Painel sob a mira: nome, nível, vida, torpor e andamento da domesticação da criatura
 * mirada; nas do jogador, também as ordens. A borda diz de quem ela é.
 */
public final class CreatureHud {
    private static final int OFFSET_BELOW_CROSSHAIR = 12;
    private static final int WIDTH = 150;
    private static final int PADDING = 5;
    private static final int BAR_HEIGHT = 9;
    private static final int GAP = 3;

    private static final int PANEL = 0xB0101418;
    private static final int BORDER_OWN = 0xFF5FB36B;
    private static final int BORDER_OTHER = 0xFFD08A3C;
    private static final int BORDER_WILD = 0xFF8A9199;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTLE = 0xFFA0A8B0;
    private static final int LEVEL = 0xFFFFC857;
    private static final int HEALTH = 0xFFD04848;
    private static final int TORPOR = 0xFF9B59D0;
    private static final int TAMING = 0xFF5FB36B;

    private CreatureHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null
                || !(CommandInput.aimedEntity() instanceof PrehistoricCreature creature)) {
            return;
        }
        Font font = minecraft.font;
        boolean own = creature.isOwner(minecraft.player);
        boolean showTorpor = creature.torporFraction() > 0;
        boolean showTaming = creature.isUnconscious() && !creature.isTame();

        int height = PADDING * 2 + font.lineHeight + GAP + font.lineHeight + GAP + BAR_HEIGHT;
        if (showTorpor) {
            height += GAP + BAR_HEIGHT;
        }
        if (showTaming) {
            height += GAP + BAR_HEIGHT;
        }
        if (own) {
            height += GAP + font.lineHeight;
        }

        int left = (graphics.guiWidth() - WIDTH) / 2;
        int top = graphics.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;
        int border = own ? BORDER_OWN : creature.isTame() ? BORDER_OTHER : BORDER_WILD;
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + height + 1, border);
        graphics.fill(left, top, left + WIDTH, top + height, PANEL);

        int x = left + PADDING;
        int right = left + WIDTH - PADDING;
        int y = top + PADDING;

        // Nome à esquerda, nível à direita.
        Component level = Component.translatable("iceagesurvival.hud.level", creature.creatureLevel());
        graphics.drawString(font, font.plainSubstrByWidth(creature.getName().getString(), right - x - font.width(level) - 4),
                x, y, TEXT);
        graphics.drawString(font, level, right - font.width(level), y, LEVEL);
        y += font.lineHeight + GAP;

        graphics.drawString(font, relation(creature, own), x, y, SUBTLE);
        if (creature.isSaddled()) {
            Component saddled = Component.translatable("iceagesurvival.hud.saddled");
            graphics.drawString(font, saddled, right - font.width(saddled), y, SUBTLE);
        }
        y += font.lineHeight + GAP;

        labeledBar(graphics, font, x, y, right - x, creature.getHealth() / creature.getMaxHealth(), HEALTH,
                String.format(Locale.ROOT, "%.0f / %.0f", creature.getHealth(), creature.getMaxHealth()));
        y += BAR_HEIGHT;
        if (showTorpor) {
            y += GAP;
            labeledBar(graphics, font, x, y, right - x, creature.torporFraction(), TORPOR,
                    Component.translatable("iceagesurvival.hud.torpor", percent(creature.torporFraction())).getString());
            y += BAR_HEIGHT;
        }
        if (showTaming) {
            y += GAP;
            labeledBar(graphics, font, x, y, right - x, creature.tamingProgress(), TAMING,
                    Component.translatable("iceagesurvival.hud.taming", percent(creature.tamingProgress())).getString());
            y += BAR_HEIGHT;
        }
        if (own) {
            y += GAP;
            graphics.drawString(font, Component.translatable("iceagesurvival.hud.orders",
                    CreatureStatusScreen.movementName(creature.movement()),
                    CreatureStatusScreen.stanceName(creature.stance())), x, y, TEXT);
            Component hint = Component.translatable("iceagesurvival.hud.status_hint", CommandInput.statusKeyName());
            graphics.drawString(font, hint, right - font.width(hint), y, SUBTLE);
        }
    }

    private static Component relation(PrehistoricCreature creature, boolean own) {
        if (own) {
            return Component.translatable("iceagesurvival.hud.yours");
        }
        if (!creature.isTame()) {
            return Component.translatable(creature.isUnconscious() ? "iceagesurvival.hud.unconscious" : "iceagesurvival.hud.wild");
        }
        LivingEntity owner = creature.getOwner();
        return owner != null
                ? Component.translatable("iceagesurvival.hud.owned_by", owner.getName())
                : Component.translatable("iceagesurvival.hud.tamed");
    }

    private static void labeledBar(GuiGraphics graphics, Font font, int x, int y, int width, float fraction, int color,
                                   String label) {
        drawBar(graphics, x, y, width, BAR_HEIGHT, fraction, color);
        graphics.drawCenteredString(font, label, x + width / 2, y + (BAR_HEIGHT - font.lineHeight) / 2 + 1, TEXT);
    }

    /** Barra com fundo escuro; {@code fraction} entre 0 e 1. */
    static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF26292E);
        int filled = Math.round(width * Math.clamp(fraction, 0.0F, 1.0F));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, color);
        }
    }

    private static int percent(float fraction) {
        return Math.round(fraction * 100);
    }
}
