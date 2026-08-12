package io.github.smoggy522.vcustomcrafts.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class SavedItemRepository {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public SavedItemRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "items/saved-items.yml");
        reload();
    }

    public synchronized void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save(String id, ItemStack item) throws IOException {
        ensureParent();
        data.set("items." + normalize(id), ItemCodec.encode(item));
        data.save(file);
    }

    public synchronized ItemStack get(String id) {
        String encoded = data.getString("items." + normalize(id));
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return ItemCodec.decode(encoded).clone();
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not decode saved item '" + id + "': " + exception.getMessage());
            return null;
        }
    }

    public synchronized Set<String> ids() {
        if (!data.isConfigurationSection("items")) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(data.getConfigurationSection("items").getKeys(false)));
    }

    private void ensureParent() throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

