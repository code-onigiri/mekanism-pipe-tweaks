package dev.codeonigiri.mekanism_pipe_tweaks;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import dev.codeonigiri.mekanism_pipe_tweaks.event.ServerEvents;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MekanismPipeTweaks.MOD_ID)
public class MekanismPipeTweaks {
    public static final String MOD_ID = "mekanism_pipe_tweaks";

    public MekanismPipeTweaks(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MPTConfig.SPEC);
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }
}
