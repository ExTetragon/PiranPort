# PiranPort

[简体中文](README.md) | English

*"I believe Lexington is the sovereign of this world, and all should become her loyal servants."*

**PiranPort** — a comprehensive **Warship Girls R**-themed mod for **Minecraft 1.21.1 (NeoForge)**: play as a shipgirl, gather resources, cook food, and craft riggings and weapons in survival, fight Abyssal enemies across the Overworld, campaign maps, and the Abyssal World, and explore an independent story. Its content is organized into three systems: combat, logistics, and story.

> **Status: early development.** Includes 60 food/related items and 14 mineral-resource items. Fuel barrels, fluids, recipes and world generation are implemented; 17 dedicated-server GameTests passed during the 2026-10-22 repair. Test sources are currently absent; see the development log. Textures are placeholders. Client visuals and third-party integrations still require manual acceptance.

## Planned scope

PiranPort is organized into three systems: combat, logistics, and story.

### Combat system

- **Rigging and load**: hull types and rigging cores, rigging inventory, load
- **Weapons**: guns, torpedoes, aircraft, depth charges, missiles and rockets
- **Equipment**: armor, sonar, melee weapons, other ranged weapons, shields and armor, tools
- **Status effects**: High-Speed Evasion, EXP Boost, Flooding, On Fire, Spotted!, Reload Speed

### Logistics system

- **Resources**: bauxite, saltpeter, crude/fuel oil, aluminum/steel, and fuel barrels; trees and crops remain planned
- **Military industry**: dry dock, blueprints, weapon assembly, ammunition assembly, reloading
- **Cooking**: naval pot, stove, knife and cutting board, millstone, block foods
- **Building and decoration**
- **Skins**: hull models, weapon models, special animations

### Story system

- **NPCs**: Aila, Abyssals
- **Ruins**: portal, supply station, outpost, Abyssal base
- **Campaign map**: sortie gate, stages
- **Abyssal Will World**: main island and boss, outer islands
- **Advancements**

## Development roadmap

| Version | Content |
|---|---|
| 0.1.x | Logistics: resources and cooking modules |
| 0.2.x | Combat: weapons 1 (guns and depth charges), equipment, status effects; logistics: military industry |
| 0.3.x | Combat: weapons 2 (torpedoes, aircraft, missiles) |
| 0.4.x | Combat: rigging and load; logistics: skins |
| 0.5.x | Story: ruins; logistics: building and decoration |
| 0.6.x | Story: NPCs |
| 0.7.x | Story: campaign map and advancements |
| 0.8.x | Story: Abyssal Will World |

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
./gradlew runData        # generate storage-block loot and mineral item tags
./gradlew runGameTestServer # requires restored test sources; see the development log
```

Open this folder as a Gradle project in IntelliJ IDEA to get code completion for Minecraft and NeoForge classes. Also set **Gradle JVM** to JDK 21 under `Settings > Build, Execution, Deployment > Build Tools > Gradle`.

## Project structure

```
src/main/java/com/lex0v0/piranport/           main class, client entry, config
src/main/java/com/lex0v0/piranport/registry/  item, block, and creative tab registration
src/main/resources/META-INF/                  neoforge.mods.toml
src/main/resources/assets/piranport/          language files and other resources
src/generated/resources/                      data generation output
```

## Mod metadata

| | |
|---|---|
| Display name | PiranPort |
| Mod ID | `piranport` |
| Package | `com.lex0v0.piranport` |
| Organization | lex0v0 |
| Maven Group | `com.lex0v0.piranport` |
| Artifact | `piranport-0.1.41.jar` |

## Version policy

- Current version: `0.1.41`, based on 24 implemented features and 17 bug fixes; see the [development log](docs/开发日志.md) for the itemized baseline.
- Increment only the last version segment for each completed feature or independent bug fix. Documentation changes do not count. Change the first or middle segment only when explicitly requested by the user.

- Development and testing use NeoForge `21.1.259`.
- The dependency lower bound in `neoforge.mods.toml` is `[21.1.0,)`, permitting other 21.1.x versions; only 21.1.259 has been validated.

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Implementation and validation

See [implementation](docs/3.1-矿物资源-实现方案.md), [acceptance checklist](docs/3.1-矿物资源-测试清单.md), [repair details](docs/3.1-矿物资源-修复说明.md), and the [resource workbook](docs/后勤-矿物.xlsx).
Generated resources and the Gradle Wrapper are versioned. GameTests use run/gametest and are excluded from the release jar.
Run python tools/verify_resources.py to check JSON and texture references. Use python tools/generate_food_placeholders.py to recreate missing food placeholders.

The [food workbook](docs/舰R食物.xlsx) records implemented basics and pending special effects. The [original design PDF](docs/策划书.pdf) is preserved unchanged. Development history and implementation corrections are recorded in the [development log](docs/开发日志.md); [AGENTS.md](AGENTS.md) describes the current project structure, constraints, and validation status.
