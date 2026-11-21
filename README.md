# Mekanism Pipe Tweaks

GregTech-style placement and connection behaviour for Mekanism transmitters
(cables, pipes, tubes, conductors, transporters), including transmitters added by
other Mekanism addons, which are supported automatically:

- Transmitters are placed with all sides disconnected, except the side facing
  the block you clicked, which starts connected.
- Placing against an existing transmitter also opens that transmitter's
  facing side, so the pair actually connects.
- A GregTech-style sided grid guide is shown while holding the Configurator,
  and grid cells can be used to choose the placement side.
- No dedicated items; the Mekanism Configurator remains the only tool.

## Configuration

The common config file (`config/mekanism_pipe_tweaks-common.toml`) has these keys:

| Key | Default | Meaning |
|-----|---------|---------|
| `enableEnergy` | `true` | Apply the rules to Universal Cables. |
| `enableFluid` | `true` | Apply the rules to Mechanical Pipes. |
| `enableChemical` | `true` | Apply the rules to Pressurized Tubes. |
| `enableHeat` | `true` | Apply the rules to Thermodynamic Conductors. |
| `enableItem` | `true` | Apply the rules to Logistical Transporters. |
| `connectClickedSide` | `true` | Open the side facing the clicked block. `false` leaves every side disconnected on placement. |
| `openCounterpartSide` | `true` | Also open the facing side of the transmitter that was placed against. |
| `nonPlayerPlacement` | `"VANILLA"` | Dispensers/structures/other non-player placements: `VANILLA` keeps Mekanism's all-sides-connected default, `NONE` disconnects every side, `CLICKED` opens the side facing the block that was placed against. |

A transmitter whose transmission type is disabled is left untouched: it keeps
Mekanism's vanilla placement and Configurator cycling behaviour.

Design document: [docs/DESIGN.md](docs/DESIGN.md)

Not affiliated with Mekanism or GregTech. The guide icons in
`assets/mekanism_pipe_tweaks/textures/gui/overlay/` are derived from
GregTech Modern (GregTechCEu), which is licensed under LGPL-3.0;
this mod is likewise distributed under LGPL-3.0 (see `LICENSE`).
This mod is unrelated to the "MekanismTweaks" mod (upgrade tweaks).
