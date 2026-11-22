## 0.1.1

- Addon transmitters are now supported automatically: all transmitter decisions use only Mekanism's base classes and public API (no block-ID lists), so transmitters from other Mekanism addons get the same placement rules, guide and configurator cycling. Verified in-game against an independent test addon mod.
- Internal refactor for clarity (shared transmitter facade, merged server event handlers). No behaviour, config or network changes.
