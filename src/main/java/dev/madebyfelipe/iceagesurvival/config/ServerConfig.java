package dev.madebyfelipe.iceagesurvival.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuração por mundo, sincronizada do servidor para os clientes.
 * Balanceamento por espécie fica nos JSONs de espécie, não aqui.
 */
public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_WILD_LEVEL = BUILDER
            .comment("Nível máximo com que uma criatura selvagem pode nascer.")
            .defineInRange("maxWildLevel", 100, 10, 1000);

    public static final ModConfigSpec.IntValue WILD_LEVEL_STEP = BUILDER
            .comment("Intervalo entre níveis selvagens possíveis (10 = 10, 20, 30...).")
            .defineInRange("wildLevelStep", 10, 1, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }
}
