package dev.codeonigiri.mekanism_pipe_tweaks.transmitter;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared access to Mekanism transmitters: locating the {@link TileEntityTransmitter} at a
 * position, applying Mekanism's standard refresh sequence after a connection change, and
 * deciding whether this mod's rules may act on a transmitter.
 *
 * <p>Every decision here is based only on Mekanism's base classes and public API
 * ({@link BlockTransmitter}, {@link TileEntityTransmitter} and
 * {@link Transmitter#getSupportedTransmissionTypes()}). Nothing is keyed by block/item registry
 * name, and no concrete per-type Mekanism class is named, so any transmitter an addon registers by
 * extending Mekanism's transmitter bases is handled exactly like a vanilla one.
 */
public final class Transmitters {
    private static final Map<Block, Set<TransmissionType>> BLOCK_TYPES = new HashMap<>();

    private Transmitters() {
    }

    /**
     * The transmitter tile at {@code pos}, or {@code null} when there is no transmitter there.
     * Being a {@link TileEntityTransmitter} is the whole transmitter test: Mekanism only ever
     * creates these tiles under {@link BlockTransmitter} blocks, including for addons.
     */
    public static TileEntityTransmitter tileAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityTransmitter tile ? tile : null;
    }

    /**
     * Mekanism's own post-change sequence (as used by the configurator's sneak right-click):
     * refresh connections, notify the tile, and sync the change to clients.
     */
    public static void refresh(TileEntityTransmitter tile) {
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        transmitter.refreshConnections();
        transmitter.notifyTileChange();
        tile.sendUpdatePacket();
    }

    /**
     * Whether the given transmitter is governed by this mod.
     *
     * <p><b>Multi-type semantics:</b> a transmitter may support several
     * {@link TransmissionType}s at once (addons are free to register such a transmitter). The rules
     * in this mod are per-block rules - placement, connection cycling and the grid guide all act on
     * the whole transmitter block, and Mekanism has no notion of a partially disabled block. A
     * multi-type transmitter is therefore governed if <em>any</em> of its supported transmission
     * types is enabled, and is only left untouched when <em>all</em> of them are disabled. The
     * result is independent of the order the types were registered in.
     */
    public static boolean isEnabled(Transmitter<?, ?, ?> transmitter) {
        return isAnyEnabled(transmitter.getSupportedTransmissionTypes());
    }

    /**
     * Whether a transmitter block (e.g. one held by a player, or being placed) is governed by this
     * mod. The transmission types are read from an instance of the block's own tile entity and its
     * {@link Transmitter}, so this works for addon blocks without a lookup table.
     */
    public static boolean isEnabled(Block block) {
        return isAnyEnabled(transmissionTypes(block));
    }

    /**
     * The transmission types a transmitter block supports, read from a detached instance of its own
     * tile entity and cached per block. An empty set means the types could not be determined.
     */
    public static Set<TransmissionType> transmissionTypes(Block block) {
        return BLOCK_TYPES.computeIfAbsent(block, Transmitters::typesOf);
    }

    private static boolean isAnyEnabled(Set<TransmissionType> types) {
        if (types.isEmpty()) {
            // Unknown/empty transmitter: be permissive and apply the rules, so an addon transmitter
            // whose types cannot be determined is still handled instead of silently ignored.
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
        // In Mekanism 10.4 the block-entity type is exposed by IHasTileEntity (BlockTransmitter is
        // not generic here, unlike 10.7), and that interface can build a detached tile directly.
        if (!(block instanceof BlockTransmitter) || !(block instanceof IHasTileEntity<?> hasTile)) {
            return Set.of();
        }
        try {
            BlockEntity blockEntity = hasTile.createDummyBlockEntity();
            if (blockEntity instanceof TileEntityTransmitter tile && tile.getTransmitter() != null) {
                return Set.copyOf(tile.getTransmitter().getSupportedTransmissionTypes());
            }
        } catch (RuntimeException ignored) {
            // A detached tile could not be built; fall back to the permissive empty set above.
        }
        return Set.of();
    }
}
