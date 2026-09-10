# Preserve

Adds new ways to make blocks and machines interact with the world.

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

Install the same Preserve version on **client and server**. Fabric requires Fabric API. Add [YACL](https://modrinth.com/mod/yacl) for **Mods → Preserve → Config**; Fabric also needs [Mod Menu](https://modrinth.com/mod/modmenu). Tooltips default to **Basic**, with **Advanced** and **Hidden** options. **Config → Display** also has independent position, scale, background color/opacity and text shadow controls for the tool HUD and serum card. Display settings stay local to your client.

Optional client mods [Legendary Tooltips](https://modrinth.com/mod/legendary-tooltips) and [Item Borders](https://modrinth.com/mod/item-borders) give all Preserve items matching frames and inventory borders in their existing colors. Either works independently; install its required dependencies too. Their own display settings and manual overrides still apply.

Open **Config → Gameplay → Open** to edit serums, enchanting, treatment limits and component trades. Single-player owners and server admins can save; other players can view settings. Saves create a world-specific override in `serverconfig/deisdev.json`; **Use data pack settings** restores pack control. Changes affect future applications and new offers. Look at a serum-treated block to see its actual speed and remaining loaded time; Hidden mode hides this display too.

Independent clocks, controllers and networks may remain partly active; Advanced inspection shows coverage limits.

Serum feedback appears immediately above the targeted block, with its saved speed and remaining loaded time. Optional [Jade](https://modrinth.com/mod/jade) shows every applied formula, permanent coating duration, and serum speed, timer and paused status. Install Jade on both sides for its live timer; Preserve's Hidden mode also hides these details.

**Uninstall:** back up the world, discard Preserve items, run `/preserve prepare-uninstall start`, and wait for **complete**. Then stop the server and remove the mod.

Experimental alpha. Build: `./gradlew build`; jars: each loader's `build/libs`.

Preserve follows [Semantic Versioning 2.0.0](https://semver.org/); see [CHANGELOG.md](CHANGELOG.md).

Licensed under [GNU GPL version 3 or later](LICENSE), with a Minecraft/mod-loader linking permission in [NOTICE](NOTICE). Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template); its [CC0 license](LICENSE-CC0) and attribution are preserved. Earlier CC0 grants remain valid.
