# Lightning Block Mod (Minecraft Forge 1.20.1)

A Forge mod that adds a **Lightning Block** — a configurable lightning-strike block
acquired by charging a Dragon Egg with lightning — along with **Lightning Ore**,
a **Lightning Portal**, and a **Lightning Core** item.

## Features

### Lightning Block

- **Name:** Lightning Block
- **Texture:** End-style purple / black mix
- **Hardness:** 100 (2× obsidian's 50), blast resistance 1200

#### Obtaining
- **Creative:** available in the "Lightning Block" creative tab
- **Survival:** **Lightning strikes** a Dragon Egg — the Dragon Egg converts into a Lightning Block.

#### GUI (right-click the block)
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

---

### Lightning Ore

- **Name:** Lightning Ore
- **Texture:** Black / obsidian-style
- **Stats:** identical to obsidian — hardness 50, blast resistance 1200
- **Tool:** requires a diamond pickaxe (or better) to drop

#### Obtaining
- **Creative:** available in the "Lightning Block" creative tab
- **Survival:**
  - Smelted from **Lightning Ore** found in **Ruined Portals with Lightning** (see below), or
  - Found as a 1% replacement in vanilla ruined portals

#### Smelting
Smelting Lightning Ore in a furnace yields a **Lightning Core**.

#### Portal Frame
Lightning Ore can be used as a frame block for **Lightning Portals** (see below).
It can be mixed freely with obsidian in the same frame.

---

### Lightning Portal

- **Name:** Lightning Portal
- **Appearance:** visually identical to a vanilla Nether Portal (same animated texture)
- **Light:** emits light level 11

#### Ignition
Build a rectangular portal frame using **obsidian and/or Lightning Ore** (the frame
must contain at least one Lightning Ore block), then use **flint and steel** on the
inside of the frame.

- **Size constraints:** interior width 2–21 blocks, interior height 3–21 blocks
  (same as vanilla Nether Portals).
- Pure-obsidian frames still produce vanilla Nether Portals (unchanged behaviour).

#### Teleportation
Lightning Portals work exactly like vanilla Nether Portals for dimension travel
between the Overworld and the Nether. Return trips correctly locate the original
Lightning Portal (via POI registration + a `PortalForcer` mixin).

---

### Lightning Core

- **Name:** Lightning Core
- **Stack size:** 9
- **Obtaining:** smelt Lightning Ore in a furnace

#### Effects (while held in either hand)
- **Immunity:** fire, lava, explosion, and lightning damage
- **Lightning aura:** every 2 seconds (40 ticks), all hostile mobs within a 10-block
  radius are struck by lightning

---

### Ruined Portal with Lightning

A new world-gen structure: `lightningblock:ruined_portal_with_lightning`.

- Generates in the Overworld using the vanilla ruined-portal generator.
- **Every** such portal has one obsidian block replaced with Lightning Ore (100% chance,
  unlike vanilla ruined portals which have a 1% chance).
- **Locatable** with `/locate lightningblock:ruined_portal_with_lightning`.
- Spacing: 35 chunks, separation: 10 chunks (rarer than vanilla ruined portals).

---

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
