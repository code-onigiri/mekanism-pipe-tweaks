package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSideMessage;
import dev.codeonigiri.mekanism_pipe_tweaks.network.ModNetwork;
import dev.codeonigiri.mekanism_pipe_tweaks.network.PlaceTransmitterMessage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GuideInteractionHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        // Use the expanded full-cube raycast, not event.getHitVec(): the vanilla ray misses the
        // thin pipe whenever the crosshair is outside its silhouette.
        GuideTarget target = GuideTarget.resolve(event.getLevel(), player);
        if (target == null) {
            return;
        }
        if (target.mode() == GuideTarget.Mode.CONFIGURATOR) {
            if (!player.isShiftKeyDown()) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            ModNetwork.CHANNEL.sendToServer(new CycleSideMessage(target.pos(), target.cell().side()));
            return;
        }
        PlaceTransmitterMessage message = placementMessage(target, event.getHand());
        if (message == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        ModNetwork.CHANNEL.sendToServer(message);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        GuideTarget target = GuideTarget.resolve(event.getLevel(), event.getEntity());
        if (target == null) {
            return;
        }
        // The vanilla ray can be a plain item use (air / entity) while the expanded ray still
        // points at a transmitter; intercept the placement in that case too.
        PlaceTransmitterMessage message = placementMessage(target, event.getHand());
        if (message == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        ModNetwork.CHANNEL.sendToServer(message);
    }

    /**
     * The cell-select placement to send for a click on {@code target}, or {@code null} when the
     * click must fall through to vanilla. The centre cell is the clicked face itself, so it is
     * left to the vanilla placement path.
     */
    private static PlaceTransmitterMessage placementMessage(GuideTarget target, InteractionHand hand) {
        if (target.mode() != GuideTarget.Mode.PLACEMENT) {
            return null;
        }
        GuideGeometry.Cell cell = target.cell();
        if (cell.side() == target.face()) {
            return null;
        }
        return new PlaceTransmitterMessage(target.pos(), cell.side(), target.face(), hand);
    }
}
