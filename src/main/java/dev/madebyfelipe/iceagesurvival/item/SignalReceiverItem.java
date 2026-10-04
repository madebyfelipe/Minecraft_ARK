package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.client.item.SignalReceiverRenderer;
import dev.madebyfelipe.iceagesurvival.network.BaseSignalPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * O Receptor de Sinal: o rádio de campo dos postos, que ainda capta o sinal de emergência da base militar. Achado nos
 * baús dos postos (no complexo, garantido no baú do térreo). Segurado em qualquer das mãos, mostra o radar do canto com a
 * direção, a distância e a diferença de altura até o centro da base; sem base na dimensão, "sem sinal". Não se gasta.
 *
 * <p>O servidor acha a base pelo anel do espalhamento ({@code structure_set/military_base}) e lê a caixa da estrutura
 * naquele chunk, para apontar o meio do hangar e não o canto do chunk. A busca fica guardada até o jogador andar
 * {@link #RESEARCH_DISTANCE} blocos ou trocar de dimensão; o alvo vai ao cliente ({@link BaseSignalPayload}) ao pegar o
 * receptor e quando muda.
 */
public class SignalReceiverItem extends Item implements GeoItem {
    public static final TagKey<Structure> BASE = TagKey.create(Registries.STRUCTURE, IceAgeSurvival.id("military_base"));
    /** Raio de busca em chunks; o anel é achado por lista, então serve qualquer valor grande. */
    public static final int SEARCH_RADIUS_CHUNKS = 100;
    /** Andou isto (na horizontal) desde a última busca, procura de novo. */
    public static final int RESEARCH_DISTANCE = 256;
    /** De quanto em quanto tempo, com o receptor na mão, o servidor confere se o alvo mudou. */
    private static final int CHECK_TICKS = 20;

    private static final Map<UUID, Signal> SIGNALS = new HashMap<>();
    private static final RawAnimation SIGNAL = RawAnimation.begin().thenLoop("animation.signal_receiver.signal");
    private static final RawAnimation NO_SIGNAL = RawAnimation.begin().thenLoop("animation.signal_receiver.no_signal");
    /** O cliente registra aqui quem diz se há sinal da base (o item não pode tocar em classes de cliente). */
    private static BooleanSupplier signalCheck = () -> false;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public SignalReceiverItem(Properties properties) {
        super(properties);
    }

    public static void setSignalCheck(BooleanSupplier check) {
        signalCheck = check;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SignalReceiverRenderer.EXTENSIONS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "signal_receiver", 5,
                state -> state.setAndContinue(signalCheck.getAsBoolean() ? SIGNAL : NO_SIGNAL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server) {
            GeoItem.getOrAssignId(stack, server);
        }
    }

    /** O id gravado no aparelho não deve fazer a mão abaixar e subir de novo. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.signal_receiver.tooltip").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isHolding(Player player) {
        return player.getMainHandItem().getItem() instanceof SignalReceiverItem
                || player.getOffhandItem().getItem() instanceof SignalReceiverItem;
    }

    /** O centro da base militar mais próxima de {@code from} nesta dimensão; {@code null} se não há nenhuma. */
    @Nullable
    public static BlockPos nearestBase(ServerLevel level, BlockPos from) {
        BlockPos ring = level.findNearestMapStructure(BASE, from, SEARCH_RADIUS_CHUNKS, false);
        if (ring == null) {
            return null;
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(IceAgeSurvival.id("military_base"));
        if (structure != null) {
            StructureStart start = level.getChunk(SectionPos.blockToSectionCoord(ring.getX()),
                    SectionPos.blockToSectionCoord(ring.getZ()), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if (start != null && start.isValid()) {
                return start.getBoundingBox().getCenter();
            }
        }
        return ring;
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Signal signal = SIGNALS.get(player.getUUID());
        if (!isHolding(player)) {
            if (signal != null) {
                signal.sent = null; // ao pegar de novo, manda outra vez
            }
            return;
        }
        if (signal == null) {
            signal = new Signal();
            SIGNALS.put(player.getUUID(), signal);
        }
        if (signal.sent != null && player.tickCount % CHECK_TICKS != 0) {
            return;
        }
        Optional<GlobalPos> target = signal.target(player);
        if (!target.equals(signal.sent)) {
            signal.sent = target;
            ModPayloads.sendToPlayer(player, new BaseSignalPayload(target));
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SIGNALS.remove(event.getEntity().getUUID());
    }

    /** A última busca de um jogador e o que o cliente dele já sabe. */
    private static final class Signal {
        @Nullable
        private ResourceKey<Level> dimension;
        private BlockPos origin = BlockPos.ZERO;
        private Optional<GlobalPos> base = Optional.empty();
        /** O último alvo mandado; {@code null} quando o cliente ainda não tem o de agora. */
        @Nullable
        private Optional<GlobalPos> sent;

        Optional<GlobalPos> target(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            BlockPos here = player.blockPosition();
            long dx = here.getX() - origin.getX();
            long dz = here.getZ() - origin.getZ();
            if (!level.dimension().equals(dimension) || dx * dx + dz * dz > (long) RESEARCH_DISTANCE * RESEARCH_DISTANCE) {
                dimension = level.dimension();
                origin = here;
                base = Optional.ofNullable(nearestBase(level, here)).map(pos -> GlobalPos.of(level.dimension(), pos));
            }
            return base;
        }
    }
}
