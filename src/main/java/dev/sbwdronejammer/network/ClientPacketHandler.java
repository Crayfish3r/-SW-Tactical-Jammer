package dev.sbwdronejammer.network;

import dev.sbwdronejammer.client.ClientRadarState;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClientPacketHandler {
    public static void handleRadar(RadarUpdatePacket message) {
        List<ClientRadarState.RadarContact> contacts = message.contacts().stream()
                .map(contact -> new ClientRadarState.RadarContact(
                        contact.dx(), contact.dz(), contact.friendly()))
                .toList();
        ClientRadarState.acceptServerContacts(message.range(), contacts);
    }

    public static void handleControl(JammedControlPacket message) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack monitor = Minecraft.getInstance().player.getMainHandItem();
        if (SuperbWarfareCompat.isMonitor(monitor)
                && monitor.hasTag()
                && message.linkedDroneId().equals(monitor.getTag().getString("LinkedDrone"))) {
            monitor.getTag().putBoolean("Using", !message.jammed());
        }
    }

    private ClientPacketHandler() {
    }
}
