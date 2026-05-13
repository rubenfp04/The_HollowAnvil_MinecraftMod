# The Hollow Anvil

A sacrificial altar mod for Minecraft (NeoForge 1.21.1+). Collect **Soul Essence** from dying mobs and spend it on powerful buffs and dark abilities.

## Features

### The Hollow Anvil Block
A craftable altar that absorbs the souls of mobs killed nearby. It tracks **Soul Essence** and, once a blood demand is met, offers the player a choice of three random rewards.

**Crafting Recipe:**
```
[Redstone Block] [Flint & Steel] [Redstone Block]
[Redstone Block]   [Obsidian]    [Redstone Block]
[Redstone Block] [Diamond Sword] [Redstone Block]
```

### Auto-Trap System
The altar automatically traps nearby mobs (up to 3), dragging them toward it with particle effects and holding them in place for easy sacrifice.

### Rewards
When the blood demand is met, you choose one of three rewards:

| Reward | Description |
|--------|-------------|
| Enchanted Golden Apple | Consumable item |
| The Hollow Devourer | Netherite Sword |
| Hollow Spirit | Totem of Undying |
| Blood-Forged Metal | Netherite Ingots |
| **Power: Enderman** | Controlled teleport — aim and teleport up to 48 blocks |
| **Power: Ghast** | Launch a fireball where you look |
| **Power: Wither** | Fire a volley of 3 wither skulls |
| **Power: Creeper** | Create an explosion 5 blocks ahead |
| **Power: Blaze** | Rain 8 small fireballs forward |
| **Power: Spider** | Place a web trap + get Jump Boost & Speed |

### Progressive Tier System
The altar grows stronger as you fulfill more demands:

| Tier | Demands Met | Bonus |
|------|-------------|-------|
| 1 | 0+ | Base buffs |
| 2 | 3+ | 1.3x duration, +1 amplifier |
| 3 | 6+ | 1.7x duration, +2 amplifier |
| 4 | 10+ | 2.5x duration, +3 amplifier |

### Buffs Granted
Every reward also grants powerful combat buffs:
- **Strength** (10 min)
- **Regeneration** (5 min)
- **Absorption** (8 min)
- **Blood Frenzy** (custom effect — 5 min)
- **Soul Armor** (custom effect — 3 min)

### Death Penalty
If you die while holding a power, all powers are revoked and your altar's tier resets to 0. The altar will mock you.

## Languages
English, Spanish, French, German, Portuguese (BR).

## Requirements
- Minecraft 1.21.1+
- NeoForge 21.1+

## Installation
1. Download the latest `.jar` from [Releases](../../releases).
2. Place it in your `mods/` folder.
3. Launch the game with NeoForge.

## Building from Source
```bash
./gradlew build
```
The output jar will be in `build/libs/`.

## License
MIT

## Author
**ImEnder** — [Kynetio](https://github.com/rubenfp04)
