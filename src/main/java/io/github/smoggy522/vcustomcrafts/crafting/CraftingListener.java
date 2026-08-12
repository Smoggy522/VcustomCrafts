package io.github.smoggy522.vcustomcrafts.crafting;

import io.github.smoggy522.vcustomcrafts.recipe.RecipeDefinition;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeMatcher;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeMatcher.MatchPlan;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeRegistry;
import io.github.smoggy522.vcustomcrafts.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

public final class CraftingListener implements Listener {
    private final RecipeRegistry registry;
    private final RecipeMatcher matcher;
    private final CraftTransaction transaction;
    private final Messages messages;

    public CraftingListener(RecipeRegistry registry, RecipeMatcher matcher,
                            CraftTransaction transaction, Messages messages) {
        this.registry = registry;
        this.matcher = matcher;
        this.transaction = transaction;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        RecipeDefinition definition = registry.byRecipe(event.getRecipe());
        if (definition == null) {
            return;
        }
        MatchPlan plan = matcher.match(definition, event.getInventory().getMatrix());
        if (plan == null) {
            event.getInventory().setResult(null);
            return;
        }
        if (!event.getViewers().isEmpty() && event.getViewers().getFirst() instanceof Player player
            && definition.permission() != null && !definition.permission().isBlank()
            && !player.hasPermission(definition.permission())) {
            event.getInventory().setResult(null);
            return;
        }
        event.getInventory().setResult(definition.result());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        RecipeDefinition definition = registry.byRecipe(event.getRecipe());
        if (definition == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT
            && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) {
            return;
        }
        CraftingInventory inventory = event.getInventory();
        MatchPlan plan = matcher.match(definition, inventory.getMatrix());
        if (plan == null) {
            inventory.setResult(null);
            return;
        }
        CraftTransaction.Outcome outcome = transaction.execute(player, inventory, definition, plan,
            event.isShiftClick(), event.getCursor());
        if (!event.isShiftClick() && outcome.cursor() != null) {
            player.setItemOnCursor(outcome.cursor());
        }
        if (outcome.messageKey() != null) {
            messages.send(player, outcome.messageKey());
        }
    }
}
