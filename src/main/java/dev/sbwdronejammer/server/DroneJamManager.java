package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import dev.sbwdronejammer.config.JammerConfig;
import dev.sbwdronejammer.logic.FallPhysics;
import dev.sbwdronejammer.network.DroneControlLostPacket;
import dev.sbwdronejammer.network.DroneJamStatePacket;
import dev.sbwdronejammer.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DroneJamManager {
    public static boolean beginFalling(Entity drone, ServerPlayer jammerOwner) {
        if (drone.level().isClientSide
                || !DroneJamState.begin(drone, jammerOwner.getUUID(), drone.level().getGameTime())) {
            return false;
        }

        List<ServerPlayer> linkedPlayers = stopLinkedControllers(drone);
        DroneJamStatePacket statePacket = statePacket(drone);
        ModNetwork.sendTracking(drone, statePacket);

        for (ServerPlayer player : linkedPlayers) {
            ModNetwork.sendTo(player, statePacket);
            ModNetwork.sendTo(player, new DroneControlLostPacket(drone.getUUID()));
        }
        return true;
    }

    public static boolean clearFalling(Entity drone) {
        if (drone.level().isClientSide || !DroneJamState.isFalling(drone)) {
            return false;
        }

        DroneJamState.clear(drone);
        DroneJamStatePacket statePacket = statePacket(drone);
        ModNetwork.sendTracking(drone, statePacket);

        MinecraftServer server = drone.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (hasLinkedMonitor(player, drone.getUUID())) {
                    ModNetwork.sendTo(player, statePacket);
                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !SuperbWarfareCompat.isDrone(event.getTarget())
                || !DroneJamState.isFalling(event.getTarget())) {
            return;
        }

        Entity drone = event.getTarget();
        boolean linked = clearLinkedMonitors(player, drone.getUUID());
        ModNetwork.sendTo(player, statePacket(drone));
        if (linked) {
            ModNetwork.sendTo(player, new DroneControlLostPacket(drone.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        for (UUID droneId : linkedDroneIds(player)) {
            Entity drone = findFallingDrone(server, droneId);
            if (drone == null) {
                continue;
            }

            clearLinkedMonitors(player, droneId);
            ModNetwork.sendTo(player, statePacket(drone));
            ModNetwork.sendTo(player, new DroneControlLostPacket(droneId));
        }
    }

    public static DroneJamStatePacket statePacket(Entity drone) {
        FallPhysics.Parameters parameters = serverParameters();
        return new DroneJamStatePacket(
                drone.getId(),
                drone.getUUID(),
                DroneJamState.isFalling(drone),
                DroneJamState.fallTicks(drone),
                parameters.initialDownwardSpeed(),
                parameters.fallAcceleration(),
                parameters.terminalFallSpeed(),
                parameters.horizontalMomentumMultiplier()
        );
    }

    public static FallPhysics.Parameters serverParameters() {
        return FallPhysics.Parameters.fromConfig(
                JammerConfig.INITIAL_DOWNWARD_SPEED.get(),
                JammerConfig.FALL_ACCELERATION.get(),
                JammerConfig.TERMINAL_FALL_SPEED.get(),
                JammerConfig.HORIZONTAL_MOMENTUM_MULTIPLIER.get()
        );
    }

    private static List<ServerPlayer> stopLinkedControllers(Entity drone) {
        List<ServerPlayer> linkedPlayers = new ArrayList<>();
        MinecraftServer server = drone.getServer();
        if (server == null) {
            return linkedPlayers;
        }

        UUID droneId = drone.getUUID();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (clearLinkedMonitors(player, droneId)) {
                linkedPlayers.add(player);
            }
        }
        return linkedPlayers;
    }

    private static boolean clearLinkedMonitors(ServerPlayer player, UUID droneId) {
        boolean found = false;
        boolean changed = false;

        for (ItemStack stack : player.getInventory().items) {
            if (!SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                continue;
            }

            found = true;
            if (stack.getTag() != null && stack.getTag().getBoolean("Using")) {
                stack.getTag().putBoolean("Using", false);
                changed = true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (!SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                continue;
            }

            found = true;
            if (stack.getTag() != null && stack.getTag().getBoolean("Using")) {
                stack.getTag().putBoolean("Using", false);
                changed = true;
            }
        }

        if (changed) {
            synchronize(player);
        }
        return found;
    }

    private static boolean hasLinkedMonitor(ServerPlayer player, UUID droneId) {
        for (ItemStack stack : player.getInventory().items) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static Set<UUID> linkedDroneIds(ServerPlayer player) {
        Set<UUID> droneIds = new HashSet<>();

        for (ItemStack stack : player.getInventory().items) {
            SuperbWarfareCompat.linkedDroneId(stack).ifPresent(droneIds::add);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            SuperbWarfareCompat.linkedDroneId(stack).ifPresent(droneIds::add);
        }
        return droneIds;
    }

    private static Entity findFallingDrone(MinecraftServer server, UUID droneId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(droneId);
            if (entity != null
                    && SuperbWarfareCompat.isDrone(entity)
                    && DroneJamState.isFalling(entity)) {
                return entity;
            }
        }
        return null;
    }

    private static void synchronize(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private DroneJamManager() {
    }
}
