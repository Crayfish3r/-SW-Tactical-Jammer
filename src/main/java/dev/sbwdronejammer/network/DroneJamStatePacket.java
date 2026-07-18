package dev.sbwdronejammer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DroneJamStatePacket(int entityId, UUID droneId, boolean falling, int fallTicks,
                                  double initialDownwardSpeed, double fallAcceleration,
                                  double terminalFallSpeed, double horizontalMomentumMultiplier) {
    public DroneJamStatePacket {
        if (entityId < 0 || fallTicks < 0 || initialDownwardSpeed <= 0.0 || fallAcceleration < 0.0
                || terminalFallSpeed < initialDownwardSpeed
                || horizontalMomentumMultiplier < 0.0 || horizontalMomentumMultiplier > 1.0) {
            throw new IllegalArgumentException("Invalid drone jam state packet");
        }
    }

    public static void encode(DroneJamStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.droneId);
        buffer.writeBoolean(message.falling);
        buffer.writeVarInt(message.fallTicks);
        buffer.writeDouble(message.initialDownwardSpeed);
        buffer.writeDouble(message.fallAcceleration);
        buffer.writeDouble(message.terminalFallSpeed);
        buffer.writeDouble(message.horizontalMomentumMultiplier);
    }

    public static DroneJamStatePacket decode(FriendlyByteBuf buffer) {
        return new DroneJamStatePacket(
                buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()
        );
    }

    public static void handle(DroneJamStatePacket message,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDroneJamState(message)
        ));
        context.setPacketHandled(true);
    }
}
