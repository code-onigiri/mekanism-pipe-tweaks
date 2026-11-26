package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class GuideTextures {
    /** A side that is connected, or (in placement mode) an empty side a transmitter can go on. */
    public static final ResourceLocation CONNECT = texture("tool_pipe_connect");
    /** A side that is disconnected. Placement mode draws nothing on occupied sides. */
    public static final ResourceLocation BLOCK = texture("tool_pipe_block");

    /** The cell icons, in the order the renderer flushes their batches. */
    public static final List<ResourceLocation> ICONS = List.of(CONNECT, BLOCK);

    private GuideTextures() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MekanismPipeTweaks.MOD_ID, "textures/gui/overlay/" + name + ".png");
    }
}
