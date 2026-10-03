package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.StationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Tela de estação desenhada em código, no visual de aparelho do {@link TechStyle}: painel com cantoneiras, título
 * com a linha animada, espaços escuros de borda verde e barras segmentadas. Dispensa textura de GUI.
 */
public abstract class StationScreen<M extends StationMenu> extends AbstractContainerScreen<M> {
    protected static final int TEXT = TechStyle.TEXT;
    protected static final int SUBTLE = TechStyle.SUBTLE;
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
        TechStyle.frame(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        for (Slot slot : menu.slots) {
            TechStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        renderStation(graphics);
    }

    /** O que é próprio da estação: barras e textos, em coordenadas da tela. */
    protected abstract void renderStation(GuiGraphics graphics);

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        TechStyle.titleLine(graphics, font, title, titleLabelX, titleLabelY, imageWidth - 8);
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
        TechStyle.bar(graphics, leftPos + x, topPos + y, width, height, fraction, color);
    }
}
