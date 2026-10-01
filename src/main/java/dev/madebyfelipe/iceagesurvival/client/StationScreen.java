package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.StationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Tela de estação desenhada em código, no mesmo visual do painel das criaturas: fundo escuro,
 * borda verde e uma moldura para cada espaço. Dispensa textura de GUI.
 */
public abstract class StationScreen<M extends StationMenu> extends AbstractContainerScreen<M> {
    protected static final int PANEL = 0xF0101418;
    protected static final int BORDER = 0xFF5FB36B;
    protected static final int SLOT = 0xFF26292E;
    protected static final int SLOT_EDGE = 0xFF454B52;
    protected static final int TEXT = 0xFFE8EAEC;
    protected static final int SUBTLE = 0xFFA0A8B0;
    protected static final int WARM = 0xFFE8743B;
    protected static final int COLD = 0xFF4FA8E8;
    protected static final int GREEN = 0xFF5FB36B;

    protected StationScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, BORDER);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
            graphics.fill(x, y, x + 16, y + 16, SLOT);
        }
        renderStation(graphics);
    }

    /** O que é próprio da estação: barras e textos, em coordenadas da tela. */
    protected abstract void renderStation(GuiGraphics graphics);

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SUBTLE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No 1.20.1 o fundo escurecido não vem do super.render: cada tela o desenha.
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    protected void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        CreatureHud.drawBar(graphics, leftPos + x, topPos + y, width, height, fraction, color);
    }
}
