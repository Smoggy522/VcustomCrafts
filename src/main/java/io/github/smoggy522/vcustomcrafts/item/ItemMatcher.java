package io.github.smoggy522.vcustomcrafts.item;

import io.github.smoggy522.vcustomcrafts.recipe.IngredientAlternative;
import io.github.smoggy522.vcustomcrafts.recipe.IngredientDefinition;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Objects;

public final class ItemMatcher {
    public boolean matches(ItemStack actual, IngredientDefinition expected) {
        if (actual == null || actual.getType().isAir() || actual.getAmount() < expected.requiredAmount()) {
            return false;
        }
        return expected.alternatives().stream().anyMatch(alternative -> matchesAlternative(actual, alternative, expected));
    }

    private boolean matchesAlternative(ItemStack actual, IngredientAlternative alternative,
                                       IngredientDefinition definition) {
        if (actual.getType() != alternative.material()) {
            return false;
        }
        ItemStack prototype = alternative.prototype();
        if (prototype == null || definition.matchMode() == MatchMode.MATERIAL) {
            return true;
        }
        if (definition.matchMode() == MatchMode.EXACT) {
            return actual.isSimilar(prototype);
        }

        ItemMeta actualMeta = actual.getItemMeta();
        ItemMeta expectedMeta = prototype.getItemMeta();
        if (actualMeta == null || expectedMeta == null) {
            return actualMeta == expectedMeta;
        }
        for (MatchField field : definition.matchFields()) {
            boolean equal = switch (field) {
                case NAME -> Objects.equals(actualMeta.displayName(), expectedMeta.displayName());
                case LORE -> Objects.equals(actualMeta.lore(), expectedMeta.lore());
                case CUSTOM_MODEL_DATA -> actualMeta.hasCustomModelData() == expectedMeta.hasCustomModelData()
                    && (!actualMeta.hasCustomModelData()
                    || actualMeta.getCustomModelData() == expectedMeta.getCustomModelData());
                case ENCHANTMENTS -> Objects.equals(actual.getEnchantments(), prototype.getEnchantments());
                case PDC -> Objects.equals(publicBukkitValues(actualMeta), publicBukkitValues(expectedMeta));
                case DAMAGE -> damage(actualMeta) == damage(expectedMeta);
            };
            if (!equal) {
                return false;
            }
        }
        return true;
    }

    private static Object publicBukkitValues(ItemMeta meta) {
        Map<String, Object> serialized = meta.serialize();
        Object value = serialized.get("PublicBukkitValues");
        return value == null ? serialized.get("public-bukkit-values") : value;
    }

    private static int damage(ItemMeta meta) {
        return meta instanceof Damageable damageable ? damageable.getDamage() : 0;
    }
}

