package dev.aari.vouch;

import dev.aari.vouch.commands.*;
import dev.aari.vouch.data.DataManager;
import dev.aari.vouch.data.PlayerData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Vouch extends JavaPlugin {

    private static Vouch instance;
    private DataManager dataManager;
    private final ConcurrentHashMap<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.dataManager.initialize();

        registerCommands();

        getLogger().info("Vouch plugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
            dataManager.close();
        }
        getLogger().info("Vouch plugin has been disabled successfully!");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("vouch")).setExecutor(new VouchCommand(this));
        Objects.requireNonNull(getCommand("supervouch")).setExecutor(new SuperVouchCommand(this));
        Objects.requireNonNull(getCommand("devouch")).setExecutor(new DeVouchCommand(this));
        Objects.requireNonNull(getCommand("reputation")).setExecutor(new ReputationCommand(this));
    }

    public static Vouch getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConcurrentHashMap<UUID, PlayerData> getPlayerDataCache() {
        return playerDataCache;
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, k -> dataManager.loadPlayerData(k));
    }
}