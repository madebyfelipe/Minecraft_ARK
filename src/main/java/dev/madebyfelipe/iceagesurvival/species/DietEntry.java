package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

/**
 * Uma linha da tabela de dieta do predador ({@code behavior.diet}): o quanto ele prefere aquela presa.
 * A presa mais fácil nem sempre é a mais vantajosa — o Alossauro não fica caçando dodô.
 *
 * @param prey       espécie, lista de espécies ou tag ({@code "#namespace:tag"})
 * @param preference 3 = favorita, 2 = boa, 1 = aceitável, 0 = último recurso (só sem nada melhor perto)
 */
public record DietEntry(HolderSet<EntityType<?>> prey, int preference) {
    /** Presa da tag {@code prey} que não aparece na tabela. */
    public static final int DEFAULT_PREFERENCE = 1;

    public static final Codec<DietEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("prey").forGetter(DietEntry::prey),
            Codec.intRange(0, 3).fieldOf("preference").forGetter(DietEntry::preference)
    ).apply(instance, DietEntry::new));
}
