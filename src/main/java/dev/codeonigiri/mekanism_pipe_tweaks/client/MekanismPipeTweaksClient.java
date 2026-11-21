package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only registration. {@link Mod.EventBusSubscriber} with {@link Dist#CLIENT} keeps this
 * class (and the client-only renderer/targeting code it pulls in) from being loaded on a
 * dedicated server, which is the 1.20.1 equivalent of the 1.21.1 dist-gated client mod class.
 */
@Mod.EventBusSubscriber(modid = MekanismPipeTweaks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MekanismPipeTweaksClient {
    private MekanismPipeTweaksClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new GuideRenderer());
        MinecraftForge.EVENT_BUS.register(new GuideInteractionHandler());
    }
}
