package dev.codeonigiri.mekanism_pipe_tweaks;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import dev.codeonigiri.mekanism_pipe_tweaks.event.ServerEvents;
import dev.codeonigiri.mekanism_pipe_tweaks.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(MekanismPipeTweaks.MOD_ID)
public class MekanismPipeTweaks {
    public static final String MOD_ID = "mekanism_pipe_tweaks";

    public MekanismPipeTweaks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MPTConfig.SPEC);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }
}
