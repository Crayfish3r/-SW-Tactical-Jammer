package dev.sbwdronejammer.network;

import dev.sbwdronejammer.client.ClientDroneJamState;
import dev.sbwdronejammer.client.ClientRadarState;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public final class ClientPacketHandler {
    private static final String SBW_CLIENT_EVENT_HANDLER =
            "com.atsuishio.superbwarfare.event.ClientEventHandler";
    private static final String SBW_LAST_CAMERA_TYPE = "lastCameraType";

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

        boolean wasUsing = clearMatchingMonitors(message.droneId());
        Entity cameraEntity = minecraft.getCameraEntity();
        boolean cameraWasDrone = cameraEntity != null && message.droneId().equals(cameraEntity.getUUID());

        if (wasUsing || cameraWasDrone) {
            restoreSbwCameraType(minecraft);
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    public static void handleDroneJamState(DroneJamStatePacket message) {
        ClientDroneJamState.accept(message);
    }

    private static boolean clearMatchingMonitors(UUID droneId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        boolean wasUsing = false;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            wasUsing |= clearMatchingMonitor(stack, droneId);
        }
        for (ItemStack stack : minecraft.player.getInventory().offhand) {
            wasUsing |= clearMatchingMonitor(stack, droneId);
        }
        return wasUsing;
    }

    private static boolean clearMatchingMonitor(ItemStack stack, UUID droneId) {
        if (!SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()
                || stack.getTag() == null
                || !stack.getTag().getBoolean("Using")) {
            return false;
        }

        stack.getTag().putBoolean("Using", false);
        return true;
    }

    private static void restoreSbwCameraType(Minecraft minecraft) {
        try {
            Class<?> handler = Class.forName(
                    SBW_CLIENT_EVENT_HANDLER,
                    false,
                    ClientPacketHandler.class.getClassLoader()
            );
            Field field = handler.getField(SBW_LAST_CAMERA_TYPE);
            Object value = field.get(null);
            if (value instanceof CameraType cameraType) {
                minecraft.options.setCameraType(cameraType);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private ClientPacketHandler() {
    }
}
