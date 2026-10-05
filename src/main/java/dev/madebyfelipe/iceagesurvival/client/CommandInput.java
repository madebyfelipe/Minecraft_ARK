package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.AttackOrderPayload;
import dev.madebyfelipe.iceagesurvival.network.FlightInputPayload;
import dev.madebyfelipe.iceagesurvival.network.MountAttackPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.MoveOrderPayload;
import dev.madebyfelipe.iceagesurvival.network.WhistlePayload;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Atalhos de comando, no esquema do ARK — um assobio por tecla, para a criatura mirada ou,
 * sem mira, para todas as do jogador ao alcance — e a entidade que o jogador está mirando.
 * O T do ARK é o chat do Minecraft, daí as teclas padrão diferentes; todas são remapeáveis.
 */
public final class CommandInput {
    private static final String CATEGORY = "key.categories.iceagesurvival";
    private static final Map<Whistle, KeyMapping> WHISTLES = new EnumMap<>(Map.of(
            Whistle.FOLLOW, whistleKey(Whistle.FOLLOW, GLFW.GLFW_KEY_Y),
            Whistle.STAY, whistleKey(Whistle.STAY, GLFW.GLFW_KEY_U),
            Whistle.PASSIVE, whistleKey(Whistle.PASSIVE, GLFW.GLFW_KEY_J),
            Whistle.NEUTRAL, whistleKey(Whistle.NEUTRAL, GLFW.GLFW_KEY_K),
            Whistle.DEFEND, whistleKey(Whistle.DEFEND, GLFW.GLFW_KEY_H),
            Whistle.FLEE, whistleKey(Whistle.FLEE, GLFW.GLFW_KEY_UNKNOWN)));
    private static final KeyMapping ORDER_ATTACK =
            new KeyMapping("key.iceagesurvival.order_attack", GLFW.GLFW_KEY_G, CATEGORY);
    private static final KeyMapping ORDER_MOVE =
            new KeyMapping("key.iceagesurvival.order_move", GLFW.GLFW_KEY_B, CATEGORY);
    private static final KeyMapping STATUS =
            new KeyMapping("key.iceagesurvival.status", GLFW.GLFW_KEY_V, CATEGORY);
    private static final KeyMapping LOCATE =
            new KeyMapping("key.iceagesurvival.locate", GLFW.GLFW_KEY_O, CATEGORY);
    private static int flightPacketCooldown;
    private static boolean lastSentFlying;

    /** Entidade viva sob a mira neste tick, até {@link CreatureCommands#TARGET_RANGE}. */
    @Nullable
    private static LivingEntity aimed;

    private CommandInput() {
    }

    private static KeyMapping whistleKey(Whistle whistle, int key) {
        return new KeyMapping("key.iceagesurvival.whistle." + whistle.id(), key, CATEGORY);
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        WHISTLES.values().forEach(event::register);
        event.register(ORDER_ATTACK);
        event.register(ORDER_MOVE);
        event.register(STATUS);
        event.register(LOCATE);
    }

    static boolean isStatusKey(int keyCode, int scanCode) {
        return STATUS.matches(keyCode, scanCode);
    }

    static Component statusKeyName() {
        return STATUS.getTranslatedKeyMessage();
    }

    @Nullable
    public static LivingEntity aimedEntity() {
        return aimed != null && aimed.isAlive() ? aimed : null;
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        aimed = minecraft.player == null || minecraft.level == null ? null : findAimedEntity(minecraft);

        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.getVehicle() instanceof PrehistoricCreature mount
                && mount.getControllingPassenger() == minecraft.player) {
            // Pulo e voo sem a barra de carga do cavalo: a criatura lê as teclas direto.
            mount.setRiderInput(minecraft.options.keyJump.isDown(), minecraft.options.keySprint.isDown());
            if (mount.isFlightMount()) {
                boolean flying = mount.isFlying();
                if (flying != lastSentFlying || --flightPacketCooldown <= 0) {
                    flightPacketCooldown = 20;
                    lastSentFlying = flying;
                    ModPayloads.sendToServer(new FlightInputPayload(mount.getId(), flying));
                }
            }
        } else {
            flightPacketCooldown = 0;
            lastSentFlying = false;
        }
        for (Map.Entry<Whistle, KeyMapping> entry : WHISTLES.entrySet()) {
            while (entry.getValue().consumeClick()) {
                int aimedId = aimed instanceof PrehistoricCreature creature && creature.canCommand(minecraft.player)
                        ? creature.getId() : WhistlePayload.NO_TARGET;
                ModPayloads.sendToServer(new WhistlePayload(entry.getKey(), aimedId));
            }
        }
        while (STATUS.consumeClick()) {
            // Sem mira, montado: o status é o da montaria.
            if (aimed instanceof PrehistoricCreature creature) {
                CreatureStatusScreen.request(creature);
            } else if (minecraft.player.getVehicle() instanceof PrehistoricCreature mount) {
                CreatureStatusScreen.request(mount);
            }
        }
        while (LOCATE.consumeClick()) {
            CreatureLocatorScreen.open();
        }
        while (ORDER_ATTACK.consumeClick()) {
            if (aimed != null) {
                ModPayloads.sendToServer(new AttackOrderPayload(aimed.getId()));
            }
        }
        while (ORDER_MOVE.consumeClick()) {
            // Locomover: o bloco mirado, até o alcance das ordens de ataque; o destino é o espaço em cima dele.
            HitResult hit = minecraft.player.pick(CreatureCommands.TARGET_RANGE, 1.0F, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                ModPayloads.sendToServer(new MoveOrderPayload(block.getDirection() == Direction.DOWN
                        ? block.getBlockPos().below() : block.getBlockPos().above()));
            }
        }
    }

    /**
     * Montado numa criatura, o clique de ataque vira mordida dela: o ataque do jogador some e o
     * servidor recebe em quem a mira está, se estiver em alguém. A mordida sai mesmo sem alvo;
     * o servidor confere alcance e dono.
     */
    public static void onAttackClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack() || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof PrehistoricCreature mount)
                || mount.getControllingPassenger() != minecraft.player) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        ModPayloads.sendToServer(new MountAttackPayload(
                aimed != null && aimed != mount ? aimed.getId() : MountAttackPayload.NO_TARGET));
    }

    @Nullable
    private static LivingEntity findAimedEntity(Minecraft minecraft) {
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return null;
        }
        double range = CreatureCommands.TARGET_RANGE;
        Vec3 eye = camera.getEyePosition();
        Vec3 look = camera.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));

        // Blocos tapam a mira: só vale a entidade que estiver antes do primeiro bloco.
        HitResult block = camera.pick(range, 1.0F, false);
        double maxDistanceSqr = block.getType() == HitResult.Type.MISS
                ? range * range
                : block.getLocation().distanceToSqr(eye);

        AABB search = camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                camera, eye, end, search,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator(),
                maxDistanceSqr);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
