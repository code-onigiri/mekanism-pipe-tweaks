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
