package fr.Boulldogo.TokenManager;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public class DatabaseManager implements Listener {

    private final Main plugin;
    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void connectToDatabase() {
        FileConfiguration config = plugin.getConfig();
        
        boolean useDatabase = config.getBoolean("use-database", true);
        if (!useDatabase) {
            plugin.getLogger().info("Connexion à la base de données désactivée dans la configuration.");
            return;
        }
        
        String host = config.getString("database.host");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password"); 
        String port = config.getString("database.port");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Connexion a la base de donnée reussi.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setupDatabase() {
        FileConfiguration config = plugin.getConfig();
        boolean useDatabase = config.getBoolean("use-database", true);
        if (!useDatabase) {
            return;
        }
        
        String tableName = config.getString("database.tableName", "player_data");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " (uuid VARCHAR(36) PRIMARY KEY, money INT DEFAULT 0)");

            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS money INT");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public String getTableName() {
        FileConfiguration config = plugin.getConfig();
        return config.getString("database.tableName", "player_data");
    }

    public int getPlayerMoney(String uuid) {
        if (connection == null) {
            plugin.getLogger().severe("Tentative d'accès à la base de données sans connexion.");
            return 0; 
        }

        int money = 0;

        try (PreparedStatement statement = connection.prepareStatement("SELECT money FROM " + getTableName() + " WHERE uuid = ?")) {
            statement.setString(1, uuid);
            statement.execute();

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                money = resultSet.getInt("money");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return money;
    }


    public void setPlayerMoney(String uuid, int money) {
        try (PreparedStatement statement = connection.prepareStatement("REPLACE INTO " + getTableName() + " (uuid, money) VALUES (?, ?)")) {
            statement.setString(1, uuid);
            statement.setInt(2, money);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean doesPlayerExist(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + getTableName() + " WHERE uuid = ?")) {
            statement.setString(1, uuid);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    public void createPlayer(String uuid, int initialTokens) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + getTableName() + " (uuid, money) VALUES (?, ?)")) {
            statement.setString(1, uuid);
            statement.setInt(2, initialTokens);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
