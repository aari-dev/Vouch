package dev.aari.vouch.data;

import dev.aari.vouch.Vouch;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataManager {

    private final Vouch plugin;
    private final File dataFolder;
    private final ScheduledExecutorService executor;
    private final ConcurrentHashMap<UUID, YamlConfiguration> loadedConfigs = new ConcurrentHashMap<>();

    public DataManager(Vouch plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.executor = Executors.newScheduledThreadPool(2);
    }

    public void initialize() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        executor.scheduleAtFixedRate(this::saveAllData, 5, 5, TimeUnit.MINUTES);
    }

    public PlayerData loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File playerFile = new File(dataFolder, playerId.toString() + ".yml");

                if (!playerFile.exists()) {
                    return new PlayerData(playerId);
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                loadedConfigs.put(playerId, config);

                PlayerData data = new PlayerData(
                        playerId,
                        config.getLong("first-joined", System.currentTimeMillis()),
                        config.getLong("last-seen", System.currentTimeMillis())
                );

                List<String> vouchers = config.getStringList("vouchers");
                for (String voucherId : vouchers) {
                    data.getVouchers().add(UUID.fromString(voucherId));
                }

                List<String> superVouchers = config.getStringList("super-vouchers");
                for (String voucherId : superVouchers) {
                    data.getSuperVouchers().add(UUID.fromString(voucherId));
                }

                List<String> devouchers = config.getStringList("devouchers");
                for (String devoucherId : devouchers) {
                    data.getDevouchers().add(UUID.fromString(devoucherId));
                }

                data.setModified(false);
                return data;

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load data for player " + playerId + ": " + e.getMessage());
                return new PlayerData(playerId);
            }
        }).join();
    }

    public void savePlayerData(UUID playerId) {
        PlayerData data = plugin.getPlayerDataCache().get(playerId);
        if (data == null || !data.isModified()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                File playerFile = new File(dataFolder, playerId.toString() + ".yml");
                YamlConfiguration config = loadedConfigs.computeIfAbsent(playerId,
                        k -> YamlConfiguration.loadConfiguration(playerFile));

                config.set("first-joined", data.getFirstJoined());
                config.set("last-seen", data.getLastSeen());

                config.set("vouchers", data.getVouchers().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()));

                config.set("super-vouchers", data.getSuperVouchers().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()));

                config.set("devouchers", data.getDevouchers().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()));

                if (!data.getLastVouchTimes().isEmpty()) {
                    ConfigurationSection vouchTimesSection = config.createSection("vouch-times");
                    data.getLastVouchTimes().forEach((targetId, vouchTime) ->
                            vouchTimesSection.set(targetId.toString(), vouchTime));
                }

                config.save(playerFile);
                data.setModified(false);

            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data for player " + playerId + ": " + e.getMessage());
            }
        }, executor);
    }

    public void saveAllData() {
        plugin.getPlayerDataCache().keySet().forEach(this::savePlayerData);
    }

    public void close() {
        saveAllData();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}