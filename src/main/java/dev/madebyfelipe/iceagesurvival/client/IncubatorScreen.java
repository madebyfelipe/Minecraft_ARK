package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.menu.IncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class IncubatorScreen extends StationScreen<IncubatorMenu> {
    public IncubatorScreen(IncubatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderStation(GuiGraphics graphics) {
        int barX = IncubatorMenu.EGG_X + 24;
        bar(graphics, barX, IncubatorMenu.EGG_Y + 5, 60, 6, menu.incubation(), GREEN);
        int seconds = menu.secondsLeft();
        Component eggStatus = seconds < 0
                ? Component.translatable("iceagesurvival.incubator.empty")
                : Component.translatable("iceagesurvival.incubator.time", seconds / 60, String.format("%02d", seconds % 60));
        graphics.drawString(font, eggStatus, leftPos + barX, topPos + IncubatorMenu.EGG_Y - 8, SUBTLE, false);

        bar(graphics, IncubatorMenu.FUEL_X + 24, IncubatorMenu.FUEL_Y + 5, 60, 6, menu.fuel(), WARM);
        Component heat = Component.translatable(menu.blockHeat() ? "iceagesurvival.incubator.heat_block"
                : menu.heated() ? "iceagesurvival.incubator.heat_fuel" : "iceagesurvival.incubator.cold");
        graphics.drawString(font, heat, leftPos + IncubatorMenu.FUEL_X + 24, topPos + IncubatorMenu.FUEL_Y - 6,
                menu.heated() ? WARM : COLD, false);
    }
}
