package dev.wonkypigs.cosmiclifesteal;
import com.tchristofferson.configupdater.ConfigUpdater;
import dev.wonkypigs.cosmiclifesteal.Commands.DeathbanCommand;
import dev.wonkypigs.cosmiclifesteal.Commands.LifestealCommand;
import dev.wonkypigs.cosmiclifesteal.Helpers.TabCompleters.DeathbanTabCompleter;
import dev.wonkypigs.cosmiclifesteal.Helpers.TabCompleters.LifestealTabCompleter;
import dev.wonkypigs.cosmiclifesteal.Listeners.HistoryMenuListener;
import dev.wonkypigs.cosmiclifesteal.Listeners.PlayerDeathListener;
import dev.wonkypigs.cosmiclifesteal.Listeners.PlayerJoinListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public final class CosmicLifesteal extends JavaPlugin {

    private static CosmicLifesteal instance;{ instance = this; }
    private Connection connection;
    public String host, database, username, password, prefix;
    public String noPermMessage, bePlayerMessage, invalidPlayerMessage, invalidArgumentsMessage, messageOnDeath, messageOnKill, lifestealForHelp, deathbanForHelp;
    public int port;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // if config version is old, update it to current version
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (config.getDouble("config-version") != 1.2) {
            config.set("config-version", 1.2);
            try {
                ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("none"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // save changes
            try {
                config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // reload config
            reloadConfig();
            getLogger().info("Updated config file to latest version");
        }

        registerCommands();
        registerListeners();
        registerPermissions();
        mySqlSetup();
        getConfigValues();
        setupMessages();

        UpdateChecker updateChecker = new UpdateChecker();
        updateChecker.check();

        int pluginId = 17076; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {getLogger().info("CosmicLifesteal has been disabled!");}

    public void registerCommands() {
        // Registering all plugin commands
        getServer().getPluginCommand("lifesteal").setExecutor(new LifestealCommand());
        getCommand("lifesteal").setTabCompleter(new LifestealTabCompleter());

        getServer().getPluginCommand("deathban").setExecutor(new DeathbanCommand());
        getCommand("deathban").setTabCompleter(new DeathbanTabCompleter());
    }

    public void registerListeners() {
        // Registering all plugin listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new HistoryMenuListener(), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(), this);
    }

    public void registerPermissions() {
        // Registering all plugin permissions
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.reload"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.hearts"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.hearts.add"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.hearts.remove"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.hearts.set"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.command.hearts.get"));

        getServer().getPluginManager().addPermission(new Permission("lifesteal.deathban"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.deathban.ban"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.deathban.unban"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.deathban.check"));
        getServer().getPluginManager().addPermission(new Permission("lifesteal.deathban.history"));
    }

    public void getConfigValues() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            host = YamlConfiguration.loadConfiguration(file).getString("database.host");
            port = YamlConfiguration.loadConfiguration(file).getInt("database.port");
            database = YamlConfiguration.loadConfiguration(file).getString("database.database");
            username = YamlConfiguration.loadConfiguration(file).getString("database.username");
            password = YamlConfiguration.loadConfiguration(file).getString("database.password");
            prefix = YamlConfiguration.loadConfiguration(file).getString("messages.prefix").replace("&", "§");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupMessages() {
        // Setting up all plugin messages
        noPermMessage = getConfig().getString("messages.no-permission")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        bePlayerMessage = getConfig().getString("messages.must-be-player")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        invalidPlayerMessage = getConfig().getString("messages.invalid-player")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        invalidArgumentsMessage = getConfig().getString("messages.invalid-arguments")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        messageOnDeath = getConfig().getString("messages.message-on-death")
                .replace("{prefix}", prefix)
                .replace("{hearts}", String.valueOf(getConfig().getInt("settings.hearts-lost-on-death")))
                .replace("&", "§");
        messageOnKill = getConfig().getString("messages.message-on-kill")
                .replace("{prefix}", prefix)
                .replace("{hearts}", String.valueOf(getConfig().getInt("settings.hearts-gained-on-kill")))
                .replace("&", "§");
        lifestealForHelp = "&cUnknown command. Type /lifesteal for help.".replace("&", "§");
        deathbanForHelp = "&cUnknown command. Type /deathban for help.".replace("&", "§");
    }

    public void mySqlSetup() {
        try {
            synchronized (this) {
                if (getConnection() != null && !getConnection().isClosed()) {
                    return;
                }

                if (getConfig().getString("database.type").equalsIgnoreCase("sqlite")) {
                    // create local database file and stuff
                    Class.forName("org.sqlite.JDBC");
                    File file = new File(getDataFolder(), "database.db");
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    setConnection(DriverManager.getConnection("jdbc:sqlite:" + file));
                } else {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    // create database if not exists
                    setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "?autoReconnect=true&useSSL=false", username, password));
                    getConnection().createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
                    setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password));
                }
                getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS hearts (UUID varchar(50), HEARTS int)").executeUpdate();
                getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS deathbans (UUID varchar(50), EXPIRY int)").executeUpdate();
                getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS deathban_history (UUID varchar(50), INITIATED int, EXPIRY int, ID int)").executeUpdate();
                getLogger().info("Successfully connected to the MySQL database");
            }
        } catch (SQLException | ClassNotFoundException | IOException e) {
            getLogger().info("Error connecting to the MySQL database");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static CosmicLifesteal getInstance() {
        return instance;
    }

}
