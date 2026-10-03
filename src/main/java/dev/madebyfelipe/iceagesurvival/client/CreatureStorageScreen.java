package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CreatureStorageScreen extends AbstractContainerScreen<CreatureStorageMenu> {
    public CreatureStorageScreen(CreatureStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = menu.rows() * 18 + 114;
        inventoryLabelY = menu.rows() * 18 + 20;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.pages() > 1) {
            addRenderableWidget(new TechButton(leftPos + imageWidth - 40, topPos + 4, 16, 14, Component.literal("<"),
                    TechStyle.BRIGHT, button -> changePage(0)));
            addRenderableWidget(new TechButton(leftPos + imageWidth - 21, topPos + 4, 16, 14, Component.literal(">"),
                    TechStyle.BRIGHT, button -> changePage(1)));
        }
    }

    private void changePage(int direction) {
        if (menu.clickMenuButton(minecraft.player, direction)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TechStyle.frame(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        if (menu.hasSaddleSlot()) {
            // Abinha da sela, à esquerda do painel.
            int x = leftPos + CreatureStorageMenu.SADDLE_X;
            int y = topPos + CreatureStorageMenu.SADDLE_Y;
            TechStyle.frame(graphics, x - 5, y - 5, x + 21, y + 21);
        }
        for (var slot : menu.slots) {
            TechStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleRight = imageWidth - 8;
        if (menu.pages() > 1) {
            Component pageLabel = Component.translatable("iceagesurvival.storage.page", menu.page() + 1, menu.pages());
            int pageX = imageWidth - 44 - font.width(pageLabel);
            graphics.drawString(font, pageLabel, pageX, 7, TechStyle.SUBTLE, false);
            titleRight = pageX - 4;
        }
        TechStyle.titleLine(graphics, font, title, 8, 6, titleRight);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TechStyle.SUBTLE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No 1.20.1 o fundo escurecido não vem do super.render: cada tela o desenha.
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
