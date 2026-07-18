package dev.sbwdronejammer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RadarUpdatePacket(int range, List<Contact> contacts) {
    public static final int MAX_CONTACTS = 128;

    public RadarUpdatePacket {
        contacts = List.copyOf(contacts.size() > MAX_CONTACTS
                ? contacts.subList(0, MAX_CONTACTS)
                : contacts);
    }

    public static void encode(RadarUpdatePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.range);
        buffer.writeVarInt(message.contacts.size());
        for (Contact contact : message.contacts) {
            buffer.writeFloat(contact.dx);
            buffer.writeFloat(contact.dz);
            buffer.writeBoolean(contact.friendly);
        }
    }

    public static RadarUpdatePacket decode(FriendlyByteBuf buffer) {
        int range = buffer.readVarInt();
        if (range < 1 || range > 256) {
            throw new IllegalArgumentException("Invalid radar range: " + range);
        }
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_CONTACTS) {
            throw new IllegalArgumentException("Invalid radar contact count: " + count);
        }
        List<Contact> contacts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            contacts.add(new Contact(buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));
        }
        return new RadarUpdatePacket(range, contacts);
    }

    public static void handle(RadarUpdatePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleRadar(message)
        ));
        context.setPacketHandled(true);
    }

    public record Contact(float dx, float dz, boolean friendly) {
    }
}
