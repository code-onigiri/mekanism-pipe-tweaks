package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSidePayload;
import dev.codeonigiri.mekanism_pipe_tweaks.network.PlaceTransmitterPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
            PacketDistributor.sendToServer(new CycleSidePayload(target.pos(), target.cell().side()));
            return;
        }
        GuideGeometry.Cell cell = target.cell();
        if (cell == null || cell.side() == target.face()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        PacketDistributor.sendToServer(new PlaceTransmitterPayload(
                target.pos(), cell.side(), target.face(), event.getHand()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        GuideTarget target = GuideTarget.resolve(event.getLevel(), player);
        if (target == null || target.mode() != GuideTarget.Mode.PLACEMENT) {
            return;
        }
        GuideGeometry.Cell cell = target.cell();
        // The vanilla ray can be a plain item use (air / entity) while the expanded ray still
        // points at a transmitter; intercept the placement in that case too.
        if (cell == null || cell.side() == target.face()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        PacketDistributor.sendToServer(new PlaceTransmitterPayload(
                target.pos(), cell.side(), target.face(), event.getHand()));
    }
}
