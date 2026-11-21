package dev.codeonigiri.mekanism_pipe_tweaks.connection;

import dev.codeonigiri.mekanism_pipe_tweaks.util.TransmitterSupport;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SideCycleLogic {
    private SideCycleLogic() {
    }

    public static void apply(ServerPlayer player, BlockPos pos, Direction side) {
        if (!(player.getMainHandItem().getItem() instanceof ItemConfigurator)) {
            return;
        }
        if (!player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TileEntityTransmitter tile)) {
            return;
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter == null || !TransmitterSupport.isEnabled(transmitter)) {
            return;
        }

        ConnectionType current = transmitter.getConnectionTypeRaw(side);
        ConnectionType next = current.getNext();
        transmitter.setConnectionTypeRaw(side, next);
        transmitter.refreshConnections();
        transmitter.notifyTileChange();
        tile.sendUpdatePacket();

        player.displayClientMessage(MekanismLang.CONNECTION_TYPE.translateColored(EnumColor.GRAY, next), true);
    }
}
