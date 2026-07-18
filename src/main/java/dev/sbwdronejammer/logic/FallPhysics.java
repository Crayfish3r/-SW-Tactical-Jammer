package dev.sbwdronejammer.logic;

public final class FallPhysics {
    public static Motion next(Motion incoming, int fallingTicks, Parameters parameters) {
        double scheduledSpeed = parameters.initialDownwardSpeed()
                + parameters.fallAcceleration() * Math.max(0, fallingTicks);
        double existingDownwardSpeed = Math.max(0.0, -incoming.y());
        double downwardSpeed = Math.min(
                parameters.terminalFallSpeed(),
                Math.max(existingDownwardSpeed, scheduledSpeed)
        );
        return new Motion(
                incoming.x() * parameters.horizontalMomentumMultiplier(),
                -downwardSpeed,
                incoming.z() * parameters.horizontalMomentumMultiplier()
        );
    }

    public record Motion(double x, double y, double z) {
    }

    public record Parameters(double initialDownwardSpeed,
                             double fallAcceleration,
                             double terminalFallSpeed,
                             double horizontalMomentumMultiplier) {
        public Parameters {
            if (initialDownwardSpeed <= 0.0
                    || fallAcceleration < 0.0
                    || terminalFallSpeed < initialDownwardSpeed
                    || horizontalMomentumMultiplier < 0.0
                    || horizontalMomentumMultiplier > 1.0) {
                throw new IllegalArgumentException("Invalid falling parameters");
            }
        }
    }

    private FallPhysics() {
    }
}
