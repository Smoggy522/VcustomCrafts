package io.github.smoggy522.vcustomcrafts.recipe;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.github.smoggy522.vcustomcrafts.api.ProviderRegistry;
import io.github.smoggy522.vcustomcrafts.item.ItemCodec;
import io.github.smoggy522.vcustomcrafts.item.MatchField;
import io.github.smoggy522.vcustomcrafts.item.MatchMode;
import io.github.smoggy522.vcustomcrafts.item.SavedItemRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecipeParser {
    private final JavaPlugin plugin;
    private final SavedItemRepository savedItems;
    private final ProviderRegistry providers;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public RecipeParser(JavaPlugin plugin, SavedItemRepository savedItems, ProviderRegistry providers) {
        this.plugin = plugin;
        this.savedItems = savedItems;
        this.providers = providers;
    }

    public RecipeDefinition parse(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        if (!config.getBoolean("enabled", true)) {
            return null;
        }

        String fallbackId = file.getName().replaceFirst("\\.ya?ml$", "");
        String id = config.getString("id", fallbackId).toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid recipe id: " + id);
        }
        RecipeType type = enumValue(RecipeType.class, config.getString("type", "SHAPED"), "type");
        ItemStack result = parseItem(requiredSection(config, "result"), "result");
        String name = config.getString("name", id);

        List<String> shape = List.of();
        Map<Character, IngredientDefinition> shaped = Map.of();
        List<IngredientDefinition> shapeless = List.of();
        if (type == RecipeType.SHAPED) {
            shape = parseShape(config.getStringList("shape"));
            ConfigurationSection ingredients = requiredSection(config, "ingredients");
            Map<Character, IngredientDefinition> parsed = new LinkedHashMap<>();
            for (String symbol : ingredients.getKeys(false)) {
                if (symbol.length() != 1 || symbol.charAt(0) == ' ') {
                    throw new IllegalArgumentException("Ingredient keys must be one non-space character");
                }
                parsed.put(symbol.charAt(0), parseIngredient(requiredSection(ingredients, symbol),
                    "ingredients." + symbol));
            }
            for (String row : shape) {
                for (char symbol : row.toCharArray()) {
                    if (symbol != ' ' && !parsed.containsKey(symbol)) {
                        throw new IllegalArgumentException("Shape uses undefined ingredient '" + symbol + "'");
                    }
                }
            }
            shaped = parsed;
        } else {
            List<Map<?, ?>> maps = config.getMapList("ingredients");
            if (maps.isEmpty()) {
                throw new IllegalArgumentException("Shapeless recipe requires an ingredients list");
            }
            List<IngredientDefinition> parsed = new ArrayList<>();
            for (int i = 0; i < maps.size(); i++) {
                parsed.add(parseIngredient(sectionFromMap(maps.get(i)), "ingredients[" + i + "]"));
            }
            if (parsed.size() > 9) {
                throw new IllegalArgumentException("Shapeless recipes support at most 9 ingredients");
            }
            shapeless = parsed;
        }

        String permission = config.getString("requirements.permission", "vcustomcrafts.craft");
        int levels = config.getInt("cost.experience-levels", 0);
        double chance = config.getDouble("success-chance", 100.0);
        boolean consumeOnFailure = config.getBoolean("failure.consume-ingredients", false);
        int priority = config.getInt("priority", 0);
        return new RecipeDefinition(id, new NamespacedKey(plugin, id), name, type, shape, shaped,
            shapeless, result, permission, levels, chance, consumeOnFailure, priority, file.toPath());
    }

    private IngredientDefinition parseIngredient(ConfigurationSection section, String path) throws IOException {
        List<IngredientAlternative> alternatives = new ArrayList<>();
        List<Map<?, ?>> anyOf = section.getMapList("any-of");
        if (anyOf.isEmpty()) {
            alternatives.addAll(parseAlternatives(section, path));
        } else {
            for (int i = 0; i < anyOf.size(); i++) {
                alternatives.addAll(parseAlternatives(sectionFromMap(anyOf.get(i)), path + ".any-of[" + i + "]"));
            }
        }

        MatchMode mode = enumValue(MatchMode.class, section.getString("match-mode",
            alternatives.stream().anyMatch(value -> value.prototype() != null) ? "EXACT" : "MATERIAL"), path + ".match-mode");
        EnumSet<MatchField> fields = EnumSet.noneOf(MatchField.class);
        ConfigurationSection match = section.getConfigurationSection("match");
        if (match == null) {
            fields = EnumSet.of(MatchField.NAME, MatchField.LORE, MatchField.CUSTOM_MODEL_DATA,
                MatchField.ENCHANTMENTS, MatchField.PDC);
        } else {
            addField(match, fields, "name", MatchField.NAME);
            addField(match, fields, "lore", MatchField.LORE);
            addField(match, fields, "custom-model-data", MatchField.CUSTOM_MODEL_DATA);
            addField(match, fields, "enchantments", MatchField.ENCHANTMENTS);
            addField(match, fields, "pdc", MatchField.PDC);
            addField(match, fields, "damage", MatchField.DAMAGE);
        }

        int amount = section.getInt("amount", 1);
        ConsumeMode consume = section.getBoolean("consume", true)
            ? enumValue(ConsumeMode.class, section.getString("consume-mode", "CONSUME"), path + ".consume-mode")
            : ConsumeMode.KEEP;
        int damage = section.getInt("damage-amount", 1);
        ItemStack returnItem = section.isConfigurationSection("return-item")
            ? parseItem(requiredSection(section, "return-item"), path + ".return-item") : null;
        if (returnItem != null) {
            consume = ConsumeMode.RETURN;
        }
        return new IngredientDefinition(alternatives, mode, fields, amount, consume, damage, returnItem);
    }

    private List<IngredientAlternative> parseAlternatives(ConfigurationSection section, String path) throws IOException {
        List<IngredientAlternative> values = new ArrayList<>();
        if (section.isString("material")) {
            Material material = Material.matchMaterial(section.getString("material", ""));
            if (material == null || material.isAir()) {
                throw new IllegalArgumentException("Unknown material at " + path);
            }
            values.add(new IngredientAlternative(material, null));
        } else if (section.isString("saved-item")) {
            ItemStack item = savedItems.get(section.getString("saved-item", ""));
            if (item == null) {
                throw new IllegalArgumentException("Unknown saved item at " + path);
            }
            values.add(new IngredientAlternative(item.getType(), item));
        } else if (section.isString("snapshot")) {
            ItemStack item = ItemCodec.decode(section.getString("snapshot", ""));
            values.add(new IngredientAlternative(item.getType(), item));
        } else if (section.isString("tag")) {
            NamespacedKey key = NamespacedKey.fromString(section.getString("tag", ""));
            Tag<Material> tag = key == null ? null : Bukkit.getTag("items", key, Material.class);
            if (tag == null || tag.getValues().isEmpty()) {
                throw new IllegalArgumentException("Unknown or empty item tag at " + path);
            }
            for (Material material : tag.getValues()) {
                if (!material.isAir()) {
                    values.add(new IngredientAlternative(material, null));
                }
            }
        } else if (section.isString("provider") && section.isString("id")) {
            ItemStack item = providers.resolve(section.getString("provider", ""), section.getString("id", ""));
            if (item == null) {
                throw new IllegalArgumentException("Provider item is unavailable at " + path);
            }
            values.add(new IngredientAlternative(item.getType(), item));
        } else {
            throw new IllegalArgumentException("Ingredient at " + path + " requires material, saved-item, snapshot, tag or provider+id");
        }
        return values;
    }

    private ItemStack parseItem(ConfigurationSection section, String path) throws IOException {
        ItemStack item;
        if (section.isString("saved-item")) {
            item = savedItems.get(section.getString("saved-item", ""));
        } else if (section.isString("snapshot")) {
            item = ItemCodec.decode(section.getString("snapshot", ""));
        } else if (section.isString("provider") && section.isString("id")) {
            item = providers.resolve(section.getString("provider", ""), section.getString("id", ""));
        } else {
            Material material = Material.matchMaterial(section.getString("material", ""));
            item = material == null || material.isAir() ? null : new ItemStack(material);
        }
        if (item == null) {
            throw new IllegalArgumentException("Unknown item at " + path);
        }

        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), section.getInt("amount", item.getAmount()))));
        ItemMeta meta = item.getItemMeta();
        if (section.isString("name")) {
            meta.displayName(miniMessage.deserialize(section.getString("name", "")));
        }
        if (section.isList("lore")) {
            meta.lore(section.getStringList("lore").stream().map(miniMessage::deserialize).toList());
        }
        if (section.isInt("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }
        ConfigurationSection enchants = section.getConfigurationSection("enchantments");
        if (enchants != null) {
            for (String keyValue : enchants.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(keyValue.toLowerCase(Locale.ROOT));
                Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
                Enchantment enchantment = key == null ? null : registry.get(key);
                if (enchantment == null) {
                    throw new IllegalArgumentException("Unknown enchantment " + keyValue + " at " + path);
                }
                meta.addEnchant(enchantment, enchants.getInt(keyValue), true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> parseShape(List<String> rows) {
        if (rows.isEmpty() || rows.size() > 3) {
            throw new IllegalArgumentException("A shaped recipe needs 1-3 rows");
        }
        int width = rows.stream().mapToInt(String::length).max().orElse(0);
        List<String> normalized = new ArrayList<>();
        for (String row : rows) {
            if (row.isEmpty() || row.length() > 3) {
                throw new IllegalArgumentException("Every shape row needs 1-3 characters");
            }
            normalized.add(String.format("%-" + width + "s", row));
        }
        return normalized;
    }

    private static void addField(ConfigurationSection section, EnumSet<MatchField> fields,
                                 String path, MatchField field) {
        if (section.getBoolean(path, false)) {
            fields.add(field);
        }
    }

    private static ConfigurationSection sectionFromMap(Map<?, ?> map) {
        YamlConfiguration config = new YamlConfiguration();
        Map<String, Object> converted = new LinkedHashMap<>();
        map.forEach((key, value) -> converted.put(String.valueOf(key), value));
        return config.createSection("value", converted);
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing configuration section: " + path);
        }
        return section;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String path) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid value '" + value + "' at " + path);
        }
    }
}
