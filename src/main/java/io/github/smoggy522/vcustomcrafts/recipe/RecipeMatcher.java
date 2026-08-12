package io.github.smoggy522.vcustomcrafts.recipe;

import io.github.smoggy522.vcustomcrafts.item.ItemMatcher;
import io.github.smoggy522.vcustomcrafts.util.Items;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeMatcher {
    private final ItemMatcher itemMatcher;

    public RecipeMatcher(ItemMatcher itemMatcher) {
        this.itemMatcher = itemMatcher;
    }

    public MatchPlan match(RecipeDefinition recipe, ItemStack[] matrix) {
        return recipe.type() == RecipeType.SHAPED ? matchShaped(recipe, matrix) : matchShapeless(recipe, matrix);
    }

    private MatchPlan matchShaped(RecipeDefinition recipe, ItemStack[] matrix) {
        if (matrix.length < 9) {
            return null;
        }
        Map<Integer, IngredientDefinition> assignments = new LinkedHashMap<>();
        for (int row = 0; row < 3; row++) {
            String shapeRow = row < recipe.shape().size() ? recipe.shape().get(row) : "";
            for (int column = 0; column < 3; column++) {
                int slot = row * 3 + column;
                char symbol = column < shapeRow.length() ? shapeRow.charAt(column) : ' ';
                ItemStack actual = matrix[slot];
                if (symbol == ' ') {
                    if (!Items.empty(actual)) {
                        return null;
                    }
                    continue;
                }
                IngredientDefinition expected = recipe.shapedIngredients().get(symbol);
                if (expected == null || !itemMatcher.matches(actual, expected)) {
                    return null;
                }
                assignments.put(slot, expected);
            }
        }
        return assignments.isEmpty() ? null : new MatchPlan(assignments);
    }

    private MatchPlan matchShapeless(RecipeDefinition recipe, ItemStack[] matrix) {
        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < matrix.length; i++) {
            if (!Items.empty(matrix[i])) {
                occupied.add(i);
            }
        }
        if (occupied.size() != recipe.shapelessIngredients().size()) {
            return null;
        }
        Map<Integer, IngredientDefinition> assignments = new LinkedHashMap<>();
        boolean[] used = new boolean[recipe.shapelessIngredients().size()];
        return assign(0, occupied, matrix, recipe.shapelessIngredients(), used, assignments)
            ? new MatchPlan(assignments) : null;
    }

    private boolean assign(int index, List<Integer> occupied, ItemStack[] matrix,
                           List<IngredientDefinition> ingredients, boolean[] used,
                           Map<Integer, IngredientDefinition> assignments) {
        if (index == occupied.size()) {
            return true;
        }
        int slot = occupied.get(index);
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            IngredientDefinition ingredient = ingredients.get(ingredientIndex);
            if (!used[ingredientIndex] && itemMatcher.matches(matrix[slot], ingredient)) {
                used[ingredientIndex] = true;
                assignments.put(slot, ingredient);
                if (assign(index + 1, occupied, matrix, ingredients, used, assignments)) {
                    return true;
                }
                assignments.remove(slot);
                used[ingredientIndex] = false;
            }
        }
        return false;
    }

    public record MatchPlan(Map<Integer, IngredientDefinition> assignments) {
        public MatchPlan {
            assignments = Map.copyOf(assignments);
        }
    }
}

