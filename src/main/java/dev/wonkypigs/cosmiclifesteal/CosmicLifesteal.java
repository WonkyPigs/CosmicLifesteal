package dev.wonkypigs.cosmiclifesteal;
import dev.wonkypigs.cosmiclifesteal.Commands.DeathbanCommand;
import dev.wonkypigs.cosmiclifesteal.Commands.LifestealCommand;
import dev.wonkypigs.cosmiclifesteal.Helpers.DeathbanHelper;
import dev.wonkypigs.cosmiclifesteal.Listeners.HistoryMenuListener;
import dev.wonkypigs.cosmiclifesteal.Listeners.PlayerDeathListener;
import dev.wonkypigs.cosmiclifesteal.Listeners.PlayerJoinListener;
import dev.wonkypigs.cosmiclifesteal.Placeholders.PlaceholderManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public final class CosmicLifesteal extends JavaPlugin {

    private static CosmicLifesteal instance;{ instance = this; }
    private Connection connection;
    public double confVersion = 1.3;
    public String host, database, username, password, prefix;
    public String noPermMessage, bePlayerMessage, invalidPlayerMessage, invalidArgumentsMessage, messageOnDeath, messageOnKill, lifestealForHelp, deathbanForHelp;
    public int port;
    public HashMap<UUID, Integer> deathbanAmounts = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic

        // check for PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("------------------------------------");
            getLogger().warning("PlaceholderAPI not found! Disabling plugin...");
            getLogger().warning("------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // register placeholders
        new PlaceholderManager(this).register();

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // if config version is old, update it to current version
        updateConfig();

        registerCommands();
        registerListeners();
        registerPermissions();
        mySqlSetup();
        getConfigValues();
        setupMessages();
        cacheLoop();

        // MISC
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
        getCommand("lifesteal").setTabCompleter(new LifestealCommand());

        getServer().getPluginCommand("deathban").setExecutor(new DeathbanCommand());
        getCommand("deathban").setTabCompleter(new DeathbanCommand());
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
            host = YamlConfiguration.loadConfiguration(file).getString("database-host");
            port = YamlConfiguration.loadConfiguration(file).getInt("database-port");
            database = YamlConfiguration.loadConfiguration(file).getString("database-database");
            username = YamlConfiguration.loadConfiguration(file).getString("database-username");
            password = YamlConfiguration.loadConfiguration(file).getString("database-password");
            prefix = YamlConfiguration.loadConfiguration(file).getString("prefix").replace("&", "§");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateConfig() {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        if (config.getDouble("config-version") <= 1.2) {
            // rename config.yml to old-config.yml
            File oldConfig = new File(getDataFolder(), "old-config.yml");
            File configFile = new File(getDataFolder(), "config.yml");
            configFile.renameTo(oldConfig);

            // create new config.yml
            saveDefaultConfig();
            getConfig().set("config-version", confVersion);
            getLogger().severe("==========================");
            getLogger().info("You were using an old format of");
            getLogger().info("the config.yml file. It has been");
            getLogger().info("updated to the current version.");
            getLogger().info("Make sure to update all values!");
            getLogger().severe("==========================");
            return;
        }

        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        if (config.getDouble("config-version") != confVersion) {
            try {
                new ConfigUpdater(this, "config.yml", "config-updater.yml").update();
            } catch (IOException e) {
                getLogger().severe("Could not update config.yml!");
                e.printStackTrace();
            }
        }
        reloadConfig();
    }

    public void setupMessages() {
        // Setting up all plugin messages
        noPermMessage = getConfig().getString("no-permission-message")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        bePlayerMessage = getConfig().getString("must-be-player-message")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        invalidPlayerMessage = getConfig().getString("invalid-player-message")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        invalidArgumentsMessage = getConfig().getString("invalid-arguments-message")
                .replace("{prefix}", prefix)
                .replace("&", "§");
        messageOnDeath = getConfig().getString("death-message")
                .replace("{prefix}", prefix)
                .replace("{hearts}", String.valueOf(getConfig().getInt("hearts-lost-on-death")))
                .replace("&", "§");
        messageOnKill = getConfig().getString("kill-message")
                .replace("{prefix}", prefix)
                .replace("{hearts}", String.valueOf(getConfig().getInt("hearts-gained-on-kill")))
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

                if (!getConfig().getString("database-type").equalsIgnoreCase("mysql")) {
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
            getLogger().warning("Error connecting to the MySQL database");
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

    // every 5 minutes
    public void cacheLoop() {
        // make loop that repeats every 5 mins
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            // loop through all online players
            for (Player p: Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                int deathbans = DeathbanHelper.getDeathbanAmount(p);

                if (deathbanAmounts.containsKey(uuid)) {
                    deathbanAmounts.replace(uuid, deathbans);
                } else {
                    deathbanAmounts.put(uuid, deathbans);
                }
            }
        }, 0L, 20 * 60 * 2);
    }

}
