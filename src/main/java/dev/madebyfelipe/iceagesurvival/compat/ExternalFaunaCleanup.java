package dev.madebyfelipe.iceagesurvival.compat;

import com.mojang.serialization.Codec;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.Set;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Desliga o conteúdo próprio dos mods de fauna usados só como fornecedores de assets (D21): o
 * Fossils and Archaeology: Revival, o Jurassic Reborn e o Prehistoric Fauna. Os modelos, texturas,
 * animações e sons deles continuam carregando em runtime; o que eles geram no mundo, não (D22).
 *
 * <p>Quase tudo é feito por dados, gerados por {@code tools/gen_fauna_cleanup.py} a partir dos jars:
 * <ul>
 *   <li>biome modifiers deles (fósseis, âmbar, plantas, spawns) sobrescritos por {@code forge:none};</li>
 *   <li>{@code structure_set} deles sem estruturas;</li>
 *   <li>global loot modifiers deles (insetos de plantações, action figures) sobrescritos por
 *       {@link DisabledLootModifier} ({@code iceagesurvival:none}, registrado aqui);</li>
 *   <li>profissões de aldeão e o world preset do Prehistoric Fauna tirados das tags vanilla com
 *       {@code "remove"} do Forge.</li>
 * </ul>
 * O nosso pacote de dados carrega depois dos deles ({@code ordering="AFTER"} no mods.toml), por isso
 * os arquivos no mesmo caminho vencem. Os itens saem do criativo em {@link RevivalCleanup}.
 */
public final class ExternalFaunaCleanup {
    /** Namespaces cujos itens saem das abas do criativo. */
    public static final Set<String> HIDDEN_NAMESPACES = Set.of(
            RevivalCleanup.REVIVAL_NAMESPACE, "jurassicreborn", "prehistoricfauna");

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, IceAgeSurvival.MODID);

    public static final RegistryObject<Codec<DisabledLootModifier>> NONE_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("none", () -> DisabledLootModifier.CODEC);

    private ExternalFaunaCleanup() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
