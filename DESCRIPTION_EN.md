# Short description

**VcustomCrafts lets you build safe shaped and shapeless recipes with virtually any item.** Capture vanilla or custom `ItemStack`s from ItemsAdder, Oraxen, Nexo and other plugins directly from your hand, assemble recipes in an in-game GUI, and match names, lore, CustomModelData, enchantments and PDC precisely. Includes a recipe book, catalysts, tool damage, container returns, and verified GitHub Release updates.

# Full description

## VcustomCrafts — Use ANY ItemStack as an Ingredient

VcustomCrafts is a custom recipe engine for Paper and Purpur 1.21.x running on Java 21. It is designed for servers that have outgrown vanilla crafting but do not want a separate Java plugin for every new item.

The main idea is simple: if another plugin exposes its item as a regular Bukkit/Paper `ItemStack`, an administrator can hold it, save it, and use it as an ingredient or result. A dedicated integration is optional. VcustomCrafts captures the item and the metadata represented by Bukkit, then compares future items against that snapshot.

### Create recipes in game

Run `/vcc editor legendary_sword shaped`, place ingredients in the real 3×3 grid, add the result, and press Save. VcustomCrafts writes a YAML recipe containing exact snapshots, validates it, and loads it immediately. The administrator's items are returned when the editor closes.

### Choose how items are matched

Every ingredient can use:

- MATERIAL — type and required amount;
- EXACT — Paper/Bukkit `ItemStack` similarity;
- ADVANCED — individually selected name, lore, CustomModelData, enchantment, PDC and damage checks.

Saved items, embedded snapshots, Minecraft item tags, per-slot alternatives, and a developer provider API are supported.

### Ingredients with behavior

An ingredient may be consumed, kept as a catalyst, damaged like a tool, or replaced by a returned item. A Magic Hammer can survive while losing durability; a Water Bucket can return an empty Bucket.

Recipes may also require a permission or XP levels, use a configurable success chance, and optionally consume inputs on failure.

### Crafting safety

VcustomCrafts handles its recipes as validated operations: it calculates craft capacity, checks the cursor and free inventory space, snapshots the crafting matrix and player storage, applies changes, and rolls back if the operation fails. Unsafe result-slot number-key and double-click paths are blocked; shift crafting is bounded by a configurable limit.

### Verified GitHub updates

The updater checks Releases from `Smoggy522/VcustomCrafts`, selects a JAR using a strict asset pattern, enforces a download limit, verifies GitHub's SHA‑256 digest, and stages the verified file in Paper's update folder. It never executes remote commands or restarts the server. Public repositories work without a token; private repositories use a read-only token supplied through an environment variable rather than `config.yml`.

### Version 1.0 feature set

- Paper/Purpur 1.21.x and Java 21;
- shaped and shapeless recipes;
- GUI editor and `/recipes` browser;
- capture virtually any ItemStack from the administrator's hand;
- MATERIAL, EXACT and ADVANCED matching;
- alternatives and Minecraft item tags;
- consume, keep, damage and return-item behavior;
- permissions, XP-level costs and success chance;
- guarded normal and shift crafting;
- transactional recipe reload;
- English and Russian localization;
- developer provider API;
- GitHub Actions and verified release updates.

Furnace, smithing, stonecutter, Vault, databases, unlock progression and custom stations are roadmap modules and are not advertised as finished 1.0 features.

