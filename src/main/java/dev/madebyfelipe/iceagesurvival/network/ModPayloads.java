package dev.madebyfelipe.iceagesurvival.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    /** Mudar quando o formato de qualquer payload mudar de forma incompatível. */
    private static final String PROTOCOL_VERSION = "1";

    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(SetOrderPayload.TYPE, SetOrderPayload.STREAM_CODEC, SetOrderPayload::handle);
        registrar.playToServer(AttackOrderPayload.TYPE, AttackOrderPayload.STREAM_CODEC, AttackOrderPayload::handle);
    }
}
