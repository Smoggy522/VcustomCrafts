package io.github.smoggy522.vcustomcrafts.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class RecipeBookHolder implements InventoryHolder {
    private Inventory inventory;

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Recipe book has not been initialized");
        }
        return inventory;
    }
}

