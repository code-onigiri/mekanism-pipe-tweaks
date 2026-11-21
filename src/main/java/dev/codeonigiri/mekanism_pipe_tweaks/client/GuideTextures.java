package dev.codeonigiri.mekanism_pipe_tweaks.client;

import dev.codeonigiri.mekanism_pipe_tweaks.MekanismPipeTweaks;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class GuideTextures {
    public static final ResourceLocation CONNECT = texture("tool_pipe_connect");
    public static final ResourceLocation BLOCK = texture("tool_pipe_block");
    public static final ResourceLocation PUSH = texture("tool_pipe_connect_push");
    public static final ResourceLocation PULL = texture("tool_pipe_connect_pull");
    public static final ResourceLocation TRIANGLE = texture("tool_triangle");

    /** The per-state cell icons, in the order the renderer flushes their batches. */
    public static final List<ResourceLocation> ICONS = List.of(CONNECT, BLOCK, PUSH, PULL);

    private GuideTextures() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MekanismPipeTweaks.MOD_ID, "textures/gui/overlay/" + name + ".png");
    }
}
