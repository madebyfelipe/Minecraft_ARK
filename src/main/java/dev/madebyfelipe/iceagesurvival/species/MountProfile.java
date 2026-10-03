package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Montaria de uma espécie. A ausência do bloco {@code mount} no JSON significa que a
 * espécie não pode ser montada — é assim que o lobo-terrível fica de fora.
 *
 * @param seatHeight      altura do assento em blocos a partir dos pés; 0 = 85% da altura da colisão
 * @param seatForward     quanto o assento fica à frente do centro, em blocos (à frente da vela do espinossauro)
 * @param minAffinity     afinidade mínima para deixar montar (0 a {@code MAX_AFFINITY})
 * @param speedMultiplier multiplicador da velocidade quando montada
 * @param jumpStrength     impulso vertical do pulo em blocos/tick; 0 = não pula (padrão: criaturas grandes não pulam)
 * @param jumpForward      impulso horizontal do pulo, para a frente, em blocos/tick — o bote do Smilodon
 * @param breakHardness    dureza máxima dos blocos que a mordida de quem monta quebra; 0 = não quebra
 *                         (terra 0,5; pedra 1,5; tronco e pedregulho 2)
 * @param breakBlocks      se presente, a mordida só quebra blocos desta tag — é o que faz do mamute um
 *                         coletor de madeira sem que ele cave pedra
 * @param flying           voo montado ({@link dev.madebyfelipe.iceagesurvival.core.mount.FlightModel})
 * @param flightSpeed      velocidade máxima de cruzeiro no voo, em blocos/tick (o impulso multiplica)
 * @param requiresSaddle   falso = monta sem sela, como uma montaria de início de jogo
 * @param flightTurnRate   curva máxima no voo, em graus por segundo, na velocidade de cruzeiro
 * @param swims            montada, nada: na água fica na superfície com quem monta fora d'água, anda na velocidade
 *                         de nado da espécie e o Espaço sobe e pula para a margem
 *                         ({@link dev.madebyfelipe.iceagesurvival.core.mount.SwimModel})
 * @param flight           como a espécie voa, além da velocidade e da curva ({@link FlightStyle}, bloco
 *                         {@code flight}); vale para o voo montado e para o selvagem
 * @param breakTool       se presente, o que a mordida quebra fora da madeira dropa como se minerado com esta
 *                         ferramenta (o Anquilossauro, {@code minecraft:stone_pickaxe}: pedra dá pedregulho, minério dá
 *                         o minério); sem ela, como se quebrado à mão
 */
public record MountProfile(double seatHeight, double seatForward, float minAffinity, double speedMultiplier, double jumpStrength,
                           double jumpForward, float breakHardness, Optional<TagKey<Block>> breakBlocks, boolean flying,
                           double flightSpeed, boolean requiresSaddle, double flightTurnRate, boolean swims,
                           FlightStyle flight, Optional<Item> breakTool) {
    public static final MountProfile DEFAULT =
            new MountProfile(0.0, 0.0, 25.0F, 1.0, 0.0, 0.0, 0.0F, Optional.empty(), false, 0.8, true, 120.0, false,
                    FlightStyle.DEFAULT, Optional.empty());

    public MountProfile(double seatHeight, double seatForward, float minAffinity, double speedMultiplier,
                        double jumpStrength, double jumpForward, float breakHardness, Optional<TagKey<Block>> breakBlocks,
                        boolean flying, double flightSpeed, boolean requiresSaddle, double flightTurnRate, boolean swims,
                        FlightStyle flight) {
        this(seatHeight, seatForward, minAffinity, speedMultiplier, jumpStrength, jumpForward, breakHardness,
                breakBlocks, flying, flightSpeed, requiresSaddle, flightTurnRate, swims, flight, Optional.empty());
    }

    public MountProfile(double seatHeight, double seatForward, float minAffinity, double speedMultiplier,
                        double jumpStrength, double jumpForward, float breakHardness, Optional<TagKey<Block>> breakBlocks,
                        boolean flying, double flightSpeed, boolean requiresSaddle, double flightTurnRate) {
        this(seatHeight, seatForward, minAffinity, speedMultiplier, jumpStrength, jumpForward, breakHardness,
                breakBlocks, flying, flightSpeed, requiresSaddle, flightTurnRate, false, FlightStyle.DEFAULT);
    }

    public MountProfile(double seatHeight, double seatForward, float minAffinity, double speedMultiplier,
                        double jumpStrength, double jumpForward, float breakHardness, Optional<TagKey<Block>> breakBlocks,
                        boolean flying, double flightSpeed, boolean requiresSaddle, double flightTurnRate, boolean swims) {
        this(seatHeight, seatForward, minAffinity, speedMultiplier, jumpStrength, jumpForward, breakHardness,
                breakBlocks, flying, flightSpeed, requiresSaddle, flightTurnRate, swims, FlightStyle.DEFAULT);
    }

    /**
     * Como a espécie voa ({@code mount.flight}); tudo opcional, e o padrão é o voo do Pteranodonte. O Quetzalcoatlus é
     * o planador de térmica: embala devagar, decola num salto e quase não gasta fôlego planando.
     *
     * <pre>"flight": { "acceleration_seconds": 4, "climb_cost": 3, "cruise_cost": 1, "glide_cost": 0.05,
     *            "leap_height": 2.5, "thermal_lift": 0.06, "thermal_ceiling": 48 }</pre>
     *
     * @param accelerationSeconds do zero ao cruzeiro com a frente apertada, montada
     * @param climbCost           fôlego gasto por segundo subindo batendo as asas
     *                            ({@link dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina})
     * @param cruiseCost          fôlego gasto por segundo voando nivelada, fora de térmica
     * @param glideCost           fôlego gasto por segundo planando em descida ou sustentada por uma térmica
     * @param leapHeight          decola num salto desta altura (blocos), parada e sem correr, e só com céu aberto acima
     *                            da caixa ({@link dev.madebyfelipe.iceagesurvival.core.mount.LeapTakeoff}); 0 = decola
     *                            como o Pteranodonte, em qualquer lugar
     * @param thermalLift         subida plena numa térmica (de dia, sem chuva, sobre terra), em blocos/tick, sem bater as
     *                            asas ({@link dev.madebyfelipe.iceagesurvival.core.mount.Thermals}); 0 = não usa térmicas
     * @param thermalCeiling      altura acima do chão em que a térmica se esgota
     */
    public record FlightStyle(double accelerationSeconds, double climbCost, double cruiseCost, double glideCost,
                              double leapHeight, double thermalLift, double thermalCeiling) {
        public static final FlightStyle DEFAULT = new FlightStyle(
                dev.madebyfelipe.iceagesurvival.core.mount.FlightModel.DEFAULT_ACCELERATION_TICKS / 20.0,
                dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina.CLIMB_RATE,
                dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina.CRUISE_RATE,
                dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina.GLIDE_RATE, 0.0, 0.0, 48.0);

        public static final Codec<FlightStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.05, 30).optionalFieldOf("acceleration_seconds", DEFAULT.accelerationSeconds())
                        .forGetter(FlightStyle::accelerationSeconds),
                Codec.doubleRange(0, 20).optionalFieldOf("climb_cost", DEFAULT.climbCost())
                        .forGetter(FlightStyle::climbCost),
                Codec.doubleRange(0, 20).optionalFieldOf("cruise_cost", DEFAULT.cruiseCost())
                        .forGetter(FlightStyle::cruiseCost),
                Codec.doubleRange(0, 20).optionalFieldOf("glide_cost", DEFAULT.glideCost())
                        .forGetter(FlightStyle::glideCost),
                Codec.doubleRange(0, 8).optionalFieldOf("leap_height", DEFAULT.leapHeight())
                        .forGetter(FlightStyle::leapHeight),
                Codec.doubleRange(0, 1).optionalFieldOf("thermal_lift", DEFAULT.thermalLift())
                        .forGetter(FlightStyle::thermalLift),
                Codec.doubleRange(1, 256).optionalFieldOf("thermal_ceiling", DEFAULT.thermalCeiling())
                        .forGetter(FlightStyle::thermalCeiling)
        ).apply(instance, FlightStyle::new));

        /** Os custos de fôlego desta espécie. */
        public dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina.Costs costs() {
            return new dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina.Costs(climbCost, cruiseCost, glideCost);
        }

        /** Decola num salto, e só com céu aberto. */
        public boolean leaps() {
            return leapHeight > 0.0;
        }

        /** Ticks do zero ao cruzeiro. */
        public double accelerationTicks() {
            return accelerationSeconds * 20.0;
        }
    }

    public static final Codec<MountProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 16).optionalFieldOf("seat_height", DEFAULT.seatHeight())
                    .forGetter(MountProfile::seatHeight),
            Codec.doubleRange(-16, 16).optionalFieldOf("seat_forward", DEFAULT.seatForward())
                    .forGetter(MountProfile::seatForward),
            Codec.floatRange(0, 100).optionalFieldOf("min_affinity", DEFAULT.minAffinity())
                    .forGetter(MountProfile::minAffinity),
            Codec.doubleRange(0.1, 5).optionalFieldOf("speed_multiplier", DEFAULT.speedMultiplier())
                    .forGetter(MountProfile::speedMultiplier),
            Codec.doubleRange(0, 2).optionalFieldOf("jump_strength", DEFAULT.jumpStrength())
                    .forGetter(MountProfile::jumpStrength),
            Codec.doubleRange(0, 3).optionalFieldOf("jump_forward", DEFAULT.jumpForward())
                    .forGetter(MountProfile::jumpForward),
            Codec.floatRange(0, 50).optionalFieldOf("break_hardness", DEFAULT.breakHardness())
                    .forGetter(MountProfile::breakHardness),
            TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("break_blocks")
                    .forGetter(MountProfile::breakBlocks),
            Codec.BOOL.optionalFieldOf("flying", DEFAULT.flying()).forGetter(MountProfile::flying),
            Codec.doubleRange(0.05, 4).optionalFieldOf("flight_speed", DEFAULT.flightSpeed())
                    .forGetter(MountProfile::flightSpeed),
            Codec.BOOL.optionalFieldOf("requires_saddle", DEFAULT.requiresSaddle())
                    .forGetter(MountProfile::requiresSaddle),
            Codec.doubleRange(10, 720).optionalFieldOf("flight_turn_rate", DEFAULT.flightTurnRate())
                    .forGetter(MountProfile::flightTurnRate),
            Codec.BOOL.optionalFieldOf("swims", DEFAULT.swims()).forGetter(MountProfile::swims),
            FlightStyle.CODEC.optionalFieldOf("flight", FlightStyle.DEFAULT).forGetter(MountProfile::flight),
            ForgeRegistries.ITEMS.getCodec().optionalFieldOf("break_tool").forGetter(MountProfile::breakTool)
    ).apply(instance, MountProfile::new));

    /** Altura do assento para uma criatura com esta caixa de colisão. */
    public double seatHeight(float collisionHeight) {
        return seatHeight > 0 ? seatHeight : collisionHeight * 0.85;
    }
}
