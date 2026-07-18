package dev.sbwdronejammer.client;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import dev.sbwdronejammer.logic.FallPhysics;
import dev.sbwdronejammer.network.DroneJamStatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDroneJamState {
    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void accept(DroneJamStatePacket packet) {
        if (!packet.falling()) {
            STATES.remove(packet.droneId());
            return;
        }
        STATES.put(packet.droneId(), new State(
                packet.entityId(), packet.fallTicks(),
                new FallPhysics.Parameters(packet.initialDownwardSpeed(), packet.fallAcceleration(),
                        packet.terminalFallSpeed(), packet.horizontalMomentumMultiplier())
        ));
    }

    public static State state(Entity drone) {
        State state = STATES.get(drone.getUUID());
        return state != null && state.entityId() == drone.getId() ? state : null;
    }

    public static void advance(Entity drone) {
        State state = state(drone);
        if (state != null) {
            STATES.put(drone.getUUID(),
                    new State(state.entityId(), state.fallTicks() + 1, state.parameters()));
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && SuperbWarfareCompat.isDrone(event.getEntity())) {
            STATES.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            STATES.clear();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    public record State(int entityId, int fallTicks, FallPhysics.Parameters parameters) {
    }

    private ClientDroneJamState() {
    }
}
