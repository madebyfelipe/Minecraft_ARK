package dev.madebyfelipe.iceagesurvival.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/**
 * O Fossils and Archaeology: Revival, o Jurassic Reborn e o Prehistoric Fauna entram só como
 * fornecedores de assets (D21). Os itens deles duplicariam os nossos (ovos de dinossauro, carnes,
 * armaduras, máquinas) e confundiriam a progressão, então saem de todas as abas do criativo — e as
 * abas próprias deles, vazias, somem.
 *
 * <p>O resto do conteúdo deles é desligado por dados: no Revival, minérios e a estátua moai pelo biome
 * modifier {@code iceagesurvival:remove_revival_features} e as estruturas por
 * {@code data/fossil/worldgen/structure_set/} vazios; no Jurassic Reborn e no Prehistoric Fauna, pelos
 * arquivos gerados por {@code tools/gen_fauna_cleanup.py} (ver {@link ExternalFaunaCleanup}). O nosso
 * pacote carrega depois dos deles ({@code ordering="AFTER"} no mods.toml).
 */
public final class RevivalCleanup {
    public static final String REVIVAL_NAMESPACE = "fossil";

    private RevivalCleanup() {
    }

    /** Tira das abas os itens de todos os mods de {@link ExternalFaunaCleanup#HIDDEN_NAMESPACES}. */
    public static void hideRevivalItems(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> hidden = new ArrayList<>();
        for (Map.Entry<ItemStack, ?> entry : event.getEntries()) {
            if (isHidden(entry.getKey())) {
                hidden.add(entry.getKey());
            }
        }
        hidden.forEach(event.getEntries()::remove);
    }

    public static boolean isRevival(ItemStack stack) {
        return REVIVAL_NAMESPACE.equals(namespace(stack));
    }

    /** O item é de um dos mods usados só como fornecedores de assets (Revival, Jurassic Reborn, Prehistoric Fauna). */
    public static boolean isHidden(ItemStack stack) {
        return ExternalFaunaCleanup.HIDDEN_NAMESPACES.contains(namespace(stack));
    }

    private static String namespace(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }
}
