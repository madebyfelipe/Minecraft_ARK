package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.item.CreatureEggItem;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.client.gui.screens.MenuScreens;
import dev.madebyfelipe.iceagesurvival.network.ColdStatusPayload;
import dev.madebyfelipe.iceagesurvival.network.CreatureLocationsPayload;
import dev.madebyfelipe.iceagesurvival.network.CreatureStatusPayload;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class IceAgeSurvivalClient {
    /** Cor da ponta da flecha tranquilizante, aplicada sobre a textura de flecha com ponta do vanilla. */
    private static final int TRANQ_ARROW_TIP_COLOR = 0xFF7A3FA0;

    public static void init(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(IceAgeSurvivalClient::registerRenderers);
        modEventBus.addListener(IceAgeSurvivalClient::registerItemColors);
        modEventBus.addListener(IceAgeSurvivalClient::registerGuiOverlays);
        modEventBus.addListener(IceAgeSurvivalClient::registerScreens);
        modEventBus.addListener(IceAgeSurvivalClient::registerReloadListeners);
        modEventBus.addListener(CommandInput::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(CommandInput::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CommandInput::onAttackClick);
        MinecraftForge.EVENT_BUS.addListener(FrozenHearts::onGuiOverlay);
        CreatureStatusPayload.setClientHandler(CreatureStatusScreen::receive);
        CreatureLocationsPayload.setClientHandler(CreatureTracker::receive);
        MinecraftForge.EVENT_BUS.addListener(CreatureTracker::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(Thermometer::onLoggingOut);
        ColdStatusPayload.setClientHandler(Thermometer::receive);
        dev.madebyfelipe.iceagesurvival.network.DinoFilePayload.setClientHandler(
                dev.madebyfelipe.iceagesurvival.client.dex.DinoFileClient::receive);
        dev.madebyfelipe.iceagesurvival.network.ScanResultPayload.setClientHandler(
                dev.madebyfelipe.iceagesurvival.client.dex.DinoFileClient::receiveScan);
        dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload.setClientHandler(
                dev.madebyfelipe.iceagesurvival.client.dex.DinoFileClient::receiveTerminal);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.client.dex.DinoFileClient::onLoggingOut);
        dev.madebyfelipe.iceagesurvival.item.AnalyzerItem.setTerminalOpener(
                dev.madebyfelipe.iceagesurvival.client.dex.AnalyzerScreen::open);
        dev.madebyfelipe.iceagesurvival.item.AnalyzerItem.setScanningCheck(
                dev.madebyfelipe.iceagesurvival.client.item.AnalyzerRenderer::isScanning);
        PrimalStationsClient.init(modEventBus);
        DefensesClient.init(modEventBus);
        WeaponsClient.init(modEventBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TEST_CREATURE.get(), TestCreatureRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANQ_ARROW.get(), TranqArrowRenderer::new);
        for (var creature : ModEntities.LAND_CREATURES) {
            // Espécies com modelo Tabula do Jurassic Reborn usam o renderer de poses; as demais, o GeckoLib.
            if (dev.madebyfelipe.iceagesurvival.client.tabula.JurassicRebornAppearance.forEntity(creature.getId())
                    .isPresent()) {
                event.registerEntityRenderer(creature.get(), context ->
                        new dev.madebyfelipe.iceagesurvival.client.tabula.TabulaCreatureRenderer(context,
                                creature.getId()));
            } else {
                event.registerEntityRenderer(creature.get(), context -> new CreatureRenderer(context, creature.getId()));
            }
        }
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? TRANQ_ARROW_TIP_COLOR : -1, ModItems.TRANQ_ARROW.get());
        // O ovo tem as cores do ovo gerador da espécie: casca na camada 0, pintas na 1.
        event.register((stack, tintIndex) -> CreatureEggItem.species(stack)
                .map(SpawnEggItem::byId)
                .map(egg -> 0xFF000000 | egg.getColor(tintIndex) & 0x00FFFFFF)
                .orElse(-1), ModItems.CREATURE_EGG.get());
    }

    private static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.INCUBATOR.get(), IncubatorScreen::new);
            MenuScreens.register(ModMenus.CHEMISTRY_BENCH.get(), ChemistryBenchScreen::new);
            MenuScreens.register(ModMenus.PREP_STATION.get(), ChemistryBenchScreen::new);
            MenuScreens.register(ModMenus.REVIVE_TABLE.get(), ChemistryBenchScreen::new);
            MenuScreens.register(ModMenus.CREATURE_STORAGE.get(), CreatureStorageScreen::new);
        });
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CreatureModelSettings.INSTANCE);
        event.registerReloadListener(dev.madebyfelipe.iceagesurvival.client.titan.TitanovenatorAssets.INSTANCE);
        event.registerReloadListener(dev.madebyfelipe.iceagesurvival.client.tabula.TabulaModels.INSTANCE);
        event.registerReloadListener(dev.madebyfelipe.iceagesurvival.client.dex.WikiManual.INSTANCE);
    }

    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("creature_hud", CreatureHud::render);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "thermometer", Thermometer::render);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "creature_tracker", CreatureTracker::render);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "analyzer_scan",
                dev.madebyfelipe.iceagesurvival.client.dex.ScanOverlay::render);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "flight_stamina", CreatureHud::renderFlightStamina);
    }
}
