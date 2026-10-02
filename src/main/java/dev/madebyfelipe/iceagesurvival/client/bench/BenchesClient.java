package dev.madebyfelipe.iceagesurvival.client.bench;

import dev.madebyfelipe.iceagesurvival.defense.bench.Benches;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registros só do cliente; chamado por {@link dev.madebyfelipe.iceagesurvival.client.DefensesClient#init}. */
public final class BenchesClient {
    private BenchesClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(BenchesClient::registerScreens);
    }

    private static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Benches.CONSTRUCTION_MENU.get(), BenchScreen::new);
            MenuScreens.register(Benches.ARMORY_MENU.get(), BenchScreen::new);
        });
    }
}
