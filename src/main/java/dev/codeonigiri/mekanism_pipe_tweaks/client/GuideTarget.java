package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
import java.util.Objects;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public record GuideTarget(BlockPos pos, Direction face, GuideGeometry.Cell cell, Transmitter<?, ?, ?> transmitter) {
    public GuideTarget {
        Objects.requireNonNull(cell);
        Objects.requireNonNull(transmitter);
    }

    public static GuideTarget resolve(Level level, Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemConfigurator)) {
            return null;
        }
        return resolve(level, GuideTargeting.raycast(level, player));
    }

    private static GuideTarget resolve(Level level, BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof BlockTransmitter)) {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof TileEntityTransmitter tile)) {
            return null;
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter == null) {
            return null;
        }
        if (!Transmitters.isEnabled(transmitter)) {
            return null;
        }
        // The expanded ray lands on the full block face, so |u| and |v| can reach 0.5 and all
        // nine cells of the face plane are reachable. The cell is derived from that hit.
        GuideGeometry.Cell cell = GuideGeometry.cellAt(hit.getDirection(), hit.getLocation(), pos);
        return cell == null ? null : new GuideTarget(pos, hit.getDirection(), cell, transmitter);
    }
}
