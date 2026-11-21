package dev.codeonigiri.mekanism_pipe_tweaks.util;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class TransmitterSupport {
    private static final Map<Block, Set<TransmissionType>> BLOCK_TYPES = new HashMap<>();

    private TransmitterSupport() {
    }

    public static boolean isEnabled(Transmitter<?, ?, ?> transmitter) {
        return isAnyEnabled(transmitter.getSupportedTransmissionTypes());
    }

    public static boolean isEnabled(Block block) {
        return isAnyEnabled(BLOCK_TYPES.computeIfAbsent(block, TransmitterSupport::typesOf));
    }

    private static boolean isAnyEnabled(Set<TransmissionType> types) {
        if (types.isEmpty()) {
            return true;
        }
        for (TransmissionType type : types) {
            if (MPTConfig.isEnabled(type)) {
                return true;
            }
        }
        return false;
    }

    private static Set<TransmissionType> typesOf(Block block) {
        if (!(block instanceof BlockTransmitter<?> transmitterBlock)) {
            return Set.of();
        }
        BlockEntityType<?> blockEntityType = transmitterBlock.getTileType().get();
        BlockEntity blockEntity = blockEntityType.create(BlockPos.ZERO, block.defaultBlockState());
        if (blockEntity instanceof TileEntityTransmitter tile && tile.getTransmitter() != null) {
            return Set.copyOf(tile.getTransmitter().getSupportedTransmissionTypes());
        }
        return Set.of();
    }
}
