package dev.sbwdronejammer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JammerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue RANGE;
    public static final ForgeConfigSpec.BooleanValue AFFECT_FRIENDLY_DRONES;
    public static final ForgeConfigSpec.IntValue SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue RADAR_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue INITIAL_DOWNWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue FALL_ACCELERATION;
    public static final ForgeConfigSpec.DoubleValue TERMINAL_FALL_SPEED;
    public static final ForgeConfigSpec.DoubleValue HORIZONTAL_MOMENTUM_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_DESTROY_ON_IMPACT;
    public static final ForgeConfigSpec.IntValue IMPACT_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_FALL_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("droneJammer");

        RANGE = builder
                .comment("Jammer radius in blocks.")
                .defineInRange("range", 32, 1, 256);
        AFFECT_FRIENDLY_DRONES = builder
                .comment("If true, a jammer affects every drone, including drones linked to its owner.")
                .define("affectFriendlyDrones", true);
        SCAN_INTERVAL_TICKS = builder
                .comment("Server ticks between jammer scans.")
                .defineInRange("scanIntervalTicks", 5, 1, 20);
        RADAR_INTERVAL_TICKS = builder
                .comment("Server ticks between independent radar updates.")
                .defineInRange("radarIntervalTicks", 10, 1, 100);
        INITIAL_DOWNWARD_SPEED = builder
                .comment("Minimum downward speed when a drone first enters FALLING.")
                .defineInRange("initialDownwardSpeed", 0.35, 0.05, 2.0);
        FALL_ACCELERATION = builder
                .comment("Downward acceleration per falling tick before SBW gravity.")
                .defineInRange("fallAcceleration", 0.08, 0.0, 0.5);
        TERMINAL_FALL_SPEED = builder
                .comment(
                        "Maximum forced downward speed before SBW applies its own gravity.",
                        "If lower than initialDownwardSpeed, the initial speed is used as the terminal speed."
                )
                .defineInRange("terminalFallSpeed", 1.4, 0.1, 5.0);
        HORIZONTAL_MOMENTUM_MULTIPLIER = builder
                .comment("Fraction of horizontal momentum retained each falling tick.")
                .defineInRange("horizontalMomentumMultiplier", 0.98, 0.0, 1.0);
        ALWAYS_DESTROY_ON_IMPACT = builder
                .comment("Use lethal crash damage when an armed falling drone impacts.")
                .define("alwaysDestroyOnImpact", true);
        IMPACT_DELAY_TICKS = builder
                .comment("Minimum falling ticks before an airborne drone can process impact.")
                .defineInRange("impactDelayTicks", 1, 1, 2);
        MAX_FALL_TICKS = builder
                .comment("Failsafe ticks before an airborne falling drone receives crash damage.")
                .defineInRange("maxFallTicks", 1200, 20, 72000);

        builder.pop();
        SPEC = builder.build();
    }

    private JammerConfig() {
    }
}
