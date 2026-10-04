package dev.madebyfelipe.iceagesurvival.client.firearm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.client.TracerRenderType;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmShots;
import dev.madebyfelipe.iceagesurvival.item.GunKinematics;
import dev.madebyfelipe.iceagesurvival.network.FirearmShotPayload;
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
 * Os traçantes das armas de fogo (D58): uma linha brilhante (mistura aditiva) da boca do cano ao ponto atingido, que
 * some em poucos ticks, mais o clarão e a fumaça do cano e as faíscas no impacto. Cor e espessura por arma: a bala
 * leve é fina e amarela, o chumbo da escopeta é curto e alaranjado, o antitanque é grosso e branco.
 */
public final class FirearmTracers {
    private static final int MAX_TRACERS = 64;

    /**
     * A boca do cano de cada arma vista em primeira pessoa (à frente, ao lado e abaixo do olho, em blocos), no mesmo
     * ponto da tela em que o modelo a mostra; calculado em {@code tools/gen_dc2_weapons.py} a partir das transformações
     * da mão, a 1,2 bloco do olho.
     */
    private record FirstPersonMuzzle(double forward, double side, double drop) {
    }

    private static final List<Tracer> TRACERS = new ArrayList<>();

    private record Style(float coreWidth, float glowWidth, float red, float green, float blue, float flash, int life) {
    }

    private static final class Tracer {
        final Vec3 start;
        final Vec3 end;
        final Style style;
        final boolean flash;
        int age;

        Tracer(Vec3 start, Vec3 end, Style style, boolean flash) {
            this.start = start;
            this.end = end;
            this.style = style;
            this.flash = flash;
        }
    }

    private FirearmTracers() {
    }

    private static FirstPersonMuzzle muzzle(Firearm gun) {
        return switch (gun) {
            case HANDGUN -> new FirstPersonMuzzle(1.153, 0.281, 0.180);
            case SHOTGUN -> new FirstPersonMuzzle(1.180, 0.157, 0.152);
            case SUBMACHINE_GUN -> new FirstPersonMuzzle(1.168, 0.225, 0.161);
            case HEAVY_MACHINE_GUN -> new FirstPersonMuzzle(1.179, 0.148, 0.170);
            case SOLID_CANNON -> new FirstPersonMuzzle(1.175, 0.198, 0.145);
            case ANTI_TANK_RIFLE -> new FirstPersonMuzzle(1.190, 0.093, 0.125);
        };
    }

    private static Style style(Firearm gun) {
        return switch (gun) {
            case HANDGUN -> new Style(0.010F, 0.040F, 1.0F, 0.86F, 0.45F, 0.12F, 3);
            case SHOTGUN -> new Style(0.008F, 0.030F, 1.0F, 0.70F, 0.35F, 0.20F, 2);
            case SUBMACHINE_GUN -> new Style(0.008F, 0.030F, 1.0F, 0.90F, 0.55F, 0.10F, 2);
            case HEAVY_MACHINE_GUN -> new Style(0.014F, 0.055F, 1.0F, 0.75F, 0.30F, 0.16F, 3);
            case SOLID_CANNON -> new Style(0.020F, 0.080F, 0.45F, 1.0F, 0.95F, 0.22F, 3);
            case ANTI_TANK_RIFLE -> new Style(0.030F, 0.120F, 1.0F, 0.97F, 0.88F, 0.30F, 6);
        };
    }

    /** Chegou um tiro: guarda os traçantes, solta a fumaça do cano e as faíscas no impacto. */
    static void receive(FirearmShotPayload payload, boolean self) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 start = payload.start();
        if (self && minecraft.player != null && minecraft.options.getCameraType().isFirstPerson()) {
            start = firstPersonMuzzle(minecraft.player, payload.mainHand(), muzzle(payload.gun()));
        }
        Style style = style(payload.gun());
        boolean first = true;
        for (FirearmShots.Segment segment : payload.segments()) {
            if (TRACERS.size() >= MAX_TRACERS) {
                TRACERS.remove(0);
            }
            TRACERS.add(new Tracer(start, segment.end(), style, first));
            first = false;
            Vec3 direction = segment.end().subtract(start).normalize();
            if (segment.impact() == FirearmShotPayload.Impact.BLOCK) {
                Vec3 back = segment.end().subtract(direction.scale(0.05));
                level.addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0.0, 0.02, 0.0);
                level.addParticle(ParticleTypes.CRIT, back.x, back.y, back.z, -direction.x * 0.1, 0.1,
                        -direction.z * 0.1);
            } else if (segment.impact() == FirearmShotPayload.Impact.ENTITY) {
                Vec3 at = segment.end();
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, -direction.x * 0.2, 0.1,
                            -direction.z * 0.2);
                }
            }
        }
        Vec3 look = payload.segments().isEmpty() ? Vec3.ZERO
                : payload.segments().get(0).end().subtract(start).normalize();
        int smoke = payload.gun() == Firearm.ANTI_TANK_RIFLE || payload.gun() == Firearm.SHOTGUN ? 6 : 2;
        for (int i = 0; i < smoke; i++) {
            double speed = 0.02 + 0.015 * i;
            level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z, look.x * speed, look.y * speed + 0.01,
                    look.z * speed);
        }
    }

    private static Vec3 firstPersonMuzzle(LocalPlayer player, boolean mainHand, FirstPersonMuzzle muzzle) {
        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        GunKinematics.Offset offset = GunKinematics.muzzleOffset(player.getYRot(), player.getXRot(),
                arm == HumanoidArm.RIGHT ? 1 : -1, muzzle.forward(), muzzle.side(), muzzle.drop());
        return player.getEyePosition().add(offset.x(), offset.y(), offset.z());
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACERS.isEmpty() || Minecraft.getInstance().isPaused()) {
            return;
        }
        TRACERS.removeIf(tracer -> ++tracer.age >= tracer.style.life());
    }

    static void clear() {
        TRACERS.clear();
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
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
            Style style = tracer.style;
            double age = tracer.age + partialTick;
            float alpha = GunKinematics.tracerAlpha(age, style.life());
            if (alpha <= 0.0F) {
                continue;
            }
            Vector3f from = relative(tracer.start, eye);
            Vector3f to = relative(tracer.end, eye);
            beam(consumer, matrix, from, to, style.glowWidth(), style.red(), style.green(), style.blue(), alpha * 0.35F);
            beam(consumer, matrix, from, to, style.coreWidth(), 1.0F, 0.98F, 0.9F, alpha);
            if (tracer.flash && age < 2.0) {
                flash(consumer, matrix, camera, from, style.flash(), (float) (1.0 - age / 2.0));
            }
        }
        buffers.endBatch(TracerRenderType.TRACER);
    }

    private static Vector3f relative(Vec3 point, Vec3 eye) {
        return new Vector3f((float) (point.x - eye.x), (float) (point.y - eye.y), (float) (point.z - eye.z));
    }

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

    private static void flash(VertexConsumer consumer, Matrix4f matrix, Camera camera, Vector3f at, float size,
                              float strength) {
        float radius = size * (0.6F + 0.4F * strength);
        Vector3f left = new Vector3f(camera.getLeftVector()).mul(radius);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(radius);
        float alpha = 0.9F * strength;
        vertex(consumer, matrix, new Vector3f(at).add(left).add(up), 1.0F, 0.88F, 0.55F, alpha);
        vertex(consumer, matrix, new Vector3f(at).add(left).sub(up), 1.0F, 0.88F, 0.55F, alpha);
        vertex(consumer, matrix, new Vector3f(at).sub(left).sub(up), 1.0F, 0.88F, 0.55F, alpha);
        vertex(consumer, matrix, new Vector3f(at).sub(left).add(up), 1.0F, 0.88F, 0.55F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f at, float red, float green,
                               float blue, float alpha) {
        consumer.vertex(matrix, at.x(), at.y(), at.z()).color(red, green, blue, alpha).endVertex();
    }
}
