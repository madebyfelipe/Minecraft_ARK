package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mod(value = IceAgeSurvival.MODID, dist = Dist.CLIENT)
public class IceAgeSurvivalClient {
    /** Cor da ponta da flecha tranquilizante, aplicada sobre a textura de flecha com ponta do vanilla. */
    private static final int TRANQ_ARROW_TIP_COLOR = 0xFF7A3FA0;
    /** O modelo do Smilodon foi feito em escala menor que a caixa de colisão. */
    private static final float SMILODON_SCALE = 1.25F;

    public IceAgeSurvivalClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(IceAgeSurvivalClient::registerRenderers);
        modEventBus.addListener(IceAgeSurvivalClient::registerItemColors);
        modEventBus.addListener(IceAgeSurvivalClient::registerGuiLayers);
        modEventBus.addListener(CommandInput::registerKeys);
        NeoForge.EVENT_BUS.addListener(CommandInput::onClientTick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TEST_CREATURE.get(), TestCreatureRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANQ_ARROW.get(), TranqArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.SMILODON.get(), context -> new GeoEntityRenderer<>(
                context, new SmilodonModel()).withScale(SMILODON_SCALE));
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? TRANQ_ARROW_TIP_COLOR : -1, ModItems.TRANQ_ARROW);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(IceAgeSurvival.id("creature_hud"), CreatureHud::render);
    }
}
