package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.item.SignalReceiverItem;
import dev.madebyfelipe.iceagesurvival.network.BaseSignalPayload;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * O Receptor de Sinal na mão: o {@link RadarHud} apontando para o centro da base militar que o servidor mandou
 * ({@link BaseSignalPayload}). Sem base na dimensão, "sem sinal" (e o modelo do aparelho mostra chiado).
 */
public final class BaseSignalHud {
    /** O que o servidor mandou por último; {@code null} antes da primeira resposta. */
    @Nullable
    private static Optional<GlobalPos> target;

    private BaseSignalHud() {
    }

    public static void receive(BaseSignalPayload payload) {
        target = payload.base();
    }

    /** Ao sair do mundo: o alvo era daquele mundo. */
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        target = null;
    }

    /** Se o radar da base está na tela: com o receptor na mão e a resposta do servidor já recebida. */
    static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        return target != null && minecraft.player != null && SignalReceiverItem.isHolding(minecraft.player);
    }

    /** Se o servidor achou a base nesta dimensão: o aparelho mostra a varredura; senão, chiado. */
    public static boolean hasSignal() {
        return target != null && target.isPresent();
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!active() || !RadarHud.visible()) {
            return;
        }
        RadarHud.Reading reading = target.isEmpty() ? RadarHud.Reading.NO_SIGNAL
                : RadarHud.read(Vec3.atBottomCenterOf(target.get().pos()), target.get().dimension());
        if (reading != null) {
            RadarHud.render(graphics, screenWidth, reading, Component.translatable("iceagesurvival.signal_receiver.title"),
                    Component.empty(), Component.translatable("iceagesurvival.signal_receiver.none"));
        }
    }
}
