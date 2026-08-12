package io.github.smoggy522.vcustomcrafts.api;

import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProviderRegistry {
    private final Map<String, CustomItemProvider> providers = new ConcurrentHashMap<>();

    public void register(CustomItemProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isBlank()) {
            throw new IllegalArgumentException("Provider and provider ID are required");
        }
        providers.put(normalize(provider.id()), provider);
    }

    public boolean unregister(String id) {
        return providers.remove(normalize(id)) != null;
    }

    public ItemStack resolve(String providerId, String itemId) {
        CustomItemProvider provider = providers.get(normalize(providerId));
        if (provider == null) {
            return null;
        }
        ItemStack item = provider.item(itemId);
        return item == null ? null : item.clone();
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

