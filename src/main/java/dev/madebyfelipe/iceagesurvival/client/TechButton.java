package dev.madebyfelipe.iceagesurvival.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Botão de placa de metal com bisel, no estilo do {@link TechStyle}, no lugar da textura de pedra do vanilla. Com o
 * mouse em cima, o cursor âmbar pisca em volta. Desligado, ou fica apagado ou — nos botões de escolha, como as ordens
 * do apito — aceso como a opção em vigor.
 */
public class TechButton extends Button {
    private final int color;
    private final boolean selectedWhenInactive;

    public TechButton(int x, int y, int width, int height, Component message, int color, OnPress onPress) {
        this(x, y, width, height, message, color, false, onPress);
    }

    /**
     * @param color                a cor do texto e da faixa de baixo (ciano nas ações, âmbar no "parar")
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

        // Placa: sombra por fora, corpo de metal escuro, bisel claro em cima e à esquerda, escuro embaixo e à direita.
        TechStyle.chamfer(graphics, x - 1, y - 1, right + 1, bottom + 1, 3, TechStyle.METAL_SHADOW);
        TechStyle.chamfer(graphics, x, y, right, bottom, 2,
                selected ? HudShapes.lerpColor(TechStyle.METAL_DARK, TechStyle.AMBER, 0.25F)
                        : disabled ? HudShapes.lerpColor(TechStyle.METAL_DARK, TechStyle.METAL_SHADOW, 0.5F)
                        : hot ? HudShapes.lerpColor(TechStyle.METAL_DARK, TechStyle.METAL, 0.45F) : TechStyle.METAL_DARK);
        int light = disabled ? TechStyle.METAL_DARK : TechStyle.METAL;
        graphics.fill(x + 2, y, right - 2, y + 1, light);
        graphics.fill(x, y + 2, x + 1, bottom - 2, light);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, TechStyle.METAL_SHADOW);
        graphics.fill(right - 1, y + 2, right, bottom - 2, TechStyle.METAL_SHADOW);
        if (selected) {
            graphics.fill(x + 2, bottom - 3, right - 2, bottom - 2, TechStyle.AMBER);
        } else if (!disabled) {
            graphics.fill(x + 2, bottom - 3, right - 2, bottom - 2, HudShapes.fade(color, hot ? 0.9F : 0.45F));
        }
        if (hot) {
            // O cursor de DC2: contorno âmbar piscando.
            TechStyle.border(graphics, x - 1, y - 1, right + 1, bottom + 1,
                    TechStyle.blink() ? TechStyle.AMBER : HudShapes.fade(TechStyle.AMBER, 0.45F));
        }

        Font font = Minecraft.getInstance().font;
        int textColor = selected ? TechStyle.AMBER : hot ? TechStyle.TEXT
                : disabled ? HudShapes.fade(TechStyle.SUBTLE, 0.5F) : color;
        String text = getMessage().getString().toUpperCase(Locale.ROOT);
        int maxWidth = width - 6;
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, maxWidth);
        }
        graphics.drawCenteredString(font, text, x + width / 2, y + (height - 8) / 2 - 1, textColor);
    }
}
