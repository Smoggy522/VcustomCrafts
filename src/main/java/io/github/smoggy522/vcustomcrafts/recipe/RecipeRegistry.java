package io.github.smoggy522.vcustomcrafts.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class RecipeRegistry {
    private final JavaPlugin plugin;
    private final RecipeParser parser;
    private Map<String, RecipeDefinition> recipesById = Map.of();
    private Map<NamespacedKey, RecipeDefinition> recipesByKey = Map.of();

    public RecipeRegistry(JavaPlugin plugin, RecipeParser parser) {
        this.plugin = plugin;
        this.parser = parser;
    }

    public synchronized LoadReport reload() {
        File directory = new File(plugin.getDataFolder(), "recipes");
        if (!directory.exists() && !directory.mkdirs()) {
            return new LoadReport(recipesById.size(), 1, List.of("Could not create recipes directory"), false);
        }

        List<String> errors = new ArrayList<>();
        Map<String, RecipeDefinition> parsed = new LinkedHashMap<>();
        List<File> files = findRecipeFiles(directory, errors);
        for (File file : files) {
            try {
                RecipeDefinition definition = parser.parse(file);
                if (definition == null) {
                    continue;
                }
                RecipeDefinition duplicate = parsed.putIfAbsent(definition.id(), definition);
                if (duplicate != null) {
                    errors.add(relative(file) + ": duplicate recipe id '" + definition.id() + "'");
                }
            } catch (Exception exception) {
                errors.add(relative(file) + ": " + rootMessage(exception));
            }
        }

        if (!files.isEmpty() && parsed.isEmpty() && !errors.isEmpty() && !recipesById.isEmpty()) {
            logErrors(errors);
            plugin.getLogger().severe("Reload rejected: every enabled recipe failed validation. Previous registry kept.");
            return new LoadReport(recipesById.size(), errors.size(), List.copyOf(errors), false);
        }

        List<RecipeDefinition> ordered = parsed.values().stream()
            .sorted(Comparator.comparingInt(RecipeDefinition::priority).reversed())
            .toList();
        Map<String, RecipeDefinition> previous = recipesById;
        unregister(previous.values());
        try {
            for (RecipeDefinition definition : ordered) {
                if (!Bukkit.addRecipe(toBukkitRecipe(definition))) {
                    throw new IllegalStateException("Bukkit rejected recipe " + definition.id());
                }
            }
        } catch (RuntimeException exception) {
            unregister(ordered);
            for (RecipeDefinition definition : previous.values()) {
                Bukkit.addRecipe(toBukkitRecipe(definition));
            }
            errors.add("Registry swap failed: " + rootMessage(exception));
            logErrors(errors);
            return new LoadReport(previous.size(), errors.size(), List.copyOf(errors), false);
        }

        Map<NamespacedKey, RecipeDefinition> byKey = new LinkedHashMap<>();
        ordered.forEach(recipe -> byKey.put(recipe.key(), recipe));
        recipesById = Map.copyOf(parsed);
        recipesByKey = Map.copyOf(byKey);
        logErrors(errors);
        return new LoadReport(parsed.size(), errors.size(), List.copyOf(errors), true);
    }

    public Optional<RecipeDefinition> byId(String id) {
        return Optional.ofNullable(recipesById.get(id.toLowerCase(Locale.ROOT)));
    }

    public RecipeDefinition byRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed ? recipesByKey.get(keyed.getKey()) : null;
    }

    public Collection<RecipeDefinition> all() {
        return List.copyOf(recipesById.values());
    }

    private Recipe toBukkitRecipe(RecipeDefinition definition) {
        if (definition.type() == RecipeType.SHAPED) {
            ShapedRecipe shaped = new ShapedRecipe(definition.key(), definition.result());
            shaped.shape(definition.shape().toArray(String[]::new));
            definition.shapedIngredients().forEach((symbol, ingredient) ->
                shaped.setIngredient(symbol, ingredient.registrationChoice()));
            return shaped;
        }
        ShapelessRecipe shapeless = new ShapelessRecipe(definition.key(), definition.result());
        definition.shapelessIngredients().forEach(ingredient ->
            shapeless.addIngredient(ingredient.registrationChoice()));
        return shapeless;
    }

    private void unregister(Collection<RecipeDefinition> definitions) {
        definitions.forEach(definition -> Bukkit.removeRecipe(definition.key()));
    }

    private List<File> findRecipeFiles(File directory, List<String> errors) {
        try (Stream<java.nio.file.Path> paths = Files.walk(directory.toPath())) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().matches("(?i).+\\.ya?ml"))
                .map(java.nio.file.Path::toFile)
                .sorted(Comparator.comparing(File::getPath))
                .toList();
        } catch (IOException exception) {
            errors.add("Could not scan recipes directory: " + exception.getMessage());
            return List.of();
        }
    }

    private String relative(File file) {
        return plugin.getDataFolder().toPath().relativize(file.toPath()).toString();
    }

    private void logErrors(List<String> errors) {
        errors.forEach(error -> plugin.getLogger().severe("Recipe rejected: " + error));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public record LoadReport(int loaded, int rejected, List<String> errors, boolean swapped) {
    }
}

