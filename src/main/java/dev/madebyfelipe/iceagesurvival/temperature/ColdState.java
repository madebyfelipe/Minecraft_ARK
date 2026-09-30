package dev.madebyfelipe.iceagesurvival.temperature;

import com.mojang.serialization.Codec;

/**
 * Frio acumulado por um jogador, anexado a ele por {@link dev.madebyfelipe.iceagesurvival.registry.ModAttachments}.
 * Só existe no servidor; o cliente vê o resultado pelo congelamento do vanilla.
 */
public final class ColdState {
    public static final Codec<ColdState> CODEC = Codec.DOUBLE.xmap(ColdState::new, ColdState::exposure);

    /** De 0 (aquecido) a 1 (congelado). */
    private double exposure;
    /** Frio líquido do ambiente, relido de segundo em segundo; não vale salvar. */
    private double severity;

    public ColdState() {
    }

    private ColdState(double exposure) {
        this.exposure = Math.clamp(exposure, 0.0, 1.0);
    }

    public double exposure() {
        return exposure;
    }

    public void setExposure(double value) {
        exposure = Math.clamp(value, 0.0, 1.0);
    }

    public boolean isFrozen() {
        return exposure >= 1.0;
    }

    public double severity() {
        return severity;
    }

    public void setSeverity(double value) {
        severity = value;
    }
}
