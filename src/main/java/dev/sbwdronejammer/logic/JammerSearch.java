package dev.sbwdronejammer.logic;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class JammerSearch {
    public static <D, C> int findFirstMatchingSource(
            List<Source<D, C>> sources, D droneDimension, UUID droneId, UUID droneOwnerId,
            double droneX, double droneY, double droneZ, double rangeSquared,
            boolean affectOwnedDrones, Counter counter) {
        for (int index = 0; index < sources.size(); index++) {
            Source<D, C> source = sources.get(index);
            if (!Objects.equals(droneDimension, source.dimension())) {
                continue;
            }
            boolean ownedBySource = Objects.equals(source.ownerId(), droneOwnerId)
                    || source.ownedDroneIds().contains(droneId);
            if (ownedBySource && !affectOwnedDrones) {
                continue;
            }
            if (counter != null) {
                counter.increment();
            }
            double dx = droneX - source.x();
            double dy = droneY - source.y();
            double dz = droneZ - source.z();
            if (dx * dx + dy * dy + dz * dz <= rangeSquared) {
                return index;
            }
        }
        return -1;
    }

    public record Source<D, C>(D dimension, UUID ownerId, double x, double y, double z,
                               Set<UUID> ownedDroneIds, C context) {
        public Source {
            ownedDroneIds = Set.copyOf(ownedDroneIds);
        }
    }

    public static final class Counter {
        private long value;

        public void increment() {
            value++;
        }

        public long value() {
            return value;
        }
    }

    private JammerSearch() {
    }
}
