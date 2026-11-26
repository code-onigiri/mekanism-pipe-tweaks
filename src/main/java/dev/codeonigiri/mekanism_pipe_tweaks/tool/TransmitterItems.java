package dev.codeonigiri.mekanism_pipe_tweaks.tool;

import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Recognises a transmitter block item (the item form of a pipe/cable/tube/...), which is what the
 * GregTech-style placement guide is shown for.
 */
public final class TransmitterItems {
    private TransmitterItems() {
    }

    /** The transmitter block behind {@code stack}, or {@code null} when it is not a transmitter item. */
    public static BlockTransmitter<?> blockOf(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockTransmitter<?> block
                ? block
                : null;
    }

    /**
     * Whether a transmitter block item may be placed onto a transmitter of the given type: GT shows
     * the placement guide only when the held pipe is the same genre as the looked-at pipe. "Same
     * genre" is a shared transmission type; an unknown/empty type set is treated permissively, in
     * line with {@link Transmitters#isEnabled(Block)}.
     */
    public static boolean sameGenre(BlockTransmitter<?> block, Transmitter<?, ?, ?> transmitter) {
        var held = Transmitters.transmissionTypes(block);
        if (held.isEmpty()) {
            return true;
        }
        for (TransmissionType type : transmitter.getSupportedTransmissionTypes()) {
            if (held.contains(type)) {
                return true;
            }
        }
        return false;
    }
}
