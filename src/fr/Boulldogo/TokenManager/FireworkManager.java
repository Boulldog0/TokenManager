package fr.Boulldogo.TokenManager;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkManager {

    public static void spawnTricolorFirework(Player player) {
        Location playerLocation = player.getLocation();

        Firework firework = (Firework) playerLocation.getWorld().spawnEntity(playerLocation, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        FireworkEffect.Builder effectBuilder = FireworkEffect.builder();

        effectBuilder.withColor(Color.BLUE);
        effectBuilder.withColor(Color.WHITE);
        effectBuilder.withColor(Color.RED);
        effectBuilder.with(FireworkEffect.Type.BALL);
        effectBuilder.trail(true);
        effectBuilder.flicker(true);

        fireworkMeta.addEffect(effectBuilder.build());
        fireworkMeta.setPower(1);
        firework.setFireworkMeta(fireworkMeta);
    }
}