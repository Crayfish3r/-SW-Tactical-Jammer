package dev.sbwdronejammer.registry;

import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.item.DroneJammerItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SBWDroneJammer.MOD_ID);

    public static final RegistryObject<Item> DRONE_JAMMER = ITEMS.register(
            "drone_jammer",
            () -> new DroneJammerItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DRONE_JAMMER);
        }
    }

    private ModItems() {
    }
}
