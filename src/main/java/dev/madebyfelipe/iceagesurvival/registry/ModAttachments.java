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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
        return findColdState(player).orElseThrow(
                () -> new IllegalStateException("Player cold-state capability is not attached"));
    }

    /**
     * Vazio quando o jogador já foi removido: ao renascer, o Forge invalida as capabilities do
     * corpo antigo, que ainda recebe um último tick.
     */
    public static Optional<ColdState> findColdState(Player player) {
        return player.getCapability(COLD).resolve();
    }

    /**
     * O Forge recria o jogador ao renascer e ao voltar do End. Morrer zera o frio; atravessar o
     * portal não.
     */
    private static void copyOnClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        original.reviveCaps();
        findColdState(original).ifPresent(old -> findColdState(event.getEntity()).ifPresent(fresh -> {
            fresh.setExposure(old.exposure());
            fresh.setSeverity(old.severity());
        }));
        original.invalidateCaps();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ColdState.class);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // Sem listener de invalidação: o LazyOptional invalidado não volta com reviveCaps(), e o
            // Clone precisa ler o jogador antigo. Quem barra o acesso ao corpo removido é o provider.
            event.addCapability(IceAgeSurvival.id("cold"), new ColdStateProvider());
        }
    }

    public static final class Registration {
        private Registration() {
        }

        public void register(IEventBus modEventBus) {
            modEventBus.addListener(ModAttachments::registerCapabilities);
            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModAttachments::attachCapabilities);
            MinecraftForge.EVENT_BUS.addListener(ModAttachments::copyOnClone);
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
    }
}
