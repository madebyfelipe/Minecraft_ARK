package dev.madebyfelipe.iceagesurvival.registry;

import com.mojang.serialization.Codec;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Valores por item vindos de datapack ({@code data/<ns>/data_maps/item/<nome>.json}). */
public final class ModDataMaps {
    /**
     * Isolamento contra o frio de uma peça de roupa vestida, na escala do frio: 1 cobre o frio
     * máximo. Couro 0,2 por peça; pele 0,35. Outros mods podem somar as roupas deles aqui.
     */
    public static final DataMapType<Item, Float> INSULATION = DataMapType
            .builder(IceAgeSurvival.id("insulation"), Registries.ITEM, Codec.floatRange(0.0F, 2.0F))
            .synced(Codec.floatRange(0.0F, 2.0F), false)
            .build();

    private ModDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(INSULATION);
    }
}
