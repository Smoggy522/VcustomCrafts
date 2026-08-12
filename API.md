# Developer API

Add VcustomCrafts as a compile-only dependency, declare it as a `softdepend` or `depend`, and access the API after both plugins are enabled:

```java
VcustomCraftsApi api = VcustomCraftsPlugin.getApi();

api.recipe("ruby_blade").ifPresent(recipe ->
    getLogger().info("Result: " + recipe.result().getType()));
```

## Custom item provider

```java
api.registerProvider(new CustomItemProvider() {
    @Override
    public String id() {
        return "myitems";
    }

    @Override
    public ItemStack item(String itemId) {
        return myItemService.create(itemId); // null when the ID is unknown
    }
});

api.reloadRecipes();
```

Recipes can then refer to:

```yaml
provider: myitems
id: namespace:ruby
match-mode: EXACT
```

The provider must return a new or safely cloneable `ItemStack`. VcustomCrafts clones the returned item before storing it in a compiled recipe.

