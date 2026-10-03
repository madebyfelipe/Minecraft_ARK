package dev.madebyfelipe.iceagesurvival;

import dev.madebyfelipe.iceagesurvival.compat.RevivalCleanup;
import net.minecraftforge.eventbus.api.EventPriority;
import com.mojang.logging.LogUtils;
import dev.madebyfelipe.iceagesurvival.command.DebugCommands;
import dev.madebyfelipe.iceagesurvival.client.IceAgeSurvivalClient;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.registry.ModArmorMaterials;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModMenus;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.temperature.ColdExposure;
import dev.madebyfelipe.iceagesurvival.world.RemappedBiomeSource;
import dev.madebyfelipe.iceagesurvival.world.GroupSpacing;
import dev.madebyfelipe.iceagesurvival.world.VillageInteractions;
import dev.madebyfelipe.iceagesurvival.world.StarterApexKeeper;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(IceAgeSurvival.MODID)
public class IceAgeSurvival {
    public static final String MODID = "iceagesurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IceAgeSurvival() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        RemappedBiomeSource.BIOME_SOURCES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        dev.madebyfelipe.iceagesurvival.registry.ModStructures.register(modEventBus);
        dev.madebyfelipe.iceagesurvival.primal.PrimalStations.register(modEventBus);
        dev.madebyfelipe.iceagesurvival.defense.Defenses.register(modEventBus);
        dev.madebyfelipe.iceagesurvival.compat.ExternalFaunaCleanup.register(modEventBus);

        modEventBus.addListener(Species::registerRegistry);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(dev.madebyfelipe.iceagesurvival.compat.tfc.TfcCompat::addPackFinders);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        modEventBus.addListener(ModItems::addToCreativeTabs);
        modEventBus.addListener(EventPriority.LOWEST, RevivalCleanup::hideRevivalItems);
        ModPayloads.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> IceAgeSurvivalClient.init(modEventBus, modContainer));

        MinecraftForge.EVENT_BUS.addListener(ColdExposure::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.item.CaveTrackerItem::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.item.CaveTrackerItem::onLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(WildSpawner::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(StarterApexKeeper::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.compat.tfc.TfcCompat::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(GroupSpacing::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(VillageInteractions::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(GroupSpacing::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(GroupSpacing::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(StarterApexKeeper::onDeath);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.entity.SwallowStrike::onAttack);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.entity.TailClubStrike::onHurt);
        MinecraftForge.EVENT_BUS.addListener(dev.madebyfelipe.iceagesurvival.effect.BrokenLegEffect::onJump);
        MinecraftForge.EVENT_BUS.addListener(DebugCommands::register);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
