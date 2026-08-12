package io.github.smoggy522.vcustomcrafts.command;

import io.github.smoggy522.vcustomcrafts.VcustomCraftsPlugin;
import io.github.smoggy522.vcustomcrafts.gui.RecipeEditor;
import io.github.smoggy522.vcustomcrafts.item.SavedItemRepository;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeRegistry;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeType;
import io.github.smoggy522.vcustomcrafts.update.GitHubUpdateService;
import io.github.smoggy522.vcustomcrafts.util.Items;
import io.github.smoggy522.vcustomcrafts.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VcustomCraftsCommand implements CommandExecutor, TabCompleter {
    private final VcustomCraftsPlugin plugin;
    private final SavedItemRepository savedItems;
    private final RecipeEditor editor;
    private final GitHubUpdateService updates;
    private final Messages messages;

    public VcustomCraftsCommand(VcustomCraftsPlugin plugin, SavedItemRepository savedItems,
                                RecipeEditor editor, GitHubUpdateService updates, Messages messages) {
        this.plugin = plugin;
        this.savedItems = savedItems;
        this.editor = editor;
        this.updates = updates;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "editor" -> editor(sender, args);
            case "item" -> item(sender, args);
            case "update" -> update(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!permission(sender, "vcustomcrafts.admin.reload")) return;
        RecipeRegistry.LoadReport report = plugin.reloadAll();
        messages.send(sender, "reloaded", Map.of("recipes", report.loaded(), "errors", report.rejected()));
    }

    private void editor(CommandSender sender, String[] args) {
        if (!permission(sender, "vcustomcrafts.admin.editor")) return;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 2 || !args[1].matches("[a-z0-9_-]+")) {
            player.sendMessage(Component.text("Usage: /vcc editor <recipe_id> [shaped|shapeless]", NamedTextColor.RED));
            return;
        }
        RecipeType type = args.length >= 3 && args[2].equalsIgnoreCase("shapeless")
            ? RecipeType.SHAPELESS : RecipeType.SHAPED;
        editor.open(player, args[1], type);
    }

    private void item(CommandSender sender, String[] args) {
        if (!permission(sender, "vcustomcrafts.admin.items")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /vcc item <save|give|list>", NamedTextColor.RED));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "save" -> saveItem(sender, args);
            case "give" -> giveItem(sender, args);
            case "list" -> sender.sendMessage(Component.text("Saved items: " + String.join(", ", savedItems.ids()),
                NamedTextColor.LIGHT_PURPLE));
            default -> sender.sendMessage(Component.text("Usage: /vcc item <save|give|list>", NamedTextColor.RED));
        }
    }

    private void saveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 3 || !validItemId(args[2])) {
            messages.send(sender, "invalid-id");
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (Items.empty(item)) {
            messages.send(sender, "hold-item");
            return;
        }
        try {
            savedItems.save(args[2], item);
            messages.send(sender, "item-saved", Map.of("id", args[2]));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save item: " + exception.getMessage());
            sender.sendMessage(Component.text("Could not save item: " + exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void giveItem(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /vcc item give <player> <id>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Player is not online.", NamedTextColor.RED));
            return;
        }
        ItemStack item = savedItems.get(args[3]);
        if (item == null) {
            messages.send(sender, "item-not-found", Map.of("id", args[3]));
            return;
        }
        target.getInventory().addItem(item).values().forEach(leftover ->
            target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        messages.send(sender, "item-given", Map.of("id", args[3], "player", target.getName()));
    }

    private void update(CommandSender sender, String[] args) {
        if (!permission(sender, "vcustomcrafts.admin.update")) return;
        boolean download = args.length >= 2 && args[1].equalsIgnoreCase("download");
        updates.check(sender, download);
    }

    private boolean permission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("vcustomcrafts.admin")) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("VcustomCrafts " + plugin.getPluginMeta().getVersion(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/recipes", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/vcc editor <id> [shaped|shapeless]", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/vcc item save <id>", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/vcc item give <player> <id>", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/vcc reload", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/vcc update <check|download>", NamedTextColor.WHITE));
    }

    private static boolean validItemId(String value) {
        return value.matches("[a-z0-9_/-]+");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("help", "reload", "editor", "item", "update"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            choices.addAll(List.of("save", "give", "list"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
            choices.addAll(List.of("check", "download"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("editor")) {
            choices.addAll(List.of("shaped", "shapeless"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName()));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
            choices.addAll(savedItems.ids());
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
