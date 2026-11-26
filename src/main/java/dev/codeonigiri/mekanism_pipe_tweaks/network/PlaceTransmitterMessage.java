package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server: place the held transmitter at the grid cell {@code chosenSide} of the
 * transmitter at {@code targetPos}, connected to it. {@code clickedFace} is the face the client
 * actually looked at, used as GregTech's fallback when the chosen side has no room.
 */
public record PlaceTransmitterMessage(BlockPos targetPos, Direction chosenSide, Direction clickedFace) {

    public static void encode(PlaceTransmitterMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.targetPos());
        buffer.writeEnum(message.chosenSide());
        buffer.writeEnum(message.clickedFace());
    }

    public static PlaceTransmitterMessage decode(FriendlyByteBuf buffer) {
        return new PlaceTransmitterMessage(buffer.readBlockPos(),
                buffer.readEnum(Direction.class), buffer.readEnum(Direction.class));
    }

    public static void handle(PlaceTransmitterMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TransmitterPlacementLogic.placeSelected(player, message.targetPos(), message.chosenSide(),
                        message.clickedFace());
            }
        });
        context.setPacketHandled(true);
    }
}
