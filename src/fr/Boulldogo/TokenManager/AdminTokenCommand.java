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

public class AdminTokenCommand implements CommandExecutor {

    private DatabaseManager databaseManager;
    private Main plugin;

    public AdminTokenCommand(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String noPerms = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission"));
        boolean AdminCommand = plugin.getConfig().getBoolean("console-admin-commands");
        if (!(sender instanceof Player) && !AdminCommand) {
            plugin.getLogger().warning("Only online players can execute that command. For enable console commands, please set 'console-admin-command' to true in your configuration.");
            return true;
        }

        Player executor = (Player) sender;

        if (!executor.hasPermission("tokenmanager.admintoken")) {
            sender.sendMessage(prefix + noPerms);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /atoken <give|withdraw|set|view|reset> <player> [amount]");
            return true;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(playerName);

        if (targetPlayer == null ) {
        	String target = args[2];
        	String noBody = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("message.unknow-player").replace("%name%", target));
            sender.sendMessage(prefix + noBody);
            return true;
        }

        int amount = 0;

        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
            	String validAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.valid-amount"));
                sender.sendMessage(prefix + validAmount);
                return true;
            }
        }

        switch (action) {
            case "give":
                giveTokens(executor, targetPlayer, amount);
                break;
            case "withdraw":
                withdrawTokens(executor, targetPlayer, amount);
                break;
            case "set":
                setTokens(executor, targetPlayer, amount);
                break;
            case "view":
                viewTokens(executor, targetPlayer);
                break;
            case "reset":
                resetTokens(executor, targetPlayer);
                break;
            default:
            	String unknowInteraction = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.unknow-interaction"));
                sender.sendMessage(prefix + unknowInteraction);
                break;
        }

        return true;
    }
    
    private boolean logAction(Player executor, String action, Player targetPlayer, int amount) {
    	
    	boolean AdminLogs = plugin.getConfig().getBoolean("logging-system.enable-admin-log");
    	if (!AdminLogs) {
    		return true;
    	}
    	
        boolean UseLogsFile = plugin.getConfig().getBoolean("logging-system.use-logs");
        if (!UseLogsFile) {
        	return true;
        }
        
        File logFile = new File(plugin.getDataFolder(), "logs.yml");
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY][HH:mm:ss");
        String formattedDate = dateFormat.format(new Date());

        String name = plugin.getConfig().getString("token-name");
        String logMessage = String.format("[%s] %s %s %s ( %d %s )",
                formattedDate,
                executor.getName(),
                action,
                targetPlayer.getName(),
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

    private void giveTokens(Player executor, Player targetPlayer, int amount) {
        int currentBalance;
        if (plugin.getConfig().getBoolean("use-database", true)) {
            currentBalance = databaseManager.getPlayerMoney(targetPlayer.getUniqueId().toString());
            databaseManager.setPlayerMoney(targetPlayer.getUniqueId().toString(), currentBalance + amount);
        } else {
            currentBalance = readFromFile(targetPlayer.getName());
            writeToUserFile(targetPlayer.getName(), currentBalance + amount);
        }
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        boolean AdminCommandsNotify = plugin.getConfig().getBoolean("notify-player-on-admin-commands");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String amountString = String.valueOf(amount);
        String giveMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.give-message").replace("%amount%", amountString).replace("%player%", targetPlayer.getName()));
        String receiveMessage = AdminCommandsNotify ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.give-message-receiver").replace("%amount%", amountString).replace("%giver%", executor.getName())) : "";
        executor.sendMessage(prefix + giveMessage);
        logAction(executor, "give", targetPlayer, amount);
        if (AdminCommandsNotify) {
        targetPlayer.sendMessage(prefix + receiveMessage);
        }
    }

    private void withdrawTokens(Player executor, Player targetPlayer, int amount) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String amountString = String.valueOf(amount);
        int currentBalance;
        if (plugin.getConfig().getBoolean("use-database", true)) {
            currentBalance = databaseManager.getPlayerMoney(targetPlayer.getUniqueId().toString());
            if (currentBalance < amount) {
            	String currentBalanceString = String.valueOf(currentBalance);
            	String insufficientToken = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.insufficient-token").replace("%amount%", amountString).replace("%player%", targetPlayer.getName()).replace("%current%", currentBalanceString));
                executor.sendMessage(prefix + insufficientToken);
                return;
            }
            databaseManager.setPlayerMoney(targetPlayer.getUniqueId().toString(), currentBalance - amount);
        } else {
            currentBalance = readFromFile(targetPlayer.getName());
            if (currentBalance < amount) {
            	String currentBalanceString = String.valueOf(currentBalance);
            	String insufficientToken = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.insufficient-token").replace("%amount%", amountString).replace("%player%", targetPlayer.getName()).replace("%current%", currentBalanceString));
                executor.sendMessage(prefix + insufficientToken);
                return;
            }
            writeToUserFile(targetPlayer.getName(), currentBalance - amount);
        }
        boolean AdminCommandsNotify = plugin.getConfig().getBoolean("notify-player-on-admin-commands");
        String withdrawMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.withdraw-message").replace("%player%", targetPlayer.getName()).replace("%amount%", amountString));
        String receiverWithdrawMessage = AdminCommandsNotify ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.withdraw-message-receiver").replace("%withdrawer%", executor.getName()).replace("%amount%", amountString)) : "";
        executor.sendMessage(prefix + withdrawMessage);
        logAction(executor, "withdraw", targetPlayer, amount);
        if (AdminCommandsNotify) {
        targetPlayer.sendMessage(prefix + receiverWithdrawMessage);
        }
    }

    private void setTokens(Player executor, Player targetPlayer, int amount) {
        if (plugin.getConfig().getBoolean("use-database", true)) {
            databaseManager.setPlayerMoney(targetPlayer.getUniqueId().toString(), amount);
        } else {
            writeToUserFile(targetPlayer.getName(), amount);
        }
        boolean AdminCommandsNotify = plugin.getConfig().getBoolean("notify-player-on-admin-commands");
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String amountString = String.valueOf(amount);
        String setTokenMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.set-message").replace("%amount%", amountString).replace("%player%", targetPlayer.getName()));
        String receiverSetTokenMessage = AdminCommandsNotify ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.set-message-receiver").replace("%amount%", amountString).replace("%setter%", executor.getName())) : "";
        executor.sendMessage(prefix + setTokenMessage);
        logAction(executor, "set", targetPlayer, amount);
        if (AdminCommandsNotify) {
        targetPlayer.sendMessage(prefix + receiverSetTokenMessage);
        }
    }

    private void viewTokens(Player executor, Player targetPlayer) {
        int balance;
        if (plugin.getConfig().getBoolean("use-database", true)) {
            balance = databaseManager.getPlayerMoney(targetPlayer.getUniqueId().toString());
        } else {
            balance = readFromFile(targetPlayer.getName());
            
            
        }
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String balanceString = String.valueOf(balance);
        String viewMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.view-player-token").replace("%amount%", balanceString).replace("%player%", targetPlayer.getName()));
        executor.sendMessage(prefix + viewMessage);
        logAction(executor, "view", targetPlayer, 0);
    }

    private void resetTokens(Player executor, Player targetPlayer) {
        int initialTokens = plugin.getConfig().getInt("initial-tokens", 0);

        if (plugin.getConfig().getBoolean("use-database", true)) {
            databaseManager.setPlayerMoney(targetPlayer.getUniqueId().toString(), initialTokens);
        } else {
            writeToUserFile(targetPlayer.getName(), initialTokens);
            
        }
        boolean AdminCommandsNotify = plugin.getConfig().getBoolean("notify-player-on-admin-commands");
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String resetMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reset-token-message").replace("%player%", targetPlayer.getName()));
        String resetMessageReceiver = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reset-token-message-receiver").replace("%resetter%", executor.getName()));
        executor.sendMessage(prefix + resetMessage);
        logAction(executor, "reset", targetPlayer, 0);
        if (AdminCommandsNotify) {
        	targetPlayer.sendMessage(prefix + resetMessageReceiver);
        }
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
