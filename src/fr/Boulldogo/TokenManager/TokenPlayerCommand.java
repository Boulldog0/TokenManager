package fr.Boulldogo.TokenManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TokenPlayerCommand implements CommandExecutor {

    private DatabaseManager databaseManager;
    private Main plugin;

    public TokenPlayerCommand(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String noPerms = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission"));
        String invalidAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-amount"));

        if (!(sender instanceof Player)) {
            plugin.getLogger().warning("Only online player can execute that command !");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("token")) {
            if (args.length == 0) {
                if (!player.hasPermission("tokenmanager.viewtoken")) {
                    player.sendMessage(prefix + noPerms);
                    return true;
                }
                displayBalance(player);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("pay")) {
                if (!player.hasPermission("tokenmanager.pay")) {
                    player.sendMessage(prefix + noPerms);
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + invalidAmount);
                    return true;
                }

                if (plugin.getConfig().getBoolean("use-database", true)) {
                    payWithDatabase(player, args[1], amount);
                } else {
                    payWithFile(player, args[1], amount);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /token pay <player> <amount>");
            }
        }
        return true;
    }
    
    private boolean logPayment(Player sender, String receiverName, int amount) {
    	
    	boolean paymentLogs = plugin.getConfig().getBoolean("logging-system.enable-pay-log");
    	if (!paymentLogs) {
    		return true;
    	}
    	
        boolean UseLogsFile = plugin.getConfig().getBoolean("logging-system.use-logs");
        if (!UseLogsFile) {
        	return true;
        }
        
        File logFile = new File(plugin.getDataFolder(), "logs.yml");
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM][HH:mm:ss");
        String formattedDate = dateFormat.format(new Date());

        String name = plugin.getConfig().getString("token-name");
        String logMessage = String.format("[%s] %s pay %s ( %d %s )",
                formattedDate,
                sender.getName(),
                receiverName,
                amount,
                name
        );

        logConfig.set(logMessage, true);

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
		return true;
    }

    private void displayBalance(Player player) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        int balance;
        if (plugin.getConfig().getBoolean("use-database", true)) {
            balance = databaseManager.getPlayerMoney(player.getUniqueId().toString());
        } else {
            balance = readFromFile(player.getName());
        }
        String balanceMsg = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.balance").replace("%balance%", String.valueOf(balance)));
        player.sendMessage(prefix + balanceMsg);
    }

    private void payWithDatabase(Player sender, String receiverName, int amount) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String notEnoughMoney = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-enough-money"));
        int senderBalance = databaseManager.getPlayerMoney(sender.getUniqueId().toString());

        if (senderBalance < amount) {
            sender.sendMessage(prefix + notEnoughMoney);
            return;
        }
        
        if (amount <= 0) {
        	String invalidAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-amount"));
        	sender.sendMessage(prefix + invalidAmount);
        	return;
        }
        

        Player receiver = plugin.getServer().getPlayer(receiverName);
        
        if (sender.equals(receiver)) {
        	String canPayYourself = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.can-pay-yourself"));
        	sender.sendMessage(prefix + canPayYourself );
        	return;
        }

        if (receiver == null || !receiver.isOnline()) {
            String playerNotOnline = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-not-online").replace("%receiver%", receiverName));
            sender.sendMessage(prefix + playerNotOnline);
            return;
        }

        databaseManager.setPlayerMoney(sender.getUniqueId().toString(), senderBalance - amount);
        databaseManager.setPlayerMoney(receiverName, databaseManager.getPlayerMoney(receiverName) + amount);
        logPayment(sender, receiverName, amount);
        String payementSuccessful = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.payement-successful").replace("%receiver%", receiverName).replace("%amount%", String.valueOf(amount)));
        sender.sendMessage(prefix + payementSuccessful);

        String recieveMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.recieve-payement").replace("%sender%", sender.getName()).replace("%amount%", String.valueOf(amount)));
        receiver.sendMessage(prefix + recieveMessage);
    }

    private void payWithFile(Player sender, String receiverName, int amount) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String notEnoughMoney = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-enough-money"));
        int senderBalance = readFromFile(sender.getName());

        if (senderBalance < amount) {
            sender.sendMessage(prefix + notEnoughMoney);
            return;
        }
        
        if (amount <= 0) {
        	String invalidAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-amount"));
        	sender.sendMessage(prefix + invalidAmount);
        	return;
        }

        Player receiver = plugin.getServer().getPlayer(receiverName);

        if (receiver == null || !receiver.isOnline()) {
            String playerNotOnline = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-not-online").replace("%receiver%", receiverName));
            sender.sendMessage(prefix + playerNotOnline);
            return;
        }

        writeToUserFile(sender.getName(), senderBalance - amount);
        int receiverBalance = readFromFile(receiverName);
        writeToUserFile(receiverName, receiverBalance + amount);

        String payementSuccessful = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.payement-successful").replace("%receiver%", receiverName).replace("%amount%", String.valueOf(amount)));
        sender.sendMessage(prefix + payementSuccessful);
        logPayment(sender, receiverName, amount);
        String recieveMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.recieve-payement").replace("%sender%", sender.getName()).replace("%amount%", String.valueOf(amount)));
        receiver.sendMessage(prefix + recieveMessage);
    }

    private int readFromFile(String playerName) {
        File userFile = new File(plugin.getDataFolder(), "userdata.yml");
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

        if (!userFile.exists()) {
            try {
                userFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return userConfig.getInt(playerName, 0);
    }

    private void writeToUserFile(String playerName, int amount) {
        File userFile = new File(plugin.getDataFolder(), "userdata.yml");
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

        userConfig.set(playerName, amount);

        try {
            userConfig.save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
