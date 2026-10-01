package dev.madebyfelipe.iceagesurvival.temperature;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.temperature.Coldness;
import dev.madebyfelipe.iceagesurvival.network.ColdStatusPayload;
import dev.madebyfelipe.iceagesurvival.world.IceAgeMode;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * Acumula frio no jogador e o transforma em congelamento.
 *
 * <p>O feedback principal é do vanilla: {@code ticksFrozen} é sincronizado sozinho, desenha a vinheta de
 * gelo, freia o jogador e é salvo com ele. O termômetro do HUD recebe, a cada segundo, a exposição e o
 * frio líquido ({@link ColdStatusPayload}), para mostrar se o corpo esfria ou esquenta.
 */
public final class ColdExposure {
    /** A leitura do ambiente varre blocos; uma vez por segundo é resolução de sobra. */
    private static final int READING_INTERVAL_TICKS = 20;
    private static final int DAMAGE_INTERVAL_TICKS = 40;

    private static final ColdSource SOURCE = new EnvironmentColdSource();

    private ColdExposure() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !ServerConfig.COLD_ENABLED.get()) {
            return;
        }
        // Corpo removido (renascimento): sem capability e sem nada a fazer.
        ColdState state = ModAttachments.findColdState(player).orElse(null);
        if (state == null) {
            return;
        }
        // Fora do modo Era do Gelo (mundo normal) não há frio: o termômetro nem aparece, porque nenhuma
        // leitura é enviada.
        if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()
                || !IceAgeMode.isActive(player.server)) {
            state.setExposure(0.0);
            state.setSeverity(0.0);
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

        if (state.severity() > 0) {
            // Tremer gasta comida.
            player.causeFoodExhaustion((float) (state.severity() * ServerConfig.COLD_SHIVER_EXHAUSTION.get()));
        }
        if (state.isFrozen() && player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(player.damageSources().freeze(), ServerConfig.COLD_DAMAGE.get().floatValue());
        }
        if (player.tickCount % READING_INTERVAL_TICKS == 0) {
            ModPayloads.sendToPlayer(player, new ColdStatusPayload(
                    (float) state.exposure(), (float) state.severity(), player.isInWaterOrRain()));
        }
    }
}
