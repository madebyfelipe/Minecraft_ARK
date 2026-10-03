package dev.madebyfelipe.iceagesurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Botão chapado com borda, no estilo do {@link TechStyle}, no lugar da textura de pedra do vanilla. Desligado, ou
 * fica apagado ou — nos botões de escolha, como as ordens do apito — aceso como a opção em vigor.
 */
public class TechButton extends Button {
    private final int color;
    private final boolean selectedWhenInactive;

    public TechButton(int x, int y, int width, int height, Component message, int color, OnPress onPress) {
        this(x, y, width, height, message, color, false, onPress);
    }

    /**
     * @param selectedWhenInactive desligado quer dizer "escolhido" (aceso), não "indisponível" (apagado)
     */
    public TechButton(int x, int y, int width, int height, Component message, int color, boolean selectedWhenInactive,
                      OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.color = color;
        this.selectedWhenInactive = selectedWhenInactive;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        boolean selected = !active && selectedWhenInactive;
        boolean disabled = !active && !selectedWhenInactive;
        boolean hot = active && isHoveredOrFocused();
        float fill = selected ? 0.45F : hot ? 0.35F : disabled ? 0.04F : 0.12F;
        float edge = selected || hot ? 1.0F : disabled ? 0.25F : 0.6F;
        graphics.fill(x, y, right, bottom, HudShapes.fade(color, fill));
        TechStyle.border(graphics, x, y, right, bottom, HudShapes.fade(color, edge));
        if (selected) {
            graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, color);
        }
        Font font = Minecraft.getInstance().font;
        int textColor = selected || hot ? TechStyle.TEXT : disabled ? HudShapes.fade(TechStyle.SUBTLE, 0.6F) : color;
        Component message = getMessage();
        int maxWidth = width - 6;
        String text = font.width(message) > maxWidth ? font.plainSubstrByWidth(message.getString(), maxWidth)
                : message.getString();
        graphics.drawCenteredString(font, text, x + width / 2, y + (height - 8) / 2, textColor);
    }
}
