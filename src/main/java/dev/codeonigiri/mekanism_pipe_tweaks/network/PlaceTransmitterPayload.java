package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record PlaceTransmitterPayload(BlockPos targetPos, Direction chosenSide, Direction clickedFace,
                                      InteractionHand hand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlaceTransmitterPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MekanismPipeTweaks.MOD_ID, "place_transmitter"));

    private static final StreamCodec<ByteBuf, InteractionHand> HAND_CODEC =
            ByteBufCodecs.VAR_INT.map(index -> InteractionHand.values()[index], InteractionHand::ordinal);

    public static final StreamCodec<ByteBuf, PlaceTransmitterPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PlaceTransmitterPayload::targetPos,
            Direction.STREAM_CODEC, PlaceTransmitterPayload::chosenSide,
            Direction.STREAM_CODEC, PlaceTransmitterPayload::clickedFace,
            HAND_CODEC, PlaceTransmitterPayload::hand,
            PlaceTransmitterPayload::new);

    @Override
    public CustomPacketPayload.Type<PlaceTransmitterPayload> type() {
        return TYPE;
    }
}
