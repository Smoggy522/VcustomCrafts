package io.github.smoggy522.vcustomcrafts.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class Messages {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration language;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String locale = plugin.getConfig().getString("language", "en_US");
        String resource = "lang/" + locale + ".yml";
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Unknown language '" + locale + "'; using en_US");
                resource = "lang/en_US.yml";
                file = new File(plugin.getDataFolder(), resource);
                if (!file.exists()) {
                    plugin.saveResource(resource, false);
                }
            }
        }
        language = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender receiver, String key) {
        send(receiver, key, Map.of());
    }

    public void send(CommandSender receiver, String key, Map<String, ?> replacements) {
        receiver.sendMessage(component(key, replacements));
    }

    public Component component(String key, Map<String, ?> replacements) {
        String prefix = language.getString("prefix", "");
        String value = language.getString(key, "<red>Missing message: " + key);
        for (Map.Entry<String, ?> entry : replacements.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return miniMessage.deserialize(prefix + value);
    }
}

