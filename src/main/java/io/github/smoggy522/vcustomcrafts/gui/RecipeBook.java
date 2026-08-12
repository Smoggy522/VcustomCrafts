package io.github.smoggy522.vcustomcrafts.gui;

import io.github.smoggy522.vcustomcrafts.recipe.RecipeDefinition;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecipeBook implements Listener {
    private final RecipeRegistry registry;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public RecipeBook(RecipeRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player) {
        RecipeBookHolder holder = new RecipeBookHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54,
            Component.text("VcustomCrafts • Recipes", NamedTextColor.DARK_PURPLE));
        holder.inventory(inventory);
        List<RecipeDefinition> recipes = registry.all().stream()
            .filter(recipe -> recipe.permission() == null || recipe.permission().isBlank()
                || player.hasPermission(recipe.permission()))
            .sorted(Comparator.comparing(RecipeDefinition::id))
            .limit(54)
            .toList();
        for (int slot = 0; slot < recipes.size(); slot++) {
            RecipeDefinition recipe = recipes.get(slot);
            ItemStack icon = recipe.result();
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(miniMessage.deserialize(recipe.displayName()));
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text("ID: " + recipe.id(), NamedTextColor.DARK_GRAY));
            lore.add(Component.text("Type: " + recipe.type(), NamedTextColor.GRAY));
            lore.add(Component.text("Success: " + recipe.successChance() + "%", NamedTextColor.GRAY));
            meta.lore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RecipeBookHolder) {
            event.setCancelled(true);
        }
    }
}

