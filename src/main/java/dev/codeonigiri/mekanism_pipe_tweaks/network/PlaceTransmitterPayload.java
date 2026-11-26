package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server: place the held transmitter at the grid cell {@code chosenSide} of the
 * transmitter at {@code targetPos}, connected to it. {@code clickedFace} is the face the client
 * actually looked at, used as GregTech's fallback when the chosen side has no room.
 */
public record PlaceTransmitterPayload(BlockPos targetPos, Direction chosenSide, Direction clickedFace)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlaceTransmitterPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MekanismPipeTweaks.MOD_ID, "place_transmitter"));

    public static final StreamCodec<ByteBuf, PlaceTransmitterPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PlaceTransmitterPayload::targetPos,
            Direction.STREAM_CODEC, PlaceTransmitterPayload::chosenSide,
            Direction.STREAM_CODEC, PlaceTransmitterPayload::clickedFace,
            PlaceTransmitterPayload::new);

    @Override
    public CustomPacketPayload.Type<PlaceTransmitterPayload> type() {
        return TYPE;
    }
}
