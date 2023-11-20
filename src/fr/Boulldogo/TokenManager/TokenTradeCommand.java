package fr.Boulldogo.TokenManager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenTradeCommand implements CommandExecutor {

    private DatabaseManager databaseManager;
    private Main plugin;
    private Economy economy;

    private Map<UUID, TradeRequest> tradeRequests;

    public TokenTradeCommand(Main plugin, DatabaseManager databaseManager, Economy economy) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economy = economy;
        this.tradeRequests = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String noPerms = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission"));
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning("Seuls les joueurs en ligne peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        boolean enableTradeSystem = plugin.getConfig().getBoolean("trade-system-enable");

        if (!enableTradeSystem) {
        	String tradeNoEnable = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.trade-system-disable"));
            sender.sendMessage(prefix + tradeNoEnable);
            return true;
        }

        if (!sender.hasPermission("tokenmanager.trade")) {
            sender.sendMessage(prefix + noPerms);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("trade")) {
            handleSendCommand(player, args[1], args[2], args[3]);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("accept")) {
            handleAcceptCommand(player);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("deny")) {
            handleDenyCommand(player);
        } else {
            player.sendMessage(ChatColor.RED + "Usage : /ttoken trade <player> <tokens> <money>");
        }

        return true;
    }
    
    private void logTrade(String sender, String receiver, int tokens, double money, String status, String cause) {
        File logFile = new File(plugin.getDataFolder(), "logs.yml");

        boolean UseTradeLogs = plugin.getConfig().getBoolean("logging-system.enable-trade-log");
        if (!UseTradeLogs) {
        	return;
        }
        
        boolean UseLogsFile = plugin.getConfig().getBoolean("logging-system.use-logs");
        if (!UseLogsFile) {
        	return;
        }
        
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(logFile, true)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy][HH:mm:ss");
            String formattedDate = dateFormat.format(new Date());

            String logMessage = String.format("[%s] \"%s\" trade with \"%s\" (\"%d\" tokens > \"%s\" money) (%s)",
                    formattedDate,
                    sender,
                    receiver,
                    tokens,
                    money,
                    status
            );

            if (cause != null && !cause.isEmpty()) {
                logMessage += " (" + cause + ")";
            }

            writer.write(logMessage + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSendCommand(Player sender, String targetPlayerName, String tokenAmountString, String moneyAmountString) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        String alreadyRequest = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.already-request"));

        if (tradeRequests.containsKey(sender.getUniqueId())) {
            sender.sendMessage(prefix + alreadyRequest);
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (sender.getName().equals(targetPlayerName)) {
        	String canTTradeYourself = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.can-trade-yourself"));
            sender.sendMessage(prefix + canTTradeYourself);
            return;
        }

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            String noBody = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-not-online").replace("%receiver%", targetPlayerName));
            sender.sendMessage(prefix + noBody);
            return;
        }

        int tokenAmount;
        double moneyAmount;

        try {
            tokenAmount = Integer.parseInt(tokenAmountString);
            moneyAmount = Double.parseDouble(moneyAmountString);
        } catch (NumberFormatException e) {
        	String invalidAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-amount"));
            sender.sendMessage(prefix + invalidAmount);
            return;
        }

        if (tokenAmount <= 0 || moneyAmount < 0) {
        	String invalidAmount = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-amount"));
            sender.sendMessage(prefix + invalidAmount);
            return;
        }

        int senderBalance = getPlayerBalance(sender);

        if (senderBalance < tokenAmount) {
        	String noEnoughToken = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-enough-money"));
            sender.sendMessage(prefix + noEnoughToken);
            return;
        }
        
        int time = plugin.getConfig().getInt("time-to-trade-is-valid");
        String timeString = String.valueOf(time);

        TradeRequest tradeRequest = new TradeRequest(sender, targetPlayer, tokenAmount, moneyAmount);
        tradeRequests.put(targetPlayer.getUniqueId(), tradeRequest);

        
        String tkAmountString = String.valueOf(tokenAmount);
        String moAmountString = String.valueOf(moneyAmount);
        String recieveProps = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.receive-trade-request").replace("%sender%", sender.getName()).replace("%token%", tkAmountString).replace("%money%", moAmountString).replace("%seconds%", timeString));
        targetPlayer.sendMessage(prefix + recieveProps);
        String sendProps = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.send-trade-request").replace("%receiver%", targetPlayer.getName()).replace("%token%", tkAmountString).replace("%money%", moAmountString).replace("%seconds%", timeString));
        sender.sendMessage(prefix + sendProps);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tradeRequests.containsKey(targetPlayer.getUniqueId())) {
                tradeRequests.remove(targetPlayer.getUniqueId());
                String expiratedTrade = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.trade-expirated"));
                sender.sendMessage(prefix + expiratedTrade);
                targetPlayer.sendMessage(prefix + expiratedTrade);
            }
        }, 20 * time);
    }

    private void handleAcceptCommand(Player receiver) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        UUID receiverUUID = receiver.getUniqueId();
        if (!tradeRequests.containsKey(receiverUUID)) {
        	String anyRequest = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.any-trade"));
            receiver.sendMessage(prefix + anyRequest);
            return;
        }

        TradeRequest tradeRequest = tradeRequests.remove(receiverUUID);
        UUID senderUUID = tradeRequest.getSenderUUID();
        Player sender = Bukkit.getPlayer(senderUUID);
        int tokenAmount = tradeRequest.getTokenAmount();
        double moneyAmount = tradeRequest.getMoneyAmount();

        if (!economy.has(receiver, moneyAmount)) {
        	String moneyString = String.valueOf(moneyAmount);
        	String noEnoughMoney = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.receiver-no-enought-money").replace("%money%", moneyString));
        	String receiverNoEnoughMoney = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.target-no-enough-money").replace("%target%", receiver.getName()));
            receiver.sendMessage(prefix + noEnoughMoney);
            sender.sendMessage(prefix + receiverNoEnoughMoney);
            logTrade(sender.getName(), receiver.getName(), tokenAmount, moneyAmount, "failed", "Not enough money");
            return;
        }

        int senderBalance = getPlayerBalance(sender);

        economy.withdrawPlayer(receiver, moneyAmount);
        economy.depositPlayer(sender, moneyAmount);
        setPlayerBalance(receiver, getPlayerBalance(receiver) + tokenAmount);
        setPlayerBalance(sender, senderBalance - tokenAmount);
    	String moneyString = String.valueOf(moneyAmount);
    	String tokenString = String.valueOf(tokenAmount);

        String acceptedTradeReceiver = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.receiver-trade-accpeted").replace("%token%", tokenString).replace("%money%", moneyString).replace("%sender%", sender.getName()));
        String acceptedTradeSender = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.sender-trade-accpeted").replace("%token%", tokenString).replace("%money%", moneyString).replace("%receiver%", receiver.getName()));
        receiver.sendMessage(prefix + acceptedTradeReceiver);
        sender.sendMessage(prefix + acceptedTradeSender);
        FireworkManager.spawnTricolorFirework(sender);
        FireworkManager.spawnTricolorFirework(receiver);
        logTrade(sender.getName(), receiver.getName(), tokenAmount, moneyAmount, "completed", null);
        Bukkit.getScheduler().cancelTasks(plugin);
    }


    private void handleDenyCommand(Player receiver) {
        boolean usePrefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
        UUID receiverUUID = receiver.getUniqueId();
        if (!tradeRequests.containsKey(receiverUUID)) {
        	String anyRequest = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.any-trade"));
            receiver.sendMessage(prefix + anyRequest);
            return;
        }

        TradeRequest tradeRequest = tradeRequests.remove(receiverUUID);
        UUID senderUUID = tradeRequest.getSenderUUID();
        Player sender = Bukkit.getPlayer(senderUUID);
        
        if (sender != null) {
        	String senderDenyMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.sender-deny-message"));
        	String receiverDenyMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.receiver-deny-message"));
            sender.sendMessage(prefix + senderDenyMessage);
            receiver.sendMessage(prefix + receiverDenyMessage);
            logTrade(sender.getName(), receiver.getName(), 0, 0, "denied", null);
        }

        Bukkit.getScheduler().cancelTasks(plugin);
    }


    private int getPlayerBalance(Player player) {
        if (plugin.getConfig().getBoolean("use-database", true)) {
            if (databaseManager != null) {
                return databaseManager.getPlayerMoney(player.getUniqueId().toString());
            } else {
                return 0;
            }
        } else {
            return readFromFile(player.getName());
        }
    }


    private void setPlayerBalance(Player player, int amount) {
        if (plugin.getConfig().getBoolean("use-database", true)) {
            databaseManager.setPlayerMoney(player.getUniqueId().toString(), amount);
        } else {
            writeToUserFile(player.getName(), amount);
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
    
    private static class TradeRequest {
        private final UUID senderUUID;
        private final UUID receiverUUID;
        private final int tokenAmount;
        private final double moneyAmount;

        public TradeRequest(Player sender, Player receiver, int tokenAmount, double moneyAmount) {
            this.senderUUID = sender.getUniqueId();
            this.receiverUUID = receiver.getUniqueId();
            this.tokenAmount = tokenAmount;
            this.moneyAmount = moneyAmount;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        @SuppressWarnings("unused")
		public UUID getReceiverUUID() {
            return receiverUUID;
        }

        public int getTokenAmount() {
            return tokenAmount;
        }

        public double getMoneyAmount() {
            return moneyAmount;
        }
    }

}
