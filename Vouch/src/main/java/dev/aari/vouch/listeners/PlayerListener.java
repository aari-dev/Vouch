package dev.aari.vouch.listeners;

import dev.aari.vouch.Vouch;
import dev.aari.vouch.data.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Vouch plugin;

    public PlayerListener(Vouch plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer().getUniqueId());

        if (data.getFirstJoined() == 0) {
            data.setFirstJoined(System.currentTimeMillis());
        }

        data.setLastSeen(System.currentTimeMillis());
        plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerData data = plugin.getPlayerData(event.getPlayer().getUniqueId());
        data.setLastSeen(System.currentTimeMillis());
        plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
    }
}