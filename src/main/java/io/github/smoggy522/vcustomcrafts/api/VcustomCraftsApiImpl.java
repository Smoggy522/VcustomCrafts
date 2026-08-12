package io.github.smoggy522.vcustomcrafts.api;

import io.github.smoggy522.vcustomcrafts.VcustomCraftsPlugin;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeDefinition;

import java.util.Collection;
import java.util.Optional;

public final class VcustomCraftsApiImpl implements VcustomCraftsApi {
    private final VcustomCraftsPlugin plugin;
    private final ProviderRegistry providers;

    public VcustomCraftsApiImpl(VcustomCraftsPlugin plugin, ProviderRegistry providers) {
        this.plugin = plugin;
        this.providers = providers;
    }

    @Override
    public Optional<RecipeDefinition> recipe(String id) {
        return plugin.recipeRegistry().byId(id);
    }

    @Override
    public Collection<RecipeDefinition> recipes() {
        return plugin.recipeRegistry().all();
    }

    @Override
    public void registerProvider(CustomItemProvider provider) {
        providers.register(provider);
    }

    @Override
    public boolean unregisterProvider(String id) {
        return providers.unregister(id);
    }

    @Override
    public int reloadRecipes() {
        return plugin.reloadAll().loaded();
    }
}

