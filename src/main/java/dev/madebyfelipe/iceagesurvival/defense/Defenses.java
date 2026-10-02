package dev.madebyfelipe.iceagesurvival.defense;

import dev.madebyfelipe.iceagesurvival.defense.bench.Benches;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Estruturas de defesa (muros altos, portões, armadilhas) e as bancadas de Construção e de Armeiro. Duas partes com
 * registros próprios: {@link DefenseBlocks} (os blocos de defesa) e {@link Benches} (as bancadas e as receitas delas).
 */
public final class Defenses {
    private Defenses() {
    }

    public static void register(IEventBus modEventBus) {
        DefenseBlocks.register(modEventBus);
        Benches.register(modEventBus);
    }
}
