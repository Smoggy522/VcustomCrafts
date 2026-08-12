package io.github.smoggy522.vcustomcrafts.crafting;

import io.github.smoggy522.vcustomcrafts.recipe.ConsumeMode;
import io.github.smoggy522.vcustomcrafts.recipe.IngredientDefinition;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeDefinition;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeMatcher.MatchPlan;
import io.github.smoggy522.vcustomcrafts.util.Items;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class CraftTransaction {
    private final int maxShiftCrafts;

    public CraftTransaction(int maxShiftCrafts) {
        this.maxShiftCrafts = Math.max(1, maxShiftCrafts);
    }

    public Outcome execute(Player player, CraftingInventory crafting, RecipeDefinition recipe,
                           MatchPlan plan, boolean shiftClick, ItemStack cursor) {
        if (recipe.permission() != null && !recipe.permission().isBlank()
            && !player.hasPermission(recipe.permission())) {
            return Outcome.failure("requirements-failed");
        }
        if (!player.hasPermission("vcustomcrafts.craft")) {
            return Outcome.failure("requirements-failed");
        }

        ItemStack[] originalMatrix = cloneContents(crafting.getMatrix());
        ItemStack[] originalStorage = cloneContents(player.getInventory().getStorageContents());
        int originalLevel = player.getLevel();
        ItemStack originalCursor = Items.cloneOrNull(cursor);

        int attempts = shiftClick ? maximumCrafts(originalMatrix, plan) : 1;
        attempts = Math.min(attempts, maxShiftCrafts);
        if (recipe.experienceLevelCost() > 0) {
            attempts = Math.min(attempts, player.getLevel() / recipe.experienceLevelCost());
        }
        if (recipe.successChance() < 100.0 && !recipe.consumeOnFailure()) {
            attempts = Math.min(attempts, 1);
        }
        if (attempts < 1) {
            return Outcome.failure("requirements-failed");
        }

        ItemStack result = recipe.result();
        if (!shiftClick && !cursorAccepts(originalCursor, result)) {
            return Outcome.failure("inventory-full");
        }
        if (shiftClick) {
            attempts = fitAttempts(originalStorage, result, plan, attempts);
            if (attempts < 1) {
                return Outcome.failure("inventory-full");
            }
        } else if (!canFit(originalStorage, returnItems(plan, 1))) {
            return Outcome.failure("inventory-full");
        }

        ItemStack[] matrix = cloneContents(originalMatrix);
        List<ItemStack> outputs = new ArrayList<>();
        List<ItemStack> returns = new ArrayList<>();
        int successes = 0;
        int consumedAttempts = 0;
        try {
            for (int attempt = 0; attempt < attempts; attempt++) {
                boolean success = ThreadLocalRandom.current().nextDouble(100.0) < recipe.successChance();
                if (success || recipe.consumeOnFailure()) {
                    consume(matrix, plan, returns);
                    consumedAttempts++;
                }
                if (success) {
                    outputs.add(result.clone());
                    successes++;
                } else if (!recipe.consumeOnFailure()) {
                    break;
                }
            }

            crafting.setMatrix(matrix);
            int levelCost = successes * recipe.experienceLevelCost();
            player.setLevel(Math.max(0, player.getLevel() - levelCost));

            ItemStack newCursor = originalCursor;
            if (shiftClick) {
                addAll(player.getInventory(), outputs);
            } else if (!outputs.isEmpty()) {
                newCursor = mergeCursor(originalCursor, outputs.getFirst());
            }
            addAll(player.getInventory(), returns);
            player.updateInventory();
            return new Outcome(true, successes, consumedAttempts, newCursor,
                successes == 0 ? "craft-failed" : null);
        } catch (RuntimeException exception) {
            crafting.setMatrix(originalMatrix);
            player.getInventory().setStorageContents(originalStorage);
            player.setLevel(originalLevel);
            player.updateInventory();
            return new Outcome(false, 0, 0, originalCursor, "inventory-full");
        }
    }

    private int maximumCrafts(ItemStack[] matrix, MatchPlan plan) {
        int maximum = Integer.MAX_VALUE;
        for (Map.Entry<Integer, IngredientDefinition> entry : plan.assignments().entrySet()) {
            ItemStack item = matrix[entry.getKey()];
            IngredientDefinition ingredient = entry.getValue();
            int possible = switch (ingredient.consumeMode()) {
                case KEEP -> Integer.MAX_VALUE;
                case CONSUME, RETURN -> item.getAmount() / ingredient.requiredAmount();
                case DAMAGE -> damageCrafts(item, ingredient.damageAmount());
            };
            maximum = Math.min(maximum, possible);
        }
        return maximum == Integer.MAX_VALUE ? maxShiftCrafts : maximum;
    }

    private int fitAttempts(ItemStack[] storage, ItemStack result, MatchPlan plan, int requested) {
        for (int attempts = requested; attempts > 0; attempts--) {
            List<ItemStack> additions = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                additions.add(result.clone());
            }
            additions.addAll(returnItems(plan, attempts));
            if (canFit(storage, additions)) {
                return attempts;
            }
        }
        return 0;
    }

    private static List<ItemStack> returnItems(MatchPlan plan, int crafts) {
        List<ItemStack> returns = new ArrayList<>();
        for (IngredientDefinition ingredient : plan.assignments().values()) {
            if (ingredient.consumeMode() != ConsumeMode.RETURN || ingredient.returnItem() == null) {
                continue;
            }
            for (int i = 0; i < crafts; i++) {
                returns.add(ingredient.returnItem());
            }
        }
        return returns;
    }

    private static void consume(ItemStack[] matrix, MatchPlan plan, List<ItemStack> returns) {
        for (Map.Entry<Integer, IngredientDefinition> entry : plan.assignments().entrySet()) {
            int slot = entry.getKey();
            IngredientDefinition ingredient = entry.getValue();
            ItemStack item = matrix[slot];
            switch (ingredient.consumeMode()) {
                case KEEP -> {
                }
                case CONSUME -> matrix[slot] = subtract(item, ingredient.requiredAmount());
                case RETURN -> {
                    matrix[slot] = subtract(item, ingredient.requiredAmount());
                    if (ingredient.returnItem() != null) {
                        returns.add(ingredient.returnItem());
                    }
                }
                case DAMAGE -> matrix[slot] = damage(item, ingredient.damageAmount());
            }
        }
    }

    private static ItemStack subtract(ItemStack item, int amount) {
        int remaining = item.getAmount() - amount;
        if (remaining <= 0) {
            return null;
        }
        ItemStack updated = item.clone();
        updated.setAmount(remaining);
        return updated;
    }

    private static ItemStack damage(ItemStack item, int amount) {
        ItemStack updated = item.clone();
        ItemMeta meta = updated.getItemMeta();
        if (!(meta instanceof Damageable damageable) || updated.getType().getMaxDurability() <= 0) {
            throw new IllegalStateException("DAMAGE consume mode requires a damageable item");
        }
        int next = damageable.getDamage() + amount;
        if (next >= updated.getType().getMaxDurability()) {
            return updated.getAmount() > 1 ? subtract(updated, 1) : null;
        }
        damageable.setDamage(next);
        updated.setItemMeta(meta);
        return updated;
    }

    private static int damageCrafts(ItemStack item, int damageAmount) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || item.getType().getMaxDurability() <= 0) {
            return 0;
        }
        int remaining = item.getType().getMaxDurability() - damageable.getDamage();
        return Math.max(0, (remaining + damageAmount - 1) / damageAmount);
    }

    private static boolean cursorAccepts(ItemStack cursor, ItemStack result) {
        return Items.empty(cursor) || cursor.isSimilar(result)
            && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize();
    }

    private static ItemStack mergeCursor(ItemStack cursor, ItemStack result) {
        if (Items.empty(cursor)) {
            return result.clone();
        }
        ItemStack merged = cursor.clone();
        merged.setAmount(merged.getAmount() + result.getAmount());
        return merged;
    }

    private static boolean canFit(ItemStack[] original, List<ItemStack> additions) {
        ItemStack[] simulated = cloneContents(original);
        for (ItemStack addition : additions) {
            int remaining = addition.getAmount();
            for (ItemStack existing : simulated) {
                if (!Items.empty(existing) && existing.isSimilar(addition)) {
                    int accepted = Math.min(remaining, existing.getMaxStackSize() - existing.getAmount());
                    existing.setAmount(existing.getAmount() + accepted);
                    remaining -= accepted;
                    if (remaining == 0) break;
                }
            }
            for (int slot = 0; slot < simulated.length && remaining > 0; slot++) {
                if (Items.empty(simulated[slot])) {
                    ItemStack placed = addition.clone();
                    int accepted = Math.min(remaining, placed.getMaxStackSize());
                    placed.setAmount(accepted);
                    simulated[slot] = placed;
                    remaining -= accepted;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static void addAll(PlayerInventory inventory, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (!inventory.addItem(item.clone()).isEmpty()) {
                throw new IllegalStateException("Inventory capacity changed during craft");
            }
        }
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = Items.cloneOrNull(contents[i]);
        }
        return clone;
    }

    public record Outcome(boolean completed, int successes, int consumedAttempts,
                          ItemStack cursor, String messageKey) {
        public static Outcome failure(String messageKey) {
            return new Outcome(false, 0, 0, null, messageKey);
        }
    }
}

