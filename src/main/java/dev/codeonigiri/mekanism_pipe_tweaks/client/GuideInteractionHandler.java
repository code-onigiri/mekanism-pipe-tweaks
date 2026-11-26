package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.network.CycleSidePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side click handling for the guide. Placement is pure vanilla; the only interaction the
 * mod intercepts is the Configurator's right-click connection cycle.
 *
 * <p>The click is intercepted at the input stage rather than in {@code RightClickBlock}: vanilla
 * only fires that event when the crosshair is on a block, so aiming at the outer guide cells
 * (outside the thin 0.5-wide transmitter core) would never reach the mod. Cancelling the input
 * event here also stops {@code startUseItem} before it sends its use-on packet, so the server runs
 * neither the vanilla placement nor Mekanism's own Configurator action. The cycle therefore works
 * with the crosshair anywhere on the block face, and sneak is not required.
 */
public final class GuideInteractionHandler {
    // startUseItem is re-run every 4 ticks while the use key is held; allow one cycle per press.
    private boolean cycledThisPress;

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
        event.setCanceled(true);
        if (cycledThisPress) {
            event.setSwingHand(false);
            return;
        }
        cycledThisPress = true;
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new CycleSidePayload(target.pos(), target.cell().side()));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().options.keyUse.isDown()) {
            cycledThisPress = false;
        }
    }
}
