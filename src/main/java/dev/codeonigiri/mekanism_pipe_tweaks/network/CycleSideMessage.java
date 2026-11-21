package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import dev.codeonigiri.mekanism_pipe_tweaks.connection.SideCycleLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server: cycle the connection type of one side of the transmitter at {@code pos}.
 * Same fields and server-side effect as the 1.21.1 {@code CycleSidePayload}.
 */
public record CycleSideMessage(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {

    public static void encode(CycleSideMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos());
        buffer.writeEnum(message.side());
    }

    public static CycleSideMessage decode(FriendlyByteBuf buffer) {
        return new CycleSideMessage(buffer.readBlockPos(), buffer.readEnum(net.minecraft.core.Direction.class));
    }

    public static void handle(CycleSideMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SideCycleLogic.apply(player, message.pos(), message.side());
            }
        });
        context.setPacketHandled(true);
    }
}
