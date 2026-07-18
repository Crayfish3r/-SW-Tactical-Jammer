package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.compat.SuperbWarfareCompat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LoadedDroneRegistry {
    private static final Map<ResourceKey<Level>, Map<UUID, WeakReference<Entity>>> DRONES = new HashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SuperbWarfareCompat.isDrone(event.getEntity())) {
            return;
        }
        DRONES.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(event.getEntity().getUUID(), new WeakReference<>(event.getEntity()));
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SuperbWarfareCompat.isDrone(event.getEntity())) {
            return;
        }
        Map<UUID, WeakReference<Entity>> dimensionDrones = DRONES.get(level.dimension());
        if (dimensionDrones != null) {
            dimensionDrones.remove(event.getEntity().getUUID());
            if (dimensionDrones.isEmpty()) {
                DRONES.remove(level.dimension());
            }
        }
    }

    public static List<Entity> loaded(ServerLevel level) {
        Map<UUID, WeakReference<Entity>> dimensionDrones = DRONES.get(level.dimension());
        if (dimensionDrones == null || dimensionDrones.isEmpty()) {
            return List.of();
        }
        List<Entity> result = new ArrayList<>(dimensionDrones.size());
        dimensionDrones.entrySet().removeIf(entry -> {
            Entity drone = entry.getValue().get();
            boolean invalid = drone == null || drone.isRemoved() || drone.level() != level
                    || !SuperbWarfareCompat.isDrone(drone);
            if (!invalid) {
                result.add(drone);
            }
            return invalid;
        });
        return result;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DRONES.clear();
    }

    private LoadedDroneRegistry() {
    }
}
