package io.github.smoggy522.vcustomcrafts.gui;

import io.github.smoggy522.vcustomcrafts.item.ItemCodec;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeRegistry;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeType;
import io.github.smoggy522.vcustomcrafts.util.Items;
import io.github.smoggy522.vcustomcrafts.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeEditor implements Listener {
    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final Set<Integer> GRID_SET = Set.of(10, 11, 12, 19, 20, 21, 28, 29, 30);
    private static final int RESULT = 25;
    private static final int SAVE = 49;
    private static final char[] SYMBOLS = "ABCDEFGHI".toCharArray();

    private final JavaPlugin plugin;
    private final RecipeRegistry registry;
    private final Messages messages;

    public RecipeEditor(JavaPlugin plugin, RecipeRegistry registry, Messages messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.messages = messages;
    }

    public void open(Player player, String recipeId, RecipeType type) {
        EditorHolder holder = new EditorHolder(recipeId, type);
        Inventory inventory = Bukkit.createInventory(holder, 54,
            Component.text("VcustomCrafts • " + recipeId, NamedTextColor.DARK_PURPLE));
        holder.inventory(inventory);

        ItemStack background = Items.named(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, background);
        }
        for (int slot : GRID) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT, null);
        inventory.setItem(23, Items.named(Material.ARROW, Component.text("Result →", NamedTextColor.GRAY)));
        inventory.setItem(47, Items.named(type == RecipeType.SHAPED ? Material.CRAFTING_TABLE : Material.BUNDLE,
            Component.text(type.name(), NamedTextColor.LIGHT_PURPLE),
            Component.text("Selected from the command", NamedTextColor.GRAY)));
        inventory.setItem(SAVE, Items.named(Material.LIME_DYE,
            Component.text("Save recipe", NamedTextColor.GREEN),
            Component.text("Items are returned after saving", NamedTextColor.GRAY)));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder holder)) {
            return;
        }
        if (event.isShiftClick() || event.getClick().isKeyboardClick()
            || event.getClick().isCreativeAction() || event.getClick().name().contains("DOUBLE")) {
            event.setCancelled(true);
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < top.getSize() && rawSlot == SAVE) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                save(player, top, holder);
            }
            return;
        }
        if (rawSlot < top.getSize() && !editable(rawSlot)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize() && !editable(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof EditorHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        List<ItemStack> returned = new ArrayList<>();
        for (int slot : GRID) {
            if (!Items.empty(inventory.getItem(slot))) {
                returned.add(inventory.getItem(slot).clone());
                inventory.setItem(slot, null);
            }
        }
        if (!Items.empty(inventory.getItem(RESULT))) {
            returned.add(inventory.getItem(RESULT).clone());
            inventory.setItem(RESULT, null);
        }
        for (ItemStack item : returned) {
            player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private void save(Player player, Inventory inventory, EditorHolder holder) {
        ItemStack result = inventory.getItem(RESULT);
        List<Integer> occupied = new ArrayList<>();
        for (int slot : GRID) {
            if (!Items.empty(inventory.getItem(slot))) {
                occupied.add(slot);
            }
        }
        if (occupied.isEmpty() || Items.empty(result)) {
            messages.send(player, "recipe-invalid");
            return;
        }

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("id", holder.recipeId());
            config.set("enabled", true);
            config.set("name", holder.recipeId());
            config.set("type", holder.type().name());
            if (holder.type() == RecipeType.SHAPED) {
                saveShaped(config, inventory);
            } else {
                saveShapeless(config, inventory);
            }
            ItemStack resultSnapshot = result.clone();
            int resultAmount = resultSnapshot.getAmount();
            resultSnapshot.setAmount(1);
            config.set("result.snapshot", ItemCodec.encode(resultSnapshot));
            config.set("result.amount", resultAmount);
            config.set("requirements.permission", "vcustomcrafts.craft");
            config.set("success-chance", 100.0);

            File file = new File(plugin.getDataFolder(), "recipes/generated/" + holder.recipeId() + ".yml");
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create " + parent);
            }
            config.save(file);
            RecipeRegistry.LoadReport report = registry.reload();
            if (registry.byId(holder.recipeId()).isEmpty()) {
                String error = report.errors().isEmpty() ? "recipe was rejected" : report.errors().getLast();
                throw new IOException(error);
            }
            messages.send(player, "recipe-saved", Map.of("id", holder.recipeId()));
            player.closeInventory();
        } catch (Exception exception) {
            messages.send(player, "recipe-save-failed", Map.of("error", exception.getMessage()));
            plugin.getLogger().warning("Editor could not save recipe " + holder.recipeId() + ": " + exception.getMessage());
        }
    }

    private static void saveShaped(YamlConfiguration config, Inventory inventory) throws IOException {
        List<String> shape = new ArrayList<>();
        Map<Character, ItemStack> ingredients = new LinkedHashMap<>();
        int symbolIndex = 0;
        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder(3);
            for (int column = 0; column < 3; column++) {
                ItemStack item = inventory.getItem(GRID[row * 3 + column]);
                if (Items.empty(item)) {
                    line.append(' ');
                } else {
                    char symbol = SYMBOLS[symbolIndex++];
                    line.append(symbol);
                    ingredients.put(symbol, item.clone());
                }
            }
            shape.add(line.toString());
        }
        config.set("shape", shape);
        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack snapshot = entry.getValue().clone();
            int amount = snapshot.getAmount();
            snapshot.setAmount(1);
            String path = "ingredients." + entry.getKey();
            config.set(path + ".snapshot", ItemCodec.encode(snapshot));
            config.set(path + ".amount", amount);
            config.set(path + ".match-mode", "EXACT");
            config.set(path + ".consume-mode", "CONSUME");
        }
    }

    private static void saveShapeless(YamlConfiguration config, Inventory inventory) throws IOException {
        List<Map<String, Object>> ingredients = new ArrayList<>();
        for (int slot : GRID) {
            ItemStack item = inventory.getItem(slot);
            if (Items.empty(item)) {
                continue;
            }
            ItemStack snapshot = item.clone();
            int amount = snapshot.getAmount();
            snapshot.setAmount(1);
            Map<String, Object> definition = new LinkedHashMap<>();
            definition.put("snapshot", ItemCodec.encode(snapshot));
            definition.put("amount", amount);
            definition.put("match-mode", "EXACT");
            definition.put("consume-mode", "CONSUME");
            ingredients.add(definition);
        }
        config.set("ingredients", ingredients);
    }

    private static boolean editable(int rawSlot) {
        return rawSlot == RESULT || GRID_SET.contains(rawSlot);
    }
}
