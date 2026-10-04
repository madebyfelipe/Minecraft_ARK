package dev.madebyfelipe.iceagesurvival.species;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Elemento, lista de elementos ou tag ({@code "#namespace:tag"}), no mesmo formato JSON do {@code HolderSet}, mas
 * guardado só pelo nome e resolvido na hora de comparar.
 *
 * <p>As espécies são um registro de datapack sincronizado com o cliente no login, antes de as tags chegarem. Um
 * {@code HolderSet} de tag não decodifica nesse momento ("Missing tag") e o cliente de outro PC cai na entrada no
 * mundo LAN ou no servidor. No singleplayer não aparece porque o servidor interno já carregou as tags.
 */
public final class LazyHolderSet<T> {
    private final ResourceKey<? extends Registry<T>> registry;
    private final Either<TagKey<T>, List<ResourceKey<T>>> refs;

    private LazyHolderSet(ResourceKey<? extends Registry<T>> registry, Either<TagKey<T>, List<ResourceKey<T>>> refs) {
        this.registry = registry;
        this.refs = refs;
    }

    public static <T> LazyHolderSet<T> of(TagKey<T> tag) {
        return new LazyHolderSet<>(tag.registry(), Either.left(tag));
    }

    @SafeVarargs
    public static <T> LazyHolderSet<T> of(ResourceKey<? extends Registry<T>> registry, ResourceKey<T>... keys) {
        return new LazyHolderSet<>(registry, Either.right(List.of(keys)));
    }

    public boolean contains(Holder<T> holder) {
        return refs.map(holder::is, keys -> keys.stream().anyMatch(holder::is));
    }

    public static <T> Codec<LazyHolderSet<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        Codec<ResourceKey<T>> key = ResourceKey.codec(registry);
        Codec<Either<TagKey<T>, ResourceKey<T>>> single = Codec.STRING.comapFlatMap(
                text -> text.startsWith("#")
                        ? parse(text.substring(1)).map(id -> Either.<TagKey<T>, ResourceKey<T>>left(TagKey.create(registry, id)))
                        : parse(text).map(id -> Either.<TagKey<T>, ResourceKey<T>>right(ResourceKey.create(registry, id))),
                either -> either.map(tag -> "#" + tag.location(), element -> element.location().toString()));
        Codec<LazyHolderSet<T>> fromSingle = single.xmap(
                either -> new LazyHolderSet<>(registry, either.mapRight(List::of)),
                set -> set.refs.mapRight(keys -> keys.get(0)));
        Codec<LazyHolderSet<T>> fromList = key.listOf().xmap(
                keys -> new LazyHolderSet<>(registry, Either.right(keys)),
                set -> set.refs.right().orElseThrow());
        return Codec.either(fromList, fromSingle).xmap(
                either -> either.map(set -> set, set -> set),
                set -> set.refs.map(tag -> Either.right(set),
                        keys -> keys.size() == 1 ? Either.right(set) : Either.left(set)));
    }

    private static DataResult<ResourceLocation> parse(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        return id == null ? DataResult.error(() -> "id inválido: " + text) : DataResult.success(id);
    }
}
