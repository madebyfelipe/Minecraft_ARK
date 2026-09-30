package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Mostra nível, torpor e andamento da domesticação da criatura sob a mira. */
public final class CreatureHud {
    private static final int OFFSET_BELOW_CROSSHAIR = 14;
    private static final int LINE_HEIGHT = 10;
    private static final int COLOR = 0xFFFFFF;

    private CreatureHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !(CommandInput.aimedEntity() instanceof PrehistoricCreature creature)) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("iceagesurvival.hud.name_and_level", creature.getName(), creature.creatureLevel()));
        if (creature.torporFraction() > 0) {
            lines.add(Component.translatable("iceagesurvival.hud.torpor", percent(creature.torporFraction())));
        }
        if (creature.isUnconscious() && !creature.isTame()) {
            lines.add(Component.translatable("iceagesurvival.hud.taming", percent(creature.tamingProgress())));
        }

        if (minecraft.player != null && creature.isOwner(minecraft.player)) {
            lines.add(Component.translatable("iceagesurvival.hud.order",
                    Component.translatable("iceagesurvival.order." + creature.order().id())));
            if (creature.isSaddled()) {
                lines.add(Component.translatable("iceagesurvival.hud.saddled"));
            }
        }

        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;
        for (Component line : lines) {
            graphics.drawCenteredString(minecraft.font, line, x, y, COLOR);
            y += LINE_HEIGHT;
        }
    }

    private static int percent(float fraction) {
        return Math.round(fraction * 100);
    }
}
