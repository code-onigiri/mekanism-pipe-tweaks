package dev.codeonigiri.mekanism_pipe_tweaks.client;

import mekanism.common.block.transmitter.BlockTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Client-side targeting helper. Transmitters keep their thin vanilla selection shape, so the
 * ordinary block pick can only land on the 0.5-wide core and never reaches the outer cells of
 * a face. For guide purposes we clip the same look ray against a full block cube for transmitter
 * blocks (and against the normal outline shape for everything else). This does not change any
 * block shape; it is only a client-side raycast used to derive the guide target and the cell.
 */
public final class GuideTargeting {
    private GuideTargeting() {
    }

    public static BlockHitResult raycast(Level level, Player player) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        return level.clip(new FullCubeTransmitterContext(from, to, player));
    }

    private static final class FullCubeTransmitterContext extends ClipContext {
        private FullCubeTransmitterContext(Vec3 from, Vec3 to, Player player) {
            super(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        }

        @Override
        public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
            if (state.getBlock() instanceof BlockTransmitter<?>) {
                return Shapes.block();
            }
            return super.getBlockShape(state, level, pos);
        }
    }
}
