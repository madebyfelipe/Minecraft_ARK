package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.network.CreatureLocationsPayload;
import dev.madebyfelipe.iceagesurvival.network.LocateRequestPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;

/**
 * A criatura que o jogador mandou localizar: guarda a lista recebida do servidor e desenha, acima da
 * barra de itens, uma bússola com o nome, a direção relativa ao olhar e a distância. Enquanto há
 * alguém sendo localizado, a posição é pedida de novo a cada 2 s; perto o bastante, a bússola some.
 */
public final class CreatureTracker {
    private static final int REFRESH_TICKS = 40;
    private static final double ARRIVED_DISTANCE = 6.0;
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final int TEXT = 0xFFFFFFFF;
    private static final int ACCENT = 0xFF5FB36B;

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

    /**
     * Seta da direção da criatura em relação a para onde o jogador olha, e a distância; ou "outra
     * dimensão".
     */
    public static Component direction(CreatureLocator.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !entry.dimension().equals(minecraft.player.level().dimension())) {
            return Component.translatable("iceagesurvival.locator.other_dimension");
        }
        Vec3 target = position(entry);
        Vec3 from = minecraft.player.position();
        double dx = target.x - from.x;
        double dz = target.z - from.z;
        float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float relative = Mth.wrapDegrees(bearing - minecraft.player.getYRot());
        int sector = Math.floorMod(Math.round(relative / 45.0F), ARROWS.length);
        return Component.literal(ARROWS[sector] + " " + String.format(Locale.ROOT, "%.0f m", target.distanceTo(from)));
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        CreatureLocator.Entry entry = tracked == null ? null : trackedEntry();
        if (entry == null || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        Component name = Component.literal(entry.name() + "  ");
        Component where = direction(entry);
        int width = minecraft.font.width(name) + minecraft.font.width(where);
        int x = (screenWidth - width) / 2;
        int y = screenHeight - 72;
        graphics.fill(x - 4, y - 3, x + width + 4, y + minecraft.font.lineHeight + 2, 0xB0101418);
        graphics.drawString(minecraft.font, name, x, y, TEXT);
        graphics.drawString(minecraft.font, where, x + minecraft.font.width(name), y, ACCENT);
    }
}
