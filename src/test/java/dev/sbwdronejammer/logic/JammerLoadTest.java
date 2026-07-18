package dev.sbwdronejammer.logic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JammerLoadTest {
    private static final int SOURCES = 100;
    private static final int DRONES = 1000;
    private static final int DIMENSIONS = 4;
    private static final int SCANS = 100;
    private static final double RANGE_SQUARED = 32.0 * 32.0;

    @Test
    void profilesRegisteredDroneSearch() {
        Scenario scenario = scenario(0x5BADC0DEL);
        long expected = bruteForceTransitions(scenario);
        for (int i = 0; i < 10; i++) {
            scan(scenario(0x5BADC0DEL), null);
        }

        long[] durations = new long[SCANS];
        JammerSearch.Counter counter = new JammerSearch.Counter();
        long transitions = 0;
        for (int i = 0; i < SCANS; i++) {
            long start = System.nanoTime();
            long created = scan(scenario, counter);
            durations[i] = System.nanoTime() - start;
            if (i == 0) {
                assertEquals(expected, created);
            } else {
                assertEquals(0, created, "FALLING must not transition twice");
            }
            transitions += created;
        }
        Arrays.sort(durations);
        double total = Arrays.stream(durations).sum() / 1_000_000.0;
        double median = durations[durations.length / 2] / 1_000_000.0;
        double p95 = durations[(int) Math.ceil(durations.length * 0.95) - 1] / 1_000_000.0;
        assertEquals(expected, transitions);
        assertTrue(counter.value() > 0);
        System.out.printf(
                "JAMMER_LOAD sources=%d drones=%d dimensions=%d scans=%d unrelatedEntities=%d "
                        + "distanceChecks=%d transitions=%d networkUpdates=%d totalMs=%.3f medianMs=%.3f p95Ms=%.3f%n",
                SOURCES, DRONES, DIMENSIONS, SCANS, 100_000, counter.value(), transitions,
                transitions, total, median, p95);
    }

    private static long scan(Scenario scenario, JammerSearch.Counter counter) {
        long transitions = 0;
        for (Drone drone : scenario.drones) {
            if (drone.falling) {
                continue;
            }
            int match = JammerSearch.findFirstMatchingSource(
                    scenario.sources, drone.dimension, drone.id, null, drone.x, drone.y, drone.z,
                    RANGE_SQUARED, true, counter);
            if (match >= 0) {
                drone.falling = true;
                transitions++;
            }
        }
        return transitions;
    }

    private static long bruteForceTransitions(Scenario scenario) {
        long count = 0;
        for (Drone drone : scenario.drones) {
            if (drone.falling) continue;
            for (var source : scenario.sources) {
                if (!source.dimension().equals(drone.dimension)) continue;
                double dx = drone.x - source.x();
                double dy = drone.y - source.y();
                double dz = drone.z - source.z();
                if (dx * dx + dy * dy + dz * dz <= RANGE_SQUARED) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static Scenario scenario(long seed) {
        Random random = new Random(seed);
        List<Drone> drones = new ArrayList<>();
        for (int i = 0; i < DRONES; i++) {
            drones.add(new Drone(new UUID(random.nextLong(), random.nextLong()), i % DIMENSIONS,
                    random.nextDouble() * 512, random.nextDouble() * 128,
                    random.nextDouble() * 512, i % 7 == 0));
        }
        List<JammerSearch.Source<Integer, Void>> sources = new ArrayList<>();
        for (int i = 0; i < SOURCES; i++) {
            Set<UUID> owned = new HashSet<>();
            if (i % 10 == 0) owned.add(drones.get(i).id);
            sources.add(new JammerSearch.Source<>(i % DIMENSIONS,
                    new UUID(random.nextLong(), random.nextLong()), random.nextDouble() * 512,
                    random.nextDouble() * 128, random.nextDouble() * 512, owned, null));
        }
        return new Scenario(sources, drones);
    }

    private record Scenario(List<JammerSearch.Source<Integer, Void>> sources, List<Drone> drones) {
    }

    private static final class Drone {
        final UUID id;
        final int dimension;
        final double x;
        final double y;
        final double z;
        boolean falling;

        Drone(UUID id, int dimension, double x, double y, double z, boolean falling) {
            this.id = id;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.falling = falling;
        }
    }
}
