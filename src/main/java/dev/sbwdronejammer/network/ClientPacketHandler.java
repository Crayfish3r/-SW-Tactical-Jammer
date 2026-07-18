package dev.sbwdronejammer.network;

import dev.sbwdronejammer.client.ClientDroneJamState;
import dev.sbwdronejammer.client.ClientRadarState;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class ClientPacketHandler {
    public static void handleRadar(RadarUpdatePacket message) {
        List<ClientRadarState.RadarContact> contacts = message.contacts().stream()
                .map(contact -> new ClientRadarState.RadarContact(
                        contact.dx(), contact.dz(), contact.friendly()))
                .toList();
        ClientRadarState.acceptServerContacts(message.range(), contacts);
    }

    public static void handleControlLost(DroneControlLostPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack monitor = findMatchingMonitor(message.droneId());
        if (monitor != null) {
            monitor.getTag().putBoolean("Using", false);
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    public static void handleDroneJamState(DroneJamStatePacket message) {
        ClientDroneJamState.accept(message);
    }

    private static ItemStack findMatchingMonitor(UUID droneId) {
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().items) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return stack;
            }
        }
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().offhand) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return stack;
            }
        }
        return null;
    }

    private ClientPacketHandler() {
    }
}
