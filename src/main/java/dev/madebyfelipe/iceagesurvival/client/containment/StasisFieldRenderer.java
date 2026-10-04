package dev.madebyfelipe.iceagesurvival.client.containment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.outpost.ContainmentCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * O cilindro de êxtase em volta do espécime: um tubo de energia ciano, translúcido e aditivo
 * ({@link RenderType#lightning()}, só posição e cor), com faixas de varredura subindo, nervuras verticais, anéis
 * brilhantes na base e no topo, o perímetro marcado no chão e um feixe saindo de cada emissor até o topo do campo.
 * Durante o colapso ({@link ContainmentCoreBlockEntity#collapse}) tudo pisca cada vez mais, encolhe e apaga.
 */
public class StasisFieldRenderer implements BlockEntityRenderer<ContainmentCoreBlockEntity> {
    private static final int SEGMENTS = 48;
    private static final int RINGS = 16;
    private static final int RIB_EVERY = 4;
    private static final float CYAN_R = 0x38 / 255.0F;
    private static final float CYAN_G = 0xC6 / 255.0F;
    private static final float CYAN_B = 0xD9 / 255.0F;
    private static final float BRIGHT_R = 0xA8 / 255.0F;
    private static final float BRIGHT_G = 0xF4 / 255.0F;
    private static final float BRIGHT_B = 1.0F;
    private static final float BEAM_WIDTH = 0.12F;

    public StasisFieldRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ContainmentCoreBlockEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(ContainmentCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!core.fieldVisible() || core.getLevel() == null) {
            return;
        }
        float time = (core.getLevel().getGameTime() % 24000L) + partialTick;
        float collapse = core.collapse(partialTick);
        // No colapso: pisca cada vez mais (apagões aleatórios por tick) e encolhe.
        float strobe = 1.0F;
        if (collapse > 0.0F) {
            long tick = core.getLevel().getGameTime();
            boolean off = Mth.sin(tick * 12.9898F + tick * tick * 0.013F) * 0.5F + 0.5F < collapse * 0.8F;
            strobe = off ? 0.15F : 1.0F - collapse * 0.5F;
        }
        float radius = (float) ContainmentCoreBlockEntity.FIELD_RADIUS * (1.0F - collapse * 0.35F);
        float height = (float) ContainmentCoreBlockEntity.FIELD_HEIGHT * (1.0F - collapse * 0.6F);
        float pulse = (0.75F + 0.25F * Mth.sin(time * 0.08F)) * strobe;

        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        float cx = 0.5F;
        float cz = 0.5F;

        for (int ring = 0; ring < RINGS; ring++) {
            float y0 = height * ring / RINGS;
            float y1 = height * (ring + 1) / RINGS;
            float a0 = wallAlpha(y0 / height, time) * pulse;
            float a1 = wallAlpha(y1 / height, time) * pulse;
            for (int seg = 0; seg < SEGMENTS; seg++) {
                float t0 = seg * Mth.TWO_PI / SEGMENTS;
                float t1 = (seg + 1) * Mth.TWO_PI / SEGMENTS;
                float x0 = cx + Mth.cos(t0) * radius;
                float z0 = cz + Mth.sin(t0) * radius;
                float x1 = cx + Mth.cos(t1) * radius;
                float z1 = cz + Mth.sin(t1) * radius;
                float rib = seg % RIB_EVERY == 0 ? 0.18F * pulse : 0.0F;
                // As duas faces: de fora e de dentro (o tipo de render não descarta face, mas a ordem importa pouco
                // no aditivo; desenhar as duas garante o brilho dos dois lados).
                quad(buffer, matrix, x0, y0, z0, x1, y1, z1, a0 + rib, a1 + rib, false);
                quad(buffer, matrix, x0, y0, z0, x1, y1, z1, a0 + rib, a1 + rib, true);
            }
        }
        ring(buffer, matrix, cx, 0.02F, cz, radius - 0.15F, radius + 0.15F, 0.55F * pulse);
        ring(buffer, matrix, cx, 0.03F, cz, radius * 0.45F, radius * 0.5F, 0.25F * pulse);
        band(buffer, matrix, cx, 0.0F, cz, radius, 0.35F, 0.6F * pulse);
        band(buffer, matrix, cx, height - 0.35F, cz, radius, 0.35F, 0.6F * pulse);

        float beamAlpha = (0.45F + 0.25F * Mth.sin(time * 0.3F)) * strobe * (1.0F - collapse);
        if (beamAlpha > 0.01F) {
            BlockPos origin = core.getBlockPos();
            for (BlockPos emitter : core.emitters()) {
                Vec3 from = new Vec3(emitter.getX() - origin.getX() + 0.5, emitter.getY() - origin.getY() + 1.0,
                        emitter.getZ() - origin.getZ() + 0.5);
                Vec3 flat = new Vec3(from.x - cx, 0.0, from.z - cz);
                if (flat.lengthSqr() < 1.0E-4) {
                    continue;
                }
                Vec3 dir = flat.normalize();
                Vec3 to = new Vec3(cx + dir.x * radius, height, cz + dir.z * radius);
                beam(buffer, matrix, from, to, beamAlpha);
            }
        }
    }

    /** Alfa da parede numa altura (0 a 1): base fraca, faixas claras subindo e o topo esmaecendo. */
    private static float wallAlpha(float h, float time) {
        float bands = Mth.sin((h * 9.0F - time * 0.035F) * Mth.TWO_PI);
        float scan = Math.max(0.0F, bands) * Math.max(0.0F, bands) * 0.22F;
        float fadeTop = 1.0F - h * 0.55F;
        return (0.10F + scan) * fadeTop;
    }

    /** Um pedaço da parede, de (x0, z0) a (x1, z1), de y0 a y1; {@code inner} inverte a ordem dos vértices. */
    private static void quad(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1,
                             float z1, float a0, float a1, boolean inner) {
        if (inner) {
            vertex(buffer, matrix, x1, y0, z1, a0);
            vertex(buffer, matrix, x0, y0, z0, a0);
            vertex(buffer, matrix, x0, y1, z0, a1);
            vertex(buffer, matrix, x1, y1, z1, a1);
        } else {
            vertex(buffer, matrix, x0, y0, z0, a0);
            vertex(buffer, matrix, x1, y0, z1, a0);
            vertex(buffer, matrix, x1, y1, z1, a1);
            vertex(buffer, matrix, x0, y1, z0, a1);
        }
    }

    /** Anel achatado no chão (perímetro do campo), de {@code inner} a {@code outer}, visto de cima e de baixo. */
    private static void ring(VertexConsumer buffer, Matrix4f matrix, float cx, float y, float cz, float inner,
                             float outer, float alpha) {
        for (int seg = 0; seg < SEGMENTS; seg++) {
            float t0 = seg * Mth.TWO_PI / SEGMENTS;
            float t1 = (seg + 1) * Mth.TWO_PI / SEGMENTS;
            float ix0 = cx + Mth.cos(t0) * inner;
            float iz0 = cz + Mth.sin(t0) * inner;
            float ix1 = cx + Mth.cos(t1) * inner;
            float iz1 = cz + Mth.sin(t1) * inner;
            float ox0 = cx + Mth.cos(t0) * outer;
            float oz0 = cz + Mth.sin(t0) * outer;
            float ox1 = cx + Mth.cos(t1) * outer;
            float oz1 = cz + Mth.sin(t1) * outer;
            vertex(buffer, matrix, ix0, y, iz0, alpha);
            vertex(buffer, matrix, ix1, y, iz1, alpha);
            vertex(buffer, matrix, ox1, y, oz1, alpha);
            vertex(buffer, matrix, ox0, y, oz0, alpha);
            vertex(buffer, matrix, ox0, y, oz0, alpha);
            vertex(buffer, matrix, ox1, y, oz1, alpha);
            vertex(buffer, matrix, ix1, y, iz1, alpha);
            vertex(buffer, matrix, ix0, y, iz0, alpha);
        }
    }

    /** Faixa brilhante em volta do cilindro (os anéis dos emissores na base e no topo). */
    private static void band(VertexConsumer buffer, Matrix4f matrix, float cx, float y, float cz, float radius,
                             float thickness, float alpha) {
        float r = radius + 0.02F;
        for (int seg = 0; seg < SEGMENTS; seg++) {
            float t0 = seg * Mth.TWO_PI / SEGMENTS;
            float t1 = (seg + 1) * Mth.TWO_PI / SEGMENTS;
            float x0 = cx + Mth.cos(t0) * r;
            float z0 = cz + Mth.sin(t0) * r;
            float x1 = cx + Mth.cos(t1) * r;
            float z1 = cz + Mth.sin(t1) * r;
            brightQuad(buffer, matrix, x0, y, z0, x1, y + thickness, z1, alpha, false);
            brightQuad(buffer, matrix, x0, y, z0, x1, y + thickness, z1, alpha, true);
        }
    }

    private static void brightQuad(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float z0, float x1,
                                   float y1, float z1, float alpha, boolean inner) {
        if (inner) {
            bright(buffer, matrix, x1, y0, z1, alpha);
            bright(buffer, matrix, x0, y0, z0, alpha);
            bright(buffer, matrix, x0, y1, z0, alpha);
            bright(buffer, matrix, x1, y1, z1, alpha);
        } else {
            bright(buffer, matrix, x0, y0, z0, alpha);
            bright(buffer, matrix, x1, y0, z1, alpha);
            bright(buffer, matrix, x1, y1, z1, alpha);
            bright(buffer, matrix, x0, y1, z0, alpha);
        }
    }

    /** Feixe do emissor ao campo: duas faixas cruzadas, nas duas faces. */
    private static void beam(VertexConsumer buffer, Matrix4f matrix, Vec3 from, Vec3 to, float alpha) {
        Vec3 along = to.subtract(from);
        Vec3 side = along.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize().scale(BEAM_WIDTH);
        Vec3 up = side.cross(along).normalize().scale(BEAM_WIDTH);
        for (Vec3 offset : new Vec3[] {side, up}) {
            Vec3 a = from.subtract(offset);
            Vec3 b = from.add(offset);
            Vec3 c = to.add(offset);
            Vec3 d = to.subtract(offset);
            bright(buffer, matrix, (float) a.x, (float) a.y, (float) a.z, alpha);
            bright(buffer, matrix, (float) b.x, (float) b.y, (float) b.z, alpha);
            bright(buffer, matrix, (float) c.x, (float) c.y, (float) c.z, alpha * 0.6F);
            bright(buffer, matrix, (float) d.x, (float) d.y, (float) d.z, alpha * 0.6F);
            bright(buffer, matrix, (float) d.x, (float) d.y, (float) d.z, alpha * 0.6F);
            bright(buffer, matrix, (float) c.x, (float) c.y, (float) c.z, alpha * 0.6F);
            bright(buffer, matrix, (float) b.x, (float) b.y, (float) b.z, alpha);
            bright(buffer, matrix, (float) a.x, (float) a.y, (float) a.z, alpha);
        }
    }

    /** Vértice ciano (POSITION_COLOR: só posição e cor). */
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float alpha) {
        buffer.vertex(matrix, x, y, z).color(CYAN_R, CYAN_G, CYAN_B, Mth.clamp(alpha, 0.0F, 1.0F)).endVertex();
    }

    /** Vértice no ciano claro dos anéis e feixes. */
    private static void bright(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float alpha) {
        buffer.vertex(matrix, x, y, z).color(BRIGHT_R, BRIGHT_G, BRIGHT_B, Mth.clamp(alpha, 0.0F, 1.0F)).endVertex();
    }
}
