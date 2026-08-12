package io.github.smoggy522.vcustomcrafts.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record IngredientAlternative(Material material, ItemStack prototype) {
    public IngredientAlternative {
        if (material == null || material.isAir()) {
            throw new IllegalArgumentException("Ingredient material cannot be air");
        }
        prototype = prototype == null ? null : prototype.clone();
    }

    @Override
    public ItemStack prototype() {
        return prototype == null ? null : prototype.clone();
    }
}

