package dev.codeonigiri.mekanism_pipe_tweaks.autotest;

import com.mojang.blaze3d.platform.NativeImage;
import dev.codeonigiri.mekanism_pipe_tweaks.client.GuideTarget;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TEMPORARY verification harness for the 1.20.1 port (v0.2.0 spec). Drives ordinary player
 * actions and reports results on the client log as MPT_AUTOTEST markers. Enabled only with
 * -Dmpt.autotest=true.
 *
 * <p>Every click goes through {@code MultiPlayerGameMode#useItemOn}, the same path a real
 * player's right-click takes (it fires Forge's RightClickBlock event first); nothing in the mod
 * is called directly. Screenshots read the main render target, so they contain the guide overlay
 * exactly as the normal renderer draws it.
 */
public final class AutoTest {
    private static final Logger LOG = LoggerFactory.getLogger("MPT-AutoTest");
    private static final File SCREENSHOT_DIR = new File("/home/hatch/workspace/mekanism-pipe-tweaks-1201/verification");

    private static final String CONFIGURATOR = "mekanism:configurator";
    private static final String PIPE = "mekanism:basic_mechanical_pipe";

    /** Wall the first pipe is placed onto; its south face is the placement target. */
    private static final BlockPos WALL = new BlockPos(14, -60, 19);
    /** First pipe, placed by the player onto the wall's south face (non-sneak -> all NONE). */
    private static final BlockPos ANCHOR = new BlockPos(14, -60, 20);
    /** Second pipe, placed by the player onto the anchor's south face. */
    private static final BlockPos PLACED = new BlockPos(14, -60, 21);

    /** The flat test world's surface is the grass layer at y=-61, so feet rest at y=-60. */
    private static final double FEET = -60.0;

    private static final Vec3 STAND_SOUTH = new Vec3(14.5, FEET, 22.6);
    /** Wall south face; plain stone, so the placed pipe must be all-NONE. */
    private static final Vec3 AIM_WALL_SOUTH = new Vec3(14.5, -59.5, 20.0);
    /** Anchor south face, centre cell (clicked face SOUTH). */
    private static final Vec3 AIM_ANCHOR_SOUTH = new Vec3(14.5, -59.5, 21.0);
    private static final Vec3 STAND_SOUTH_FAR = new Vec3(14.5, FEET, 23.4);
    /** Placed south face, centre cell (col 1 row 1 -> side SOUTH). */
    private static final Vec3 AIM_PLACED_SOUTH = new Vec3(14.5, -59.5, 22.0);

    /** Close view of the anchor's west face for the guide screenshots. */
    private static final Vec3 STAND_WEST = new Vec3(12.8, FEET, 20.5);
    /** Anchor west face, centre cell. */
    private static final Vec3 AIM_ANCHOR_WEST = new Vec3(14.0, -59.5, 20.5);

    /** South-west three-quarter view framing both placed pipes. */
    private static final Vec3 STAND_PAIR = new Vec3(12.2, FEET, 22.2);
    private static final Vec3 AIM_PAIR = new Vec3(14.5, -59.5, 20.5);

    private enum State {
        NEED_SETUP,
        NEED_TP,
        NEED_AIM,
        NEED_CLICK,
        NEED_SHOT,
        DONE
    }

    /**
     * One verification scene. All fields are optional except {@code label}.
     * {@code setup} is a named world setup forwarded to the RCON driver; {@code hold} and
     * {@code holdAfter} are item ids, or {@code "empty"} for an empty hotbar slot.
     */
    private record Scene(String label, String setup, Vec3 tp, Vec3 aim, String hold, int clicks, boolean sneak,
                         String holdAfter, String shot) {
    }

    private final List<Scene> scenes = new ArrayList<>();
    private int sceneIndex;
    private State state = State.NEED_SETUP;
    private boolean worldReady;
    private boolean shotAimed;
    private int tpTries;
    private int heartbeat;
    private int wait = 40;
    private boolean failure;
    private boolean anchorChecked;
    private boolean nonsneakChecked;
    private boolean sneakChecked;
    private boolean cycleChecked;
    private boolean guideChecked;
    private boolean noGuideChecked;

    public AutoTest() {
        buildScenes();
    }

    private void buildScenes() {
        scenes.add(new Scene("setup", "base", STAND_SOUTH, null, null, 0, false, null, null));

        // 1. Place the first pipe against a plain stone wall (non-sneak): every side NONE.
        scenes.add(new Scene("place-anchor", null, STAND_SOUTH, AIM_WALL_SOUTH, PIPE, 1, false, null, null));
        scenes.add(new Scene("anchor-readback", null, STAND_PAIR, AIM_PAIR, "empty", 0, false, "empty",
                "screenshot-1201-spec-anchor-all-none.png"));

        // 2. Guide: shown with the Configurator, absent with a transmitter item in hand.
        scenes.add(new Scene("guide-configurator", null, STAND_WEST, AIM_ANCHOR_WEST, CONFIGURATOR, 0, false,
                CONFIGURATOR, "screenshot-1201-spec-guide-configurator.png"));
        scenes.add(new Scene("guide-transmitter-item", null, STAND_WEST, AIM_ANCHOR_WEST, PIPE, 0, false,
                PIPE, "screenshot-1201-spec-guide-transmitter-item.png"));

        // 3. (a) Non-sneak placement against the transmitter: placed all NONE, anchor unchanged.
        scenes.add(new Scene("place-nonsneak", null, STAND_SOUTH, AIM_ANCHOR_SOUTH, PIPE, 1, false, null, null));
        scenes.add(new Scene("nonsneak-readback", null, STAND_PAIR, AIM_PAIR, "empty", 0, false, "empty",
                "screenshot-1201-spec-nonsneak-placement.png"));

        // 4. (b) Sneak placement against the transmitter: clicked side NORMAL, counterpart opened.
        scenes.add(new Scene("reset-placed", "reset-placed", STAND_SOUTH, null, null, 0, false, null, null));
        scenes.add(new Scene("place-sneak", null, STAND_SOUTH, AIM_ANCHOR_SOUTH, PIPE, 1, true, null, null));
        scenes.add(new Scene("sneak-readback", null, STAND_PAIR, AIM_PAIR, "empty", 0, false, "empty",
                "screenshot-1201-spec-sneak-placement.png"));

        // 5. (d) Configurator sneak + right-click cycling still works: SOUTH NONE -> NORMAL -> PUSH.
        scenes.add(new Scene("cycle-push", null, STAND_SOUTH_FAR, AIM_PLACED_SOUTH, CONFIGURATOR, 2, true,
                null, "screenshot-1201-spec-cycle-push.png"));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            marker("RESPAWN after death");
            player.respawn();
            wait = 60;
            return;
        }
        if (!worldReady) {
            if (minecraft.screen == null) {
                worldReady = true;
                marker("world_ready player=" + player.getGameProfile().getName()
                        + " pos=(" + format(player.getX()) + ", " + format(player.getY()) + ", "
                        + format(player.getZ()) + ") level=" + minecraft.level.dimension().location());
                logHotbar(player);
                wait = 40;
            } else if (++heartbeat % 40 == 0) {
                marker("waiting screen=" + minecraft.screen.getClass().getSimpleName()
                        + " pos=(" + format(player.getX()) + ", " + format(player.getY()) + ", "
                        + format(player.getZ()) + ")");
            }
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (state == State.DONE) {
            return;
        }
        Scene scene = scenes.get(sceneIndex);
        switch (state) {
            case NEED_SETUP -> {
                if (scene.setup() != null) {
                    marker("SETUP " + scene.setup());
                    state = State.NEED_TP;
                    wait = 45;
                } else {
                    state = State.NEED_TP;
                }
            }
            case NEED_TP -> {
                if (scene.tp() != null) {
                    marker(String.format(Locale.ROOT, "TP %.3f %.3f %.3f", scene.tp().x, scene.tp().y, scene.tp().z));
                    aim(player, scene.aim());
                    state = State.NEED_AIM;
                    wait = 50;
                } else {
                    state = State.NEED_AIM;
                }
            }
            case NEED_AIM -> {
                if (scene.tp() != null && !arrived(player, scene.tp())) {
                    if (tpTries++ > 30) {
                        marker("TP_FAILED scene=" + scene.label() + " pos=(" + format(player.getX()) + ", "
                                + format(player.getY()) + ", " + format(player.getZ()) + ")");
                        failure = true;
                        advance();
                        return;
                    }
                    if (wait <= 0) {
                        marker(String.format(Locale.ROOT, "TP %.3f %.3f %.3f",
                                scene.tp().x, scene.tp().y, scene.tp().z));
                    }
                    wait = 20;
                    return;
                }
                tpTries = 0;
                select(player, scene.hold());
                if (scene.aim() != null) {
                    aim(player, scene.aim());
                    minecraft.gameRenderer.pick(1.0F);
                }
                state = State.NEED_CLICK;
                wait = scene.clicks() > 0 ? 8 : 1;
            }
            case NEED_CLICK -> {
                if (scene.clicks() > 0) {
                    performClicks(minecraft, player, scene);
                    wait = 45;
                }
                state = State.NEED_SHOT;
            }
            case NEED_SHOT -> {
                if (scene.tp() != null && !arrived(player, scene.tp())) {
                    if (tpTries++ > 30) {
                        marker("TP_FAILED scene=" + scene.label() + " (shot)");
                        failure = true;
                        advance();
                        return;
                    }
                    if (wait <= 0) {
                        marker(String.format(Locale.ROOT, "TP %.3f %.3f %.3f",
                                scene.tp().x, scene.tp().y, scene.tp().z));
                    }
                    wait = 20;
                    return;
                }
                tpTries = 0;
                if (!shotAimed) {
                    select(player, scene.holdAfter());
                    if (scene.aim() != null) {
                        aim(player, scene.aim());
                        minecraft.gameRenderer.pick(1.0F);
                    }
                    shotAimed = true;
                    wait = 6;
                    return;
                }
                shotAimed = false;
                marker("SCENE " + scene.label() + " hit=" + describe(minecraft.hitResult)
                        + " guide=" + describeTarget(minecraft, player, scene));
                logReadback(minecraft, scene.label());
                if (scene.shot() != null) {
                    capture(minecraft, scene.shot());
                }
                advance();
            }
            default -> {
            }
        }
    }

    private static boolean arrived(LocalPlayer player, Vec3 target) {
        return Math.abs(player.getX() - target.x) < 0.35
                && Math.abs(player.getY() - target.y) < 0.35
                && Math.abs(player.getZ() - target.z) < 0.35;
    }

    private void performClicks(Minecraft minecraft, LocalPlayer player, Scene scene) {
        for (int i = 0; i < scene.clicks(); i++) {
            select(player, scene.hold());
            aim(player, scene.aim());
            minecraft.gameRenderer.pick(1.0F);
            HitResult hit = minecraft.hitResult;
            GuideTarget guide = GuideTarget.resolve(minecraft.level, player);
            if (scene.sneak()) {
                startSneak(player);
            }
            BlockHitResult blockHit = hit instanceof BlockHitResult bhr ? bhr : null;
            ConnectionType before = blockHit == null ? null
                    : rawType(minecraft, blockHit.getBlockPos(), blockHit.getDirection());
            InteractionResult result = blockHit == null
                    ? minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND)
                    : minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, blockHit);
            marker("CLICK scene=" + scene.label() + " #" + (i + 1)
                    + " path=" + (blockHit == null ? "RightClickItem" : "RightClickBlock")
                    + " hit=" + describe(hit)
                    + " guide=" + describeTarget(guide)
                    + " item=" + itemId(player.getMainHandItem())
                    + " rawBefore=" + before + " sneak=" + scene.sneak() + " -> " + result);
            if (scene.sneak()) {
                releaseSneak(player);
            }
            if (result == InteractionResult.FAIL) {
                marker("CLICK_FAILED scene=" + scene.label() + " #" + (i + 1));
                failure = true;
            }
        }
    }

    private void advance() {
        sceneIndex++;
        tpTries = 0;
        shotAimed = false;
        if (sceneIndex >= scenes.size()) {
            marker("finished failure=" + (failure ? "yes" : "none")
                    + " anchorChecked=" + anchorChecked
                    + " nonsneakChecked=" + nonsneakChecked
                    + " sneakChecked=" + sneakChecked
                    + " cycleChecked=" + cycleChecked
                    + " guideChecked=" + guideChecked
                    + " noGuideChecked=" + noGuideChecked);
            state = State.DONE;
            return;
        }
        state = State.NEED_SETUP;
        wait = 10;
    }

    private void logReadback(Minecraft minecraft, String label) {
        StringBuilder builder = new StringBuilder("READBACK ").append(label);
        for (BlockPos pos : List.of(ANCHOR, PLACED)) {
            builder.append(" | ").append(pos).append(' ').append(connections(minecraft, pos));
        }
        marker(builder.toString());

        if (!anchorChecked && "anchor-readback".equals(label)) {
            anchorChecked = true;
            boolean allNone = allNone(minecraft, ANCHOR);
            boolean ok = isMechanicalPipe(minecraft, ANCHOR) && allNone;
            marker("ANCHOR_RESULT allSidesNone=" + allNone + " ok=" + ok);
            if (!ok) {
                failure = true;
            }
        }

        if (!guideChecked && "guide-configurator".equals(label)) {
            guideChecked = true;
            boolean present = GuideTarget.resolve(minecraft.level, minecraft.player) != null;
            marker("GUIDE_CONFIGURATOR_RESULT guidePresent=" + present + " ok=" + present);
            if (!present) {
                failure = true;
            }
        }

        if (!noGuideChecked && "guide-transmitter-item".equals(label)) {
            noGuideChecked = true;
            boolean absent = GuideTarget.resolve(minecraft.level, minecraft.player) == null;
            marker("GUIDE_TRANSMITTER_ITEM_RESULT guideAbsent=" + absent + " ok=" + absent);
            if (!absent) {
                failure = true;
            }
        }

        if (!nonsneakChecked && "nonsneak-readback".equals(label)) {
            nonsneakChecked = true;
            boolean placedAllNone = isMechanicalPipe(minecraft, PLACED) && allNone(minecraft, PLACED);
            boolean anchorUnchanged = isMechanicalPipe(minecraft, ANCHOR) && allNone(minecraft, ANCHOR);
            boolean ok = placedAllNone && anchorUnchanged;
            marker("NONSNEAK_RESULT placedAllNone=" + placedAllNone
                    + " anchorAllNone=" + anchorUnchanged + " ok=" + ok);
            if (!ok) {
                failure = true;
            }
        }

        if (!sneakChecked && "sneak-readback".equals(label)) {
            sneakChecked = true;
            ConnectionType placedNorth = rawType(minecraft, PLACED, Direction.NORTH);
            ConnectionType anchorSouth = rawType(minecraft, ANCHOR, Direction.SOUTH);
            boolean placedOnlyNorth = allNoneExcept(minecraft, PLACED, Direction.NORTH);
            boolean anchorOnlySouth = allNoneExcept(minecraft, ANCHOR, Direction.SOUTH);
            boolean ok = isMechanicalPipe(minecraft, PLACED) && isMechanicalPipe(minecraft, ANCHOR)
                    && placedNorth == ConnectionType.NORMAL
                    && anchorSouth == ConnectionType.NORMAL
                    && placedOnlyNorth && anchorOnlySouth;
            marker("SNEAK_RESULT placedNorth=" + placedNorth + " anchorSouth=" + anchorSouth
                    + " placedOnlyNorth=" + placedOnlyNorth + " anchorOnlySouth=" + anchorOnlySouth
                    + " ok=" + ok);
            if (!ok) {
                failure = true;
            }
        }

        if (!cycleChecked && "cycle-push".equals(label)) {
            cycleChecked = true;
            ConnectionType south = rawType(minecraft, PLACED, Direction.SOUTH);
            marker("CYCLE_RESULT centreSouth=" + south + " ok=" + (south == ConnectionType.PUSH));
            if (south != ConnectionType.PUSH) {
                failure = true;
            }
        }
    }

    private static void logHotbar(LocalPlayer player) {
        StringBuilder builder = new StringBuilder("HOTBAR");
        for (int slot = 0; slot < 9; slot++) {
            builder.append(' ').append(slot).append('=').append(itemId(player.getInventory().getItem(slot)));
        }
        marker(builder.toString());
    }

    private static void select(LocalPlayer player, String id) {
        if (id == null) {
            return;
        }
        if ("empty".equals(id)) {
            for (int slot = 0; slot < 9; slot++) {
                if (player.getInventory().getItem(slot).isEmpty()) {
                    player.getInventory().selected = slot;
                    return;
                }
            }
            return;
        }
        int slot = slotFor(player, id);
        if (slot >= 0) {
            player.getInventory().selected = slot;
        }
    }

    private static int slotFor(LocalPlayer player, String id) {
        for (int slot = 0; slot < 9; slot++) {
            if (id.equals(itemId(player.getInventory().getItem(slot)))) {
                return slot;
            }
        }
        return -1;
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private void capture(Minecraft minecraft, String name) {
        try {
            NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
            File file = new File(SCREENSHOT_DIR, name);
            image.writeToFile(file);
            int width = image.getWidth();
            int height = image.getHeight();
            image.close();
            marker("screenshot saved " + file + " (" + width + "x" + height + ")");
        } catch (IOException | RuntimeException exception) {
            marker("screenshot FAILED " + exception);
            failure = true;
        }
    }

    private static void aim(LocalPlayer player, Vec3 target) {
        if (target == null) {
            return;
        }
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 delta = target.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        float pitch = (float) (Math.toDegrees(Math.atan2(-delta.y, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
    }

    private static void startSneak(LocalPlayer player) {
        player.input.shiftKeyDown = true;
        player.setShiftKeyDown(true);
        player.connection.send(new ServerboundPlayerCommandPacket(player,
                ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
    }

    private static void releaseSneak(LocalPlayer player) {
        player.input.shiftKeyDown = false;
        player.setShiftKeyDown(false);
        player.connection.send(new ServerboundPlayerCommandPacket(player,
                ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
    }

    private static String connections(Minecraft minecraft, BlockPos pos) {
        if (!(minecraft.level.getBlockEntity(pos) instanceof TileEntityTransmitter tile)
                || tile.getTransmitter() == null) {
            return "none";
        }
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        StringBuilder builder = new StringBuilder();
        for (Direction direction : Direction.values()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(direction.getName()).append('=')
                    .append(configured(transmitter, direction)).append('/')
                    .append(transmitter.getConnectionType(direction));
        }
        return builder.toString();
    }

    private static ConnectionType rawType(Minecraft minecraft, BlockPos pos, Direction side) {
        if (!(minecraft.level.getBlockEntity(pos) instanceof TileEntityTransmitter tile)
                || tile.getTransmitter() == null) {
            return null;
        }
        return configured(tile.getTransmitter(), side);
    }

    private static boolean allNone(Minecraft minecraft, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (rawType(minecraft, pos, direction) != ConnectionType.NONE) {
                return false;
            }
        }
        return true;
    }

    private static boolean allNoneExcept(Minecraft minecraft, BlockPos pos, Direction except) {
        for (Direction direction : Direction.values()) {
            if (direction != except && rawType(minecraft, pos, direction) != ConnectionType.NONE) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMechanicalPipe(Minecraft minecraft, BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        return "mekanism:basic_mechanical_pipe".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static ConnectionType configured(Transmitter<?, ?, ?> transmitter, Direction side) {
        ConnectionType type = transmitter.getConnectionTypeRaw(side);
        return type == null ? ConnectionType.NONE : type;
    }

    private static String describe(HitResult hit) {
        if (hit instanceof BlockHitResult blockHit) {
            return "BLOCK pos=" + blockHit.getBlockPos() + " face=" + blockHit.getDirection().getName()
                    + " loc=(" + format(blockHit.getLocation().x) + ", " + format(blockHit.getLocation().y) + ", "
                    + format(blockHit.getLocation().z) + ")";
        }
        return hit == null ? "null" : hit.getType().name();
    }

    private static String describeTarget(Minecraft minecraft, LocalPlayer player, Scene scene) {
        if (scene.holdAfter() == null && scene.hold() == null) {
            return "n/a";
        }
        return describeTarget(GuideTarget.resolve(minecraft.level, player));
    }

    private static String describeTarget(GuideTarget target) {
        if (target == null) {
            return "null";
        }
        return "pos=" + target.pos() + " face=" + target.face().getName()
                + " cell=" + target.cell().side().getName() + " col=" + target.cell().col()
                + " row=" + target.cell().row();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void marker(String message) {
        LOG.info("MPT_AUTOTEST {}", message);
    }
}
