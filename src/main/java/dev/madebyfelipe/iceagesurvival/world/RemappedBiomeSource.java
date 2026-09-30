package dev.madebyfelipe.iceagesurvival.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Fonte de biomas que embrulha outra e troca biomas por outros (D6): o world preset
 * {@code iceagesurvival:ice_age} usa o overworld do vanilla — relevo, cavernas, estruturas —
 * com cada bioma quente trocado por um frio equivalente. Sem a lista de milhares de pontos
 * climáticos em JSON e sem mod de worldgen.
 *
 * <pre>{ "type": "iceagesurvival:remapped", "source": {...}, "replacements": { "minecraft:plains": "minecraft:snowy_plains" } }</pre>
 */
public class RemappedBiomeSource extends BiomeSource {
    public static final MapCodec<RemappedBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("source").forGetter(source -> source.source),
            Codec.unboundedMap(ResourceKey.codec(Registries.BIOME), Biome.CODEC).fieldOf("replacements")
                    .forGetter(source -> source.replacements)
    ).apply(instance, RemappedBiomeSource::new));

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, IceAgeSurvival.MODID);

    static {
        BIOME_SOURCES.register("remapped", () -> CODEC);
    }

    private final BiomeSource source;
    private final Map<ResourceKey<Biome>, Holder<Biome>> replacements;

    public RemappedBiomeSource(BiomeSource source, Map<ResourceKey<Biome>, Holder<Biome>> replacements) {
        this.source = source;
        this.replacements = Map.copyOf(replacements);
    }

    public Holder<Biome> replace(Holder<Biome> biome) {
        return biome.unwrapKey().map(replacements::get).orElse(biome);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return source.possibleBiomes().stream().map(this::replace).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return replace(source.getNoiseBiome(x, y, z, sampler));
    }
}
