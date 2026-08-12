package io.github.smoggy522.vcustomcrafts.recipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RecipeDefinition {
    private final String id;
    private final NamespacedKey key;
    private final String displayName;
    private final RecipeType type;
    private final List<String> shape;
    private final Map<Character, IngredientDefinition> shapedIngredients;
    private final List<IngredientDefinition> shapelessIngredients;
    private final ItemStack result;
    private final String permission;
    private final int experienceLevelCost;
    private final double successChance;
    private final boolean consumeOnFailure;
    private final int priority;
    private final Path source;

    public RecipeDefinition(String id, NamespacedKey key, String displayName, RecipeType type,
                            List<String> shape, Map<Character, IngredientDefinition> shapedIngredients,
                            List<IngredientDefinition> shapelessIngredients, ItemStack result,
                            String permission, int experienceLevelCost, double successChance,
                            boolean consumeOnFailure, int priority, Path source) {
        this.id = id;
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.shape = List.copyOf(shape);
        this.shapedIngredients = Map.copyOf(shapedIngredients);
        this.shapelessIngredients = List.copyOf(shapelessIngredients);
        this.result = result.clone();
        this.permission = permission;
        this.experienceLevelCost = Math.max(0, experienceLevelCost);
        this.successChance = Math.max(0.0, Math.min(100.0, successChance));
        this.consumeOnFailure = consumeOnFailure;
        this.priority = priority;
        this.source = source;
    }

    public String id() { return id; }
    public NamespacedKey key() { return key; }
    public String displayName() { return displayName; }
    public RecipeType type() { return type; }
    public List<String> shape() { return shape; }
    public Map<Character, IngredientDefinition> shapedIngredients() { return shapedIngredients; }
    public List<IngredientDefinition> shapelessIngredients() { return shapelessIngredients; }
    public ItemStack result() { return result.clone(); }
    public String permission() { return permission; }
    public int experienceLevelCost() { return experienceLevelCost; }
    public double successChance() { return successChance; }
    public boolean consumeOnFailure() { return consumeOnFailure; }
    public int priority() { return priority; }
    public Path source() { return source; }
}

