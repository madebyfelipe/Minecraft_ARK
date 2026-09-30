package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatProfile;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Definição de uma espécie, carregada de
 * {@code data/<namespace>/iceagesurvival/species/<nome>.json} e sincronizada para os clientes.
 *
 * <p>A espécie de uma entidade é a que tem o mesmo id do seu {@link EntityType}.
 *
 * @param knockbackResistance de 0 a 1; espécies maiores que o jogador usam 1 (não são arremessadas por golpes)
 */
public record Species(
        StatProfile stats,
        double knockbackResistance,
        Optional<TamingProfile> taming,
        Optional<BehaviorProfile> behavior) {
    public static final ResourceKey<Registry<Species>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(IceAgeSurvival.id("species"));

    private static final Codec<Stat> STAT_CODEC = Codec.STRING.comapFlatMap(Species::parseStat, Stat::id);

    private static final Codec<StatProfile.Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, Double.MAX_VALUE).fieldOf("base").forGetter(StatProfile.Entry::base),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("per_point", 0.0).forGetter(StatProfile.Entry::perPoint)
    ).apply(instance, StatProfile.Entry::new));

    private static final Codec<StatProfile> STATS_CODEC = Codec.unboundedMap(STAT_CODEC, ENTRY_CODEC)
            .comapFlatMap(entries -> {
                try {
                    return DataResult.success(new StatProfile(entries));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            }, StatProfile::entries);

    public static final Codec<Species> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATS_CODEC.fieldOf("stats").forGetter(Species::stats),
            Codec.doubleRange(0, 1).optionalFieldOf("knockback_resistance", 0.0).forGetter(Species::knockbackResistance),
            TamingProfile.CODEC.optionalFieldOf("taming").forGetter(Species::taming),
            BehaviorProfile.CODEC.optionalFieldOf("behavior").forGetter(Species::behavior)
    ).apply(instance, Species::new));

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY_KEY, CODEC, CODEC);
    }

    public static Optional<Species> of(RegistryAccess registries, EntityType<?> type) {
        return registries.registry(REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(BuiltInRegistries.ENTITY_TYPE.getKey(type)));
    }

    private static DataResult<Stat> parseStat(String id) {
        try {
            return DataResult.success(Stat.byId(id));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }
}
