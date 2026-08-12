package io.github.smoggy522.vcustomcrafts.api;

import org.bukkit.inventory.ItemStack;

/**
 * Extension point for item plugins. Providers can be registered at runtime and
 * become available after VcustomCrafts is reloaded.
 */
public interface CustomItemProvider {
    String id();

    ItemStack item(String itemId);

    default boolean matches(ItemStack actual, String itemId) {
        ItemStack expected = item(itemId);
        return expected != null && actual != null && actual.isSimilar(expected);
    }
}

