# Lightning Block Mod (Minecraft Forge 1.20.1)

A Forge mod that adds a **Lightning Block** — a configurable lightning-strike block
acquired by charging a Dragon Egg with lightning.

## Features

### Block
- **Name:** Lightning Block
- **Texture:** End-style purple / black mix
- **Hardness:** 100 (2× obsidian's 50), blast resistance 1200

### Obtaining
- **Creative:** available in the "Lightning Block" creative tab
- **Survival:**
  1. A **charged (lightning) Creeper** explodes next to a Dragon Egg, or
  2. **Lightning strikes** a Dragon Egg
  - In both cases the Dragon Egg converts into a Lightning Block.

### GUI (right-click the block)
Three input slots + a filter text box:

| Slot | Item | Effect |
|------|------|--------|
| Gold / Nether Star | Gold Block | `remaining += count × 10` |
| Gold / Nether Star | Nether Star ×64 | `remaining = ∞` (infinite) |
| Emerald | Emerald Block | `freq = 32 / count × 20` ticks (count=0 → never) |
| Diamond | Diamond Block | `area side = count / 8` blocks, cube centred on the block |

- Every `freq` ticks, **all entities** inside the area cube that match the `filter`
  are struck by lightning **simultaneously** (same tick).
- Each strike consumes 1 use (infinite mode never consumes).
- **Filter** is a vanilla target selector; default:
  `@e[type=#minecraft:living_entity,type=!player]`
  (all living entities except players). Edit it in the GUI and it is saved on close.

## Building

Requires JDK 17+.

```bash
./gradlew build
```

The compiled mod jar lands in `build/libs/lightningblock-<version>.jar`.

## GitHub Actions

`.github/workflows/build.yml` builds the mod on every push / PR and uploads the jar
as a workflow artifact.

## Installation

Drop the jar into your Forge 1.20.1 `mods/` folder.
