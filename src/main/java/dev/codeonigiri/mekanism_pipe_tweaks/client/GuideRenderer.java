package dev.codeonigiri.mekanism_pipe_tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import mekanism.common.lib.transmitter.ConnectionType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class GuideRenderer {
    private static final double GRID_OFFSET = 0.002;
    private static final double ICON_OFFSET = 0.004;
    private static final double GRID_HALF_THICKNESS = 0.006;
    private static final double ICON_HALF = 0.08;
    private static final int NON_HOVERED_ALPHA = 0x44;
    private static final int HOVERED_ALPHA = 0xFF;

    @SubscribeEvent
    public void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        // Render from the expanded client raycast instead of the vanilla hit result, so the guide
        // appears even when the crosshair is outside the thin transmitter silhouette.
        GuideTarget target = GuideTarget.resolve(level, player);
        if (target == null) {
            return;
        }

        BlockPos pos = target.pos();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        GuideGeometry.Basis basis = GuideGeometry.basis(target.face());
        Vec3 right = basis.right();
        Vec3 up = basis.up();
        Vec3 normal = basis.normal();

        Vec3 blockCentre = new Vec3(0.5, 0.5, 0.5);
        Vec3 gridCentre = blockCentre.add(normal.scale(0.5 + GRID_OFFSET));
        Vec3 iconCentre = blockCentre.add(normal.scale(0.5 + ICON_OFFSET));

        drawGrid(buffers, pose, gridCentre, right, up);
        drawCellIcons(buffers, pose, level, target, iconCentre, right, up);

        poseStack.popPose();
        flush(buffers);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        // The guide replaces the vanilla outline; the thin pipe outline (or an unrelated block
        // hit behind it) must not be drawn while the guide target is active.
        if (GuideTarget.resolve(minecraft.level, minecraft.player) != null) {
            event.setCanceled(true);
        }
    }

    private static void flush(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(RenderType.guiOverlay());
        for (ResourceLocation icon : GuideTextures.ICONS) {
            buffers.endBatch(RenderType.textSeeThrough(icon));
        }
    }

    private static void drawGrid(MultiBufferSource buffers, PoseStack.Pose pose, Vec3 centre, Vec3 right, Vec3 up) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.guiOverlay());
        int argb = gridArgb();
        for (double line : new double[]{-GuideGeometry.CELL_BOUNDARY, GuideGeometry.CELL_BOUNDARY}) {
            addColoredQuad(consumer, pose, centre, right, up,
                    line - GRID_HALF_THICKNESS, line + GRID_HALF_THICKNESS, -0.5, 0.5, argb);
            addColoredQuad(consumer, pose, centre, right, up,
                    -0.5, 0.5, line - GRID_HALF_THICKNESS, line + GRID_HALF_THICKNESS, argb);
        }
    }

    /**
     * Draw data for one cell of the 3x3 face grid. {@code icon} is {@code null} when the cell has
     * nothing to show (placement mode only marks empty neighbouring positions).
     */
    private record CellVisual(GuideGeometry.Cell cell, ResourceLocation icon, int alpha, Vec3 centre) {
    }

    private static void drawCellIcons(MultiBufferSource buffers, PoseStack.Pose pose, Level level,
                                      GuideTarget target, Vec3 iconCentre, Vec3 right, Vec3 up) {
        for (CellVisual cell : cellVisuals(level, target, iconCentre, right, up)) {
            if (cell.icon() == null) {
                continue;
            }
            VertexConsumer consumer = buffers.getBuffer(RenderType.textSeeThrough(cell.icon()));
            addTexturedQuad(consumer, pose, cell.centre(), right, up, ICON_HALF, ICON_HALF, 0, 0, 1, 1,
                    (cell.alpha() << 24) | 0xFFFFFF);
        }
    }

    private static List<CellVisual> cellVisuals(Level level, GuideTarget target, Vec3 iconCentre, Vec3 right, Vec3 up) {
        Direction face = target.face();
        GuideGeometry.Cell hovered = target.cell();
        List<CellVisual> cells = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Direction side = GuideGeometry.sideFor(face, col, row);
                int alpha = col == hovered.col() && row == hovered.row() ? HOVERED_ALPHA : NON_HOVERED_ALPHA;
                cells.add(new CellVisual(new GuideGeometry.Cell(side, col, row), iconFor(level, target, side), alpha,
                        cellCentre(iconCentre, right, up, col, row)));
            }
        }
        return cells;
    }

    private static Vec3 cellCentre(Vec3 faceCentre, Vec3 right, Vec3 up, int col, int row) {
        return faceCentre.add(right.scale(GuideGeometry.cellU(col))).add(up.scale(GuideGeometry.cellV(row)));
    }

    /**
     * The two GT states. With the Configurator every side is either connected or disconnected;
     * PUSH/PULL are only Mekanism-internal variants of "connected" and are shown as connected.
     * While holding a transmitter, only empty neighbouring positions get the connect icon: those
     * are the sides a new transmitter can be placed on.
     */
    private static ResourceLocation iconFor(Level level, GuideTarget target, Direction side) {
        if (target.mode() == GuideTarget.Mode.PLACEMENT) {
            return level.isEmptyBlock(target.pos().relative(side)) ? GuideTextures.CONNECT : null;
        }
        ConnectionType type = target.transmitter().getConnectionTypeRaw(side);
        return type == null || type == ConnectionType.NONE ? GuideTextures.BLOCK : GuideTextures.CONNECT;
    }

    private static void addTexturedQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 centre,
                                        Vec3 right, Vec3 up, double halfWidth, double halfHeight,
                                        float u0, float v0, float u1, float v1, int argb) {
        Vec3 bottomLeft = centre.add(right.scale(-halfWidth)).add(up.scale(-halfHeight));
        Vec3 bottomRight = centre.add(right.scale(halfWidth)).add(up.scale(-halfHeight));
        Vec3 topRight = centre.add(right.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 topLeft = centre.add(right.scale(-halfWidth)).add(up.scale(halfHeight));
        texturedVertex(consumer, pose, bottomLeft, u0, v1, argb);
        texturedVertex(consumer, pose, bottomRight, u1, v1, argb);
        texturedVertex(consumer, pose, topRight, u1, v0, argb);
        texturedVertex(consumer, pose, topLeft, u0, v0, argb);
    }

    private static void texturedVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position,
                                       float u, float v, int argb) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static void addColoredQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 centre,
                                       Vec3 right, Vec3 up, double u0, double u1, double v0, double v1, int argb) {
        Vec3 bottomLeft = centre.add(right.scale(u0)).add(up.scale(v0));
        Vec3 bottomRight = centre.add(right.scale(u1)).add(up.scale(v0));
        Vec3 topRight = centre.add(right.scale(u1)).add(up.scale(v1));
        Vec3 topLeft = centre.add(right.scale(u0)).add(up.scale(v1));
        coloredVertex(consumer, pose, bottomLeft, argb);
        coloredVertex(consumer, pose, bottomRight, argb);
        coloredVertex(consumer, pose, topRight, argb);
        coloredVertex(consumer, pose, topLeft, argb);
    }

    private static void coloredVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, int argb) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z).setColor(argb);
    }

    private static int gridArgb() {
        double phase = (Util.getMillis() % 3000L) / 3000.0 * Math.PI * 2.0;
        float wave = (float) Math.sin(phase);
        float red = 0.2F + 0.2F * wave;
        float green = 0.2F + 0.2F * wave;
        float alpha = 0.55F + 0.2F * wave;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int r = Mth.clamp((int) (red * 255.0F), 0, 255);
        int g = Mth.clamp((int) (green * 255.0F), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | 0xFF;
    }
}
