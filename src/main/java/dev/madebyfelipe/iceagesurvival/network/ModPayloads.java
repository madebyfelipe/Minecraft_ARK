package dev.madebyfelipe.iceagesurvival.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    /** Mudar quando o formato de qualquer payload mudar de forma incompatível. */
    private static final String PROTOCOL_VERSION = "4";

    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(WhistlePayload.TYPE, WhistlePayload.STREAM_CODEC, WhistlePayload::handle);
        registrar.playToServer(MountAttackPayload.TYPE, MountAttackPayload.STREAM_CODEC, MountAttackPayload::handle);
        registrar.playToServer(AttackOrderPayload.TYPE, AttackOrderPayload.STREAM_CODEC, AttackOrderPayload::handle);
        registrar.playToServer(ToggleMatingPayload.TYPE, ToggleMatingPayload.STREAM_CODEC, ToggleMatingPayload::handle);
        registrar.playToServer(StatusRequestPayload.TYPE, StatusRequestPayload.STREAM_CODEC, StatusRequestPayload::handle);
        registrar.playToClient(ColdStatusPayload.TYPE, ColdStatusPayload.STREAM_CODEC, ColdStatusPayload::handle);
        registrar.playToClient(CreatureStatusPayload.TYPE, CreatureStatusPayload.STREAM_CODEC, CreatureStatusPayload::handle);
    }
}
