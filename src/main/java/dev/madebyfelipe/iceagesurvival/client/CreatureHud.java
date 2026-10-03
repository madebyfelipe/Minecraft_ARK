package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Painel no topo central da tela sobre a criatura sob a mira: nome, nível, vida, torpor e andamento da domesticação da criatura
 * mirada; nas do jogador, também as ordens. No visual do {@link TechStyle}; a barra de cor à esquerda diz de quem ela é.
 */
public final class CreatureHud {
    /** Distância do topo da tela; abaixo da barra de vida de chefes do vanilla. */
    private static final int TOP_MARGIN = 6;
    private static final int WIDTH = 150;
    private static final int PADDING = 5;
    private static final int BAR_HEIGHT = 9;
    private static final int GAP = 3;

    private static final int BORDER_OWN = TechStyle.ACCENT;
    private static final int BORDER_OTHER = 0xFFD08A3C;
    private static final int BORDER_WILD = 0xFF8A9199;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTLE = 0xFFA0A8B0;
    private static final int LEVEL = 0xFFFFC857;
    private static final int HEALTH = 0xFFD04848;
    private static final int TORPOR = 0xFF9B59D0;
    private static final int TAMING = 0xFF5FB36B;
    private static final int WARNING = 0xFFFFB347;
    private static final int WARNING_DIM = 0xFFC9822E;
    private static final int MALE = 0xFF5BA8FF;
    private static final int FEMALE = 0xFFFF7BC4;
    private static final int FLIGHT = 0xFF6FC3E8;

    private CreatureHud() {
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
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
        // Sem comida, a domesticação para e o torpor continua caindo: o aviso fica logo abaixo da barra.
        List<FormattedCharSequence> foodWarning = showTaming && creature.needsTamingFood()
                ? font.split(Component.translatable("iceagesurvival.hud.taming_needs_food"), WIDTH - PADDING * 2 - 2)
                : List.of();
        if (!foodWarning.isEmpty()) {
            height += GAP + foodWarning.size() * font.lineHeight;
        }
        if (own) {
            height += GAP + font.lineHeight;
        }

        int left = (screenWidth - WIDTH) / 2;
        int top = TOP_MARGIN;
        int border = own ? BORDER_OWN : creature.isTame() ? BORDER_OTHER : BORDER_WILD;
        TechStyle.hudFrame(graphics, left, top, left + WIDTH, top + height, border);

        int x = left + 2 + PADDING;
        int right = left + WIDTH - PADDING;
        int y = top + PADDING;

        // Nome e sexo à esquerda, nível à direita.
        Component level = Component.translatable("iceagesurvival.hud.level", creature.creatureLevel());
        String sex = PrehistoricCreature.sexSymbol(creature.isFemale());
        String name = font.plainSubstrByWidth(creature.getName().getString(),
                right - x - font.width(level) - font.width(" " + sex) - 4);
        graphics.drawString(font, name, x, y, TEXT);
        graphics.drawString(font, sex, x + font.width(name + " "), y, creature.isFemale() ? FEMALE : MALE);
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
        if (!foodWarning.isEmpty()) {
            y += GAP;
            // Pisca devagar para chamar atenção sem ficar ilegível.
            int color = (minecraft.level.getGameTime() / 10) % 2 == 0 ? WARNING : WARNING_DIM;
            for (FormattedCharSequence line : foodWarning) {
                graphics.drawString(font, line, x, y, color);
                y += font.lineHeight;
            }
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

    /**
     * Fôlego de voo da montaria voadora, acima da barra de experiência, enquanto o jogador monta. Esgotada, pisca:
     * não sobe mais, só plana.
     */
    public static void renderFlightStamina(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth,
                                           int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof PrehistoricCreature mount)
                || !mount.isFlightMount() || mount.maxFlightStamina() <= 0) {
            return;
        }
        int width = 182;
        int x = (screenWidth - width) / 2;
        int y = screenHeight - 32 - 3 - 8;
        float fraction = mount.flightStaminaFraction();
        boolean blink = mount.isFlightExhausted() && (minecraft.level.getGameTime() / 6) % 2 == 0;
        TechStyle.bar(graphics, x, y, width, 4, fraction, blink ? WARNING_DIM : FLIGHT);
        Component label = Component.translatable("iceagesurvival.hud.flight_stamina", percent(fraction));
        graphics.drawString(minecraft.font, label, (screenWidth - minecraft.font.width(label)) / 2, y - 10,
                mount.isFlightExhausted() ? WARNING : TEXT);
    }

    private static Component relation(PrehistoricCreature creature, boolean own) {
        if (own) {
            return Component.translatable("iceagesurvival.hud.yours");
        }
        if (!creature.isTame()) {
            if (creature.isUnconscious()) {
                return Component.translatable("iceagesurvival.hud.unconscious");
            }
            // O humor do bicho selvagem: o termômetro de estresse dele.
            return Component.translatable("iceagesurvival.hud.wild_mood", Component.translatable(
                    "iceagesurvival.mood." + creature.mood().name().toLowerCase(java.util.Locale.ROOT)));
        }
        LivingEntity owner = creature.getOwner();
        return owner != null
                ? Component.translatable("iceagesurvival.hud.owned_by", owner.getName())
                : Component.translatable("iceagesurvival.hud.tamed");
    }

    private static void labeledBar(GuiGraphics graphics, Font font, int x, int y, int width, float fraction, int color,
                                   String label) {
        TechStyle.bar(graphics, x, y, width, BAR_HEIGHT, fraction, color);
        graphics.drawCenteredString(font, label, x + width / 2, y + (BAR_HEIGHT - font.lineHeight) / 2 + 1, TEXT);
    }

    private static int percent(float fraction) {
        return Math.round(fraction * 100);
    }
}
