package com.alpsbte.pSMigrationPlugin;

import com.alpsbte.alpslib.io.YamlFileFactory;
import com.alpsbte.alpslib.io.config.ConfigNotImplementedException;
import com.alpsbte.pSMigrationPlugin.commands.CMD_GenerateLanguageSection;
import com.alpsbte.pSMigrationPlugin.commands.CMD_MigratePlots;
import com.alpsbte.pSMigrationPlugin.core.config.ConfigUtil;
import com.alpsbte.pSMigrationPlugin.core.database.DatabaseV1Connection;
import com.alpsbte.pSMigrationPlugin.core.database.DatabaseV2Connection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public final class PSMigrationPlugin extends JavaPlugin {
    private static PSMigrationPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;

        // Init Config
        try {
            YamlFileFactory.registerPlugin(this);
            ConfigUtil.init();
        } catch (ConfigNotImplementedException ex) {
            getComponentLogger().warn(text("Could not load configuration file."));
            Bukkit.getConsoleSender().sendMessage(text("The config file must be configured!", YELLOW));
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();

        // Initialize database connections
        try {
            DatabaseV1Connection.InitializeDatabase();
            DatabaseV2Connection.InitializeDatabase();
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(text("Could not initialize database connection."));
            getComponentLogger().error(text(ex.getMessage()), ex);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // register commands
        Objects.requireNonNull(getCommand("migrateplots")).setExecutor(new CMD_MigratePlots());
        Objects.requireNonNull(getCommand("generatecitylang")).setExecutor(new CMD_GenerateLanguageSection());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return ConfigUtil.getInstance().configs[0];
    }

    @Override
    public void reloadConfig() {
        ConfigUtil.getInstance().reloadFiles();
        ConfigUtil.getInstance().saveFiles();
    }

    @Override
    public void saveConfig() {
        ConfigUtil.getInstance().saveFiles();
    }

    public static PSMigrationPlugin getPlugin() {
        return plugin;
    }
}
