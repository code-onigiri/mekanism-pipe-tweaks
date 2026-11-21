package dev.codeonigiri.mekanism_pipe_tweaks.event;

import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import dev.codeonigiri.mekanism_pipe_tweaks.util.TransmitterSupport;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class NeoForgeEventHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuideCycleRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getFace() == null) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !(player.getMainHandItem().getItem() instanceof ItemConfigurator)) {
            return;
        }
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof BlockTransmitter<?>)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof TileEntityTransmitter tile)) {
            return;
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter == null || !TransmitterSupport.isEnabled(transmitter)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Direction face = event.getFace();
        if (face == null) {
            return;
        }
        Player player = event.getEntity();
        // A successful PlaceTransmitterPayload leaves a one-shot mark. The matching vanilla
        // use-on packet still arrives, so deny it completely before it can place a second pipe
        // and before a click record is written.
        if (TransmitterPlacementLogic.consumeGuard(player.getUUID(), event.getPos(), face, level.getGameTime())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        TransmitterPlacementLogic.recordClick(player.getUUID(), level.dimension(), event.getPos(), face, level.getGameTime());
    }

    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TransmitterPlacementLogic.onBlockPlaced(level, event.getPos(), event.getEntity(), event.getPlacedAgainst());
        }
    }
}
