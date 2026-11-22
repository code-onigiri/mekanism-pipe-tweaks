package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The client-to-server messages of the mod, reimplemented on Forge's {@link SimpleChannel}.
 * The wire contents and the server-side semantics follow the 1.21.1 build:
 *
 * <ul>
 *   <li>{@link CycleSideMessage}: cycle one side's connection type on a transmitter.</li>
 * </ul>
 */
public final class ModNetwork {
    /**
     * Protocol version 2: the place-transmitter message was removed in 0.2.0, so the channel
     * is incompatible with 0.1.x clients and servers.
     */
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MekanismPipeTweaks.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CycleSideMessage.class,
                CycleSideMessage::encode, CycleSideMessage::decode, CycleSideMessage::handle);
    }
}
