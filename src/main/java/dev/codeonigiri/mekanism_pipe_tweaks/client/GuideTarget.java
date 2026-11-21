package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.util.TransmitterSupport;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public record GuideTarget(BlockPos pos, Direction face, GuideGeometry.Cell cell, Transmitter<?, ?, ?> transmitter,
                          Mode mode) {
    public enum Mode {
        CONFIGURATOR,
        PLACEMENT
    }

    public static GuideTarget resolve(Level level, Player player) {
        ItemStack held = player.getMainHandItem();
        Mode mode = modeFor(held);
        if (mode == null) {
            return null;
        }
        return resolve(level, held, mode, GuideTargeting.raycast(level, player));
    }

    private static GuideTarget resolve(Level level, ItemStack held, Mode mode, BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof BlockTransmitter<?>)) {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof TileEntityTransmitter tile)) {
            return null;
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter == null) {
            return null;
        }
        if (mode == Mode.CONFIGURATOR) {
            if (!TransmitterSupport.isEnabled(transmitter)) {
                return null;
            }
        } else if (!(held.getItem() instanceof BlockItem blockItem) || !TransmitterSupport.isEnabled(blockItem.getBlock())) {
            return null;
        }
        // The expanded ray lands on the full block face, so |u| and |v| can reach 0.5 and all
        // nine cells of the face plane are reachable. The cell is derived from that hit.
        GuideGeometry.Cell cell = GuideGeometry.cellAt(hit.getDirection(), hit.getLocation(), pos);
        return cell == null ? null : new GuideTarget(pos, hit.getDirection(), cell, transmitter, mode);
    }

    private static Mode modeFor(ItemStack held) {
        if (held.getItem() instanceof ItemConfigurator) {
            return Mode.CONFIGURATOR;
        }
        if (held.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockTransmitter<?>) {
            return Mode.PLACEMENT;
        }
        return null;
    }
}
