package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.ChemistryBenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela das estações de duas entradas e uma saída: mesa química, estação de preparação, mesa de reviver. */
public class ChemistryBenchScreen extends StationScreen<ChemistryBenchMenu> {
    public ChemistryBenchScreen(ChemistryBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderStation(GuiGraphics graphics) {
        // Seta de progresso entre as entradas e o resultado.
        bar(graphics, ChemistryBenchMenu.INPUT_B_X + 22, ChemistryBenchMenu.INPUT_Y + 5, 26, 6, menu.progress(), GREEN);
        graphics.drawString(font, Component.translatable(menu.hintKey()),
                leftPos + 8, topPos + 60, SUBTLE, false);
    }
}
