package dev.madebyfelipe.iceagesurvival.temperature;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.temperature.Coldness;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Acumula frio no jogador e o transforma em congelamento.
 *
 * <p>O feedback é todo do vanilla: {@code ticksFrozen} é sincronizado sozinho, desenha a vinheta de
 * gelo, freia o jogador e é salvo com ele — por isso não há HUD, payload nem menu novo aqui.
 */
public final class ColdExposure {
    /** A leitura do ambiente varre blocos; uma vez por segundo é resolução de sobra. */
    private static final int READING_INTERVAL_TICKS = 20;
    private static final int DAMAGE_INTERVAL_TICKS = 40;

    private static final ColdSource SOURCE = new EnvironmentColdSource();

    private ColdExposure() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ServerConfig.COLD_ENABLED.get()) {
            return;
        }
        ColdState state = player.getData(ModAttachments.COLD);
        if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
            state.setExposure(0.0);
            return;
        }
        // Dentro de powder snow o congelamento é do vanilla; não disputamos o mesmo contador.
        if (player.isInPowderSnow) {
            return;
        }

        if (player.tickCount % READING_INTERVAL_TICKS == 0) {
            state.setSeverity(SOURCE.severity(player));
        }
        state.setExposure(state.exposure()
                + Coldness.exposurePerTick(state.severity(), ServerConfig.COLD_SECONDS_TO_FREEZE.get()));
        player.setTicksFrozen(Coldness.frozenTicks(state.exposure(), player.getTicksRequiredToFreeze()));

        if (state.isFrozen() && player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(player.damageSources().freeze(), ServerConfig.COLD_DAMAGE.get().floatValue());
        }
    }
}
