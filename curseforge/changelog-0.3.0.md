## 0.3.0 — GregTech placement and connection behaviour

- The sided placement guide is back while holding a transmitter item of the same genre as the transmitter you are looking at. Empty neighbouring sides show a connect icon; clicking one places the held transmitter on that side, connected to the target (falling back to the looked-at face when the chosen cell is occupied).
- Placing a transmitter against a same-genre transmitter connects both facing sides without sneaking (`connectPlacedPipe`, default `true`). Placement against machines still connects only while sneaking (`connectClickedSide`).
- Configurator clicks now toggle a side between connected and disconnected (`NONE` ⇄ `NORMAL`). `PUSH`/`PULL` count as connected; the four-state cycle and direction arrows are gone. Sneaking is no longer needed, and the whole block face is clickable.
- Connecting a side also opens the facing side of a compatible neighbour transmitter (`openCounterpartSide`). Disconnecting does not change the neighbour.
- Network protocol raised to 3; client and server must both run 0.3.0.
