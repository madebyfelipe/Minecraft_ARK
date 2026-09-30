package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.menu.ChemistryBenchMenu;
import dev.madebyfelipe.iceagesurvival.menu.IncubatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IceAgeSurvival.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<IncubatorMenu>> INCUBATOR =
            MENUS.register("incubator", () -> new MenuType<>(IncubatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ChemistryBenchMenu>> CHEMISTRY_BENCH =
            MENUS.register("chemistry_bench", () -> new MenuType<>(ChemistryBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
