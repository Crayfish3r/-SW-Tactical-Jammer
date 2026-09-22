package dev.sbwdronejammer.server;

import dev.sbwdronejammer.SBWDroneJammer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class DroneJamState {
    private static final String PREFIX = SBWDroneJammer.MOD_ID + ":";

    public static final String FALLING = PREFIX + "Falling";
    public static final String DRONE_ID = PREFIX + "DroneId";
    public static final String JAMMER_OWNER = PREFIX + "JammerOwner";
    public static final String TRIGGER_TICK = PREFIX + "TriggerTick";
    public static final String FALL_TICKS = PREFIX + "FallTicks";
    public static final String FALL_ARMED = PREFIX + "FallArmed";
    public static final String IMPACT_PROCESSED = PREFIX + "ImpactProcessed";

    public static boolean begin(Entity drone, UUID jammerOwner, long triggerTick) {
        CompoundTag data = drone.getPersistentData();
        repairInvalidState(data);
        if (drone.onGround()) {
            return false;
        }
        return begin(data, drone.getUUID(), jammerOwner, triggerTick, true);
    }

    public static boolean begin(CompoundTag data, UUID droneId, UUID jammerOwner,
                                long triggerTick, boolean airborne) {
        repairInvalidState(data);
        if (!airborne || data.getBoolean(FALLING)) {
            return false;
        }

        data.putBoolean(FALLING, true);
        data.putUUID(DRONE_ID, droneId);
        data.putUUID(JAMMER_OWNER, jammerOwner);
        data.putLong(TRIGGER_TICK, triggerTick);
        data.putInt(FALL_TICKS, 0);
        data.putBoolean(FALL_ARMED, true);
        data.putBoolean(IMPACT_PROCESSED, false);
        return true;
    }

    public static boolean isFalling(Entity drone) {
        return isFalling(drone.getPersistentData());
    }

    public static boolean isFalling(CompoundTag data) {
        repairInvalidState(data);
        return data.getBoolean(FALLING);
    }

    public static int fallTicks(Entity drone) {
        return Math.max(0, drone.getPersistentData().getInt(FALL_TICKS));
    }

    public static void advanceFallTick(Entity drone) {
        CompoundTag data = drone.getPersistentData();
        int ticks = Math.min(Integer.MAX_VALUE - 1, Math.max(0, data.getInt(FALL_TICKS)) + 1);
        data.putInt(FALL_TICKS, ticks);
    }

    public static boolean isArmed(Entity drone) {
        return drone.getPersistentData().getBoolean(FALL_ARMED);
    }

    public static boolean impactProcessed(Entity drone) {
        return drone.getPersistentData().getBoolean(IMPACT_PROCESSED);
    }

    public static boolean markImpactProcessed(CompoundTag data) {
        repairInvalidState(data);
        if (!data.getBoolean(FALLING)
                || !data.getBoolean(FALL_ARMED)
                || data.getBoolean(IMPACT_PROCESSED)) {
            return false;
        }

        data.putBoolean(IMPACT_PROCESSED, true);
        return true;
    }

    public static void clear(Entity drone) {
        clear(drone.getPersistentData());
    }

    public static void clear(CompoundTag data) {
        data.remove(FALLING);
        data.remove(DRONE_ID);
        data.remove(JAMMER_OWNER);
        data.remove(TRIGGER_TICK);
        data.remove(FALL_TICKS);
        data.remove(FALL_ARMED);
        data.remove(IMPACT_PROCESSED);
    }

    public static Optional<UUID> jammerOwner(Entity drone) {
        CompoundTag data = drone.getPersistentData();
        return data.hasUUID(JAMMER_OWNER) ? Optional.of(data.getUUID(JAMMER_OWNER)) : Optional.empty();
    }

    public static Snapshot read(CompoundTag data) {
        repairInvalidState(data);
        Status status = !data.getBoolean(FALLING)
                ? Status.NORMAL
                : data.getBoolean(IMPACT_PROCESSED) ? Status.IMPACT_PROCESSED : Status.FALLING;
        UUID droneId = data.hasUUID(DRONE_ID) ? data.getUUID(DRONE_ID) : null;
        UUID jammerOwner = data.hasUUID(JAMMER_OWNER) ? data.getUUID(JAMMER_OWNER) : null;
        return new Snapshot(
                status,
                droneId,
                jammerOwner,
                data.getLong(TRIGGER_TICK),
                Math.max(0, data.getInt(FALL_TICKS)),
                data.getBoolean(FALL_ARMED),
                data.getBoolean(IMPACT_PROCESSED)
        );
    }

    private static void repairInvalidState(CompoundTag data) {
        if (data.getBoolean(FALLING) && !data.getBoolean(FALL_ARMED)) {
            clear(data);
        }
    }

    public enum Status {
        NORMAL,
        FALLING,
        IMPACT_PROCESSED
    }

    public record Snapshot(Status status, UUID droneId, UUID jammerOwner, long triggerTick,
                           int fallTicks, boolean armed, boolean impactProcessed) {
    }

    private DroneJamState() {
    }
}
