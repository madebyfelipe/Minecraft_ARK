package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.client.bench.BenchesClient;
import dev.madebyfelipe.iceagesurvival.client.defense.DefenseBlocksClient;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registros só do cliente desta frente de trabalho; chamado por {@link IceAgeSurvivalClient#init}. */
public final class DefensesClient {
    private DefensesClient() {
    }

    public static void init(IEventBus modEventBus) {
        DefenseBlocksClient.init(modEventBus);
        BenchesClient.init(modEventBus);
    }
}
