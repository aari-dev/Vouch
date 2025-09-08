package dev.aari.vouch.commands;

import dev.aari.vouch.Vouch;
import dev.aari.vouch.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DeVouchCommand implements CommandExecutor {

    private final Vouch plugin;

    public DeVouchCommand(Vouch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("vouch.remove")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /devouch <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID targetId = target.getUniqueId();
        UUID playerId = player.getUniqueId();

        if (targetId.equals(playerId)) {
            player.sendMessage(ChatColor.RED + "You cannot devouch yourself.");
            return true;
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' has never played on this server.");
            return true;
        }

        PlayerData playerData = plugin.getPlayerData(playerId);
        if (!playerData.canVouchTarget(targetId)) {
            long timeLeft = playerData.getTimeUntilCanVouch(targetId);
            long hoursLeft = timeLeft / (1000 * 60 * 60);
            long minutesLeft = (timeLeft % (1000 * 60 * 60)) / (1000 * 60);

            player.sendMessage(ChatColor.RED + "You can only change your vouch for " + target.getName() +
                    " once per day. Time remaining: " + hoursLeft + "h " + minutesLeft + "m");
            return true;
        }

        PlayerData targetData = plugin.getPlayerData(targetId);

        if (targetData.getDevouchers().contains(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "You have already devouched " + target.getName() + ".");
            return true;
        }

        String previousAction = "";
        if (targetData.getVouchers().contains(playerId)) {
            previousAction = "Your vouch has been removed and replaced with a devouch. ";
        } else if (targetData.getSuperVouchers().contains(playerId)) {
            previousAction = "Your super vouch has been removed and replaced with a devouch. ";
        }

        targetData.addDevoucher(playerId);
        playerData.recordVouchTime(targetId);

        plugin.getDataManager().savePlayerData(targetId);
        plugin.getDataManager().savePlayerData(playerId);

        player.sendMessage(ChatColor.RED + previousAction + "You have devouched " + target.getName() + ".");

        Player targetPlayer = target.getPlayer();
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.RED + player.getName() + " has devouched you.");
        }

        return true;
    }
}