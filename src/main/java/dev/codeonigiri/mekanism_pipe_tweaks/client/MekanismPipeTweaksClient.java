package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MekanismPipeTweaks.MOD_ID, dist = Dist.CLIENT)
public final class MekanismPipeTweaksClient {
    public MekanismPipeTweaksClient() {
        NeoForge.EVENT_BUS.register(new GuideRenderer());
        NeoForge.EVENT_BUS.register(new GuideInteractionHandler());
    }
}
