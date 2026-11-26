# Changelog — Minecraft 1.20.1 / Forge

This is the changelog for the 1.20.1 / Forge build. The 1.21.1 / NeoForge build has its own
changelog on the `main` branch.

## 0.3.0 — 2026-11-26

- Same 0.3.0 spec as the 1.21.1 build, ported to Minecraft 1.20.1 / MinecraftForge 47 / Mekanism
  10.4.16:
  - GregTech-style placement guide while holding a transmitter of the same genre as the transmitter
    you are looking at; connect icons on empty sides and cell-select placement, connected to the
    target (with the looked-at face as the fallback when the chosen cell is occupied).
  - Placing against a same-genre transmitter connects both facing sides without sneaking
    (`connectPlacedPipe`, default `true`). Placement against machines still needs sneaking
    (`connectClickedSide`).
  - Configurator clicks toggle `NONE` ⇄ `NORMAL`, treating `PUSH`/`PULL` as connected.
  - Connecting a side opens the facing side of a compatible neighbour transmitter
    (`openCounterpartSide`); disconnecting does not affect the neighbour.
- Also includes the 0.2.1 guide fixes that this branch had not received yet: the guide toggles
  without sneaking and the whole block face is clickable, not just the pipe silhouette.
- Network protocol raised to 3; client and server must both run 0.3.0.

## 0.2.0 — 2026-11-22

- Port to Minecraft 1.20.1 / Forge 47.4.0 / Mekanism 10.4.16.
- Behaviour aligned with GregTech: the guide appears only while holding the Configurator,
  cell-select placement was removed, and a placed transmitter starts disconnected on all sides.
  Placing while sneaking opens the side facing the clicked block.
- `connectClickedSide` controls the sneak-connect behaviour. Network protocol raised to 2.
