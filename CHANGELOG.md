# Changelog

All notable changes to this project are documented here. The Minecraft 1.20.1 / Forge build has
its own changelog on the `1.20.1` branch.

## 0.3.0 — 2026-11-26

- Restored the GregTech-style placement guide while holding a transmitter item of the same genre as
  the transmitter you are looking at. Sides whose neighbour is empty show a connect icon; clicking
  one places the held transmitter on that side, connected to the target. If the chosen side is
  occupied, placement falls back to the face you were looking at.
- Placing a transmitter against a same-genre transmitter now connects both facing sides without
  sneaking (`connectPlacedPipe`, default `true`). Placement against machines and other blocks still
  connects only while sneaking (`connectClickedSide`).
- Configurator clicks now toggle a side between connected and disconnected (`NONE` ⇄ `NORMAL`).
  `PUSH`/`PULL` are treated as connected and a click disconnects them; the four-state cycle and the
  direction arrows are gone.
- Connecting a side also opens the facing side of a compatible neighbour transmitter
  (`openCounterpartSide`). Disconnecting does not change the neighbour's setting.
- Network protocol raised to 3; client and server must both run 0.3.0.

## 0.2.1 — 2026-11-26

- Configurator guide cells can be toggled without sneaking.
- The whole block face is clickable, not just the thin pipe silhouette.

## 0.2.0 — 2026-11-22

- The sided guide appears only while holding the Configurator; holding a transmitter item no longer
  shows it.
- Cell-select placement was removed and right-click placement returned to vanilla behaviour.
- A placed transmitter now starts with every side disconnected; placing while sneaking opens the
  side facing the clicked block (and the clicked transmitter's facing side too).
- `connectClickedSide` controls the sneak-connect behaviour. Network protocol raised to 2.

## 0.1.1 — 2026-11-21

- Transmitter blocks added by other Mekanism addons are supported automatically: all decisions now
  use Mekanism's base classes and public API instead of a block-id list.
- Internal refactor of the transmitter facade and server event handlers, with no behaviour, config
  or network changes.

## 0.1.0 — 2026-11-21

- First release for Minecraft 1.21.1 / NeoForge / Mekanism 10.7.
- Placed transmitters start disconnected except toward the clicked block, with mutual connection
  when placing against an existing transmitter.
- GregTech-style 3x3 sided guide while holding the Configurator, with sneak + right-click cycling
  through Normal / Push / Pull / None.
- Per-type config toggles for energy, fluid, chemical, heat and item transmitters.
