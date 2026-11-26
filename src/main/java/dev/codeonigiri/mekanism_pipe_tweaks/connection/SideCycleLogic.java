package dev.codeonigiri.mekanism_pipe_tweaks.connection;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import dev.codeonigiri.mekanism_pipe_tweaks.tool.Configurators;
import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * One Configurator click on a guide cell.
 *
 * <p>GregTech's wrench has only two states, connected and disconnected, so this toggles the raw
 * connection type between {@link ConnectionType#NORMAL} and {@link ConnectionType#NONE}. Mekanism's
 * directional {@code PUSH}/{@code PULL} modes are treated as "connected": a click disconnects them,
 * and the next click reconnects as {@code NORMAL}. The 4-state cycle (and its PUSH/PULL arrows) is
 * gone.
 *
 * <p>When a side is connected and the neighbour is a compatible transmitter, the neighbour's facing
 * side is opened too. Mekanism only connects two transmitters when both sides are open, so without
 * this the clicked side would visually connect but carry nothing.
 */
public final class SideCycleLogic {
    private SideCycleLogic() {
    }

    public static void apply(ServerPlayer player, BlockPos pos, Direction side) {
        if (!Configurators.isCycling(player.getMainHandItem())) {
            return;
        }
        if (!player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        TileEntityTransmitter tile = Transmitters.tileAt(level, pos);
        if (tile == null) {
            return;
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter == null || !Transmitters.isEnabled(transmitter)) {
            return;
        }

        ConnectionType current = transmitter.getConnectionTypeRaw(side);
        ConnectionType next = current == ConnectionType.NONE ? ConnectionType.NORMAL : ConnectionType.NONE;
        transmitter.setConnectionTypeRaw(side, next);
        if (next != ConnectionType.NONE) {
            openCounterpart(level, pos, transmitter, side);
        }
        Transmitters.refresh(tile);

        player.displayClientMessage(MekanismLang.CONNECTION_TYPE.translateColored(EnumColor.GRAY, next), true);
    }

    /**
     * Open the neighbour's facing side as well, matching GregTech's {@code syncPipeConnections}.
     * Only a side that is still {@code NONE} is touched: an existing PUSH/PULL configuration is
     * left intact. Disconnecting is not mirrored, because a {@code NONE} on this side already
     * breaks the pair (Mekanism requires mutual agreement) and the neighbour's own setting is
     * worth preserving.
     */
    private static void openCounterpart(ServerLevel level, BlockPos pos, Transmitter<?, ?, ?> transmitter, Direction side) {
        if (!MPTConfig.OPEN_COUNTERPART_SIDE.get()) {
            return;
        }
        TileEntityTransmitter counterpartTile = Transmitters.tileAt(level, pos.relative(side));
        if (counterpartTile == null) {
            return;
        }
        Transmitter<?, ?, ?> counterpart = counterpartTile.getTransmitter();
        if (counterpart == null || !Transmitters.isEnabled(counterpart)
                || !transmitter.supportsTransmissionType(counterpart)) {
            return;
        }
        Direction counterpartSide = side.getOpposite();
        if (counterpart.getConnectionTypeRaw(counterpartSide) == ConnectionType.NONE) {
            counterpart.setConnectionTypeRaw(counterpartSide, ConnectionType.NORMAL);
            Transmitters.refresh(counterpartTile);
        }
    }
}
