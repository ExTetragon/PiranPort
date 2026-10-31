# PiranPort

[简体中文](README.md) | English

*"I believe Lexington is the sovereign of this world, and all should become her loyal servants."*

**PiranPort** — a comprehensive **Warship Girls R**-themed mod for **Minecraft 1.21.1 (NeoForge)**: play as a shipgirl, gather resources, cook food, and craft riggings and weapons in survival, fight Abyssal enemies across the Overworld, campaign maps, and the Abyssal World, and explore an independent story. Its content is organized into three systems: combat, logistics, and story.

> **Status: early development, version 0.1.125.** 104 registered items include food, resources, blueprints, eight ammunition types, rigging cores, the shipyard, and 15 equipment categories with 21 durable models and 2 plain consumables. Equipment has durability, aircraft fuel storage, creative variants and 32×32 icons; the development factory provides 23 recipes, blueprint reservation, refunds, persistent storage, automation and category/text filters. Repair, automatic consumable effects and combat remain unimplemented.

The [ammunition production line](docs/3.2.4-弹药生产线-实现方案.md) now provides eight recipes, finite production plans, per-cycle charging, pause/resume, graceful interruption, sided item access and copper nugget conversions. Its GUI shows single-cycle or total requirements and preserves the last closed browsing state on the machine.

The shipyard now uses a dedicated block/item model with facing-aware selection and collision shapes matched to its geometry.

---

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

---

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

---

## Mod metadata

| Item | Value |
|---|---|
| Display name | PiranPort |
| Mod ID | `piranport` |
| Package | `com.lex0v0.piranport` |
| Organization | lex0v0 |
| Maven Group | `com.lex0v0.piranport` |
| Artifact | `piranport-0.1.125.jar` |

---

## License

GPL-3.0
