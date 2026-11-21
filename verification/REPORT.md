# Mekanism Pipe Tweaks — in-game verification report

Date: 2026-11-20/21
Target: Minecraft 1.21.1 · NeoForge 21.1.264 · Mekanism 10.7.19 · mod 0.1.0
Method: temporary client-side `AutoTest` harness, dedicated server + dev client under Xvfb `:99`
(1280x720, Mesa llvmpipe), driven from outside with a stdlib Python RCON client.

All four screenshots were produced by an unmodified mod build; the harness only
performs ordinary player actions (item selection, `gameMode.useItemOn`, teleport,
sneak packets) and captures the main render target.

---

## 1. Screenshots

### screenshot-1-guide.png — GregTech 3x3 guide

The player stands 1.2 blocks west of the anchor pipe at `(14, 4, 14)`, holding the
Configurator, with the crosshair exactly on the pipe's west face centre.

* Client hit result: `pos=14,4,14 face=west location=(14.25, 4.50005, 14.5)`
  → `target cell=west col=1 row=1 mode=CONFIGURATOR`.
* The 3x3 grid is drawn on the whole block face (grid lines at ±0.25, icons at
  ±0.375), exactly as `docs/DESIGN.md` §5.1 describes. The hovered centre icon is
  at 100% opacity, the other eight at 27%. No cell backgrounds, no white frames.
* The left/right columns extend past the (thin) pipe core onto the arm/air, which
  is the intended GT behaviour: the grid describes the *block face*, not the pipe.
* The hotbar shows the selected Configurator, proving the display condition.

Note: the pipe core is 0.5 blocks wide (the hit is exactly on `x=14.25`), so the
icons at ±0.375 sit outside the pipe model — expected, same as GT.

### screenshot-2-placement.png — placed pipe connects only toward the clicked side

Scene: anchor pipe A `(14,4,14)` (placed with `/setblock`, i.e. vanilla placement),
the pipe C the player placed `(14,4,15)`, and an unrelated pipe D `(14,4,16)`
(also `/setblock`, all six faces `NORMAL` and directly adjacent to C).

The player held `basic_mechanical_pipe`, faced A's south face and called
`gameMode.useItemOn`. Server-side connection data read back from the client tile:

```
anchor (14,4,14): down=NORMAL/NONE up=NORMAL/NONE north=NORMAL/NONE
                  south=NORMAL/NORMAL west=NORMAL/NONE east=NORMAL/NONE
placed (14,4,15): down=NONE/NONE up=NONE/NONE north=NORMAL/NORMAL
                  south=NONE/NONE west=NONE/NONE east=NONE/NONE
extra  (14,4,16): down=NORMAL/NONE up=NORMAL/NONE north=NORMAL/NONE
                  south=NORMAL/NONE west=NORMAL/NONE east=NORMAL/NONE
```

So C has exactly one configured and effective connection (north, toward A), the
counterpart side of A was opened symmetrically, and C did **not** auto-connect to
the adjacent pipe D even though D's raw connection is `NORMAL` on that side
(mutual-consent check works). In the image the middle green band is the single
connection arm; the pipe on the right is D and has no arm.

### screenshot-3-cycle.png — sneak + right-click cycles the looked-at side

The player sneaks (real `PRESS_SHIFT_KEY`/`RELEASE_SHIFT_KEY` packets plus
`player.input.shiftKeyDown`, which is what `LocalPlayer.isShiftKeyDown()` reads)
and right-clicks the same west face.

* `(14,4,14) west` changed `NORMAL → PUSH` (raw/effective `PUSH/NONE`, since there
  is no neighbour on that side).
* The guide's centre icon switches to the PUSH texture: the radiation marks are
  now blue and the small white direction triangle is drawn.
* The action bar shows the mod's own feedback: **“Connection type changed to: Push”**.

### screenshot-4-cell-place.png — cell-select placement (best effort)

Holding the pipe item again, the harness hovers the **top cell** of the guide
(`cell side=up col=1 row=0`) on A's west face and activates it. Result:

```
anchor  (14,4,14): ... up=NORMAL/NORMAL   (counterpart side opened)
new pipe(14,5,14): down=NORMAL/NORMAL, all other faces NONE/NONE
```

The pipe was placed **above** A (the selected cell's direction), not on the
clicked west side, and it connects only downward — i.e. the cell-selection
placement path works server-side. The camera view also shows the placement guide
(held pipe item) on A.

Important caveat (historical, fixed in §4): this original result was produced with a
**synthesised** hit position (`y = 4.875`, i.e. `v = +0.375`). A real player could not
reach a non-centre cell at the time — see finding B below. The §4 fix round re-captured
this screenshot using only a real crosshair aim.

---

## 2. Phase log (final run)

```
world_ready player=Dev ... fov=70
setup_done pipeSlot=1 configuratorSlot=0 emptySlot=3
phase -> PLACE_TP     arrived at (15.5, 3.0, 16.8)
placement click       anchor=(14,4,14) face=SOUTH item=mekanism:basic_mechanical_pipe
placement useItemOn -> SUCCESS
phase -> PLACE_SETTLE placed block at (14,4,15): mekanism:basic_mechanical_pipe transmitter
phase -> PLACE_VIEW_TP / PLACE_VIEW   screenshot-2-placement.png
phase -> GUIDE_TP     guide position (13.3, 3.0, 14.5) holding mekanism:configurator
guide hitResult       face=west location=(14.25, 4.50005, 14.5) cell=west col=1 row=1
                      screenshot-1-guide.png
cycle                 raw before=NORMAL, sneaking=true
cycle useItemOn -> SUCCESS      cycle result: anchor west raw=PUSH
                      screenshot-3-cycle.png
cell placement        synthetic hover ... cell side=up col=1 row=0
cell placement useItemOn -> SUCCESS
cell placed at (14,5,14)   screenshot-4-cell-place.png
phase -> DONE         failure=none cycleChanged=true cellPlaced=true
```

Full logs: `verification/logs/client-latest.log`, `server.log`, `rcon-driver.log`.

---

## 3. Findings / failures

**A. Block ids in the brief do not exist in Mekanism 10.7.**
`mekanism:mechanical_pipe` and `mekanism:universal_cable` are not registered;
Mekanism 10.7 uses tiered ids. `/setblock` reported
`Unknown block type 'mekanism:mechanical_pipe'`. The harness used
`mekanism:basic_mechanical_pipe` / `mekanism:basic_universal_cable`
(confirmed against `assets/mekanism/lang/en_us.json`). Worth remembering for
README/examples.

**B. Cell-select placement is unreachable with a real crosshair (bug / spec gap).**
The transmitter core is 0.5 blocks wide, so the hit position on a face can only
have `|u|,|v| <= 0.25`. `GuideGeometry.CELL_BOUNDARY` is exactly `0.25` and the
tests are `u < -0.25` / `u > 0.25`, so `cellAt()` always returns `col=1,row=1`
(the looked-at face). `GuideInteractionHandler` then returns early because
`cell.side() == face`. Net effect: a player can never select a non-centre cell,
so the GT-style cell placement of §5.1 cannot be triggered by real input.
Fix options: expand the transmitter selection shape to the full block while the
guide item is held (as DESIGN §5.1 already calls for, but which is not
implemented), or derive the cell from the crosshair ray against the face plane
instead of the pipe hit position. Screenshot 4 therefore required a synthetic
hit vector to exercise the server-side path.

**C. Sneak state must come from the input, not `Entity.setShiftKeyDown`.**
`LocalPlayer.isShiftKeyDown()` returns `input.shiftKeyDown`, and `aiStep()`
overwrites the entity flag every tick, so the harness must set
`player.input.shiftKeyDown` (and send `ServerboundPlayerCommandPacket`
PRESS/RELEASE_SHIFT_KEY). Without the packets the server still sees a normal
right-click: the server-side `RightClickBlock` cancel is skipped and the
Configurator performs its own sneak action (“Redstone sensitivity turned on”
appeared in the action bar instead of the mod message). With them, the
cycle works through the real client handler
(`GuideInteractionHandler` → `CycleSidePayload`) and the mod's action-bar
message is shown.

**D. Intermittent double placement from the cell click (observed in iterations 3–4).**
In two earlier runs, the cell-select click also produced a vanilla-placed pipe at
the clicked face, `(13,4,14)`, with NBT
`connection: [3,3,3,3,3,0]` (only EAST = `NORMAL`, i.e. toward the anchor).
That is exactly what `TransmitterPlacementLogic.applyOpen(openSide=EAST)`
produces for a click recorded on the anchor's west face. Cause: the client-side
event cancel in `GuideInteractionHandler` does not stop
`MultiPlayerGameMode.startPrediction()` from sending `ServerboundUseItemOnPacket`
(see `MultiPlayerGameMode.useItemOn`/`startPrediction` — the packet is always
sent), so the server runs the vanilla placement in addition to the custom
`PlaceTransmitterPayload`. In the final run this did not reproduce, so it is
probably ordering/timing dependent, but the code path remains. Suggested fix:
have the client skip the packet when the guide intercepts the click (or make the
server-side handler ignore a right-click that carries the guide's synthetic
cell hit), and/or reject the vanilla placement when a `PlaceTransmitterPayload`
already handled it.

**E. Environment notes (not mod issues).**
* NeoForm Runtime’s asset downloader uses `http://resources.download.minecraft.net`,
  which Mojang now answers with `AccountRequiresHttps`; the download hangs. The
  asset cache was pre-populated over HTTPS (all 3913 index entries, 148 large
  music files fetched with retries), after which the launch tasks ran normally.
* Two long-running Gradle `run*` tasks cannot share one `GRADLE_USER_HOME`
  (journal lock), so the server was started directly from its generated command
  line (`build/moddev/testServerRunVmArgs.txt` + `testServerRunProgramArgs.txt`)
  after the first build; the client kept using the Gradle run task.
* The dedicated-server launcher logs `No key layers in MapLike[{}]` for the
  flat world preset; harmless in 1.21.1.

---

## 4. Fix round (2026-11-21): bugs B and D closed

### 4.1 What changed (event-based, no Mixin)

**Bug B — real-crosshair cell selection.**

* New client-only `GuideTargeting` performs a `ClipContext` ray from the player's eye
  along the look vector up to `Player.blockInteractionRange()`, with a custom shape
  getter that returns `Shapes.block()` for `BlockTransmitter` blocks and the normal
  outline shape for everything else. No block shape (collision or selection) is changed.
* `GuideTarget` now resolves from that expanded raycast instead of the vanilla hit, so
  the guide target/face/cell use the full face plane (`|u|,|v|` up to 0.5). The nearest
  hit still wins, and a normal block in front of a transmitter hides it.
* `GuideRenderer` draws the grid from `RenderLevelStageEvent.AFTER_ENTITIES` using the
  same expanded target, so the grid appears and the hovered cell follows the crosshair
  even when the vanilla ray misses the thin pipe. The vanilla outline is cancelled while
  a guide target is active.
* `GuideInteractionHandler` resolves the expanded target in both
  `RightClickBlock` and `RightClickItem`. It sends `PlaceTransmitterPayload` whenever the
  expanded hit selects a redirected cell, covering (a) the vanilla ray hitting a farther
  block and (b) the vanilla ray being a plain item use. The Configurator cycle path is
  unchanged: it still uses the hovered cell side and requires sneak.

**Bug D — one-shot server guard.**

* `PlaceTransmitterPayload` now also carries the actually-clicked face
  (`target.face()` from the expanded hit).
* When the payload handler successfully places (`InteractionResult.consumesAction()`),
  `TransmitterPlacementLogic` records a one-shot guard: player UUID, target position,
  clicked face, expiry `gameTime + 10`.
* The server `PlayerInteractEvent.RightClickBlock` handler (HIGHEST) checks the guard
  before recording a click. On a matching live mark it consumes the mark, cancels the
  event completely (block and item use denied) and writes no click record. On a
  non-match or after expiry the click proceeds normally. A failed payload sets no mark.

### 4.2 Retest method

The temporary `AutoTest` harness was recreated (then removed again) and driven exactly
as in §2: dedicated server started from its generated command line, dev client under
Xvfb `:99` at 1280x720, RCON driver for world setup/teleports/server-side block checks.
The cell phases use only a real crosshair aim and the normal `gameMode` path: the
harness rotates the player, reads `Minecraft.hitResult`, and calls `gameMode.useItemOn`
when it is a block or `gameMode.useItem` when it is a miss. No synthetic hit vector is
passed into the mod.

* Cell aim: player eye at `(12.6, 4.62, 14.5)` looking through `(14.0, 4.875, 14.5)` —
  the top cell of the anchor's west face, above the 0.5-wide pipe core.
* Five repeats at fresh anchors `(14,4,20/26/32/38/44)`, same top-cell aim.
* Guard exercise at `(14,4,50)`: player eye `(12.6, 5.62, 50.5)` aiming at
  `(14.0, 4.76, 50.5)`. The expanded ray selects the top cell, but the steeper ray also
  clips the thin core, so the vanilla path is `RightClickBlock` and the vanilla
  `ServerboundUseItemOnPacket` is sent — this is the exact double-placement race.

### 4.3 Retest evidence

Phase log (`verification/logs/rcon-driver-fix.log`, client `client-fix.log`,
server `server-fix.log`):

```
world_ready / setup_done pipeSlot=1 configuratorSlot=0
placement   hit=BLOCK face=south location=(14.361,4.426,14.750) useItemOn -> SUCCESS
guide       vanillaHit=BLOCK face=west location=(14.250,4.457,14.500)
            expanded=cell=west col=1 row=1 mode=CONFIGURATOR
cycle       sneaking=true useItemOn -> SUCCESS, anchor west raw NORMAL -> PUSH
cell aim    vanillaHit=MISS expanded=cell=up col=1 row=0 mode=PLACEMENT
cell place  (RightClickItem) -> SUCCESS
repeat[0..4] vanillaHit=MISS expanded=cell=up col=1 row=0 -> SUCCESS
guard aim   vanillaHit=BLOCK pos=(14,4,50) face=west location=(14.250,4.607,50.500)
            expanded=cell=up col=1 row=0
guard place (RightClickBlock) -> SUCCESS
```

Server-side RCON checks (`execute if block … mekanism:basic_mechanical_pipe`):

```
CHECK screenshot anchor (14,4,14): PIPE   cell (14,5,14): PIPE   duplicate (13,4,14): air
CHECK repeat0..4   anchor: PIPE           cell (14,5,z): PIPE    duplicate (13,4,z): air   (z=20..44)
CHECK guard        anchor (14,4,50): PIPE cell (14,5,50): PIPE   duplicate (13,4,50): air
```

The cell pipe connections read back as
`cell (14,5,14): down=NORMAL/NORMAL, all other faces NONE/NONE`, and the anchor's
counterpart `up` became `NORMAL/NORMAL`:

```
anchor-after-cell (14,4,14): down=NORMAL/NONE up=NORMAL/NORMAL north=NORMAL/NONE
                             south=NORMAL/NORMAL west=PUSH/NONE east=NORMAL/NONE
cell-placed      (14,5,14): down=NORMAL/NORMAL up=NONE/NONE north=NONE/NONE
                             south=NONE/NONE west=NONE/NONE east=NONE/NONE
```

The guard run matters because the client did send the vanilla use-on packet for
`(14,4,50)/WEST`; without the guard the server would have placed a second pipe at
`(13,4,50)`. That position remained air, so the guard consumed the mark and cancelled
the vanilla placement.

Screenshots were re-captured at 1280x720: `screenshot-1-guide.png` (3x3 grid on the
whole face, centre hovered), `screenshot-2-placement.png` (single configured connection,
still no auto-connect to the adjacent pipe), `screenshot-3-cycle.png` (blue PUSH
radiation marks + “Connection type changed to: Push”), and
`screenshot-4-cell-place.png` (real crosshair on the top cell; new pipe above the
anchor, grid top cell highlighted).

### 4.4 Status

* **B — closed.** A real crosshair can select all nine cells; the top-cell placement was
  reached via `RightClickItem` on a vanilla miss, and the guide follows the expanded hit.
* **D — closed.** The guard was directly exercised (vanilla `RightClickBlock` on the pipe
  core + injected redirect) and no duplicate appeared; five additional real-crosshair
  cell placements also produced exactly one pipe each.

## 5. Cleanup

The harness and both test runs were removed, `run-test-server/` and `run-test-client/`
were deleted, and a clean `gradle clean build` was run for this fix round.

* `build/libs/mekanism_pipe_tweaks-0.1.0.jar` — 53 entries,
  **no `autotest` classes** (`unzip -l … | grep -i autotest` → empty) and
  **no Mixin config** (`unzip -l … | grep -i mixin` → empty). The new
  `GuideTargeting` / `GuardRecord` classes are present.
* The fixed-run logs were preserved as `verification/logs/client-fix.log`,
  `server-fix.log` and `rcon-driver-fix.log`. Screenshots, this report, `logs/`, the
  RCON driver and the asset helper remain under `verification/`.
* The temporary `build.gradle` run tweaks (`--quickPlayMultiplayer`,
  `-Dmpt.autotest`) and the `run-test-*` game directories were reverted/deleted.

Repro helpers kept for reference: `verification/rcon_driver.py`,
`verification/fetch_assets.py`.
