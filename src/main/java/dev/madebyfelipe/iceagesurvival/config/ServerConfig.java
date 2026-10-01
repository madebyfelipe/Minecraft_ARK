package dev.madebyfelipe.iceagesurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuração por mundo, sincronizada do servidor para os clientes.
 * Balanceamento por espécie fica nos JSONs de espécie, não aqui.
 */
public final class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_WILD_LEVEL = BUILDER
            .comment("Nível máximo com que uma criatura selvagem pode nascer.")
            .defineInRange("maxWildLevel", 100, 10, 1000);

    public static final ForgeConfigSpec.IntValue WILD_LEVEL_STEP = BUILDER
            .comment("Intervalo entre níveis selvagens possíveis (10 = 10, 20, 30...).")
            .defineInRange("wildLevelStep", 10, 1, 100);

    public static final ForgeConfigSpec.DoubleValue TRANQ_ARROW_TORPOR = BUILDER
            .comment("Torpor aplicado por uma flecha tranquilizante disparada de um arco totalmente puxado.")
            .defineInRange("tranqArrowTorpor", 25.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue NARCOTIC_TORPOR = BUILDER
            .comment("Torpor aplicado por um narcótico dado a uma criatura inconsciente.")
            .defineInRange("narcoticTorpor", 40.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue TAMING_BONUS_LEVEL_FRACTION = BUILDER
            .comment("Fração do nível que vira pontos extras numa domesticação com eficiência de 100%.")
            .defineInRange("tamingBonusLevelFraction", 0.5, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue MIN_OBEDIENCE = BUILDER
            .comment("Chance de uma criatura com afinidade zero obedecer a um comando. Com afinidade máxima, obedece sempre.")
            .defineInRange("minObedience", 0.6, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue WILD_SPAWN_ENABLED = BUILDER
            .comment("Se o mod repõe fauna selvagem em chunks já gerados. Desligar deixa a fauna só na geração do terreno.")
            .define("wildSpawnEnabled", true);

    public static final ForgeConfigSpec.IntValue WILD_SPAWN_INTERVAL_SECONDS = BUILDER
            .comment("Segundos entre duas tentativas de reposição de fauna por jogador.")
            .defineInRange("wildSpawnIntervalSeconds", 60, 5, 3600);

    public static final ForgeConfigSpec.IntValue WILD_SPAWN_MIN_DISTANCE = BUILDER
            .comment("Distância mínima do jogador para uma criatura nascer, para não aparecer à vista.")
            .defineInRange("wildSpawnMinDistance", 40, 8, 256);

    public static final ForgeConfigSpec.IntValue WILD_SPAWN_MAX_DISTANCE = BUILDER
            .comment("Distância máxima do jogador para uma criatura nascer. Acima da distância de simulação não nasce nada.")
            .defineInRange("wildSpawnMaxDistance", 96, 16, 512);

    public static final ForgeConfigSpec.IntValue WILD_SPAWN_DENSITY_RADIUS = BUILDER
            .comment("Raio em que a densidade de cada espécie é conferida contra o max_nearby do JSON da espécie.")
            .defineInRange("wildSpawnDensityRadius", 128, 16, 512);

    public static final ForgeConfigSpec.IntValue WILD_SPAWN_MAX_TOTAL = BUILDER
            .comment("Teto de criaturas selvagens do mod, somando todas as espécies, no raio de densidade de um jogador.")
            .defineInRange("wildSpawnMaxTotal", 10, 0, 200);

    public static final ForgeConfigSpec.IntValue FULL_DANGER_DISTANCE = BUILDER
            .comment("Distância do spawn do mundo em que criaturas selvagens já nascem com o nível máximo. No spawn, até 30% dele.")
            .defineInRange("fullDangerDistance", 3000, 0, 1_000_000);

    public static final ForgeConfigSpec.DoubleValue STIMULANT_TORPOR = BUILDER
            .comment("Torpor que um estimulante tira de uma criatura.")
            .defineInRange("stimulantTorpor", 150.0, 0.0, 10000.0);

    public static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE = BUILDER
            .comment("Chance de cada tentativa de mutação numa cria (o ARK usa 2,5%).")
            .defineInRange("mutationChance", 0.025, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MUTATION_ATTEMPTS = BUILDER
            .comment("Tentativas de mutação por cria.")
            .defineInRange("mutationAttempts", 3, 0, 10);

    public static final ForgeConfigSpec.DoubleValue HEALTH_GENE_CHANCE = BUILDER
            .comment("Chance de o gene de mutação de vida surgir numa cria de pais sem ele.")
            .defineInRange("healthGeneChance", 0.01, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue WILD_HEALTH_GENE_CHANCE = BUILDER
            .comment("Chance de uma criatura selvagem já carregar o gene de mutação de vida.")
            .defineInRange("wildHealthGeneChance", 0.05, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue COLD_ENABLED = BUILDER
            .comment("Se o frio afeta os jogadores. Desligar entrega a mecânica de temperatura a outro mod.")
            .define("coldEnabled", true);

    public static final ForgeConfigSpec.DoubleValue COLD_ALTITUDE_DROP_PER_BLOCK = BUILDER
            .comment("Queda de temperatura por bloco acima do nível do mar.")
            .defineInRange("coldAltitudeDropPerBlock", 0.002, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue COLD_NIGHT_DROP = BUILDER
            .comment("Queda de temperatura à noite.")
            .defineInRange("coldNightDrop", 0.2, 0.0, 2.0);

    public static final ForgeConfigSpec.DoubleValue COLD_STORM_DROP = BUILDER
            .comment("Queda de temperatura sob precipitação a céu aberto.")
            .defineInRange("coldStormDrop", 0.25, 0.0, 2.0);

    public static final ForgeConfigSpec.DoubleValue COLD_SHELTER_WARMTH = BUILDER
            .comment("Proteção contra o frio de ter um teto sobre a cabeça.")
            .defineInRange("coldShelterWarmth", 0.25, 0.0, 2.0);

    public static final ForgeConfigSpec.DoubleValue COLD_HEAT_WARMTH = BUILDER
            .comment("Proteção de estar em cima de uma fonte de calor; cai com a distância até zero no raio.")
            .defineInRange("coldHeatWarmth", 0.8, 0.0, 2.0);

    public static final ForgeConfigSpec.IntValue COLD_HEAT_RADIUS = BUILDER
            .comment("Distância em que uma fonte de calor ainda aquece. Raios maiores custam mais por jogador.")
            .defineInRange("coldHeatRadius", 4, 1, 8);

    public static final ForgeConfigSpec.DoubleValue COLD_WET_DROP = BUILDER
            .comment("Queda de temperatura para quem está na água ou tomando chuva. Couro dá 0,2 de isolamento por peça; pele do mod, 0,35.")
            .defineInRange("coldWetDrop", 0.3, 0.0, 2.0);

    public static final ForgeConfigSpec.DoubleValue COLD_SHIVER_EXHAUSTION = BUILDER
            .comment("Cansaço (fome) por tick no frio máximo: tremer gasta comida. 0,005 = um ponto de fome a cada 40 s.")
            .defineInRange("coldShiverExhaustion", 0.005, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue COLD_SECONDS_TO_FREEZE = BUILDER
            .comment("Segundos no frio extremo para um jogador sem proteção congelar. Também é o tempo de recuperação no calor.")
            .defineInRange("coldSecondsToFreeze", 120, 1, 6000);

    public static final ForgeConfigSpec.DoubleValue COLD_DAMAGE = BUILDER
            .comment("Dano a cada dois segundos enquanto o jogador está congelado.")
            .defineInRange("coldDamage", 1.0, 0.0, 100.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {
    }
}
