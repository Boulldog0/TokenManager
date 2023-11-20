package fr.Boulldogo.TokenManager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;

public class JoinListener implements Listener {

    private final DatabaseManager databaseManager;
    private final Main plugin;

    public JoinListener(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean useprefix = plugin.getConfig().getBoolean("use-prefix");
        String prefix = useprefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";

        if (plugin.getConfig().getBoolean("use-database", true)) {
            if (!doesPlayerExist(player.getUniqueId().toString())) {
                int initialTokens = plugin.getConfig().getInt("initial-tokens");
                databaseManager.createPlayer(player.getUniqueId().toString(), initialTokens);
                String initialTokensString = String.valueOf(initialTokens);
                String createPlayerMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.new-user-message").replace("%player%", player.getName()).replace("%amount%", initialTokensString));
                boolean useWelcome = plugin.getConfig().getBoolean("use-welcome-message");
            if (useWelcome) {
                player.sendMessage( prefix + createPlayerMessage );
               }
            }
        } else {
            int initialTokens = plugin.getConfig().getInt("initial-tokens");
            if (readFromFile(player.getName()) == 0) {
                String initialTokensString = String.valueOf(initialTokens);
                String createPlayerMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.new-user-message").replace("%player%", player.getName()).replace("%amount%", initialTokensString));
                player.sendMessage( prefix + createPlayerMessage );
                writeToUserFile(player.getName(), initialTokens);
            }
        }
    }

    private boolean doesPlayerExist(String uuid) {
        return databaseManager.doesPlayerExist(uuid);
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
