package io.github.smoggy522.vcustomcrafts.api;

import io.github.smoggy522.vcustomcrafts.recipe.RecipeDefinition;

import java.util.Collection;
import java.util.Optional;

public interface VcustomCraftsApi {
    Optional<RecipeDefinition> recipe(String id);

    Collection<RecipeDefinition> recipes();

    void registerProvider(CustomItemProvider provider);

    boolean unregisterProvider(String id);

    int reloadRecipes();
}

