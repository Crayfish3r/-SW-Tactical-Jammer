package dev.sbwdronejammer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record JammedControlPacket(boolean jammed, String linkedDroneId) {
    private static final int MAX_ID_LENGTH = 36;

    public static void encode(JammedControlPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.jammed);
        buffer.writeUtf(message.linkedDroneId, MAX_ID_LENGTH);
    }

    public static JammedControlPacket decode(FriendlyByteBuf buffer) {
        return new JammedControlPacket(buffer.readBoolean(), buffer.readUtf(MAX_ID_LENGTH));
    }

    public static void handle(JammedControlPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleControl(message)
        ));
        context.setPacketHandled(true);
    }
}
