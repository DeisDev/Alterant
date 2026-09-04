# Preserve

Keep gardens, delicate blocks, structures and machines just as you left them.

**Minecraft 26.2 · Fabric & NeoForge · Java 25 · By DeisDev**

| Compound | Effect | Uses per jar |
|---|---|---:|
| Growth Inhibitor | Supported plants stop natural growth and spreading; bone meal still works. | 32 |
| Preserving Sealant | Protects supported blocks from melting, drying, decay and oxidation. | 16 |
| Structural Stasis | Holds supported connection shapes, gravity blocks and trapdoors. | 8 |
| Temporal Stasis | Pauses standard ticks and supported automation transfers. | 4 |
| Time Serum | Accelerates supported ticks 2× for 20 loaded minutes. | 1 |
| Suspicious Time Serum | Rolls a saved speed from 1.5×–8× per application, for 20 loaded minutes. | 1 |

Use a **Preserving Brush** on a block with a jar in your offhand. Sneak-use air to toggle a 3×3 surface; sneak-use a block to replace its coating. **Scrape** to remove without refunds. Failed applications cost nothing, empty jars return a bottle, and blocks remain breakable.

Craft vanilla materials into **Binding Paste, Inert Powder, Waxed Membrane, Stabilizing Lattice** and a **Temporal Core**, then jars. Temporal Stasis needs three full base jars, two lattice and a core; it returns two bottles. See the recipe book. Costs are experimental.

Time serums use a reusable **Quantum Applicator** with a dose in the offhand. Craft **Chronal Dust, Resonant Crystals** and a **Quantum Lens** using End stone and costly materials. Enchant a full Time Serum in the third slot of a table with 15 unobstructed bookshelves: require level 35, spend 3 levels and 3 lapis. Scraping removes serum; existing coatings must be removed first. Lifetime pauses outside ticking chunks. Supported routes include furnace, brewing, hopper and campfire ticks, random block ticks, and repeater/comparator/observer delays; scheduled work runs at most once per game tick and fractional delays round up. External clocks and networks keep their normal rate.

Clerics sell time components for 24–64 emeralds plus End stone. Other component trades use suitable professions. Data packs can override `data/deisdev/deisdev/settings.json` for serum strength, duration, enchanting requirements and component trades; `component_trades: []` disables new offers. Override recipes for crafting costs and `deisdev/rules/serum_*.json` for supported blocks. `/reload` changes future applications and newly generated trades; existing serums retain their saved strength and lifetime. Invalid rules keep the last valid settings.

Install on **client and server**. Fabric requires Fabric API. Add [YACL](https://modrinth.com/mod/yacl) for **Mods → Preserve → Config**; Fabric also needs [Mod Menu](https://modrinth.com/mod/modmenu). Tooltips default to **Basic**, with **Advanced** and **Hidden** options.

Independent clocks, controllers and networks may remain partly active; Advanced inspection shows coverage limits.

**Uninstall:** back up the world, discard Preserve items, run `/preserve prepare-uninstall start`, and wait for **complete**. Then stop the server and remove the mod.

Experimental alpha. Build: `./gradlew build`; jars: each loader's `build/libs`. Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template), under CC0.
