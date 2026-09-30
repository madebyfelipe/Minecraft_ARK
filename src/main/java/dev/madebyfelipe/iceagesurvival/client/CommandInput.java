package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.AttackOrderPayload;
import dev.madebyfelipe.iceagesurvival.network.MountAttackPayload;
import dev.madebyfelipe.iceagesurvival.network.SetOrderPayload;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Atalhos de comando e a entidade que o jogador está mirando à distância. */
public final class CommandInput {
    private static final String CATEGORY = "key.categories.iceagesurvival";
    private static final KeyMapping CYCLE_ORDER =
            new KeyMapping("key.iceagesurvival.cycle_order", GLFW.GLFW_KEY_R, CATEGORY);
    private static final KeyMapping ORDER_ATTACK =
            new KeyMapping("key.iceagesurvival.order_attack", GLFW.GLFW_KEY_G, CATEGORY);

    /** Entidade viva sob a mira neste tick, até {@link CreatureCommands#TARGET_RANGE}. */
    @Nullable
    private static LivingEntity aimed;

    private CommandInput() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_ORDER);
        event.register(ORDER_ATTACK);
    }

    @Nullable
    public static LivingEntity aimedEntity() {
        return aimed != null && aimed.isAlive() ? aimed : null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        aimed = minecraft.player == null || minecraft.level == null ? null : findAimedEntity(minecraft);

        while (CYCLE_ORDER.consumeClick()) {
            if (aimed instanceof PrehistoricCreature creature && creature.isOwner(minecraft.player)
                    && creature.distanceToSqr(minecraft.player) <= CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
                PacketDistributor.sendToServer(new SetOrderPayload(creature.getId(), creature.order().next()));
            }
        }
        while (ORDER_ATTACK.consumeClick()) {
            if (aimed != null) {
                PacketDistributor.sendToServer(new AttackOrderPayload(aimed.getId()));
            }
        }
    }

    /**
     * Montado numa criatura, o clique de ataque vira mordida dela: o ataque do jogador some e o
     * servidor recebe em quem a mira está. O servidor confere alcance e dono.
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
        if (aimed != null && aimed != mount) {
            PacketDistributor.sendToServer(new MountAttackPayload(aimed.getId()));
        }
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
