package dev.madebyfelipe.iceagesurvival.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente: o que o console da contenção responde. {@code open} abre a tela (com a inicialização nas
 * linhas); {@code clear} limpa a tela antes; {@code close} fecha depois de mostrar. {@code prompt} e {@code secret}
 * dizem como fica a linha de digitação (a senha sai com asteriscos).
 */
public record TerminalScreenPayload(BlockPos terminal, boolean open, boolean clear, boolean close, List<String> lines,
                                    String prompt, boolean secret) {
    private static final int MAX_LINES = 64;
    private static final int MAX_TEXT = 256;
    private static Consumer<TerminalScreenPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<TerminalScreenPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(TerminalScreenPayload message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.terminal);
        buf.writeBoolean(message.open);
        buf.writeBoolean(message.clear);
        buf.writeBoolean(message.close);
        List<String> lines = message.lines.subList(0, Math.min(MAX_LINES, message.lines.size()));
        buf.writeVarInt(lines.size());
        lines.forEach(line -> buf.writeUtf(line, MAX_TEXT));
        buf.writeUtf(message.prompt, MAX_TEXT);
        buf.writeBoolean(message.secret);
    }

    public static TerminalScreenPayload decode(FriendlyByteBuf buf) {
        BlockPos terminal = buf.readBlockPos();
        boolean open = buf.readBoolean();
        boolean clear = buf.readBoolean();
        boolean close = buf.readBoolean();
        int count = Math.min(MAX_LINES, buf.readVarInt());
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readUtf(MAX_TEXT));
        }
        return new TerminalScreenPayload(terminal, open, clear, close, List.copyOf(lines), buf.readUtf(MAX_TEXT),
                buf.readBoolean());
    }

    public static void handle(TerminalScreenPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
