package dev.codeonigiri.mekanism_pipe_tweaks.network;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import dev.codeonigiri.mekanism_pipe_tweaks.connection.SideCycleLogic;
import dev.codeonigiri.mekanism_pipe_tweaks.placement.TransmitterPlacementLogic;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = MekanismPipeTweaks.MOD_ID)
public final class ModPayloads {
    private ModPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CycleSidePayload.TYPE, CycleSidePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        SideCycleLogic.apply(player, payload.pos(), payload.side());
                    }
                });
        registrar.playToServer(PlaceTransmitterPayload.TYPE, PlaceTransmitterPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        TransmitterPlacementLogic.placeSelected(player, payload.targetPos(), payload.chosenSide(),
                                payload.clickedFace(), payload.hand());
                    }
                });
    }
}
