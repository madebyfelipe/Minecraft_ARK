package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModPayloads {
    private static final String PROTOCOL_VERSION = "16";
    private static int nextMessageId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(IceAgeSurvival.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        registerServerbound(WhistlePayload.class, WhistlePayload::encode, WhistlePayload::decode,
                WhistlePayload::handle);
        registerServerbound(FlightInputPayload.class, FlightInputPayload::encode, FlightInputPayload::decode,
                FlightInputPayload::handle);
        registerServerbound(MountAttackPayload.class, MountAttackPayload::encode, MountAttackPayload::decode,
                MountAttackPayload::handle);
        registerServerbound(AttackOrderPayload.class, AttackOrderPayload::encode, AttackOrderPayload::decode,
                AttackOrderPayload::handle);
        registerServerbound(ToggleMatingPayload.class, ToggleMatingPayload::encode, ToggleMatingPayload::decode,
                ToggleMatingPayload::handle);
        registerServerbound(SpendBonusPointPayload.class, SpendBonusPointPayload::encode,
                SpendBonusPointPayload::decode, SpendBonusPointPayload::handle);
        registerServerbound(CryoCapsulePayload.class, CryoCapsulePayload::encode, CryoCapsulePayload::decode,
                CryoCapsulePayload::handle);
        registerServerbound(StatusRequestPayload.class, StatusRequestPayload::encode, StatusRequestPayload::decode,
                StatusRequestPayload::handle);
        registerServerbound(LocateRequestPayload.class, LocateRequestPayload::encode, LocateRequestPayload::decode,
                LocateRequestPayload::handle);
        registerServerbound(TerminalCommandPayload.class, TerminalCommandPayload::encode,
                TerminalCommandPayload::decode, TerminalCommandPayload::handle);
        registerServerbound(AnalyzerHoldPayload.class, AnalyzerHoldPayload::encode, AnalyzerHoldPayload::decode,
                AnalyzerHoldPayload::handle);
        registerClientbound(CreatureLocationsPayload.class, CreatureLocationsPayload::encode,
                CreatureLocationsPayload::decode, CreatureLocationsPayload::handle);
        registerClientbound(ColdStatusPayload.class, ColdStatusPayload::encode, ColdStatusPayload::decode,
                ColdStatusPayload::handle);
        registerClientbound(CreatureStatusPayload.class, CreatureStatusPayload::encode, CreatureStatusPayload::decode,
                CreatureStatusPayload::handle);
        registerClientbound(RifleTracerPayload.class, RifleTracerPayload::encode, RifleTracerPayload::decode,
                RifleTracerPayload::handle);
        registerClientbound(DinoFilePayload.class, DinoFilePayload::encode, DinoFilePayload::decode,
                DinoFilePayload::handle);
        registerClientbound(ScanResultPayload.class, ScanResultPayload::encode, ScanResultPayload::decode,
                ScanResultPayload::handle);
        registerClientbound(TerminalReadPayload.class, TerminalReadPayload::encode, TerminalReadPayload::decode,
                TerminalReadPayload::handle);
        registerClientbound(TerminalScreenPayload.class, TerminalScreenPayload::encode, TerminalScreenPayload::decode,
                TerminalScreenPayload::handle);
        registerClientbound(BaseSignalPayload.class, BaseSignalPayload::encode, BaseSignalPayload::decode,
                BaseSignalPayload::handle);
        registerServerbound(ReloadRequestPayload.class, ReloadRequestPayload::encode, ReloadRequestPayload::decode,
                ReloadRequestPayload::handle);
        registerClientbound(FirearmShotPayload.class, FirearmShotPayload::encode, FirearmShotPayload::decode,
                FirearmShotPayload::handle);
        registerClientbound(FirearmReloadPayload.class, FirearmReloadPayload::encode, FirearmReloadPayload::decode,
                FirearmReloadPayload::handle);
        registerClientbound(CryoListPayload.class, CryoListPayload::encode, CryoListPayload::decode,
                CryoListPayload::handle);
    }

    private ModPayloads() {
    }

    private static <T> void registerServerbound(Class<T> type,
            java.util.function.BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<T, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextMessageId++, type, encoder, decoder, handler,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <T> void registerClientbound(Class<T> type,
            java.util.function.BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<T, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextMessageId++, type, encoder, decoder, handler,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /** Para quem rastreia a entidade e, se for um jogador, para ela mesma. */
    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        if (player.connection.connection.channel() != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
