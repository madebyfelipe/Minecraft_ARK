package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.madebyfelipe.iceagesurvival.primal.KilnBlock;
import dev.madebyfelipe.iceagesurvival.primal.PrimalStationBlockEntity;
import dev.madebyfelipe.iceagesurvival.primal.PrimalStations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registros só do cliente desta frente de trabalho; chamado por {@link IceAgeSurvivalClient#init}. */
public final class PrimalStationsClient {
    private PrimalStationsClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(PrimalStationsClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Grelha e varal: grade 2 × 2 em cima. Forno: no chão, atrás da boca. Tora e bigorna: um no meio.
        event.registerBlockEntityRenderer(PrimalStations.GRILL_ENTITY.get(),
                context -> new StationItemRenderer<>(3.2 / 16.0, 0.375F, true));
        event.registerBlockEntityRenderer(PrimalStations.DRYING_RACK_ENTITY.get(),
                context -> new StationItemRenderer<>(15.2 / 16.0, 0.375F, true));
        event.registerBlockEntityRenderer(PrimalStations.KILN_ENTITY.get(),
                context -> new StationItemRenderer<>(1.5 / 16.0, 0.5F, false));
        event.registerBlockEntityRenderer(PrimalStations.CUTTING_LOG_ENTITY.get(),
                context -> new StationItemRenderer<>(3.2 / 16.0, 0.5F, false));
        event.registerBlockEntityRenderer(PrimalStations.STONE_ANVIL_ENTITY.get(),
                context -> new StationItemRenderer<>(12.2 / 16.0, 0.5F, false));
    }

    /** Desenha os itens de uma estação deitados em cima dela. */
    private static final class StationItemRenderer<T extends PrimalStationBlockEntity> implements BlockEntityRenderer<T> {
        private final double height;
        private final float scale;
        private final boolean grid;

        StationItemRenderer(double height, float scale, boolean grid) {
            this.height = height;
            this.scale = scale;
            this.grid = grid;
        }

        @Override
        public void render(T station, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                int overlay) {
            float facing = station.getBlockState().hasProperty(KilnBlock.FACING)
                    ? -station.getBlockState().getValue(KilnBlock.FACING).toYRot()
                    : 0.0F;
            for (int slot = 0; slot < station.size(); slot++) {
                ItemStack stack = station.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                pose.pushPose();
                double x = grid ? (slot % 2 == 0 ? 0.25 : 0.75) : 0.5;
                double z = grid ? (slot < 2 ? 0.25 : 0.75) : 0.5;
                var renderer = Minecraft.getInstance().getItemRenderer();
                // Bloco (tora, pedra) fica de pé, apoiado; item chato deita.
                boolean block = renderer.getModel(stack, station.getLevel(), null, 0).isGui3d();
                pose.translate(x, height + (block ? scale * 0.25 : 0.0), z);
                pose.mulPose(Axis.YP.rotationDegrees(facing + slot * 90.0F));
                if (!block) {
                    pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                }
                pose.scale(scale, scale, scale);
                renderer.renderStatic(stack, ItemDisplayContext.FIXED, light,
                        OverlayTexture.NO_OVERLAY, pose, buffers, station.getLevel(),
                        (int) station.getBlockPos().asLong() + slot);
                pose.popPose();
            }
        }
    }
}
