package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.network.CreatureLocationsPayload;
import dev.madebyfelipe.iceagesurvival.network.LocateRequestPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;

/**
 * A criatura que o jogador mandou localizar: guarda a lista recebida do servidor e mostra o {@link RadarHud} com
 * ela, o nível, a distância e a diferença de altura. Enquanto há alguém sendo localizado, a posição é pedida de novo
 * a cada 2 s; perto o bastante, o radar some.
 */
public final class CreatureTracker {
    private static final int REFRESH_TICKS = 40;
    private static final double ARRIVED_DISTANCE = 6.0;

    private static List<CreatureLocator.Entry> entries = List.of();
    @Nullable
    private static UUID tracked;
    private static int refreshCountdown;

    private CreatureTracker() {
    }

    public static List<CreatureLocator.Entry> entries() {
        return entries;
    }

    @Nullable
    public static UUID tracked() {
        return tracked;
    }

    /** Pede a lista ao servidor (a tela chama ao abrir). */
    public static void requestList() {
        ModPayloads.sendToServer(new LocateRequestPayload(Optional.empty()));
    }

    /** Começa a localizar a criatura; se estiver carregada no servidor, ela brilha. */
    public static void track(UUID creature) {
        tracked = creature;
        refreshCountdown = REFRESH_TICKS;
        ModPayloads.sendToServer(new LocateRequestPayload(Optional.of(creature)));
    }

    public static void stop() {
        tracked = null;
    }

    public static void receive(CreatureLocationsPayload payload) {
        entries = List.copyOf(payload.entries());
        if (tracked != null && entries.stream().noneMatch(entry -> entry.creature().equals(tracked))) {
            tracked = null; // morreu, foi solta ou mudou de dono
        }
        if (Minecraft.getInstance().screen instanceof CreatureLocatorScreen screen) {
            screen.refresh();
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || tracked == null || minecraft.player == null) {
            return;
        }
        if (--refreshCountdown <= 0) {
            refreshCountdown = REFRESH_TICKS;
            requestList();
        }
        CreatureLocator.Entry entry = trackedEntry();
        if (entry != null && entry.dimension().equals(minecraft.player.level().dimension())
                && position(entry).distanceTo(minecraft.player.position()) < ARRIVED_DISTANCE) {
            tracked = null;
        }
    }

    @Nullable
    private static CreatureLocator.Entry trackedEntry() {
        for (CreatureLocator.Entry entry : entries) {
            if (entry.creature().equals(tracked)) {
                return entry;
            }
        }
        return null;
    }

    /** Posição de agora se a criatura estiver carregada no cliente; senão a última que o servidor mandou. */
    public static Vec3 position(CreatureLocator.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension().equals(entry.dimension())) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity.getUUID().equals(entry.creature())) {
                    return entity.position();
                }
            }
        }
        return Vec3.atBottomCenterOf(entry.pos());
    }

    /** A leitura da criatura para o jogador de agora; {@code null} sem jogador. */
    @Nullable
    static RadarHud.Reading read(CreatureLocator.Entry entry) {
        return RadarHud.read(position(entry), entry.dimension());
    }

    static Component distanceText(RadarHud.Reading reading) {
        return RadarHud.distanceText(reading, Component.translatable("iceagesurvival.locator.other_dimension"));
    }

    /** O radar no canto. */
    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        CreatureLocator.Entry entry = tracked == null ? null : trackedEntry();
        // Com o Receptor de Sinal na mão, o radar do canto é dele.
        if (entry == null || BaseSignalHud.active() || !RadarHud.visible()) {
            return;
        }
        RadarHud.Reading reading = read(entry);
        if (reading != null) {
            RadarHud.render(graphics, screenWidth, reading, Component.literal(entry.name()),
                    Component.translatable("iceagesurvival.hud.level", entry.level()),
                    Component.translatable("iceagesurvival.locator.other_dimension"));
        }
    }
}
