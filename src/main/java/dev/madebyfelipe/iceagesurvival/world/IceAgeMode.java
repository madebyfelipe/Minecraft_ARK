package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * O modo Era do Gelo, como uma "DLC" do mod: as criaturas existem em qualquer mundo, mas o frio (e o
 * termômetro) só vale num mundo criado com o preset Era do Gelo. O mundo é reconhecido pela fonte de
 * biomas do Overworld, que só o preset usa ({@link RemappedBiomeSource}) — nada a gravar no save, e
 * mundos Era do Gelo antigos continuam sendo Era do Gelo.
 *
 * <p>{@code iceAgeMode} na config do mundo força: {@code on} liga o frio em qualquer mundo,
 * {@code off} desliga até no preset.
 */
public final class IceAgeMode {
    public enum Setting { AUTO, ON, OFF }

    @Nullable
    private static MinecraftServer cachedServer;
    private static boolean cachedPreset;

    private IceAgeMode() {
    }

    /** O frio e o resto do modo Era do Gelo valem neste servidor. */
    public static boolean isActive(MinecraftServer server) {
        return switch (ServerConfig.ICE_AGE_MODE.get()) {
            case ON -> true;
            case OFF -> false;
            case AUTO -> isIceAgePreset(server);
        };
    }

    /** O Overworld foi gerado pelo preset Era do Gelo. */
    public static boolean isIceAgePreset(MinecraftServer server) {
        if (server != cachedServer) {
            ServerLevel overworld = server.overworld();
            cachedPreset = overworld != null && isIceAgeSource(overworld.getChunkSource().getGenerator().getBiomeSource());
            cachedServer = server;
        }
        return cachedPreset;
    }

    public static boolean isIceAgeSource(BiomeSource source) {
        return source instanceof RemappedBiomeSource;
    }
}
