package io.github.smoggy522.vcustomcrafts.command;

import io.github.smoggy522.vcustomcrafts.gui.RecipeBook;
import io.github.smoggy522.vcustomcrafts.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RecipesCommand implements CommandExecutor {
    private final RecipeBook recipeBook;
    private final Messages messages;

    public RecipesCommand(RecipeBook recipeBook, Messages messages) {
        this.recipeBook = recipeBook;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("vcustomcrafts.use")) {
            messages.send(player, "no-permission");
            return true;
        }
        recipeBook.open(player);
        return true;
    }
}

