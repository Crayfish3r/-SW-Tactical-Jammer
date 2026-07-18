package dev.sbwdronejammer.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JammerSearchTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DRONE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void handlesInsideBoundaryAndOutside() {
        var sources = List.of(source("overworld", 0, Set.of()));
        assertEquals(0, find(sources, "overworld", 31.99, DRONE, false));
        assertEquals(0, find(sources, "overworld", 32.0, DRONE, false));
        assertEquals(-1, find(sources, "overworld", 32.0001, DRONE, false));
    }

    @Test
    void separatesDimensions() {
        assertEquals(-1, find(List.of(source("nether", 0, Set.of())), "overworld", 0, DRONE, false));
    }

    @Test
    void ownedDroneRespectsSetting() {
        var sources = List.of(source("overworld", 0, Set.of(DRONE)));
        assertEquals(-1, find(sources, "overworld", 1, DRONE, false));
        assertEquals(0, find(sources, "overworld", 1, DRONE, true));
    }

    @Test
    void controllerOwnershipWorksWithoutMonitorSet() {
        var sources = List.of(source("overworld", 0, Set.of()));
        assertEquals(-1, JammerSearch.findFirstMatchingSource(
                sources, "overworld", DRONE, OWNER, 1, 0, 0, 32 * 32, false, null));
        assertEquals(0, JammerSearch.findFirstMatchingSource(
                sources, "overworld", DRONE, OWNER, 1, 0, 0, 32 * 32, true, null));
    }

    @Test
    void overlappingSourcesStopAtFirstMatch() {
        var sources = List.of(source("overworld", 0, Set.of()), source("overworld", 2, Set.of()));
        JammerSearch.Counter counter = new JammerSearch.Counter();
        assertEquals(0, JammerSearch.findFirstMatchingSource(
                sources, "overworld", DRONE, null, 1, 0, 0, 32 * 32, true, counter));
        assertEquals(1, counter.value());
    }

    private static int find(List<JammerSearch.Source<String, Void>> sources, String dimension,
                            double x, UUID drone, boolean affectOwned) {
        return JammerSearch.findFirstMatchingSource(
                sources, dimension, drone, null, x, 0, 0, 32 * 32, affectOwned, null);
    }

    private static JammerSearch.Source<String, Void> source(String dimension, double x, Set<UUID> owned) {
        return new JammerSearch.Source<>(dimension, OWNER, x, 0, 0, owned, null);
    }
}
