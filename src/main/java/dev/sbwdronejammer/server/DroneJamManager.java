package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import dev.sbwdronejammer.config.JammerConfig;
import dev.sbwdronejammer.logic.FallPhysics;
import dev.sbwdronejammer.network.DroneControlLostPacket;
import dev.sbwdronejammer.network.DroneJamStatePacket;
import dev.sbwdronejammer.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DroneJamManager {
    public static boolean beginFalling(Entity drone, ServerPlayer jammerOwner) {
        if (drone.level().isClientSide
                || !DroneJamState.begin(drone, jammerOwner.getUUID(), drone.level().getGameTime())) {
            return false;
        }

        ServerPlayer controller = stopLinkedController(drone);
        DroneJamStatePacket statePacket = statePacket(drone);
        ModNetwork.sendTracking(drone, statePacket);
        if (controller != null) {
            ModNetwork.sendTo(controller, statePacket);
            ModNetwork.sendTo(controller, new DroneControlLostPacket(drone.getUUID()));
        }
        return true;
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SuperbWarfareCompat.isDrone(event.getTarget())
                && DroneJamState.isFalling(event.getTarget())) {
            ModNetwork.sendTo(player, statePacket(event.getTarget()));
        }
    }

    public static DroneJamStatePacket statePacket(Entity drone) {
        FallPhysics.Parameters parameters = serverParameters();
        return new DroneJamStatePacket(
                drone.getId(), drone.getUUID(), true, DroneJamState.fallTicks(drone),
                parameters.initialDownwardSpeed(), parameters.fallAcceleration(),
                parameters.terminalFallSpeed(), parameters.horizontalMomentumMultiplier()
        );
    }

    public static FallPhysics.Parameters serverParameters() {
        return new FallPhysics.Parameters(
                JammerConfig.INITIAL_DOWNWARD_SPEED.get(),
                JammerConfig.FALL_ACCELERATION.get(),
                JammerConfig.TERMINAL_FALL_SPEED.get(),
                JammerConfig.HORIZONTAL_MOMENTUM_MULTIPLIER.get()
        );
    }

    private static ServerPlayer stopLinkedController(Entity drone) {
        ServerPlayer fallback = null;
        for (ServerPlayer player : drone.getServer().getPlayerList().getPlayers()) {
            ItemStack matching = findMatchingMonitor(player, drone.getUUID());
            if (matching == null) {
                continue;
            }
            if (matching.getTag().getBoolean("Using")) {
                matching.getTag().putBoolean("Using", false);
                synchronize(player);
                return player;
            }
            if (fallback == null) {
                fallback = player;
            }
        }
        if (fallback != null) {
            ItemStack matching = findMatchingMonitor(fallback, drone.getUUID());
            if (matching != null) {
                matching.getTag().putBoolean("Using", false);
                synchronize(fallback);
            }
        }
        return fallback;
    }

    private static ItemStack findMatchingMonitor(ServerPlayer player, UUID droneId) {
        for (ItemStack stack : player.getInventory().items) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (SuperbWarfareCompat.linkedDroneId(stack).filter(droneId::equals).isPresent()) {
                return stack;
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
