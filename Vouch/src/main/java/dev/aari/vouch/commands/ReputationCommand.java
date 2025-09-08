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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ReputationCommand implements CommandExecutor {

    private final Vouch plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

    public ReputationCommand(Vouch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vouch.check")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /reputation <player>");
                return true;
            }
            target = (Player) sender;
        } else if (args.length == 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /reputation [player]");
            return true;
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' has never played on this server.");
            return true;
        }

        UUID targetId = target.getUniqueId();
        PlayerData data = plugin.getPlayerData(targetId);

        String playerName = target.getName() != null ? target.getName() : "Unknown";
        double stars = data.getReputationStars();
        String starsDisplay = generateStarsDisplay(stars);
        String trustLevel = data.getTrustLevel();
        String trustColor = getTrustColor(stars);

        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        sender.sendMessage(ChatColor.AQUA + "📊 Reputation Report for " + ChatColor.WHITE + playerName);
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.YELLOW + "⭐ Rating: " + ChatColor.WHITE + starsDisplay + " " + ChatColor.GRAY + "(" + stars + "/5.0)");
        sender.sendMessage(ChatColor.YELLOW + "🏆 Trust Level: " + trustColor + trustLevel);
        sender.sendMessage("");

        sender.sendMessage(ChatColor.YELLOW + "📈 Statistics:");
        sender.sendMessage(ChatColor.GREEN + "  ✓ Regular Vouches: " + ChatColor.WHITE + data.getVouchers().size());
        sender.sendMessage(ChatColor.GOLD + "  ⭐ Super Vouches: " + ChatColor.WHITE + data.getSuperVouchers().size());
        sender.sendMessage(ChatColor.RED + "  ✗ Devouches: " + ChatColor.WHITE + data.getDevouchers().size());
        sender.sendMessage(ChatColor.AQUA + "  📊 Net Reputation: " + getReputationColor(data.getNetReputation()) + data.getNetReputation());
        sender.sendMessage("");

        sender.sendMessage(ChatColor.YELLOW + "📅 Activity:");
        sender.sendMessage(ChatColor.GRAY + "  First Seen: " + ChatColor.WHITE + dateFormat.format(new Date(data.getFirstJoined())));
        sender.sendMessage(ChatColor.GRAY + "  Last Seen: " + ChatColor.WHITE + dateFormat.format(new Date(data.getLastSeen())));

        long daysSinceJoined = (System.currentTimeMillis() - data.getFirstJoined()) / (1000 * 60 * 60 * 24);
        sender.sendMessage(ChatColor.GRAY + "  Days on Server: " + ChatColor.WHITE + daysSinceJoined);
        sender.sendMessage("");

        String recommendation = getRecommendation(stars, data);
        sender.sendMessage(ChatColor.YELLOW + "💡 Recommendation: " + ChatColor.WHITE + recommendation);
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");

        return true;
    }

    private String generateStarsDisplay(double stars) {
        StringBuilder display = new StringBuilder();
        int fullStars = (int) stars;
        boolean hasHalfStar = (stars - fullStars) >= 0.5;

        for (int i = 0; i < fullStars; i++) {
            display.append(ChatColor.GOLD).append("★");
        }

        if (hasHalfStar) {
            display.append(ChatColor.YELLOW).append("☆");
            fullStars++;
        }

        for (int i = fullStars; i < 5; i++) {
            display.append(ChatColor.GRAY).append("☆");
        }

        return display.toString();
    }

    private String getTrustColor(double stars) {
        if (stars >= 4.5) return ChatColor.DARK_GREEN.toString();
        if (stars >= 3.5) return ChatColor.GREEN.toString();
        if (stars >= 2.5) return ChatColor.YELLOW.toString();
        if (stars >= 1.5) return ChatColor.GOLD.toString();
        if (stars >= 0.5) return ChatColor.RED.toString();
        return ChatColor.DARK_RED.toString();
    }

    private String getReputationColor(int netRep) {
        if (netRep > 0) return ChatColor.GREEN.toString();
        if (netRep == 0) return ChatColor.YELLOW.toString();
        return ChatColor.RED.toString();
    }

    private String getRecommendation(double stars, PlayerData data) {
        if (stars >= 4.5) return "Excellent reputation - highly recommended for trades and collaborations.";
        if (stars >= 3.5) return "Good reputation - generally safe to interact with.";
        if (stars >= 2.5) return "Average reputation - proceed with normal caution.";
        if (stars >= 1.5) return "Mixed reputation - consider the context before trusting.";
        if (stars >= 0.5) return "Poor reputation - exercise significant caution.";
        return "Very poor reputation - high risk interactions.";
    }
}