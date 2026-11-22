package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSidePayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side click handling for the guide. Placement is pure vanilla; the only interaction the
 * mod intercepts is the Configurator's sneak + right-click connection cycle.
 */
public final class GuideInteractionHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }
        // Use the expanded full-cube raycast, not event.getHitVec(): the vanilla ray misses the
        // thin pipe whenever the crosshair is outside its silhouette.
        GuideTarget target = GuideTarget.resolve(event.getLevel(), player);
        if (target == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        PacketDistributor.sendToServer(new CycleSidePayload(target.pos(), target.cell().side()));
    }
}
