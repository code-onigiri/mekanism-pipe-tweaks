package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The two client-to-server messages of the 1.21.1 build, reimplemented on Forge's
 * {@link SimpleChannel}. The wire contents and the server-side semantics are unchanged:
 *
 * <ul>
 *   <li>{@link CycleSideMessage}: cycle one side's connection type on a transmitter.</li>
 *   <li>{@link PlaceTransmitterMessage}: place the held transmitter with the selected side open.</li>
 * </ul>
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

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

    public static void writeBlockPos(FriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeBlockPos(pos);
    }

    public static void writeDirection(FriendlyByteBuf buffer, Direction direction) {
        buffer.writeEnum(direction);
    }

    public static Direction readDirection(FriendlyByteBuf buffer) {
        return buffer.readEnum(Direction.class);
    }
}
