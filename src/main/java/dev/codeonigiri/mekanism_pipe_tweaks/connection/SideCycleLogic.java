package dev.codeonigiri.mekanism_pipe_tweaks.connection;

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
        ConnectionType next = current.getNext();
        transmitter.setConnectionTypeRaw(side, next);
        Transmitters.refresh(tile);

        player.displayClientMessage(MekanismLang.CONNECTION_TYPE.translateColored(EnumColor.GRAY, next), true);
    }
}
