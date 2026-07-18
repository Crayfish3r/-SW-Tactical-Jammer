package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.DroneControllerAccess;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import dev.sbwdronejammer.config.JammerConfig;
import dev.sbwdronejammer.item.DroneJammerItem;
import dev.sbwdronejammer.logic.JammerSearch;
import dev.sbwdronejammer.network.ModNetwork;
import dev.sbwdronejammer.network.RadarUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ActiveJammerScanner {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        boolean scanDue = tick % JammerConfig.SCAN_INTERVAL_TICKS.get() == 0;
        boolean radarDue = tick % JammerConfig.RADAR_INTERVAL_TICKS.get() == 0;
        if (!scanDue && !radarDue) {
            return;
        }
        Map<ServerLevel, List<JammerSearch.Source<ResourceKey<Level>, ServerPlayer>>> byLevel =
                collectSources(server);
        if (scanDue) {
            scanForNewFallingDrones(byLevel);
        }
        if (radarDue) {
            sendRadarUpdates(byLevel);
        }
    }

    private static Map<ServerLevel, List<JammerSearch.Source<ResourceKey<Level>, ServerPlayer>>> collectSources(
            MinecraftServer server) {
        Map<ServerLevel, List<JammerSearch.Source<ResourceKey<Level>, ServerPlayer>>> result =
                new IdentityHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!hasActiveJammer(player)) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            result.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new JammerSearch.Source<>(
                    level.dimension(), player.getUUID(), player.getX(), player.getY(), player.getZ(),
                    linkedDrones(player), player
            ));
        }
        return result;
    }

    private static void scanForNewFallingDrones(
            Map<ServerLevel, List<JammerSearch.Source<ResourceKey<Level>, ServerPlayer>>> byLevel) {
        double rangeSquared = Math.pow(JammerConfig.RANGE.get(), 2.0);
        boolean affectOwned = JammerConfig.AFFECT_FRIENDLY_DRONES.get();
        byLevel.forEach((level, sources) -> {
            for (Entity drone : LoadedDroneRegistry.loaded(level)) {
                if (DroneJamState.isFalling(drone)) {
                    continue;
                }
                int match = JammerSearch.findFirstMatchingSource(
                        sources, level.dimension(), drone.getUUID(),
                        drone instanceof DroneControllerAccess access
                                ? access.sbwdronejammer$controllerId().orElse(null)
                                : null,
                        drone.getX(), drone.getY(), drone.getZ(),
                        rangeSquared, affectOwned, null
                );
                if (match >= 0) {
                    DroneJamManager.beginFalling(drone, sources.get(match).context());
                }
            }
        });
    }

    private static void sendRadarUpdates(
            Map<ServerLevel, List<JammerSearch.Source<ResourceKey<Level>, ServerPlayer>>> byLevel) {
        double rangeSquared = Math.pow(JammerConfig.RANGE.get(), 2.0);
        boolean affectOwned = JammerConfig.AFFECT_FRIENDLY_DRONES.get();
        byLevel.forEach((level, sources) -> {
            List<Entity> drones = LoadedDroneRegistry.loaded(level);
            for (JammerSearch.Source<ResourceKey<Level>, ServerPlayer> source : sources) {
                List<RadarContactDistance> contacts = new ArrayList<>();
                for (Entity drone : drones) {
                    boolean ownedBySource = source.ownedDroneIds().contains(drone.getUUID());
                    if (ownedBySource && !affectOwned) {
                        continue;
                    }
                    double distanceSquared = source.context().distanceToSqr(drone);
                    if (distanceSquared <= rangeSquared) {
                        contacts.add(new RadarContactDistance(distanceSquared, new RadarUpdatePacket.Contact(
                                (float) (drone.getX() - source.x()),
                                (float) (drone.getZ() - source.z()),
                                ownedBySource
                        )));
                    }
                }
                contacts.sort(Comparator.comparingDouble(RadarContactDistance::distanceSquared));
                List<RadarUpdatePacket.Contact> packetContacts = contacts.stream()
                        .limit(RadarUpdatePacket.MAX_CONTACTS)
                        .map(RadarContactDistance::contact)
                        .toList();
                ModNetwork.sendTo(source.context(),
                        new RadarUpdatePacket(JammerConfig.RANGE.get(), packetContacts));
            }
        });
    }

    private static boolean hasActiveJammer(ServerPlayer player) {
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

    private static Set<UUID> linkedDrones(ServerPlayer player) {
        Set<UUID> result = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            SuperbWarfareCompat.linkedDroneId(stack).ifPresent(result::add);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            SuperbWarfareCompat.linkedDroneId(stack).ifPresent(result::add);
        }
        return result;
    }

    private record RadarContactDistance(double distanceSquared, RadarUpdatePacket.Contact contact) {
    }

    private ActiveJammerScanner() {
    }
}
