package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell;
import dev.madebyfelipe.iceagesurvival.outpost.ContainmentTerminals;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: uma linha digitada no console da contenção. O servidor confere se a pessoa tem uma sessão aberta
 * nesse console e ainda está perto dele ({@link ContainmentTerminals}); o cliente só manda o texto.
 */
public record TerminalCommandPayload(BlockPos terminal, String line) {
    public static void encode(TerminalCommandPayload message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.terminal);
        buf.writeUtf(message.line, ContainmentShell.MAX_LINE);
    }

    public static TerminalCommandPayload decode(FriendlyByteBuf buf) {
        return new TerminalCommandPayload(buf.readBlockPos(), buf.readUtf(ContainmentShell.MAX_LINE));
    }

    public static void handle(TerminalCommandPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> ContainmentTerminals.command(player, message.terminal, message.line));
        }
        context.setPacketHandled(true);
    }
}
