package dev.sbwdronejammer.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SuperbWarfareCompat {
    public static final ResourceLocation DRONE_ID = new ResourceLocation("superbwarfare", "drone");
    public static final ResourceLocation MONITOR_ID = new ResourceLocation("superbwarfare", "monitor");

    // Resolved lazily and cached. isDrone() runs once per entity inside every
    // jammer's search radius, and isMonitor() runs once per inventory slot, both
    // every 2 ticks - the original reverse registry lookup (getKey + equals) on
    // every single call was the single hottest cost in the mod. We resolve the
    // forward direction once and then compare by reference instead.
    //
    // Note: BuiltInRegistries.ENTITY_TYPE / ITEM are *defaulted* registries (they
    // fall back to minecraft:pig / minecraft:air for unknown keys), so a plain
    // get() would silently hand back the wrong object if Superb Warfare isn't
    // installed. getOptional() avoids that trap - it only returns a value when
    // the id was actually registered.
    private static EntityType<?> droneType;
    private static Item monitorItem;

    public static boolean isDrone(Entity entity) {
        if (droneType == null) {
            droneType = BuiltInRegistries.ENTITY_TYPE.getOptional(DRONE_ID).orElse(null);
            if (droneType == null) {
                // Superb Warfare not present (or not loaded yet) - nothing is a drone.
                return false;
            }
        }
        return entity.getType() == droneType;
    }

    public static boolean isMonitor(ItemStack stack) {
        if (monitorItem == null) {
            monitorItem = BuiltInRegistries.ITEM.getOptional(MONITOR_ID).orElse(null);
            if (monitorItem == null) {
                return false;
            }
        }
        return stack.getItem() == monitorItem;
    }

    public static boolean isLinkedTo(ItemStack monitor, Entity drone) {
        return isMonitor(monitor)
                && monitor.hasTag()
                && monitor.getTag().getBoolean("Linked")
                && drone.getStringUUID().equals(monitor.getTag().getString("LinkedDrone"));
    }

    private SuperbWarfareCompat() {
    }
}
