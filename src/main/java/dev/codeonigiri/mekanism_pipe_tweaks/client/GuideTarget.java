package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.tool.Configurators;
import dev.codeonigiri.mekanism_pipe_tweaks.tool.TransmitterItems;
import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
import java.util.Objects;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What the guide is currently pointed at, and why it is shown.
 *
 * <p>{@link Mode#CONFIGURATOR} means the held Configurator changes the hovered side's connection.
 * {@link Mode#PLACEMENT} means the held transmitter item places a new transmitter at the hovered
 * grid cell, connected to the looked-at transmitter (GregTech's pipe placement guide).
 */
public record GuideTarget(BlockPos pos, Direction face, GuideGeometry.Cell cell,
                          Transmitter<?, ?, ?> transmitter, Mode mode) {
    public enum Mode {
        CONFIGURATOR,
        PLACEMENT
    }

    public GuideTarget {
        Objects.requireNonNull(cell);
        Objects.requireNonNull(transmitter);
        Objects.requireNonNull(mode);
    }

    public static GuideTarget resolve(Level level, Player player) {
        ItemStack held = player.getMainHandItem();
        Mode mode;
        BlockTransmitter<?> heldBlock = null;
        if (Configurators.isCycling(held)) {
            mode = Mode.CONFIGURATOR;
        } else if ((heldBlock = TransmitterItems.blockOf(held)) != null) {
            mode = Mode.PLACEMENT;
        } else {
            return null;
        }
        GuideTarget target = resolve(level, GuideTargeting.raycast(level, player), mode);
        if (target == null) {
            return null;
        }
        // The placement guide is only for a held transmitter of the same genre as the target.
        if (mode == Mode.PLACEMENT && !TransmitterItems.sameGenre(heldBlock, target.transmitter())) {
            return null;
        }
        return target;
    }

    private static GuideTarget resolve(Level level, BlockHitResult hit, Mode mode) {
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
        if (!Transmitters.isEnabled(transmitter)) {
            return null;
        }
        // The expanded ray lands on the full block face, so |u| and |v| can reach 0.5 and all
        // nine cells of the face plane are reachable. The cell is derived from that hit.
        GuideGeometry.Cell cell = GuideGeometry.cellAt(hit.getDirection(), hit.getLocation(), pos);
        return cell == null ? null : new GuideTarget(pos, hit.getDirection(), cell, transmitter, mode);
    }
}
