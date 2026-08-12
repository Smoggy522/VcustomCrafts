package io.github.smoggy522.vcustomcrafts.gui;

import io.github.smoggy522.vcustomcrafts.recipe.RecipeType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class EditorHolder implements InventoryHolder {
    private final String recipeId;
    private final RecipeType type;
    private Inventory inventory;

    public EditorHolder(String recipeId, RecipeType type) {
        this.recipeId = recipeId;
        this.type = type;
    }

    public String recipeId() {
        return recipeId;
    }

    public RecipeType type() {
        return type;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Editor inventory has not been initialized");
        }
        return inventory;
    }
}

