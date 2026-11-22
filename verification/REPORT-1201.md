# Verification report — Minecraft 1.20.1 port (branch `1.20.1`)

Port of Mekanism Pipe Tweaks 0.1.1 from Minecraft 1.21.1 / NeoForge / Mekanism 10.7
to **Minecraft 1.20.1 / MinecraftForge 47.4.0 / Mekanism 10.4.16 / Java 17**,
built with ForgeGradle 6. Feature parity with the 1.21.1 build (see REPORT.md)
was the requirement; the mod version stays 0.1.1.

## Platform adaptations

- Build: ModDevGradle replaced by ForgeGradle 6 (`net.minecraftforge.gradle`
  `[6.0,6.2)`), official mappings, `META-INF/mods.toml` in Forge format
  (`javafml`, loader `[47,)`), pack_format 15.
- Networking: the 1.21.1 payload system does not exist on Forge 1.20.1, so the
  two messages (cycle-side, place-transmitter) were reimplemented on a Forge
  `SimpleChannel` (`network/ModNetwork.java`) with the same fields and
  server-side semantics.
- Config: `ForgeConfigSpec` instead of NeoForge's `ModConfigSpec`; all keys,
  defaults and semantics unchanged.
- Events, registries and rendering hooks moved from `net.neoforged.*` to the
  `net.minecraftforge.*` equivalents; 1.20.1 API differences (e.g.
  `ResourceLocation` constructors) adapted.
- Mekanism 10.4.16: the transmitter API surface used by the mod
  (`Transmitter`, `TileEntityTransmitter`, `BlockTransmitter`,
  `ConnectionType`, `getConnectionTypeRaw`, `getSupportedTransmissionTypes`,
  `ItemConfigurator`) is present with the same semantics as 10.7, so the
  audit property holds unchanged: decisions only via Mekanism base classes
  and public API, no block-id lists.

## The login ConcurrentModificationException and its fix

During the first verification attempt the dev client was disconnected during
login / Forge registry sync:

```
Dev (/127.0.0.1) lost connection: Internal Exception: java.util.ConcurrentModificationException
```

(server log, 2026-11-21 18:12; sync had just passed `minecraft:block`.)

The fix present in the final tree: the `SimpleChannel` is created via
`NetworkRegistry.newSimpleChannel(...)` and **all messages are registered from
the mod constructor** — `MekanismPipeTweaks.<init>` calls
`ModNetwork.register()` — i.e. channel setup happens during mod construction,
not lazily or during play. After this change, clients log in and join normally
(server log 2026-11-21 19:38 and all later sessions: `Dev joined the game`).

## In-game verification (2026-11-21, final session 23:23–23:26)

Method as in REPORT.md §2: dedicated server, dev client under Xvfb `:99` at
1280x720, RCON driver, temporary AutoTest harness gated by `-Dmpt.autotest`
(harness sources preserved in `verification/autotest-1201-src/`, removed from
`src` afterwards). AutoTest markers from the client log:

- Anchor placement: `ANCHOR_RESULT block=mekanism:basic_mechanical_pipe
  allSidesNone=true ok=true`
- Single-side placement + symmetric open:
  `PLACE_RESULT ... placedNorth=NORMAL anchorSouth=NORMAL
  placedOnlyNorth=true ok=true`
- Final summary: `MPT_AUTOTEST finished failure=none anchorChecked=true
  placementChecked=true cycleChecked=true mixedChecked=true`

So placement rules, configurator cycling over the SimpleChannel, and the
mixed-state guide were all exercised successfully on 1.20.1.

## Screenshots (verification/screenshot-1201-*.png)

- `screenshot-1201-guide-configurator.png`, `screenshot-1201-guide-configurator-corner-cell.png` — guide in configurator mode
- `screenshot-1201-guide-placement.png`, `screenshot-1201-guide-placement-corner-cell.png` — guide in placement mode
- `screenshot-1201-guide-mixed.png` — mixed connection states in one grid
- `screenshot-1201-guide-type-mechanical-pipe.png`, `...-universal-cable.png`, `...-pressurized-tube.png`, `...-thermodynamic-conductor.png`, `...-logistical-transporter.png` — guide per transmitter type
- `screenshot-1201-placement-anchor-all-none.png`, `screenshot-1201-placement-result.png` — placement results
- `screenshot-1201-cycle-push.png` — after cycling a side to PUSH
- `screenshot-1201-network-scene.png`, `screenshot-1201-network-scene-guide.png` — two machines joined by a logistical-transporter run, without and with the guide
- `screenshot-1201-test-area-overview.png` — the test area

## Release jar

Final clean `gradle build` after harness removal (2026-11-22):
`build/libs/mekanism_pipe_tweaks-0.1.1.jar` (57,202 bytes) — contains
`META-INF/mods.toml` (Forge format), LICENSE and NOTICE, and **no** autotest
classes.

## Spec change v0.2.0 — regression verification (2026-11-22)

Applied the GT Modern behaviour revision to the 1.20.1 tree and verified it on
a dedicated server + dev client under Xvfb `:99` (same method as above). The
temporary harness was adapted from `verification/autotest-1201-src/`, copied
into `src` for the run, then removed again.

### Code removed / changed

- **Guide trigger:** `GuideTarget.Mode` (CONFIGURATOR/PLACEMENT) and every
  placement-mode branch removed. `GuideTarget.resolve` now returns a target only
  when the main-hand item is `ItemConfigurator`; holding a transmitter item
  yields `null`.
- **Cell-select placement:** deleted `PlaceTransmitterMessage`,
  `ModNetwork`'s second registration, and the client `RightClickItem`
  interception / `PlaceTransmitterMessage` send path in
  `GuideInteractionHandler`. Ordinary placement is vanilla again.
- **Server guard:** deleted `GuardRecord`, `GUARDS`, `consumeGuard`,
  `placeSelected` and the guard check in `ServerEvents`. The remaining
  `CLICKS` map only maps a player's last right-click to the placed block's
  face, as required to know which side to open when sneaking.
- **Protocol:** `SimpleChannel` protocol version `1` → `2`.
- **Placement rule:** a player-placed transmitter starts all-`NONE`; only when
  `player.isShiftKeyDown()` and `connectClickedSide=true` is the clicked-facing
  side opened. `openCounterpartSide` still gates the symmetric open.
- **Version:** `0.2.0`; docs (`README.md`, `docs/DESIGN.md`) updated to match.

### How the sneak state is read server-side

`TransmitterPlacementLogic.onBlockPlaced` runs from
`BlockEvent.EntityPlaceEvent` and reads `Player#isShiftKeyDown()` on the
server-side player. The client's `ServerboundPlayerCommandPacket`
PRESS/RELEASE_SHIFT_KEY updates that state before the subsequent
`use_item_on` packet is handled, so the value is current at placement time.

### Harness runs (client log markers)

- Anchor placed non-sneak: `ANCHOR_RESULT allSidesNone=true ok=true`.
- (a) Non-sneak against a transmitter:
  `READBACK nonsneak-readback ... 20: all NONE | 21: all NONE`,
  `NONSNEAK_RESULT placedAllNone=true anchorAllNone=true ok=true`.
- (b) Sneak against a transmitter:
  `READBACK sneak-readback ... 20: south=NORMAL/NORMAL, others NONE | 21: north=NORMAL/NORMAL, others NONE`,
  `SNEAK_RESULT placedNorth=NORMAL anchorSouth=NORMAL placedOnlyNorth=true anchorOnlySouth=true ok=true`.
- (c) Guide:
  `GUIDE_CONFIGURATOR_RESULT guidePresent=true ok=true`,
  `GUIDE_TRANSMITTER_ITEM_RESULT guideAbsent=true ok=true`.
- (d) Cycling:
  `CYCLE_RESULT centreSouth=PUSH ok=true` (two sneak+right-click cycles on the
  centre cell).
- Summary:
  `MPT_AUTOTEST finished failure=none anchorChecked=true nonsneakChecked=true sneakChecked=true cycleChecked=true guideChecked=true noGuideChecked=true`.

### Screenshots (`verification/screenshot-1201-spec-*.png`)

- `screenshot-1201-spec-guide-configurator.png` — guide drawn while holding the
  Configurator (pixel check: 3,477 distinctive grid-blue pixels in the centre
  crop).
- `screenshot-1201-spec-guide-transmitter-item.png` — no guide while holding a
  Mechanical Pipe (same check: 0 grid-blue pixels).
- `screenshot-1201-spec-sneak-placement.png` — result of the sneak placement.
- Also captured: `...-spec-anchor-all-none.png`,
  `...-spec-nonsneak-placement.png`, `...-spec-cycle-push.png`.

### Release jar

Final clean `gradle clean build` after harness removal (2026-11-22):
`build/libs/mekanism_pipe_tweaks-0.2.0.jar` (50,441 bytes). It contains
Forge-format `META-INF/mods.toml` (`modLoader = "javafml"`,
`loaderVersion = "[47,)"`, `version = "0.2.0"`), LICENSE and NOTICE, and **no**
`autotest` classes.
