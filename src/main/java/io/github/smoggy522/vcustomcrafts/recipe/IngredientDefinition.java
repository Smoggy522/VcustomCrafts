package io.github.smoggy522.vcustomcrafts.recipe;

import io.github.smoggy522.vcustomcrafts.item.MatchField;
import io.github.smoggy522.vcustomcrafts.item.MatchMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IngredientDefinition {
    private final List<IngredientAlternative> alternatives;
    private final MatchMode matchMode;
    private final EnumSet<MatchField> matchFields;
    private final int requiredAmount;
    private final ConsumeMode consumeMode;
    private final int damageAmount;
    private final ItemStack returnItem;

    public IngredientDefinition(List<IngredientAlternative> alternatives, MatchMode matchMode,
                                EnumSet<MatchField> matchFields, int requiredAmount,
                                ConsumeMode consumeMode, int damageAmount, ItemStack returnItem) {
        if (alternatives == null || alternatives.isEmpty()) {
            throw new IllegalArgumentException("An ingredient needs at least one alternative");
        }
        if (requiredAmount < 1 || requiredAmount > 64) {
            throw new IllegalArgumentException("Ingredient amount must be between 1 and 64");
        }
        this.alternatives = List.copyOf(alternatives);
        this.matchMode = matchMode;
        this.matchFields = matchFields.clone();
        this.requiredAmount = requiredAmount;
        this.consumeMode = consumeMode;
        this.damageAmount = Math.max(1, damageAmount);
        this.returnItem = returnItem == null ? null : returnItem.clone();
    }

    public List<IngredientAlternative> alternatives() {
        return alternatives;
    }

    public MatchMode matchMode() {
        return matchMode;
    }

    public Set<MatchField> matchFields() {
        return Collections.unmodifiableSet(matchFields);
    }

    public int requiredAmount() {
        return requiredAmount;
    }

    public ConsumeMode consumeMode() {
        return consumeMode;
    }

    public int damageAmount() {
        return damageAmount;
    }

    public ItemStack returnItem() {
        return returnItem == null ? null : returnItem.clone();
    }

    public RecipeChoice registrationChoice() {
        Set<Material> materials = new LinkedHashSet<>();
        for (IngredientAlternative alternative : alternatives) {
            materials.add(alternative.material());
        }
        return new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
    }
}

