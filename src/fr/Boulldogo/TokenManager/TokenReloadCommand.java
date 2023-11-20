package fr.Boulldogo.TokenManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class TokenReloadCommand implements CommandExecutor {

    private Main plugin;

    public TokenReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Usage: /treload");
            return true;
        }

        Player executor = (Player) sender;

        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String noPerms = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission"));
        boolean AdminCommand = plugin.getConfig().getBoolean("console-admin-commands");
        if (!(sender instanceof Player) && !AdminCommand) {
            plugin.getLogger().warning("Only online players can execute that command. For enable console commands, please set 'console-admin-command' to true in your configuration.");
            return true;
        }
        
        if (!executor.hasPermission("tokenmanager.reload")) {
            sender.sendMessage(prefix + noPerms);
            return true;
        }

        
        plugin.reloadConfig();
        String version = plugin.getConfig().getString("version");
        String reloadMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reload-message").replace("%version%", version));
        sender.sendMessage(prefix + reloadMessage);
        plugin.getLogger().info("Le plugin TokenManager version" + version + "a été reload avec succès !");
        return true;
    }
}
