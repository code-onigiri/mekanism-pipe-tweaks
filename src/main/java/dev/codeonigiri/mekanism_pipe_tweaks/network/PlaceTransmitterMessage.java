package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server: place the held transmitter on {@code targetPos} with {@code chosenSide} open.
 * Same fields and server-side effect as the 1.21.1 {@code PlaceTransmitterPayload}, including the
 * actually-clicked face used by the one-shot double-placement guard.
 */
public record PlaceTransmitterMessage(BlockPos targetPos, Direction chosenSide, Direction clickedFace,
                                      InteractionHand hand) {

    public static void encode(PlaceTransmitterMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.targetPos());
        buffer.writeEnum(message.chosenSide());
        buffer.writeEnum(message.clickedFace());
        buffer.writeVarInt(message.hand().ordinal());
    }

    public static PlaceTransmitterMessage decode(FriendlyByteBuf buffer) {
        BlockPos targetPos = buffer.readBlockPos();
        Direction chosenSide = buffer.readEnum(Direction.class);
        Direction clickedFace = buffer.readEnum(Direction.class);
        InteractionHand hand = InteractionHand.values()[buffer.readVarInt()];
        return new PlaceTransmitterMessage(targetPos, chosenSide, clickedFace, hand);
    }

    public static void handle(PlaceTransmitterMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TransmitterPlacementLogic.placeSelected(player, message.targetPos(), message.chosenSide(),
                        message.clickedFace(), message.hand());
            }
        });
        context.setPacketHandled(true);
    }
}
