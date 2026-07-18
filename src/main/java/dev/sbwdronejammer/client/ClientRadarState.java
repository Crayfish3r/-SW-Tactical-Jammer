package dev.sbwdronejammer.client;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.item.DroneJammerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRadarState {
    private static final int CONTACT_TIMEOUT_TICKS = 20;
    private static List<RadarContact> contacts = List.of();
    private static boolean active;
    private static int serverRange = 32;
    private static int ticksSinceServerUpdate = CONTACT_TIMEOUT_TICKS;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clear();
            return;
        }

        active = hasActiveJammer(player);
        if (!active) {
            contacts = List.of();
            ticksSinceServerUpdate = CONTACT_TIMEOUT_TICKS;
            return;
        }

        if (++ticksSinceServerUpdate > CONTACT_TIMEOUT_TICKS) {
            contacts = List.of();
        }
    }

    public static void acceptServerContacts(int range, List<RadarContact> serverContacts) {
        serverRange = range;
        contacts = List.copyOf(serverContacts);
        ticksSinceServerUpdate = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static List<RadarContact> contacts() {
        return contacts;
    }

    public static int serverRange() {
        return serverRange;
    }

    private static boolean hasActiveJammer(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (DroneJammerItem.isActive(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (DroneJammerItem.isActive(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void clear() {
        active = false;
        contacts = List.of();
        ticksSinceServerUpdate = CONTACT_TIMEOUT_TICKS;
    }

    public record RadarContact(double dx, double dz, boolean friendly) {
    }

    private ClientRadarState() {
    }
}
