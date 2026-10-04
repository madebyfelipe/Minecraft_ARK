package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.client.dex.AnalyzerScreen;
import dev.madebyfelipe.iceagesurvival.network.AnalyzerHoldPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * O lado do cliente do slot do analisador ({@link dev.madebyfelipe.iceagesurvival.item.AnalyzerSlot}): a tecla que,
 * segurada, traz o aparelho para a mão, e o slot desenhado no inventário, ao lado do boneco, acima da mão secundária.
 * Clicar no slot abre o terminal do aparelho.
 */
public final class AnalyzerSlotClient {
    private static final KeyMapping HOLD =
            new KeyMapping("key.iceagesurvival.analyzer", GLFW.GLFW_KEY_R, "key.categories.iceagesurvival");
    /** Canto do slot no inventário, relativo à janela (o da mão secundária fica 18 abaixo). */
    private static final int SLOT_X = 76;
    private static final int SLOT_Y = 43;
    /** Com a tecla segurada e o aparelho fora da mão (trocou o slot da barra), pede de novo a cada tanto. */
    private static final int RETRY_TICKS = 10;

    private static boolean sentHeld;
    private static int retry;

    private AnalyzerSlotClient() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HOLD);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            sentHeld = false;
            return;
        }
        // Abrir uma tela solta as teclas; com tela aberta, o aparelho volta ao slot.
        boolean held = HOLD.isDown() && minecraft.screen == null;
        if (held != sentHeld) {
            sentHeld = held;
            retry = RETRY_TICKS;
            ModPayloads.sendToServer(new AnalyzerHoldPayload(held));
        } else if (held && !minecraft.player.getMainHandItem().is(ModItems.ANALYZER.get()) && --retry <= 0) {
            retry = RETRY_TICKS;
            ModPayloads.sendToServer(new AnalyzerHoldPayload(true));
        }
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int x = screen.getGuiLeft() + SLOT_X;
        int y = screen.getGuiTop() + SLOT_Y;
        // A moldura de um slot do vanilla: sombra em cima e à esquerda, luz embaixo e à direita.
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.renderItem(new ItemStack(ModItems.ANALYZER.get()), x + 1, y + 1);
        if (inside(screen, event.getMouseX(), event.getMouseY())) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
            graphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(
                    Component.translatable("item.iceagesurvival.analyzer"),
                    Component.translatable("iceagesurvival.analyzer.slot.hold", HOLD.getTranslatedKeyMessage())
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("iceagesurvival.analyzer.slot.open").withStyle(ChatFormatting.GRAY)),
                    event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof InventoryScreen screen && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && screen.getMenu().getCarried().isEmpty()
                && inside(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            AnalyzerScreen.open();
        }
    }

    private static boolean inside(InventoryScreen screen, double mouseX, double mouseY) {
        int x = screen.getGuiLeft() + SLOT_X;
        int y = screen.getGuiTop() + SLOT_Y;
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }
}
