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

    public static final ModConfigSpec.DoubleValue TRANQ_ARROW_TORPOR = BUILDER
            .comment("Torpor aplicado por uma flecha tranquilizante disparada de um arco totalmente puxado.")
            .defineInRange("tranqArrowTorpor", 25.0, 0.0, 10000.0);

    public static final ModConfigSpec.DoubleValue NARCOTIC_TORPOR = BUILDER
            .comment("Torpor aplicado por um narcótico dado a uma criatura inconsciente.")
            .defineInRange("narcoticTorpor", 40.0, 0.0, 10000.0);

    public static final ModConfigSpec.DoubleValue TAMING_BONUS_LEVEL_FRACTION = BUILDER
            .comment("Fração do nível que vira pontos extras numa domesticação com eficiência de 100%.")
            .defineInRange("tamingBonusLevelFraction", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue MIN_OBEDIENCE = BUILDER
            .comment("Chance de uma criatura com afinidade zero obedecer a um comando. Com afinidade máxima, obedece sempre.")
            .defineInRange("minObedience", 0.6, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }
}
