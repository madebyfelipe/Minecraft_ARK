package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Tipo de render do traçante do rifle e das armas de fogo (D58): quadrados de cor sem textura, mistura aditiva (como
 * o raio), sem gravar profundidade e com teste de profundidade (paredes escondem a linha). Estende {@link RenderType}
 * só para alcançar os estados protegidos.
 */
public final class TracerRenderType extends RenderType {
    public static final RenderType TRACER = RenderType.create("iceagesurvival_rifle_tracer", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setOutputState(PARTICLES_TARGET)
                    .createCompositeState(false));

    private TracerRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
