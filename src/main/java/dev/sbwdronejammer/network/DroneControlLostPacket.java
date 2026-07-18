package dev.sbwdronejammer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DroneControlLostPacket(UUID droneId) {
    public static void encode(DroneControlLostPacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.droneId);
    }

    public static DroneControlLostPacket decode(FriendlyByteBuf buffer) {
        return new DroneControlLostPacket(buffer.readUUID());
    }

    public static void handle(DroneControlLostPacket message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleControlLost(message)
        ));
        context.setPacketHandled(true);
    }
}
