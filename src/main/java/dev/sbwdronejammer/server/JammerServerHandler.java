package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import dev.sbwdronejammer.config.JammerConfig;
import dev.sbwdronejammer.item.DroneJammerItem;
import dev.sbwdronejammer.network.JammedControlPacket;
import dev.sbwdronejammer.network.ModNetwork;
import dev.sbwdronejammer.network.RadarUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JammerServerHandler {
    private static final Map<UUID, JammedControl> JAMMED_CONTROLS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 2 != 0) {
            return;
        }

        MinecraftServer server = event.getServer();

        // Single pass over the player list. The original code walked
        // server.getPlayerList().getPlayers() twice per tick - once to find jammer
        // holders, once to find monitor holders - each doing a hasTag()/registry
        // check per player. One traversal gets both.
        List<ServerPlayer> sources = new ArrayList<>();
        List<ServerPlayer> controllers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasActiveJammer(player)) {
                sources.add(player);
            }
            ItemStack monitor = player.getMainHandItem();
            if (SuperbWarfareCompat.isMonitor(monitor) && monitor.hasTag()) {
                controllers.add(player);
            }
        }

        Set<UUID> jammedThisTick = new HashSet<>();

        if (!sources.isEmpty()) {
            boolean sendRadarUpdate = server.getTickCount() % 4 == 0;

            // findNearbyDroneJammers() does a spatial entity query per source - the
            // most expensive operation in this handler. Skip it entirely on ticks
            // where nothing would consume the result: no radar packet is due this
            // tick, and nobody is currently piloting a drone to potentially jam.
            if (sendRadarUpdate || !controllers.isEmpty()) {
                Map<UUID, Set<UUID>> nearbyDroneJammers = findNearbyDroneJammers(sources, sendRadarUpdate);

                for (ServerPlayer controller : controllers) {
                    ItemStack monitor = controller.getMainHandItem();
                    String linkedDroneId = monitor.getTag().getString("LinkedDrone");
                    UUID linkedDroneUuid = parseUuidOrNull(linkedDroneId);
                    if (linkedDroneUuid == null) {
                        continue;
                    }

                    Set<UUID> jammerOwners = nearbyDroneJammers.get(linkedDroneUuid);
                    if (jammerOwners == null) {
                        continue;
                    }

                    if (isJammedByAnySource(controller, jammerOwners)) {
                        boolean wasUsing = monitor.getTag().getBoolean("Using");
                        if (wasUsing || JAMMED_CONTROLS.containsKey(controller.getUUID())) {
                            JAMMED_CONTROLS.putIfAbsent(
                                    controller.getUUID(),
                                    new JammedControl(linkedDroneId, wasUsing)
                            );
                            if (monitor.getTag().getBoolean("Using")) {
                                monitor.getTag().putBoolean("Using", false);
                                controller.containerMenu.broadcastChanges();
                                ModNetwork.sendTo(controller, new JammedControlPacket(true, linkedDroneId));
                            }
                            jammedThisTick.add(controller.getUUID());
                        }
                    }
                }
            }
        }

        restoreControlsOutsideJamming(server, jammedThisTick);
    }

    /**
     * Parses a UUID without leaning on exception-driven control flow for the common
     * case. {@code UUID.fromString} throws for every non-UUID string, and it was
     * being called every tick for every online monitor that isn't currently linked
     * to a drone (whose tag holds "" or "none") - filling in a stack trace on the
     * common path for nothing. The length check filters that out cheaply; the
     * try/catch remains as a safety net for genuinely malformed 36-character input.
     */
    private static UUID parseUuidOrNull(String value) {
        if (value.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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

    private static Map<UUID, Set<UUID>> findNearbyDroneJammers(List<ServerPlayer> sources,
                                                               boolean sendRadarUpdate) {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        int range = JammerConfig.RANGE.get();
        double rangeSquared = (double) range * range;
        for (ServerPlayer source : sources) {
            Set<String> ownLinkedDrones = findLinkedDroneIds(source);
            List<RadarUpdatePacket.Contact> radarContacts = sendRadarUpdate
                    ? new ArrayList<>()
                    : List.of();
            for (Entity entity : source.serverLevel().getEntitiesOfClass(
                    Entity.class,
                    source.getBoundingBox().inflate(range),
                    SuperbWarfareCompat::isDrone
            )) {
                if (source.distanceToSqr(entity) > rangeSquared) {
                    continue;
                }
                result.computeIfAbsent(entity.getUUID(), ignored -> new HashSet<>())
                        .add(source.getUUID());

                if (sendRadarUpdate && radarContacts.size() < RadarUpdatePacket.MAX_CONTACTS) {
                    boolean friendly = ownLinkedDrones.contains(entity.getStringUUID());
                    if (!friendly || JammerConfig.AFFECT_FRIENDLY_DRONES.get()) {
                        radarContacts.add(new RadarUpdatePacket.Contact(
                                (float) (entity.getX() - source.getX()),
                                (float) (entity.getZ() - source.getZ()),
                                friendly
                        ));
                    }
                }
            }
            if (sendRadarUpdate) {
                ModNetwork.sendTo(source, new RadarUpdatePacket(range, radarContacts));
            }
        }
        return result;
    }

    private static Set<String> findLinkedDroneIds(ServerPlayer player) {
        Set<String> result = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            addLinkedDroneId(result, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            addLinkedDroneId(result, stack);
        }
        return result;
    }

    private static void addLinkedDroneId(Set<String> result, ItemStack stack) {
        if (SuperbWarfareCompat.isMonitor(stack) && stack.hasTag()) {
            String id = stack.getTag().getString("LinkedDrone");
            if (!id.isBlank() && !"none".equals(id)) {
                result.add(id);
            }
        }
    }

    private static boolean isJammedByAnySource(ServerPlayer controller, Set<UUID> jammerOwners) {
        if (JammerConfig.AFFECT_FRIENDLY_DRONES.get()) {
            return !jammerOwners.isEmpty();
        }
        for (UUID jammerOwner : jammerOwners) {
            if (!jammerOwner.equals(controller.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void restoreControlsOutsideJamming(MinecraftServer server, Set<UUID> jammedThisTick) {
        JAMMED_CONTROLS.entrySet().removeIf(entry -> {
            if (jammedThisTick.contains(entry.getKey())) {
                return false;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                restoreControl(player, entry.getValue());
            }
            return true;
        });
    }

    private static void restoreControl(ServerPlayer player, JammedControl control) {
        if (!control.wasUsing) {
            return;
        }
        ItemStack monitor = player.getMainHandItem();
        if (SuperbWarfareCompat.isMonitor(monitor)
                && monitor.hasTag()
                && control.linkedDroneId.equals(monitor.getTag().getString("LinkedDrone"))) {
            monitor.getTag().putBoolean("Using", true);
            player.containerMenu.broadcastChanges();
            ModNetwork.sendTo(player, new JammedControlPacket(false, control.linkedDroneId));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JammedControl control = JAMMED_CONTROLS.remove(player.getUUID());
            if (control != null) {
                restoreControl(player, control);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        JAMMED_CONTROLS.forEach((playerId, control) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                restoreControl(player, control);
            }
        });
        JAMMED_CONTROLS.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JAMMED_CONTROLS.clear();
    }

    private record JammedControl(String linkedDroneId, boolean wasUsing) {
    }

    private JammerServerHandler() {
    }
}
