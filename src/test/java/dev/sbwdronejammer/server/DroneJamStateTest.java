package dev.sbwdronejammer.server;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneJamStateTest {
    private static final UUID DRONE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void transitionIsIrreversibleAndCreatedOnlyOnce() {
        CompoundTag data = new CompoundTag();
        assertTrue(DroneJamState.begin(data, DRONE, OWNER, 42, true));
        assertFalse(DroneJamState.begin(data, DRONE, OWNER, 99, true));
        assertEquals(42, DroneJamState.read(data).triggerTick());
        assertEquals(DroneJamState.Status.FALLING, DroneJamState.read(data).status());
    }

    @Test
    void persistentStateRoundTrips() {
        CompoundTag data = new CompoundTag();
        DroneJamState.begin(data, DRONE, OWNER, 1234, true);
        data.putInt(DroneJamState.FALL_TICKS, 17);
        DroneJamState.Snapshot state = DroneJamState.read(data.copy());
        assertEquals(DRONE, state.droneId());
        assertEquals(OWNER, state.jammerOwner());
        assertEquals(17, state.fallTicks());
        assertTrue(state.armed());
    }

    @Test
    void impactIsProcessedOnce() {
        CompoundTag data = new CompoundTag();
        DroneJamState.begin(data, DRONE, OWNER, 0, true);
        assertTrue(DroneJamState.markImpactProcessed(data));
        assertFalse(DroneJamState.markImpactProcessed(data));
        assertEquals(DroneJamState.Status.IMPACT_PROCESSED, DroneJamState.read(data).status());
    }

    @Test
    void groundDroneIsNotArmed() {
        CompoundTag data = new CompoundTag();
        DroneJamState.begin(data, DRONE, OWNER, 0, false);
        assertFalse(DroneJamState.read(data).armed());
    }
}
