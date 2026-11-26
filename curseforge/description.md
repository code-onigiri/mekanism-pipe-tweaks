# Mekanism Pipe Tweaks

**GregTech-style placement and connection behaviour for Mekanism transmitters.**

Tired of Mekanism cables and pipes auto-connecting to everything they touch? Mekanism Pipe Tweaks changes how transmitters (Universal Cables, Mechanical Pipes, Pressurized Tubes, Thermodynamic Conductors and Logistical Transporters) behave when placed and configured, following the model GregTech players are used to: **you decide what connects where — nothing connects by accident.**

## What changes

- **Placed transmitters start disconnected.** Every side starts as *None*, so a new pipe never latches onto adjacent machines or other pipes by accident.
- **Pipes connect to pipes.** Placing a transmitter against another transmitter of the same genre connects the two facing sides automatically — no sneaking needed, just like GregTech. If you place against a machine or any other block, the side facing that block connects only when you place **while sneaking**.
- **The other side opens too.** When a side connects, the facing side of a compatible neighbour transmitter is opened as well, so the pair actually carries power/fluid/items. Disconnecting one side does not change the neighbour's setting.
- **GregTech-style sided guide.** While holding the Configurator (in one of its Configurate modes) and looking at a transmitter, a 3×3 grid overlay shows the connection state of all six sides at a glance: the centre cell is the side you are looking at, the edge cells are the neighbouring sides, and the corner cells are the opposite side. There are only two states — connected and disconnected; right-click a cell to toggle it. Sneaking is not required, and the whole block face is clickable, not just the thin pipe silhouette.
- **Place with the guide.** While holding a transmitter item of the same genre as the one you are looking at, the same grid appears with a connect icon on every empty neighbouring side. Click an icon to place the held transmitter there, connected to the looked-at transmitter. If that side is occupied, placement falls back to the face you were looking at.
- **No new items.** Everything works through the Mekanism Configurator and ordinary item placement, exactly as before.

## Addon support

Transmitters added by **other Mekanism addons are supported automatically**. The mod decides everything through Mekanism's base classes and public API — there is no list of block IDs — so any addon transmitter gets the same placement rules, guide and connection toggling. This was verified in-game against an independent test addon.

## Configuration

Per-type and per-behaviour toggles live in `config/mekanism_pipe_tweaks-common.toml`:

| Key | Default | Meaning |
|-----|---------|---------|
| `enableEnergy` | `true` | Apply the rules to Universal Cables. |
| `enableFluid` | `true` | Apply the rules to Mechanical Pipes. |
| `enableChemical` | `true` | Apply the rules to Pressurized Tubes. |
| `enableHeat` | `true` | Apply the rules to Thermodynamic Conductors. |
| `enableItem` | `true` | Apply the rules to Logistical Transporters. |
| `connectPlacedPipe` | `true` | Connect a placed transmitter to a same-genre transmitter automatically, without sneaking. |
| `connectClickedSide` | `true` | When placing **while sneaking** against a machine or other block, open the side facing the clicked block. |
| `openCounterpartSide` | `true` | When connecting a side, also open the facing side of a compatible neighbour transmitter. |
| `nonPlayerPlacement` | `"VANILLA"` | Dispensers/structures/other non-player placements: `VANILLA` keeps Mekanism's default, `NONE` disconnects every side, `CLICKED` opens the side facing the block that was placed against. |

A transmitter whose transmission type is disabled is left completely untouched.

## Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1.x
- **Mekanism** 10.7.x (required dependency)
- Required on both client and server (the guide is rendered client-side; placement rules run server-side). Client and server must both run 0.3.0 — the network protocol changed.

Existing worlds are safe: already-placed transmitters are not modified; the rules apply when a player places or configures a transmitter.

## Notes

- Looking for Minecraft 1.20.1 / Forge? The separate build is published on GitHub as `v0.3.0-1.20.1`.
- Not affiliated with Mekanism or GregTech.
- The guide icons are derived from GregTech Modern (GregTechCEu) textures; this mod is distributed under **LGPL-3.0** (see the bundled LICENSE and NOTICE files).
- Unrelated to the "MekanismTweaks" mod (which tweaks upgrades).
- Source code and issue tracker: https://github.com/code-onigiri/mekanism-pipe-tweaks
