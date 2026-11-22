package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSideMessage;
import dev.codeonigiri.mekanism_pipe_tweaks.network.ModNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        ModNetwork.CHANNEL.sendToServer(new CycleSideMessage(target.pos(), target.cell().side()));
    }
}
