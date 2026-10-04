package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmShots;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, a cada tiro de arma de fogo (D58): os traçantes (um por raio; nenhum no canhão, cuja esfera é
 * uma entidade), o clarão e, para quem vê o atirador de fora, o coice e a animação do tiro. Vai para quem rastreia o
 * atirador e para ele mesmo; o tiro (raio, dano, fúria) é todo do servidor.
 *
 * @param shooterId id de quem atirou
 * @param mainHand  a arma está na mão principal
 * @param start     boca do cano
 * @param segments  até onde foi cada raio e o que ele atingiu
 */
public record FirearmShotPayload(int shooterId, Firearm gun, boolean mainHand, Vec3 start,
                                 List<FirearmShots.Segment> segments) {
    /** Onde o raio parou: no alcance, num bloco ou numa criatura. */
    public enum Impact {
        NONE, BLOCK, ENTITY
    }

    private static Consumer<FirearmShotPayload> clientHandler = payload -> { };

    public static void encode(FirearmShotPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeEnum(message.gun);
        buf.writeBoolean(message.mainHand);
        writeVec(buf, message.start);
        buf.writeVarInt(message.segments.size());
        for (FirearmShots.Segment segment : message.segments) {
            writeVec(buf, segment.end());
            buf.writeEnum(segment.impact());
        }
    }

    public static FirearmShotPayload decode(FriendlyByteBuf buf) {
        int shooter = buf.readVarInt();
        Firearm gun = buf.readEnum(Firearm.class);
        boolean mainHand = buf.readBoolean();
        Vec3 start = readVec(buf);
        int count = Math.min(buf.readVarInt(), 32);
        List<FirearmShots.Segment> segments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            segments.add(new FirearmShots.Segment(readVec(buf), buf.readEnum(Impact.class)));
        }
        return new FirearmShotPayload(shooter, gun, mainHand, start, segments);
    }

    public static void setClientHandler(Consumer<FirearmShotPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(FirearmShotPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }

    static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
