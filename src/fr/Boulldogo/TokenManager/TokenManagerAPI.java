package fr.Boulldogo.TokenManager;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class TokenManagerAPI {

    private final Main plugin;
    private DatabaseManager databaseManager;

    public TokenManagerAPI(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public int getPlayerTokens(Player player) {
        int balance;
        if (plugin.getConfig().getBoolean("use-database", true)) {
            balance = databaseManager.getPlayerMoney(player.getUniqueId().toString());
        } else {
            balance = readFromFile(player.getName());
        }
        return balance;
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
}
