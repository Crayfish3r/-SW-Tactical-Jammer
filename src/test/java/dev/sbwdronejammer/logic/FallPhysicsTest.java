package dev.sbwdronejammer.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FallPhysicsTest {
    private static final FallPhysics.Parameters PARAMETERS =
            new FallPhysics.Parameters(0.35, 0.08, 1.4, 0.98);

    @Test
    void startsWithConfiguredDownwardSpeed() {
        FallPhysics.Motion motion =
                FallPhysics.next(new FallPhysics.Motion(0, 0.2, 0), 0, PARAMETERS);
        assertEquals(-0.35, motion.y(), 1.0e-9);
    }

    @Test
    void acceleratesOnFollowingTicks() {
        FallPhysics.Motion motion =
                FallPhysics.next(new FallPhysics.Motion(0, -0.35, 0), 1, PARAMETERS);
        assertEquals(-0.43, motion.y(), 1.0e-9);
    }

    @Test
    void clampsToTerminalVelocity() {
        FallPhysics.Motion motion =
                FallPhysics.next(new FallPhysics.Motion(0, -4.0, 0), 100, PARAMETERS);
        assertEquals(-1.4, motion.y(), 1.0e-9);
    }

    @Test
    void retainsConfiguredHorizontalMomentum() {
        FallPhysics.Motion motion =
                FallPhysics.next(new FallPhysics.Motion(2.0, 0, -3.0), 0, PARAMETERS);
        assertEquals(1.96, motion.x(), 1.0e-9);
        assertEquals(-2.94, motion.z(), 1.0e-9);
    }

    @Test
    void normalizesTerminalSpeedBelowInitialForConfig() {
        FallPhysics.Parameters parameters =
                FallPhysics.Parameters.fromConfig(2.0, 0.08, 0.1, 0.98);

        assertEquals(2.0, parameters.initialDownwardSpeed(), 1.0e-9);
        assertEquals(2.0, parameters.terminalFallSpeed(), 1.0e-9);
    }

    @Test
    void directParametersStillRejectInvalidCrossFieldValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FallPhysics.Parameters(2.0, 0.08, 0.1, 0.98)
        );
    }

    @Test
    void rejectsNonFiniteParameters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FallPhysics.Parameters(Double.NaN, 0.08, 1.4, 0.98)
        );
    }
}
