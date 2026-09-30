package dev.madebyfelipe.iceagesurvival;

import com.mojang.logging.LogUtils;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(IceAgeSurvival.MODID)
public class IceAgeSurvival {
    public static final String MODID = "iceagesurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IceAgeSurvival(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(Species::registerRegistry);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModItems::addToCreativeTabs);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
