package dev.sbwdronejammer.compat;

import java.util.Optional;
import java.util.UUID;

public interface DroneControllerAccess {
    Optional<UUID> sbwdronejammer$controllerId();
}
