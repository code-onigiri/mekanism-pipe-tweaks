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
 *   <li>{@link CycleSideMessage}: toggle one side's connection on a transmitter.</li>
 *   <li>{@link PlaceTransmitterMessage}: place the held transmitter on a guide cell.</li>
 * </ul>
 */
public final class ModNetwork {
    /**
     * Protocol version 3: the placement-guide message returned in 0.3.0, and the toggle became
     * two-state. Older clients/servers are incompatible.
     */
    private static final String PROTOCOL_VERSION = "3";

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
        CHANNEL.registerMessage(id++, PlaceTransmitterMessage.class,
                PlaceTransmitterMessage::encode, PlaceTransmitterMessage::decode, PlaceTransmitterMessage::handle);
    }
}
