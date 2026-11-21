package dev.codeonigiri.mekanism_pipe_tweaks.autotest;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TEMPORARY registration for the AutoTest harness, gated by -Dmpt.autotest=true.
 * Removed from the release build after verification.
 */
@Mod.EventBusSubscriber(modid = MekanismPipeTweaks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AutoTestRegistration {
    private static final Logger LOG = LoggerFactory.getLogger("MPT-AutoTest");

    private AutoTestRegistration() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        boolean enabled = Boolean.getBoolean("mpt.autotest");
        LOG.info("MPT_AUTOTEST harness registration: enabled={}", enabled);
        if (enabled) {
            MinecraftForge.EVENT_BUS.register(new AutoTest());
            LOG.info("MPT_AUTOTEST harness registered on the game bus");
        }
    }
}
