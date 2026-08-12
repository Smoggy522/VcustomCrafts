# VcustomCrafts

**Use virtually any Minecraft `ItemStack` as a recipe ingredient or result.**

VcustomCrafts is a Paper/Purpur 1.21.x custom-crafting plugin for Java 21. It provides safe shaped and shapeless recipes, exact custom-item snapshots, an in-game recipe editor, a recipe book, reusable or damageable ingredients, returned containers, and verified GitHub Release updates.

## What is implemented in 1.0.0

- shaped and shapeless crafting recipes;
- MATERIAL, EXACT and configurable ADVANCED matching;
- complete Bukkit `ItemStack` snapshots, including metadata exposed by Bukkit/Paper;
- `/vcc item save <id>` capture for items from other plugins;
- GUI editor: `/vcc editor <id> [shaped|shapeless]`;
- recipe browser: `/recipes`;
- material, saved item, embedded snapshot, Minecraft tag, alternatives and provider ingredients;
- consume, keep, durability damage and return-item behavior;
- permission and XP-level requirements;
- success chance and optional consumption on failure;
- guarded normal-click and shift-click crafting with rollback on failure;
- transactional recipe reload (the previous registry is restored if the swap fails);
- English and Russian messages;
- public custom item provider API;
- public or private GitHub Release updater with asset-name, size and SHA-256 verification;
- GitHub Actions build and release workflows.

## Quick start

1. Install Java 21 and Paper/Purpur 1.21.x.
2. Build with `gradle clean build`.
3. Copy `build/libs/VcustomCrafts-1.0.0.jar` into `plugins/`.
4. Start the server.
5. Hold any item and run `/vcc item save my_item`, or create a recipe with `/vcc editor my_recipe shaped`.

See [INSTALLATION.md](INSTALLATION.md), [RECIPES.md](RECIPES.md), and [GITHUB_UPDATES.md](GITHUB_UPDATES.md).

## Important scope note

The attached product concept describes a much larger multi-stage platform. Version 1.0.0 in this repository is a working, reviewable core release. Furnace, smithing, stonecutter recipes, Vault/economy, databases, unlocks, custom stations, timed queues, PlaceholderAPI and direct third-party provider modules are documented in [ROADMAP.md](ROADMAP.md), not falsely advertised as finished.

## License

MIT. See [LICENSE](LICENSE).

