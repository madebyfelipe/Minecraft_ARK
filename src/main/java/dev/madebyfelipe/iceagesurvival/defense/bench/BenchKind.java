package dev.madebyfelipe.iceagesurvival.defense.bench;

import java.util.Locale;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

/** Qual bancada: o campo {@code "bench"} da receita ({@code "construction"} ou {@code "armory"}). */
public enum BenchKind {
    /** Bancada de Construção: muros, portões e armadilhas. */
    CONSTRUCTION,
    /** Bancada de Armeiro: rifle, besta e dardo tranquilizantes. */
    ARMORY;

    /** O nome no JSON da receita. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BenchKind byName(String name) {
        for (BenchKind kind : values()) {
            if (kind.serializedName().equals(name)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("bancada desconhecida: " + name + " (use construction ou armory)");
    }

    public Block block() {
        return switch (this) {
            case CONSTRUCTION -> Benches.CONSTRUCTION_BENCH.get();
            case ARMORY -> Benches.ARMORY_BENCH.get();
        };
    }

    public MenuType<BenchMenu> menu() {
        return switch (this) {
            case CONSTRUCTION -> Benches.CONSTRUCTION_MENU.get();
            case ARMORY -> Benches.ARMORY_MENU.get();
        };
    }

    /** O som que a bancada faz ao fabricar. */
    public SoundEvent craftSound() {
        return switch (this) {
            case CONSTRUCTION -> SoundEvents.AXE_STRIP;
            case ARMORY -> SoundEvents.SMITHING_TABLE_USE;
        };
    }
}
