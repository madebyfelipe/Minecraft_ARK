package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.menu.ChemistryBenchMenu;
import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import dev.madebyfelipe.iceagesurvival.menu.IncubatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IceAgeSurvival.MODID);

    public static final RegistryObject<MenuType<IncubatorMenu>> INCUBATOR =
            MENUS.register("incubator", () -> new MenuType<>(IncubatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<ChemistryBenchMenu>> CHEMISTRY_BENCH =
            MENUS.register("chemistry_bench", () -> new MenuType<>(ChemistryBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<dev.madebyfelipe.iceagesurvival.menu.PrepStationMenu>> PREP_STATION =
            MENUS.register("prep_station", () -> new MenuType<>(
                    dev.madebyfelipe.iceagesurvival.menu.PrepStationMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<dev.madebyfelipe.iceagesurvival.menu.ReviveTableMenu>> REVIVE_TABLE =
            MENUS.register("revive_table", () -> new MenuType<>(
                    dev.madebyfelipe.iceagesurvival.menu.ReviveTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<CreatureStorageMenu>> CREATURE_STORAGE =
            MENUS.register("creature_storage", () -> IForgeMenuType.create(CreatureStorageMenu::new));

    private ModMenus() {
    }
}
