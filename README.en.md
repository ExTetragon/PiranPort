# PiranPort

[简体中文](README.md) | English

*"I believe Lexington is the sovereign of this world, and all should become her loyal servants."*

**PiranPort** — a comprehensive **Warship Girls R**-themed mod for **Minecraft 1.21.1 (NeoForge)**: play as a shipgirl, gather resources and craft riggings in survival, fight Abyssal enemies across the Overworld, campaign maps, and the Abyssal World, and enjoy Warship Girls-style cooking and furniture.

> **Status: early development.** The NeoForge 21.1.259 skeleton is in place and builds successfully; content is not implemented yet.

## Planned scope

### Rigging system

- **Rigging cores and rigging inventory**: large/medium/small/submarine cores determine the load limit, slot count, base speed, and ammo/fuel capacity
- **Skins and rigging models**: skin cores, hull models, modular weapon models, and shipgirl launch animations
- **Load and transformation**: load calculation, shipgirl state, transform and unequip

### Equipment and combat

- **Gunnery**: multiple gun calibers, HE/AP/AA shells, explosive and armor-piercing damage
- **Torpedoes**: torpedo launchers, magnetic/acoustic/oxygen torpedoes, and reload facilities
- **Aviation**: scout, fighter, and attack aircraft with fire-control lock, arming, and recovery
- **Anti-submarine warfare**: sonar, depth charge projectors, and submarines
- **Missiles and rockets**: anti-ship missiles, anti-air missiles, and rockets
- **Armor and handheld items**: additional armor and Warship Girls-themed items such as damage control, smoke generators, and flare launchers

### Resources and blocks

- Bauxite and salt, plus aluminum and salt-related materials
- 8 crops and 7 tree types, with seasonal foliage color changes
- Decorative and functional blocks: weapon assembly table, ammunition assembly table, ammunition reloader, blueprint storage

### Food and buffs

- Cooking blocks: pot, stove, knife and cutting board, millstone
- Placeable food blocks that can be eaten in portions
- Buffs such as High-Speed Evasion, EXP Boost, Flooding, On Fire, Spotted!, and Reload Speed

### NPCs and campaign

- Abyssal and shipgirl NPCs
- Overworld ruins: portal ruins, supply stations, outposts, Abyssal bases
- Campaign map dungeons with chapter-based stages, and the Abyssal World dimension

## Development roadmap

| Version | Content |
|---|---|
| 0.1.x | Basic items and food registration; ore, crop, and tree generation with related recipes |
| 0.2.x | Block foods, decorative furniture and cooking blocks, port machines |
| 0.3.x | Combat system 1: guns, depth charges, armor, handheld items, and buffs |
| 0.4.x | Combat system 2: torpedoes, aircraft, missiles, and their AI |
| 0.5.x | Rigging inventory, skin slot, and transformation system |
| 0.6.x | Shipgirl and Abyssal NPC models and AI |
| 0.7.x | Overworld ruins, structures, and new biomes |
| 0.8.x | Dungeon dimension, portals, story, and advancements |
| 0.9.x | Overall testing and balance tuning |

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.259 |
| ModDevGradle | 2.0.148 |
| Gradle | 9.2.1 (Wrapper) |
| JDK | 21 |
| Parchment | 1.21.1 / 2024.11.17 |

## Building

`JAVA_HOME` must point to JDK 21. If the system variable still points to an older version, override it for the current terminal only:

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"
```

```bash
./gradlew build          # build
./gradlew runClient      # launch a dev client
./gradlew runServer      # launch a dev server
./gradlew runData        # run data generation
```

Open this folder as a Gradle project in IntelliJ IDEA to get code completion for Minecraft and NeoForge classes. Also set **Gradle JVM** to JDK 21 under `Settings > Build, Execution, Deployment > Build Tools > Gradle`.

## Project structure

```
src/main/java/com/lex0v0/piranport/  main class, client entry, config
src/main/resources/META-INF/          neoforge.mods.toml
src/main/resources/assets/piranport/  language files and other resources
src/generated/resources/              data generation output
```

## Mod metadata

| | |
|---|---|
| Display name | PiranPort |
| Mod ID | `piranport` |
| Package | `com.lex0v0.piranport` |
| Organization | lex0v0 |
| Maven Group | `com.lex0v0.piranport` |
| Artifact | `piranport-0.1.0.jar` |

## Version policy

- Development and testing use NeoForge `21.1.259`.
- The dependency lower bound in `neoforge.mods.toml` is `[21.1.0,)`, compatible with all 21.1.x versions.

## License

GPL-3.0 — see [LICENSE](LICENSE).
