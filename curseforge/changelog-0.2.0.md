## 0.2.0 — behaviour aligned with GregTech

- The sided guide now appears only while holding the Configurator. It no longer appears while holding a transmitter item.
- Cell-select placement has been removed; right-click placement is fully vanilla again.
- Placement rule changed: a placed transmitter now starts with all sides disconnected. If you place **while sneaking**, the side facing the clicked block connects (and the clicked transmitter's facing side opens too, as before).
- The `connectClickedSide` config key now switches this sneak-connect behaviour on/off. Other config keys are unchanged.
- Network protocol updated (a message was removed); client and server must both run 0.2.0.
