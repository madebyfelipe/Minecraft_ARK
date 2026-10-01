package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.temperature.ColdState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

/**
 * Dados do mod anexados a entidades que não são nossas. Em Forge 1.20.1, capabilities substituem
 * os attachments do NeoForge; o estado é persistido pelo codec de {@link ColdState}.
 */
public final class ModAttachments {
    public static final Capability<ColdState> COLD = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Registration ATTACHMENT_TYPES = new Registration();

    private ModAttachments() {
    }

    public static ColdState coldState(Player player) {
        return player.getCapability(COLD).orElseThrow(
                () -> new IllegalStateException("Player cold-state capability is not attached"));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ColdState.class);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            ColdStateProvider provider = new ColdStateProvider();
            event.addCapability(IceAgeSurvival.id("cold"), provider);
            event.addListener(provider::invalidate);
        }
    }

    public static final class Registration {
        private Registration() {
        }

        public void register(IEventBus modEventBus) {
            modEventBus.addListener(ModAttachments::registerCapabilities);
            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModAttachments::attachCapabilities);
        }
    }

    private static final class ColdStateProvider implements ICapabilitySerializable<CompoundTag> {
        private final ColdState state = new ColdState();
        private final LazyOptional<ColdState> optional = LazyOptional.of(() -> state);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return capability == COLD ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) ColdState.CODEC.encodeStart(NbtOps.INSTANCE, state)
                    .getOrThrow(false, message -> IceAgeSurvival.LOGGER.error("Could not save player cold state: {}", message));
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            ColdState.CODEC.parse(NbtOps.INSTANCE, tag).result()
                    .ifPresent(loaded -> state.setExposure(loaded.exposure()));
        }

        private void invalidate() {
            optional.invalidate();
        }
    }
}
