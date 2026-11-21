package dev.codeonigiri.mekanism_pipe_tweaks.placement;

import dev.codeonigiri.mekanism_pipe_tweaks.config.MPTConfig;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TransmitterPlacementLogic {
    private static final int CLICK_TIMEOUT_TICKS = 40;
    private static final int GUARD_TIMEOUT_TICKS = 10;
    private static final Map<UUID, ClickRecord> CLICKS = new HashMap<>();
    private static final Map<UUID, GuardRecord> GUARDS = new HashMap<>();

    private TransmitterPlacementLogic() {
    }

    /**
     * Consumes a one-shot guard mark written by a successful {@link #placeSelected} call. The
     * client cancel does not always stop the vanilla use-on packet, so the matching vanilla
     * right-click must be denied once before it can place a second pipe or record a click.
     */
    public static boolean consumeGuard(UUID playerId, BlockPos pos, Direction face, long gameTime) {
        GuardRecord guard = GUARDS.get(playerId);
        if (guard == null) {
            return false;
        }
        if (gameTime > guard.expiry()) {
            GUARDS.remove(playerId);
            return false;
        }
        if (guard.pos().equals(pos) && guard.face() == face) {
            GUARDS.remove(playerId);
            return true;
        }
        return false;
    }

    public static void recordClick(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, Direction face, long gameTime) {
        CLICKS.put(playerId, new ClickRecord(dimension, pos.immutable(), face, gameTime));
    }

    public static void removeClick(UUID playerId) {
        CLICKS.remove(playerId);
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
        ClickRecord click = placer instanceof Player player ? takeMatchingClick(player.getUUID(), level, placedPos) : null;
        if (!Transmitters.isEnabled(transmitter)) {
            return;
        }

        if (click != null) {
            applyOpen(level, placedPos, placed, click.face().getOpposite());
        } else {
            applyNonPlayerPlacement(level, placedPos, placed, placedAgainst);
        }
    }

    public static void placeSelected(ServerPlayer player, BlockPos targetPos, Direction chosenSide,
                                     Direction clickedFace, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof BlockTransmitter<?>)) {
            return;
        }
        if (!player.canInteractWithBlock(targetPos, 1.0)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (Transmitters.tileAt(level, targetPos) == null) {
            return;
        }
        if (!Transmitters.isEnabled(block)) {
            return;
        }

        Vec3 hitLocation = Vec3.atCenterOf(targetPos).add(Vec3.atLowerCornerOf(chosenSide.getNormal()).scale(0.5));
        BlockHitResult hit = new BlockHitResult(hitLocation, chosenSide, targetPos, false);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, hand, hit));

        recordClick(player.getUUID(), level.dimension(), targetPos, chosenSide, level.getGameTime());
        // ItemStack#useOn is the server-side path that fires BlockEvent.EntityPlaceEvent after placement.
        InteractionResult result = stack.useOn(context);
        if (result.consumesAction()) {
            GUARDS.put(player.getUUID(),
                    new GuardRecord(targetPos.immutable(), clickedFace, level.getGameTime() + GUARD_TIMEOUT_TICKS));
        } else {
            removeClick(player.getUUID());
        }
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
        boolean connectClickedSide = MPTConfig.CONNECT_CLICKED_SIDE.get()
                && (neighbour instanceof TileEntityTransmitter || transmitter.getAcceptor(openSide) != null);
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

    private record ClickRecord(ResourceKey<Level> dimension, BlockPos pos, Direction face, long gameTime) {
    }

    private record GuardRecord(BlockPos pos, Direction face, long expiry) {
    }
}
