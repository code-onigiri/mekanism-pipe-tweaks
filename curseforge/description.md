# Mekanism Pipe Tweaks

**GregTech-style placement and connection behaviour for Mekanism transmitters.**

Tired of Mekanism cables and pipes auto-connecting to everything they touch? Mekanism Pipe Tweaks changes how transmitters (Universal Cables, Mechanical Pipes, Pressurized Tubes, Thermodynamic Conductors and Logistical Transporters) behave when placed and configured, following the model GregTech players are used to: **you decide what connects where — nothing connects by accident.**

## What changes

- **Placed transmitters start disconnected.** When you place a transmitter, every side starts as *None*. No more pipes latching onto every adjacent machine and each other.
- **Sneak-place to connect.** If you place a transmitter **while sneaking**, the side facing the block you clicked starts connected — and if that block is another transmitter, its facing side is opened as well, so the pair actually connects. (Mekanism's own mutual-agreement rule still applies everywhere else.)
- **GregTech-style sided guide.** While holding the Configurator and looking at a transmitter, a 3×3 grid overlay shows the connection state of all six sides at a glance: the centre cell is the side you are looking at, the edge cells are the neighbouring sides, and the corner cells are the opposite side. Icons follow the side's live state — connected, blocked, push (with a blue marker) and pull (yellow marker).
- **No new items.** Everything works through the Mekanism Configurator, exactly as before — sneak + right-click cycles connection types with Mekanism's own feedback messages.

## Addon support

Transmitters added by **other Mekanism addons are supported automatically**. The mod decides everything through Mekanism's base classes and public API — there is no list of block IDs — so any addon transmitter gets the same placement rules, guide and cycling. This was verified in-game against an independent test addon.

## Configuration

Per-type and per-behaviour toggles live in `config/mekanism_pipe_tweaks-common.toml`:

| Key | Default | Meaning |
|-----|---------|---------|
| `enableEnergy` | `true` | Apply the rules to Universal Cables. |
| `enableFluid` | `true` | Apply the rules to Mechanical Pipes. |
| `enableChemical` | `true` | Apply the rules to Pressurized Tubes. |
| `enableHeat` | `true` | Apply the rules to Thermodynamic Conductors. |
| `enableItem` | `true` | Apply the rules to Logistical Transporters. |
| `connectClickedSide` | `true` | When placing while sneaking, open the side facing the clicked block. `false` disables sneak-connect; every side stays disconnected on placement. |
| `openCounterpartSide` | `true` | Also open the facing side of the transmitter that was placed against. |
| `nonPlayerPlacement` | `"VANILLA"` | Dispensers/structures/other non-player placements: `VANILLA` keeps Mekanism's default, `NONE` disconnects every side, `CLICKED` opens the side facing the block that was placed against. |

A transmitter whose transmission type is disabled is left completely untouched.

## Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1.x
- **Mekanism** 10.7.x (required dependency)
- Required on both client and server (the guide is rendered client-side; placement rules run server-side).

Existing worlds are safe: already-placed transmitters are not modified; the rules apply when a player places a transmitter.

## Notes

- Not affiliated with Mekanism or GregTech.
- The guide icons are derived from GregTech Modern (GregTechCEu) textures; this mod is distributed under **LGPL-3.0** (see the bundled LICENSE and NOTICE files).
- Unrelated to the "MekanismTweaks" mod (which tweaks upgrades).
- Source code and issue tracker: https://github.com/code-onigiri/mekanism-pipe-tweaks
