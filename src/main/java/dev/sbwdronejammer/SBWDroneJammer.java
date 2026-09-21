package dev.sbwdronejammer;

import dev.sbwdronejammer.config.JammerConfig;
import dev.sbwdronejammer.config.RadarClientConfig;
import dev.sbwdronejammer.network.ModNetwork;
import dev.sbwdronejammer.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SBWDroneJammer.MOD_ID)
public final class SBWDroneJammer {
    public static final String MOD_ID = "sbwdronejammer";

    public SBWDroneJammer() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, JammerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RadarClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
