package dev.codeonigiri.mekanism_pipe_tweaks.event;

import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import dev.codeonigiri.mekanism_pipe_tweaks.tool.TransmitterItems;
import mekanism.common.item.ItemConfigurator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Server-side event entry points for the placement rules.
 *
 * <p>Connection cycling is driven entirely by {@code CycleSidePayload}; the client cancels the
 * right-click at the input stage, so no use-on packet reaches the server for a cycle click. A
 * Configurator click is therefore never recorded as a placement click.
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
        // A Configurator (in any mode) does not place a transmitter, so its click must not seed the
        // placement matching used to open the clicked side.
        if (player.getMainHandItem().getItem() instanceof ItemConfigurator) {
            return;
        }
        TransmitterPlacementLogic.recordClick(player.getUUID(), level.dimension(), event.getPos(), event.getFace(),
                TransmitterItems.blockOf(player.getMainHandItem()), level.getGameTime());
    }

    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TransmitterPlacementLogic.onBlockPlaced(level, event.getPos(), event.getEntity(), event.getPlacedAgainst());
        }
    }
}
