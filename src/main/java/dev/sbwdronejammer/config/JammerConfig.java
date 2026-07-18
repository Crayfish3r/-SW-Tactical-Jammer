package dev.sbwdronejammer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JammerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue RANGE;
    public static final ForgeConfigSpec.BooleanValue AFFECT_FRIENDLY_DRONES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("droneJammer");
        RANGE = builder
                .comment("Jammer radius in blocks.")
                .defineInRange("range", 32, 1, 256);
        AFFECT_FRIENDLY_DRONES = builder
                .comment("If false, a jammer does not affect the drone controlled by its owner.")
                .define("affectFriendlyDrones", true);
        builder.pop();
        SPEC = builder.build();
    }

    private JammerConfig() {
    }
}
