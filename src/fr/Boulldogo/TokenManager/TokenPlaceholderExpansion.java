package fr.Boulldogo.TokenManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;

public class TokenPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final TokenManagerAPI tokenManagerAPI;

    public TokenPlaceholderExpansion(Main plugin, TokenManagerAPI tokenManagerAPI) {
        this.plugin = plugin;
        this.tokenManagerAPI = tokenManagerAPI;
    }

    @Override
    public @Nonnull String getIdentifier() {
        return "tokenmanager";
    }

    @Override
    public @Nonnull String getAuthor() {
        return "Boulldogo";
    }

    @Override
    public @Nonnull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @Nonnull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("tokens")) {
            int tokens = tokenManagerAPI.getPlayerTokens(player);
            return String.valueOf(tokens);
        }

        return null;
    }
}
