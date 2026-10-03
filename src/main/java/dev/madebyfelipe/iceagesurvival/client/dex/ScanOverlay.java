package dev.madebyfelipe.iceagesurvival.client.dex;

import dev.madebyfelipe.iceagesurvival.client.HudShapes;
import dev.madebyfelipe.iceagesurvival.client.TechStyle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.AnalyzerItem;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * A mira do Analisador enquanto o jogador segura o clique numa criatura: colchetes âmbar que se fecham em volta da
 * mira conforme o scan avança, "ANALISANDO" piscando, o nome da espécie (ou "???" se ainda não registrada), a
 * distância e uma barra segmentada.
 */
public final class ScanOverlay {
    private ScanOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !minecraft.player.isUsingItem()
                || !(minecraft.player.getUseItem().getItem() instanceof AnalyzerItem)) {
            return;
        }
        float progress = Mth.clamp((minecraft.player.getTicksUsingItem() + partialTick) / AnalyzerItem.SCAN_TICKS,
                0.0F, 1.0F);
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Colchetes: de 34 px até 12 px em volta da mira.
        float reach = Mth.lerp(progress, 34.0F, 12.0F);
        int arm = 7;
        int color = TechStyle.blink() ? TechStyle.AMBER : HudShapes.fade(TechStyle.AMBER, 0.7F);
        for (int corner = 0; corner < 4; corner++) {
            int sx = corner % 2 == 0 ? -1 : 1;
            int sy = corner < 2 ? -1 : 1;
            float x = centerX + sx * reach;
            float y = centerY + sy * reach;
            HudShapes.line(graphics, x, y, x - sx * arm, y, 1.5F, color);
            HudShapes.line(graphics, x, y, x, y - sy * arm, 1.5F, color);
        }
        HudShapes.ring(graphics, centerX, centerY, reach * 0.55F, reach * 0.55F + 1.0F,
                HudShapes.fade(TechStyle.ACCENT, 0.5F));
        HudShapes.arc(graphics, centerX, centerY, reach * 0.55F - 2.0F, reach * 0.55F, 0.0F, 360.0F * progress,
                TechStyle.BRIGHT, TechStyle.BRIGHT);

        Font font = minecraft.font;
        Component scanning = Component.translatable("iceagesurvival.analyzer.scanning");
        if (TechStyle.blink()) {
            graphics.drawCenteredString(font, TechStyle.titleText(scanning), centerX, centerY - 34 - 16, TechStyle.AMBER);
        }
        PrehistoricCreature target = AnalyzerItem.aimed(minecraft.player);
        int textY = centerY + 34 + 8;
        if (target != null) {
            String species = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
            String name = DinoFileClient.isRegistered(species)
                    ? WikiManual.get().sheet(species).map(sheet -> sheet.name()).orElse(target.getName().getString())
                    : "???";
            String distance = String.format(Locale.ROOT, "%.0f m", target.distanceTo(minecraft.player));
            graphics.drawCenteredString(font, "▶ " + name.toUpperCase(Locale.ROOT) + "  " + distance, centerX, textY,
                    TechStyle.BRIGHT);
        }
        int barWidth = 80;
        TechStyle.bar(graphics, centerX - barWidth / 2, textY + 12, barWidth, 5, progress, TechStyle.ACCENT);
        String percent = Math.round(progress * 100) + "%";
        graphics.drawString(font, percent, centerX + barWidth / 2 + 4, textY + 10, TechStyle.TEXT);
    }
}
