package io.github.smoggy522.vcustomcrafts;

import io.github.smoggy522.vcustomcrafts.api.ProviderRegistry;
import io.github.smoggy522.vcustomcrafts.api.VcustomCraftsApi;
import io.github.smoggy522.vcustomcrafts.api.VcustomCraftsApiImpl;
import io.github.smoggy522.vcustomcrafts.command.RecipesCommand;
import io.github.smoggy522.vcustomcrafts.command.VcustomCraftsCommand;
import io.github.smoggy522.vcustomcrafts.crafting.CraftTransaction;
import io.github.smoggy522.vcustomcrafts.crafting.CraftingListener;
import io.github.smoggy522.vcustomcrafts.gui.RecipeBook;
import io.github.smoggy522.vcustomcrafts.gui.RecipeEditor;
import io.github.smoggy522.vcustomcrafts.item.ItemMatcher;
import io.github.smoggy522.vcustomcrafts.item.SavedItemRepository;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeMatcher;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeParser;
import io.github.smoggy522.vcustomcrafts.recipe.RecipeRegistry;
import io.github.smoggy522.vcustomcrafts.update.GitHubUpdateService;
import io.github.smoggy522.vcustomcrafts.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VcustomCraftsPlugin extends JavaPlugin {
    private static VcustomCraftsApi api;

    private Messages messages;
    private SavedItemRepository savedItems;
    private RecipeRegistry recipeRegistry;
    private GitHubUpdateService updateService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledExample();

        messages = new Messages(this);
        savedItems = new SavedItemRepository(this);
        ProviderRegistry providers = new ProviderRegistry();
        RecipeParser parser = new RecipeParser(this, savedItems, providers);
        recipeRegistry = new RecipeRegistry(this, parser);
        RecipeMatcher matcher = new RecipeMatcher(new ItemMatcher());
        CraftTransaction transaction = new CraftTransaction(getConfig().getInt("crafting.max-shift-crafts", 64));
        RecipeEditor editor = new RecipeEditor(this, recipeRegistry, messages);
        RecipeBook book = new RecipeBook(recipeRegistry);
        updateService = new GitHubUpdateService(this, messages);

        getServer().getPluginManager().registerEvents(new CraftingListener(recipeRegistry, matcher, transaction, messages), this);
        getServer().getPluginManager().registerEvents(editor, this);
        getServer().getPluginManager().registerEvents(book, this);

        VcustomCraftsCommand adminCommand = new VcustomCraftsCommand(this, savedItems, editor, updateService, messages);
        PluginCommand vcc = requireCommand("vcustomcrafts");
        vcc.setExecutor(adminCommand);
        vcc.setTabCompleter(adminCommand);
        requireCommand("recipes").setExecutor(new RecipesCommand(book, messages));

        api = new VcustomCraftsApiImpl(this, providers);
        RecipeRegistry.LoadReport report = recipeRegistry.reload();
        getLogger().info("Ready. Loaded " + report.loaded() + " recipes; rejected " + report.rejected() + ".");
        updateService.start();
    }

    @Override
    public void onDisable() {
        api = null;
    }

    public static VcustomCraftsApi getApi() {
        if (api == null) {
            throw new IllegalStateException("VcustomCrafts is not enabled");
        }
        return api;
    }

    public static VcustomCraftsApi getAPI() {
        return getApi();
    }

    public RecipeRegistry.LoadReport reloadAll() {
        reloadConfig();
        messages.reload();
        savedItems.reload();
        return recipeRegistry.reload();
    }

    public RecipeRegistry recipeRegistry() {
        return recipeRegistry;
    }

    private void saveBundledExample() {
        if (!new java.io.File(getDataFolder(), "recipes/example_crystal.yml").exists()) {
            saveResource("recipes/example_crystal.yml", false);
        }
    }

    private PluginCommand requireCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' is missing from plugin.yml");
        }
        return command;
    }
}
