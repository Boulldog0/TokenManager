package fr.Boulldogo.TokenManager;


import java.io.File;

import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {

    private TokenManagerAPI tokenManagerAPI;
    private FileConfiguration config;

    @Override
    public void onEnable() {
    	
        saveDefaultConfig();
        config = getConfig();
        
        File userFile = new File(getDataFolder(), "userdata.yml");
        if (!userFile.exists()) {
            try {
                userFile.createNewFile();
                getLogger().info("Le fichier userdata.yml qui était introuvable a été créé avec succès !");
            } catch (IOException e) {
                e.printStackTrace();
                getLogger().severe("Erreur lors de la création du fichier userdata.yml !");
            }
        }
        
        boolean UseLogsFile = config.getBoolean("logging-system.use-logs");
        
        if (UseLogsFile) {
        File logsFile = new File(getDataFolder(), "logs.yml");
        if (!logsFile.exists()) {
            try {
                logsFile.createNewFile();
                getLogger().info("Le fichier logs.yml qui était introuvable a été créé avec succès !");
            } catch (IOException e) {
                e.printStackTrace();
                getLogger().severe("Erreur lors de la création du fichier logs.yml !");
            }
        }
      } else {
    	  getLogger().warning("Le fichier logs.yml n'a pas été créé car le systéme de logs est désactivé dans la configuration.");
          File logsFile = new File(getDataFolder(), "logs.yml");
          boolean deleteLogsFile = config.getBoolean("logging-system.delete-unuse-logsfile");
         if (deleteLogsFile) {
          if (!logsFile.exists()) {
              logsFile.delete();
			  getLogger().info("Le fichier logs.yml à été supprimé car les logs sont désactivés et la configuration est configuré pour supprimer le fichier logs innutilisé.");           
            }
         }       
      }

        String version = config.getString("version");
        boolean database = config.getBoolean("use-database");
        String port = config.getString("database.port");
        String host = config.getString("database.host");
        String defaultHost = "yourdatabaseip";
        
        
        String mode = database ? "database ( MySQL > " + host + ":" + port + " )" : "local-file ( TokenManager/userdata.yml )";
        
        Economy economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        
        DatabaseManager databaseManager = new DatabaseManager(this);
        databaseManager.connectToDatabase();
        
        VersionChecker versionChecker = new VersionChecker(this, version, "https://api.github.com/repos/Boulldog0/TokenManager/releases/latest");
        versionChecker.checkVersion();

        tokenManagerAPI = new TokenManagerAPI(this, databaseManager);
        new TokenPlaceholderExpansion(this, tokenManagerAPI).register();
        
        this.getCommand("token").setExecutor(new TokenPlayerCommand(this, databaseManager));
        this.getCommand("atoken").setExecutor(new AdminTokenCommand(this, databaseManager));
        this.getCommand("treload").setExecutor(new TokenReloadCommand(this));
        this.getCommand("ttoken").setExecutor(new TokenTradeCommand(this, databaseManager, economy));
        this.getServer().getPluginManager().registerEvents(new JoinListener(this, databaseManager), this);
        getLogger().info("Le plugin TokenManager" + version + " a été chargé avec succès !");
        getLogger().info("Mode de traitement des données de token : " + mode );
        databaseManager.setupDatabase();
       
        if (host.equals(defaultHost) && database) {
        	getLogger().warning("Vous n'avez pas correctement configuré votre base de donnée, le plugin ne pourra pas fonctionner correctement. Merci de configurer votre base de donnée puis de faire /treload., ou de changer le mode de donnée en définissant use-database sur false.");
        }
    }

    @Override
    public void onDisable() {

        getLogger().info("Le plugin TokenManager a été déchargé avec succès !");
    }

}
