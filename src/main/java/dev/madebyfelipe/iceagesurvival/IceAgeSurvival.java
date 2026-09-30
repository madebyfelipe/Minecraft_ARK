package dev.madebyfelipe.iceagesurvival;

import com.mojang.logging.LogUtils;
import dev.madebyfelipe.iceagesurvival.command.DebugCommands;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.temperature.ColdExposure;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(IceAgeSurvival.MODID)
public class IceAgeSurvival {
    public static final String MODID = "iceagesurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IceAgeSurvival(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(Species::registerRegistry);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        modEventBus.addListener(ModItems::addToCreativeTabs);
        modEventBus.addListener(ModPayloads::register);

        NeoForge.EVENT_BUS.addListener(ColdExposure::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(WildSpawner::onServerTick);
        NeoForge.EVENT_BUS.addListener(DebugCommands::register);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
