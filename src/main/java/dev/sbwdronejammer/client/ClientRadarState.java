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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRadarState {
    private static final long POSITION_BLEND_NANOS = 280_000_000L;
    private static final long CONTACT_HOLD_NANOS = 6_000_000_000L;
    private static final long CONTACT_FADE_NANOS = 2_000_000_000L;

    private static final List<TrackedContact> contacts = new ArrayList<>();
    private static boolean active;
    private static int serverRange = 32;
    private static int nextContactId;

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
            contacts.clear();
            nextContactId = 0;
            return;
        }

        long now = System.nanoTime();
        contacts.removeIf(contact -> now - contact.lastSeenNanos
                > CONTACT_HOLD_NANOS + CONTACT_FADE_NANOS);
    }

    public static void acceptServerContacts(int range, List<RadarContact> serverContacts) {
        serverRange = range;
        long now = System.nanoTime();
        List<TrackedContact> unmatched = new ArrayList<>(contacts);

        for (RadarContact incoming : serverContacts) {
            TrackedContact match = unmatched.stream()
                    .filter(contact -> contact.friendly == incoming.friendly())
                    .min(Comparator.comparingDouble(contact -> contact.distanceSquaredTo(incoming)))
                    .orElse(null);

            if (match == null) {
                contacts.add(new TrackedContact(nextContactId++, incoming, now));
            } else {
                match.retarget(incoming, now);
                unmatched.remove(match);
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static List<VisibleContact> visibleContacts(long nowNanos) {
        return contacts.stream()
                .map(contact -> contact.sample(nowNanos))
                .filter(contact -> contact.alpha() > 0.0F)
                .toList();
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
        contacts.clear();
        nextContactId = 0;
    }

    public record RadarContact(double dx, double dz, boolean friendly) {
    }

    public record VisibleContact(int id, double dx, double dz, boolean friendly, float alpha) {
        public double distance() {
            return Math.hypot(dx, dz);
        }
    }

    private static final class TrackedContact {
        private final int id;
        private final boolean friendly;
        private double fromX;
        private double fromZ;
        private double targetX;
        private double targetZ;
        private long updateNanos;
        private long lastSeenNanos;

        private TrackedContact(int id, RadarContact contact, long now) {
            this.id = id;
            friendly = contact.friendly();
            fromX = targetX = contact.dx();
            fromZ = targetZ = contact.dz();
            updateNanos = lastSeenNanos = now;
        }

        private void retarget(RadarContact contact, long now) {
            VisibleContact current = sample(now);
            fromX = current.dx();
            fromZ = current.dz();
            targetX = contact.dx();
            targetZ = contact.dz();
            updateNanos = lastSeenNanos = now;
        }

        private double distanceSquaredTo(RadarContact contact) {
            double dx = targetX - contact.dx();
            double dz = targetZ - contact.dz();
            return dx * dx + dz * dz;
        }

        private VisibleContact sample(long now) {
            double blend = Math.min(
                    1.0,
                    Math.max(0.0, (now - updateNanos) / (double) POSITION_BLEND_NANOS)
            );
            blend = 1.0 - Math.pow(1.0 - blend, 3.0);
            double x = fromX + (targetX - fromX) * blend;
            double z = fromZ + (targetZ - fromZ) * blend;
            long age = now - lastSeenNanos;
            float alpha = age <= CONTACT_HOLD_NANOS
                    ? 1.0F
                    : 1.0F - (float) (age - CONTACT_HOLD_NANOS) / CONTACT_FADE_NANOS;
            return new VisibleContact(id, x, z, friendly, Math.max(0.0F, alpha));
        }
    }

    private ClientRadarState() {
    }
}
