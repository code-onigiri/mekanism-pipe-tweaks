package dev.codeonigiri.mekanism_pipe_tweaks.event;

import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import dev.codeonigiri.mekanism_pipe_tweaks.transmitter.Transmitters;
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

/**
 * Server-side event entry points for the placement rules and the guide's connection cycling.
 */
public final class ServerEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getFace() == null) {
            return;
        }
        Player player = event.getEntity();

        // Shift + configurator on a governed transmitter cycles the hovered cell's connection type.
        // Cancelling here also skips the guard and click recording below, so a cycle click is never
        // mistaken for a placement click.
        if (player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof ItemConfigurator) {
            TileEntityTransmitter tile = Transmitters.tileAt(level, event.getPos());
            Transmitter<?, ?, ?> transmitter = tile == null ? null : tile.getTransmitter();
            if (transmitter != null && Transmitters.isEnabled(transmitter)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        Direction face = event.getFace();
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
