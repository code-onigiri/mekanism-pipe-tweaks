package dev.codeonigiri.mekanism_pipe_tweaks.placement;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
import dev.codeonigiri.mekanism_pipe_tweaks.tool.TransmitterItems;
import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Placement rules. Ordinary placement is vanilla; this mod only rewrites the placed transmitter's
 * connection state afterwards.
 *
 * <p>A player-placed transmitter starts with every side {@link ConnectionType#NONE}. Two things
 * open the side facing the clicked block ({@code NORMAL}): placing while sneaking, and placing a
 * transmitter against a same-genre transmitter (GregTech connects pipes on placement regardless of
 * sneaking). When the clicked block is a transmitter, its counterpart side is opened as well. The
 * click is recorded server-side so the placed block can be matched to the face the player actually
 * clicked.
 *
 * <p>Holding a transmitter also shows the placement guide; {@link #placeSelected} performs the
 * server-side placement for a click on one of its grid cells.
 */
public final class TransmitterPlacementLogic {
    private static final int CLICK_TIMEOUT_TICKS = 40;
    private static final Map<UUID, ClickRecord> CLICKS = new HashMap<>();

    private TransmitterPlacementLogic() {
    }

    public static void recordClick(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, Direction face,
                                   BlockTransmitter<?> heldTransmitter, long gameTime) {
        CLICKS.put(playerId, new ClickRecord(dimension, pos.immutable(), face, heldTransmitter, gameTime));
    }

    public static void removeClick(UUID playerId) {
        CLICKS.remove(playerId);
    }

    /**
     * Server side of the placement guide: place the held transmitter at the grid cell
     * {@code chosenSide} of the transmitter at {@code targetPos}. GregTech falls back to the face
     * the player actually looked at when the chosen cell has no room.
     */
    public static void placeSelected(ServerPlayer player, BlockPos targetPos, Direction chosenSide, Direction clickedFace) {
        ItemStack stack = player.getMainHandItem();
        BlockTransmitter<?> held = TransmitterItems.blockOf(stack);
        if (held == null || !Transmitters.isEnabled(held)) {
            return;
        }
        if (!player.canInteractWithBlock(targetPos, 1.0)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        TileEntityTransmitter targetTile = Transmitters.tileAt(level, targetPos);
        if (targetTile == null) {
            return;
        }
        Transmitter<?, ?, ?> target = targetTile.getTransmitter();
        if (target == null || !Transmitters.isEnabled(target) || !TransmitterItems.sameGenre(held, target)) {
            return;
        }

        Direction face = level.isEmptyBlock(targetPos.relative(chosenSide)) ? chosenSide : clickedFace;
        BlockPos placedPos = targetPos.relative(face);
        if (!level.isEmptyBlock(placedPos) || !player.canInteractWithBlock(placedPos, 1.0)) {
            return;
        }

        Vec3 hitLocation = Vec3.atCenterOf(targetPos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        BlockHitResult hit = new BlockHitResult(hitLocation, face, targetPos, false);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        recordClick(player.getUUID(), level.dimension(), targetPos, face, held, level.getGameTime());
        // ItemStack#useOn is the server-side path that fires BlockEvent.EntityPlaceEvent after
        // placement, so the normal post-placement connection logic runs unchanged.
        InteractionResult result = stack.useOn(context);
        if (!result.consumesAction()) {
            removeClick(player.getUUID());
        }
    }

    public static void onBlockPlaced(ServerLevel level, BlockPos placedPos, Entity placer, BlockState placedAgainst) {
        TileEntityTransmitter placed = Transmitters.tileAt(level, placedPos);
        if (placed == null) {
            return;
        }
        Transmitter<?, ?, ?> transmitter = placed.getTransmitter();
        if (transmitter == null) {
            return;
        }
        if (!Transmitters.isEnabled(transmitter)) {
            return;
        }

        if (placer instanceof Player player) {
            ClickRecord click = takeMatchingClick(player.getUUID(), level, placedPos);
            if (click == null) {
                applyNonPlayerPlacement(level, placedPos, placed, placedAgainst);
                return;
            }
            // GregTech connects a placed pipe to a same-genre pipe regardless of sneaking; other
            // placements keep the mod's sneak-to-connect rule.
            if ((MPTConfig.CONNECT_PLACED_PIPE.get() && connectsToClickedTransmitter(level, click))
                    || (player.isShiftKeyDown() && MPTConfig.CONNECT_CLICKED_SIDE.get())) {
                applyOpen(level, placedPos, placed, click.face().getOpposite());
            } else {
                disconnectAllSides(placed);
            }
        } else {
            applyNonPlayerPlacement(level, placedPos, placed, placedAgainst);
        }
    }

    private static boolean connectsToClickedTransmitter(ServerLevel level, ClickRecord click) {
        if (click.heldTransmitter() == null) {
            return false;
        }
        TileEntityTransmitter clickedTile = Transmitters.tileAt(level, click.pos());
        if (clickedTile == null) {
            return false;
        }
        Transmitter<?, ?, ?> clicked = clickedTile.getTransmitter();
        return clicked != null && Transmitters.isEnabled(clicked)
                && TransmitterItems.sameGenre(click.heldTransmitter(), clicked);
    }

    private static void applyNonPlayerPlacement(ServerLevel level, BlockPos placedPos, TileEntityTransmitter placed, BlockState placedAgainst) {
        switch (MPTConfig.NON_PLAYER_PLACEMENT.get()) {
            case VANILLA -> {
            }
            case NONE -> disconnectAllSides(placed);
            case CLICKED -> {
                Direction openSide = MPTConfig.CONNECT_CLICKED_SIDE.get() ? openSideOf(level, placedPos, placedAgainst) : null;
                if (openSide == null) {
                    disconnectAllSides(placed);
                } else {
                    applyOpen(level, placedPos, placed, openSide);
                }
            }
        }
    }

    private static void applyOpen(ServerLevel level, BlockPos placedPos, TileEntityTransmitter placed, Direction openSide) {
        Transmitter<?, ?, ?> transmitter = placed.getTransmitter();
        BlockEntity neighbour = level.getBlockEntity(placedPos.relative(openSide));
        boolean connectClickedSide = (neighbour instanceof TileEntityTransmitter || transmitter.getAcceptor(openSide) != null);
        setConnectionTypes(transmitter, connectClickedSide ? openSide : null);

        if (connectClickedSide && MPTConfig.OPEN_COUNTERPART_SIDE.get() && neighbour instanceof TileEntityTransmitter counterpart) {
            Transmitter<?, ?, ?> counterpartTransmitter = counterpart.getTransmitter();
            Direction counterpartSide = openSide.getOpposite();
            if (counterpartTransmitter != null && Transmitters.isEnabled(counterpartTransmitter)
                    && counterpartTransmitter.getConnectionTypeRaw(counterpartSide) == ConnectionType.NONE) {
                counterpartTransmitter.setConnectionTypeRaw(counterpartSide, ConnectionType.NORMAL);
                Transmitters.refresh(counterpart);
            }
        }
        Transmitters.refresh(placed);
    }

    private static void disconnectAllSides(TileEntityTransmitter placed) {
        setConnectionTypes(placed.getTransmitter(), null);
        Transmitters.refresh(placed);
    }

    private static void setConnectionTypes(Transmitter<?, ?, ?> transmitter, Direction openSide) {
        for (Direction side : Direction.values()) {
            transmitter.setConnectionTypeRaw(side, side == openSide ? ConnectionType.NORMAL : ConnectionType.NONE);
        }
    }

    private static Direction openSideOf(ServerLevel level, BlockPos placedPos, BlockState placedAgainst) {
        if (placedAgainst == null || placedAgainst.isAir()) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(placedPos.relative(direction)).equals(placedAgainst)) {
                return direction;
            }
        }
        return null;
    }

    private static ClickRecord takeMatchingClick(UUID playerId, ServerLevel level, BlockPos placedPos) {
        ClickRecord click = CLICKS.get(playerId);
        if (click == null) {
            return null;
        }
        if (level.getGameTime() - click.gameTime() > CLICK_TIMEOUT_TICKS || !click.dimension().equals(level.dimension())) {
            CLICKS.remove(playerId);
            return null;
        }
        if (!placedPos.equals(click.pos().relative(click.face()))) {
            return null;
        }
        CLICKS.remove(playerId);
        return click;
    }

    private record ClickRecord(ResourceKey<Level> dimension, BlockPos pos, Direction face,
                               BlockTransmitter<?> heldTransmitter, long gameTime) {
    }
}
