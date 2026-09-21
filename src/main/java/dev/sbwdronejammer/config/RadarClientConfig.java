package dev.sbwdronejammer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RadarClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    public static final ForgeConfigSpec.EnumValue<ScreenPosition> POSITION;
    public static final ForgeConfigSpec.IntValue OFFSET_X;
    public static final ForgeConfigSpec.IntValue OFFSET_Y;
    public static final ForgeConfigSpec.DoubleValue SCAN_SPEED;
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_OPACITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_TARGET_LABEL;
    public static final ForgeConfigSpec.BooleanValue SHOW_LEGEND;
    public static final ForgeConfigSpec.BooleanValue COMPACT_WHEN_IDLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("radarHud");
        HUD_SCALE = builder
                .comment("Visual scale of the radar HUD.")
                .defineInRange("scale", 1.0, 0.5, 2.0);
        POSITION = builder
                .comment("Screen corner used to anchor the radar HUD.")
                .defineEnum("position", ScreenPosition.BOTTOM_RIGHT);
        OFFSET_X = builder
                .comment("Horizontal offset from the selected screen corner, in scaled pixels.")
                .defineInRange("offsetX", 12, -4096, 4096);
        OFFSET_Y = builder
                .comment("Vertical offset from the selected screen corner, in scaled pixels.")
                .defineInRange("offsetY", 12, -4096, 4096);
        SCAN_SPEED = builder
                .comment("Radar sweep revolutions per second. Visual only.")
                .defineInRange("scanSpeed", 0.22, 0.02, 2.0);
        BACKGROUND_OPACITY = builder
                .comment("Opacity of the dark radar backing.")
                .defineInRange("backgroundOpacity", 0.78, 0.15, 1.0);
        SHOW_TARGET_LABEL = builder
                .comment("Show the nearest-contact detail block.")
                .define("showTargetLabel", true);
        SHOW_LEGEND = builder
                .comment("Show marker color and affiliation legend.")
                .define("showLegend", true);
        COMPACT_WHEN_IDLE = builder
                .comment("Collapse the radar to a small active-status indicator when no contacts remain.")
                .define("compactWhenIdle", true);
        builder.pop();
        SPEC = builder.build();
    }

    public enum ScreenPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private RadarClientConfig() {
    }
}
