package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CycleSidePayload(BlockPos pos, Direction side) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CycleSidePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MekanismPipeTweaks.MOD_ID, "cycle_side"));

    public static final StreamCodec<ByteBuf, CycleSidePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CycleSidePayload::pos,
            Direction.STREAM_CODEC, CycleSidePayload::side,
            CycleSidePayload::new);

    @Override
    public CustomPacketPayload.Type<CycleSidePayload> type() {
        return TYPE;
    }
}
