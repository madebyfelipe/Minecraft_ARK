package dev.madebyfelipe.iceagesurvival.compat.tfc;

import java.util.Set;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * A única classe que toca no TerraFirmaCraft. Só é carregada por {@link TfcCompat} depois de conferir que o TFC
 * está instalado — sem ele, nenhuma linha daqui roda e as classes do TFC nunca são procuradas.
 */
final class TfcBridge {
    private TfcBridge() {
    }

    static boolean isTfcGenerator(ChunkGenerator generator) {
        return generator instanceof ChunkGeneratorExtension;
    }

    /**
     * O preset Era do Gelo com o TFC: temperatura sem variação pelo mapa ({@code temperature_scale} 0) e média
     * abaixo de zero ({@code temperature_constant} negativo).
     */
    static boolean isIceAgeGenerator(ChunkGenerator generator) {
        if (!(generator instanceof ChunkGeneratorExtension tfc)) {
            return false;
        }
        Settings settings = tfc.settings();
        return settings.temperatureScale() == 0 && settings.temperatureConstant() < 0.0F;
    }

    /** A temperatura do clima do TFC naquele ponto, na escala da temperatura de bioma do vanilla. */
    static float vanillaTemperature(ServerLevel level, BlockPos pos) {
        return Climate.toVanillaTemperature(Climate.getTemperature(level, pos));
    }

    /**
     * Os biomas primitivos (decisão do Felipe): montanhas antigas, vulcânicas, badlands, cânions e terras baixas —
     * os ids internos que o {@code ChooseBiomes} do TFC sorteia.
     */
    static Set<Integer> primitiveBiomes() {
        return Set.of(TFCLayers.OLD_MOUNTAINS, TFCLayers.VOLCANIC_MOUNTAINS, TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS,
                TFCLayers.BADLANDS, TFCLayers.CANYONS, TFCLayers.LOWLANDS);
    }
}
