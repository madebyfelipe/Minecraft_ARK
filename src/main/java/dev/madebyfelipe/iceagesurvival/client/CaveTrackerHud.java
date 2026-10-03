package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.item.CaveTrackerItem;
import dev.madebyfelipe.iceagesurvival.network.CaveTargetPayload;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * O rastreador da caverna na mão: o {@link RadarHud} apontando para a boca da caverna da arena que o servidor mandou
 * ({@link CaveTargetPayload}), e o alvo da agulha do ícone. Sem caverna na dimensão, "sem sinal" e a agulha gira à
 * toa, como a bússola vanilla longe do spawn.
 */
public final class CaveTrackerHud {
    /** O que o servidor mandou por último; {@code null} antes da primeira resposta. */
    @Nullable
    private static Optional<GlobalPos> target;

    private CaveTrackerHud() {
    }

    public static void receive(CaveTargetPayload payload) {
        target = payload.mouth();
    }

    /** Ao sair do mundo: o alvo era daquele mundo. */
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        target = null;
    }

    /** Se o radar da caverna está na tela: com o rastreador na mão e a resposta do servidor já recebida. */
    static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        return target != null && minecraft.player != null && CaveTrackerItem.isHolding(minecraft.player);
    }

    /** Para a agulha do ícone ({@code CompassItemPropertyFunction}); {@code null} faz ela girar. */
    @Nullable
    static GlobalPos compassTarget(ClientLevel level, ItemStack stack, Entity entity) {
        return target == null ? null : target.orElse(null);
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!active() || !RadarHud.visible()) {
            return;
        }
        RadarHud.Reading reading = target.isEmpty() ? RadarHud.Reading.NO_SIGNAL
                : RadarHud.read(Vec3.atBottomCenterOf(target.get().pos()), target.get().dimension());
        if (reading != null) {
            RadarHud.render(graphics, screenWidth, reading, Component.translatable("iceagesurvival.cave_tracker.title"),
                    Component.empty(), Component.translatable("iceagesurvival.cave_tracker.none"));
        }
    }
}
