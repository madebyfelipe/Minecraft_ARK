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

    public static final ModConfigSpec.BooleanValue COLD_ENABLED = BUILDER
            .comment("Se o frio afeta os jogadores. Desligar entrega a mecânica de temperatura a outro mod.")
            .define("coldEnabled", true);

    public static final ModConfigSpec.DoubleValue COLD_ALTITUDE_DROP_PER_BLOCK = BUILDER
            .comment("Queda de temperatura por bloco acima do nível do mar.")
            .defineInRange("coldAltitudeDropPerBlock", 0.002, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue COLD_NIGHT_DROP = BUILDER
            .comment("Queda de temperatura à noite.")
            .defineInRange("coldNightDrop", 0.2, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue COLD_STORM_DROP = BUILDER
            .comment("Queda de temperatura sob precipitação a céu aberto.")
            .defineInRange("coldStormDrop", 0.25, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue COLD_SHELTER_WARMTH = BUILDER
            .comment("Proteção contra o frio de ter um teto sobre a cabeça.")
            .defineInRange("coldShelterWarmth", 0.25, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue COLD_HEAT_WARMTH = BUILDER
            .comment("Proteção de estar em cima de uma fonte de calor; cai com a distância até zero no raio.")
            .defineInRange("coldHeatWarmth", 0.8, 0.0, 2.0);

    public static final ModConfigSpec.IntValue COLD_HEAT_RADIUS = BUILDER
            .comment("Distância em que uma fonte de calor ainda aquece. Raios maiores custam mais por jogador.")
            .defineInRange("coldHeatRadius", 4, 1, 8);

    public static final ModConfigSpec.DoubleValue COLD_INSULATION_PER_ARMOR_PIECE = BUILDER
            .comment("Proteção por peça de armadura da tag iceagesurvival:insulating_armor.")
            .defineInRange("coldInsulationPerArmorPiece", 0.2, 0.0, 1.0);

    public static final ModConfigSpec.IntValue COLD_SECONDS_TO_FREEZE = BUILDER
            .comment("Segundos no frio extremo para um jogador sem proteção congelar. Também é o tempo de recuperação no calor.")
            .defineInRange("coldSecondsToFreeze", 120, 1, 6000);

    public static final ModConfigSpec.DoubleValue COLD_DAMAGE = BUILDER
            .comment("Dano a cada dois segundos enquanto o jogador está congelado.")
            .defineInRange("coldDamage", 1.0, 0.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }
}
