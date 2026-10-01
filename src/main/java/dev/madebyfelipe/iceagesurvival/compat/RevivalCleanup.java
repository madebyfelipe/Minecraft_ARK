package dev.madebyfelipe.iceagesurvival.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/**
 * O Fossils and Archaeology: Revival entra só como fornecedor de assets (D21). Os itens dele
 * duplicariam os nossos (ovos de dinossauro, carnes, armaduras, máquinas) e confundiriam a
 * progressão, então saem de todas as abas do criativo — e as abas próprias dele, vazias, somem.
 *
 * <p>O resto do conteúdo dele é desligado por dados: minérios e a estátua moai pelo biome modifier
 * {@code iceagesurvival:remove_revival_features}, e as estruturas por {@code data/fossil/worldgen/structure_set/}
 * vazios (o nosso pacote carrega depois do dele, {@code ordering="AFTER"} no mods.toml).
 */
public final class RevivalCleanup {
    public static final String REVIVAL_NAMESPACE = "fossil";

    private RevivalCleanup() {
    }

    public static void hideRevivalItems(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> revival = new ArrayList<>();
        for (Map.Entry<ItemStack, ?> entry : event.getEntries()) {
            if (isRevival(entry.getKey())) {
                revival.add(entry.getKey());
            }
        }
        revival.forEach(event.getEntries()::remove);
    }

    public static boolean isRevival(ItemStack stack) {
        return REVIVAL_NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }
}
