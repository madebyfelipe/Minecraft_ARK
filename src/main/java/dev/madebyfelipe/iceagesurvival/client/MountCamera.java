package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Câmera da montaria: ao subir numa criatura do mod, guarda a visão atual e passa para a
 * terceira pessoa atrás; ao descer, devolve a visão guardada. Só age nas transições — montado,
 * o F5 continua livre. Barco, carrinho e cavalo ficam de fora. Morrer montado também é descer.
 */
public final class MountCamera {
    /** Visão de antes de montar; nula enquanto o jogador não está numa criatura. */
    @Nullable
    private static CameraType previous;

    private MountCamera() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean mounted = minecraft.player != null
                && minecraft.player.getVehicle() instanceof PrehistoricCreature;
        if (mounted && previous == null) {
            previous = minecraft.options.getCameraType();
            apply(minecraft, CameraType.THIRD_PERSON_BACK);
        } else if (!mounted && previous != null) {
            restore(minecraft);
        }
    }

    /** Sair do mundo montado: a visão salva nas opções volta a ser a de antes da montaria. */
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (previous != null) {
            restore(Minecraft.getInstance());
        }
    }

    private static void restore(Minecraft minecraft) {
        CameraType target = previous;
        previous = null;
        apply(minecraft, target);
    }

    /** O mesmo que o F5 do vanilla faz ao trocar de visão. */
    private static void apply(Minecraft minecraft, CameraType type) {
        CameraType current = minecraft.options.getCameraType();
        if (current == type) {
            return;
        }
        minecraft.options.setCameraType(type);
        if (current.isFirstPerson() != type.isFirstPerson()) {
            minecraft.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? minecraft.getCameraEntity() : null);
        }
        minecraft.levelRenderer.needsUpdate();
    }
}
