package dev.madebyfelipe.iceagesurvival.temperature;

import net.minecraft.server.level.ServerPlayer;

/**
 * De onde sai a medida de frio de um jogador. É a costura prevista na D5: trocar o sistema interno
 * por Cold Sweat (ou outro) é implementar isto e apontar {@link ColdExposure} para a implementação
 * nova, sem tocar em mais nada.
 */
public interface ColdSource {
    /** Frio líquido, de −1 (calor de sobra) a 1 (frio extremo). Só faz sentido no servidor. */
    double severity(ServerPlayer player);
}
