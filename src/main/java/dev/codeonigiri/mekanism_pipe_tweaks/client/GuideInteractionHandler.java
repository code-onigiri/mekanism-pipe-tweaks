package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSideMessage;
import dev.codeonigiri.mekanism_pipe_tweaks.network.ModNetwork;
import dev.codeonigiri.mekanism_pipe_tweaks.network.PlaceTransmitterMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side click handling for the guide.
 *
 * <p>Two things can be intercepted, both at the input stage rather than in {@code RightClickBlock}:
 * the Configurator's connection toggle, and the placement of a held transmitter onto a grid cell.
 * Vanilla only fires {@code RightClickBlock} when the crosshair is on a block, so aiming at the
 * outer guide cells (outside the thin 0.5-wide transmitter core) would never reach the mod.
 * Cancelling the input event here also stops {@code startUseItem} before it sends its use-on
 * packet, so the server runs neither the vanilla placement nor Mekanism's own action. Both
 * interactions therefore work with the crosshair anywhere on the block face, and sneak is not
 * required.
 */
public final class GuideInteractionHandler {
    // startUseItem is re-run every 4 ticks while the use key is held; allow one toggle per press.
    private boolean toggledThisPress;

    @SubscribeEvent
    public void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        // The input stage runs before vanilla picks what the click hits. Leave entity interactions
        // alone so a mob/item frame in front of a pipe is not robbed of the click.
        if (minecraft.hitResult instanceof EntityHitResult) {
            return;
        }
        // Full-cube raycast, not the vanilla hit: the vanilla ray misses the thin pipe whenever the
        // crosshair is outside its silhouette.
        GuideTarget target = GuideTarget.resolve(level, player);
        if (target == null) {
            return;
        }
        if (target.mode() == GuideTarget.Mode.PLACEMENT) {
            // Holding a transmitter: place it at the hovered grid cell, connected to the target.
            // Cancelling at the input stage stops the vanilla use-on packet, so the server only
            // runs the single placement below.
            event.setCanceled(true);
            event.setSwingHand(true);
            ModNetwork.CHANNEL.sendToServer(new PlaceTransmitterMessage(
                    target.pos(), target.cell().side(), target.face()));
            return;
        }
        event.setCanceled(true);
        if (toggledThisPress) {
            event.setSwingHand(false);
            return;
        }
        toggledThisPress = true;
        event.setSwingHand(true);
        ModNetwork.CHANNEL.sendToServer(new CycleSideMessage(target.pos(), target.cell().side()));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().options.keyUse.isDown()) {
            toggledThisPress = false;
        }
    }
}
