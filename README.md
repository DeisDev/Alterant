# Preserve

Keep gardens, delicate blocks, structures and machines just as you left them.

**Minecraft 26.2 · Fabric & NeoForge · Java 25 · By DeisDev**

| Compound | Keeps things as you left them | Uses per jar |
|---|---|---:|
| Growth Inhibitor | Supported plants stop natural growth and spreading; bone meal still works. | 32 |
| Preserving Sealant | Protects supported blocks from melting, drying, decay and oxidation. | 16 |
| Structural Stasis | Holds supported connection shapes, gravity blocks and trapdoors. | 8 |
| Temporal Stasis | Pauses standard ticks and supported automation transfers. | 4 |

Use a **Preserving Brush** on a block with a jar in your offhand. Sneak-use air to toggle a 3×3 surface; sneak-use a block to replace its coating. **Scrape** to remove without refunds. Failed applications cost nothing, empty jars return a bottle, and blocks remain breakable.

Craft vanilla materials into **Binding Paste, Inert Powder, Waxed Membrane, Stabilizing Lattice** and a **Temporal Core**, then jars. Temporal Stasis needs three full base jars, two lattice and a core; it returns two bottles. See the recipe book. Costs are experimental.

Install on **client and server**. Fabric requires Fabric API. Independent clocks, controllers and networks may remain partly active; tool inspection shows coverage limits.

**Uninstall:** back up the world, discard Preserve items, run `/preserve prepare-uninstall start`, and wait for **complete**. Then stop the server and remove the mod.

Experimental alpha. Build: `./gradlew build`; jars: each loader's `build/libs`. Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template), under CC0.
