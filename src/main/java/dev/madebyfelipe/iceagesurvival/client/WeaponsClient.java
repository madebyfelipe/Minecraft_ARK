package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.item.TranqGunItem;
import dev.madebyfelipe.iceagesurvival.network.RifleTracerPayload;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registros só do cliente desta frente de trabalho; chamado por {@link IceAgeSurvivalClient#init}. */
public final class WeaponsClient {
    private WeaponsClient() {
    }

    public static void init(IEventBus modEventBus) {
        // Armas tranquilizantes: coice no clique, traçante do rifle. A pose e a mão vêm de GunClientExtensions,
        // ligada pelo próprio item (initializeClient).
        TranqGunItem.setClientShotHook(GunClientExtensions::onLocalShot);
        RifleTracerPayload.setClientHandler(RifleTracers::receive);
        MinecraftForge.EVENT_BUS.addListener(RifleTracers::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(RifleTracers::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(WeaponsClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RifleTracers.clear();
        GunClientExtensions.clear();
    }
}
