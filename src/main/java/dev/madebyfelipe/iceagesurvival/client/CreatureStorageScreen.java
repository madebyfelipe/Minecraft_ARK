package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CreatureStorageScreen extends AbstractContainerScreen<CreatureStorageMenu> {
    private static final int PANEL = 0xF0101418;
    private static final int BORDER = 0xFF5FB36B;
    private static final int SLOT = 0xFF26292E;
    private static final int SLOT_EDGE = 0xFF454B52;

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
            addRenderableWidget(Button.builder(Component.literal("<"),
                            button -> changePage(0))
                    .bounds(leftPos + imageWidth - 40, topPos + 5, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"),
                            button -> changePage(1))
                    .bounds(leftPos + imageWidth - 21, topPos + 5, 16, 16).build());
        }
    }

    private void changePage(int direction) {
        if (menu.clickMenuButton(minecraft.player, direction)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, BORDER);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
            graphics.fill(x, y, x + 16, y + 16, SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFE8EAEC, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFFA0A8B0, false);
        if (menu.pages() > 1) {
            Component pageLabel = Component.translatable("iceagesurvival.storage.page", menu.page() + 1, menu.pages());
            graphics.drawString(font, pageLabel, imageWidth - 78, 8, 0xFFE8EAEC, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No 1.20.1 o fundo escurecido não vem do super.render: cada tela o desenha.
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
