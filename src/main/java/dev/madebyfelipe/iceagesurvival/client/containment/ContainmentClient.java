package dev.madebyfelipe.iceagesurvival.client.containment;

import dev.madebyfelipe.iceagesurvival.network.TerminalScreenPayload;
import dev.madebyfelipe.iceagesurvival.outpost.Outposts;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** O lado do cliente da contenção da base: o cilindro de êxtase do núcleo e a tela do console. */
public final class ContainmentClient {
    private ContainmentClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ContainmentClient::registerRenderers);
        TerminalScreenPayload.setClientHandler(TerminalScreen::receive);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Outposts.CONTAINMENT_CORE_ENTITY.get(), StasisFieldRenderer::new);
    }
}
