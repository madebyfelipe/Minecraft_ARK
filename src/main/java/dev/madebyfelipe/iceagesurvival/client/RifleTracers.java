package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.item.GunKinematics;
import dev.madebyfelipe.iceagesurvival.item.TranqRifleItem;
import dev.madebyfelipe.iceagesurvival.network.RifleTracerPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * O traçante do rifle no cliente: uma linha fina e brilhante (miolo branco-amarelado e halo amarelo, mistura aditiva)
 * da boca do cano ao ponto atingido, que some em {@link GunKinematics#TRACER_LIFETIME_TICKS} ticks, mais um clarão e
 * uma fumacinha na boca do cano. Lista curta com idade, esvaziada a cada tick.
 */
public final class RifleTracers {
    /** Teto da lista: mesmo num tiroteio, nada se acumula. */
    private static final int MAX_TRACERS = 32;
    /** Meia largura do miolo e do halo, em blocos. */
    private static final float CORE_HALF_WIDTH = 0.012F;
    private static final float GLOW_HALF_WIDTH = 0.05F;
    /** Clarão da boca do cano: raio em blocos e quantos ticks dura. */
    private static final float FLASH_RADIUS = 0.14F;
    private static final double FLASH_TICKS = 2.0;
    /** Boca do cano vista em primeira pessoa: mais à frente, ao lado e abaixo, onde a arma aparece na tela. */
    private static final double FIRST_PERSON_FORWARD = 0.9;
    private static final double FIRST_PERSON_SIDE = 0.32;
    private static final double FIRST_PERSON_DROP = 0.26;

    private static final List<Tracer> TRACERS = new ArrayList<>();

    private RifleTracers() {
    }

    private static final class Tracer {
        final Vec3 start;
        final Vec3 end;
        int age;

        Tracer(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }
    }

    /** Chegou um tiro: guarda o traçante, solta a fumaça e avisa o coice em terceira pessoa. */
    public static void receive(RifleTracerPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 start = payload.start();
        LocalPlayer self = minecraft.player;
        if (self != null && self.getId() == payload.shooterId() && minecraft.options.getCameraType().isFirstPerson()) {
            start = firstPersonMuzzle(self);
        }
        GunClientExtensions.onRemoteShot(payload.shooterId());
        if (TRACERS.size() >= MAX_TRACERS) {
            TRACERS.remove(0);
        }
        TRACERS.add(new Tracer(start, payload.end()));

        Vec3 direction = payload.end().subtract(start).normalize();
        for (int i = 0; i < 3; i++) {
            double speed = 0.02 + 0.02 * i;
            level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z,
                    direction.x * speed, direction.y * speed + 0.01, direction.z * speed);
        }
        if (payload.impact() == RifleTracerPayload.Impact.BLOCK) {
            Vec3 back = payload.end().subtract(direction.scale(0.05));
            level.addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0.0, 0.02, 0.0);
        } else if (payload.impact() == RifleTracerPayload.Impact.ENTITY) {
            Vec3 at = payload.end();
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z,
                        -direction.x * 0.2, 0.1, -direction.z * 0.2);
            }
        }
    }

    /** A boca do cano onde a arma aparece na tela, para quem atirou em primeira pessoa. */
    private static Vec3 firstPersonMuzzle(LocalPlayer player) {
        boolean mainHand = player.getMainHandItem().getItem() instanceof TranqRifleItem;
        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        GunKinematics.Offset offset = GunKinematics.muzzleOffset(player.getYRot(), player.getXRot(),
                arm == HumanoidArm.RIGHT ? 1 : -1, FIRST_PERSON_FORWARD, FIRST_PERSON_SIDE, FIRST_PERSON_DROP);
        return player.getEyePosition().add(offset.x(), offset.y(), offset.z());
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACERS.isEmpty() || Minecraft.getInstance().isPaused()) {
            return;
        }
        TRACERS.removeIf(tracer -> ++tracer.age >= GunKinematics.TRACER_LIFETIME_TICKS);
    }

    public static void clear() {
        TRACERS.clear();
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TRACERS.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TracerRenderType.TRACER);
        float partialTick = event.getPartialTick();
        for (Tracer tracer : TRACERS) {
            double age = tracer.age + partialTick;
            float alpha = GunKinematics.tracerAlpha(age, GunKinematics.TRACER_LIFETIME_TICKS);
            if (alpha <= 0.0F) {
                continue;
            }
            Vector3f from = relative(tracer.start, eye);
            Vector3f to = relative(tracer.end, eye);
            beam(consumer, matrix, from, to, GLOW_HALF_WIDTH, 1.0F, 0.85F, 0.45F, alpha * 0.35F);
            beam(consumer, matrix, from, to, CORE_HALF_WIDTH, 1.0F, 0.98F, 0.88F, alpha);
            if (age < FLASH_TICKS) {
                flash(consumer, matrix, camera, from, (float) (1.0 - age / FLASH_TICKS));
            }
        }
        buffers.endBatch(TracerRenderType.TRACER);
    }

    private static Vector3f relative(Vec3 point, Vec3 eye) {
        return new Vector3f((float) (point.x - eye.x), (float) (point.y - eye.y), (float) (point.z - eye.z));
    }

    /** Uma faixa voltada para a câmera, de {@code from} a {@code to} (coordenadas relativas ao olho da câmera). */
    private static void beam(VertexConsumer consumer, Matrix4f matrix, Vector3f from, Vector3f to, float halfWidth,
                             float red, float green, float blue, float alpha) {
        Vector3f along = new Vector3f(to).sub(from);
        Vector3f sideFrom = facingSide(along, from, halfWidth);
        Vector3f sideTo = facingSide(along, to, halfWidth);
        vertex(consumer, matrix, new Vector3f(from).add(sideFrom), red, green, blue, alpha);
        vertex(consumer, matrix, new Vector3f(from).sub(sideFrom), red, green, blue, alpha);
        vertex(consumer, matrix, new Vector3f(to).sub(sideTo), red, green, blue, alpha);
        vertex(consumer, matrix, new Vector3f(to).add(sideTo), red, green, blue, alpha);
    }

    /** Lado da faixa num ponto: perpendicular à linha e à direção do olhar até ele. */
    private static Vector3f facingSide(Vector3f along, Vector3f point, float halfWidth) {
        Vector3f side = new Vector3f(along).cross(point);
        if (side.lengthSquared() < 1.0E-8F) {
            side.set(0.0F, 1.0F, 0.0F).cross(along);
            if (side.lengthSquared() < 1.0E-8F) {
                side.set(1.0F, 0.0F, 0.0F);
            }
        }
        return side.normalize(halfWidth);
    }

    /** Clarão da boca do cano: um quadrado voltado para a câmera, que se apaga em {@link #FLASH_TICKS}. */
    private static void flash(VertexConsumer consumer, Matrix4f matrix, Camera camera, Vector3f at, float strength) {
        float radius = FLASH_RADIUS * (0.6F + 0.4F * strength);
        Vector3f left = new Vector3f(camera.getLeftVector()).mul(radius);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(radius);
        float alpha = 0.9F * strength;
        vertex(consumer, matrix, new Vector3f(at).add(left).add(up), 1.0F, 0.9F, 0.6F, alpha);
        vertex(consumer, matrix, new Vector3f(at).add(left).sub(up), 1.0F, 0.9F, 0.6F, alpha);
        vertex(consumer, matrix, new Vector3f(at).sub(left).sub(up), 1.0F, 0.9F, 0.6F, alpha);
        vertex(consumer, matrix, new Vector3f(at).sub(left).add(up), 1.0F, 0.9F, 0.6F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f at, float red, float green,
                               float blue, float alpha) {
        consumer.vertex(matrix, at.x(), at.y(), at.z()).color(red, green, blue, alpha).endVertex();
    }
}
